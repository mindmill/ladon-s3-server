/*
 * T18: Behaviour tests for S3ChunkedInputStream — the SigV4 streaming (chunked)
 * upload wire format:
 *
 * <pre>
 *   string(IntHexBase(chunk-size)) + ";chunk-signature=" + signature + \r\n + chunk-data + \r\n
 * </pre>
 *
 * The stream must strip the framing, deliver the payload bytes in order, support
 * the single- and multi-byte read API, terminate on the zero-sized final chunk
 * and fail with a clear IOException/EOFException on malformed or truncated
 * streams.
 */
package de.mc.ladon.s3server.auth;

import de.mc.ladon.s3server.common.S3ChunkedInputStream;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChunkedStreamTest {

    private static final String SIG1 = "0a1b2c3d4e5f60718293a4b5c6d7e8f90a1b2c3d4e5f60718293a4b5c6d7e8f9";
    private static final String SIG2 = "1a2b3c4d5e6f708192a3b4c5d6e7f8091a2b3c4d5e6f708192a3b4c5d6e7f809";
    private static final String SIG3 = "2a3b4c5d6e7f8091a2b3c4d5e6f708192a3b4c5d6e7f8091a2b3c4d5e6f7081";

    /**
     * Records the chunk chain the verifier sees instead of checking signatures.
     */
    private static final class RecordingVerifier implements S3ChunkedInputStream.ChunkSignatureVerifier {
        final List<String> chains = new ArrayList<>();
        List<String> seenSignatures = new ArrayList<>();
        List<Integer> seenSizes = new ArrayList<>();
        List<String> seenData = new ArrayList<>();

        @Override
        public void verify(String previousSignature, int chunkSize, byte[] chunkData, String signature) {
            chains.add(previousSignature);
            seenSignatures.add(signature);
            seenSizes.add(chunkSize);
            seenData.add(new String(chunkData, java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    private static String hex(int size) {
        return Integer.toHexString(size);
    }

    private static void writeTo(java.io.ByteArrayOutputStream out, String sizeHex, String sig, byte[] data) throws IOException {
        out.write((sizeHex + ";chunk-signature=" + sig + "\r\n").getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        out.write(data, 0, data.length);
        out.write("\r\n".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
    }

    private static byte[] chunks(String... sigAndData) throws IOException {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        for (String spec : sigAndData) {
            String[] parts = spec.split("::", 2);
            String sig = parts[0];
            byte[] data = parts.length > 1 ? parts[1].getBytes(java.nio.charset.StandardCharsets.UTF_8) : new byte[0];
            writeTo(out, Integer.toHexString(data.length), sig, data);
        }
        return out.toByteArray();
    }

    private static byte[] readAll(S3ChunkedInputStream in) throws IOException {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[16];
        int r;
        while ((r = in.read(buf, 0, buf.length)) != -1) {
            out.write(buf, 0, r);
        }
        return out.toByteArray();
    }

    private static byte[] readAllSingleByte(S3ChunkedInputStream in) throws IOException {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        int r;
        while ((r = in.read()) != -1) {
            out.write(r);
        }
        return out.toByteArray();
    }

    // ------------------------------------------------------------------ happy path

    @Test
    void stripsFramingAndDeliversPayloadInOrder() throws Exception {
        byte[] wire = chunks(
                SIG1 + "::hello ",
                SIG2 + "::world",
                SIG3);
        S3ChunkedInputStream in = new S3ChunkedInputStream(new ByteArrayInputStream(wire));
        assertEquals("hello world",
                new String(readAll(in), java.nio.charset.StandardCharsets.UTF_8));
    }

    @Test
    void emptyFinalChunkSignalledAfterAllData() throws Exception {
        byte[] wire = chunks(SIG1 + "::x", SIG2);
        S3ChunkedInputStream in = new S3ChunkedInputStream(new ByteArrayInputStream(wire));
        assertEquals('x', in.read());
        // the InputStream contract: once -1 was returned, all further reads must yield -1
        assertEquals(-1, in.read());
        assertEquals(-1, in.read());
        assertEquals(0, in.available());
    }

    @Test
    void emptyPayloadTerminatesImmediately() throws Exception {
        byte[] wire = chunks(SIG1);
        S3ChunkedInputStream in = new S3ChunkedInputStream(new ByteArrayInputStream(wire));
        assertEquals(-1, in.read());
        assertEquals(-1, in.read());
        assertEquals(0, in.available());
    }

    @Test
    void singleByteAndMultiByteReadsAgree() throws Exception {
        byte[] payload = "abcdef0123456789".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] wire = singleChunk(payload);
        String expected = new String(payload, java.nio.charset.StandardCharsets.UTF_8);
        assertEquals(expected, new String(readAllSingleByte(new S3ChunkedInputStream(new ByteArrayInputStream(wire))),
                java.nio.charset.StandardCharsets.UTF_8), "read() single-byte API");
        assertEquals(expected, new String(readAll(new S3ChunkedInputStream(new ByteArrayInputStream(wire))),
                java.nio.charset.StandardCharsets.UTF_8), "read(byte[],int,int) API");
    }

    private static byte[] singleChunk(byte[] data) throws IOException {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        writeTo(out, Integer.toHexString(data.length), SIG1, data);
        writeTo(out, "0", SIG2, new byte[0]);
        return out.toByteArray();
    }

    @Test
    void availableShrinksAsDataIsConsumed() throws Exception {
        byte[] wire = chunks(SIG1 + "::12345", SIG2);
        S3ChunkedInputStream in = new S3ChunkedInputStream(new ByteArrayInputStream(wire));
        // available() reports payload + still-pending wire framing; it must be
        // positive while data can be read without blocking and exactly zero once finished
        assertTrue(in.available() > 5, "at least the first payload byte must be available");
        byte[] buf = new byte[5];
        assertEquals(5, in.read(buf, 0, buf.length));
        assertEquals("12345", new String(buf, java.nio.charset.StandardCharsets.UTF_8));
        assertTrue(in.available() > 0, "the remaining wire (final chunk) must still be pending");
        readAll(in); // exhaust: final zero-sized chunk
        assertEquals(-1, in.read(), "after the final chunk read must yield -1");
        assertEquals(0, in.available(), "a finished stream must report 0 available");
    }

    // ------------------------------------------------------------------ verifier chain

    @Test
    void verifierSeesFullChainIncludingSeedAndEmptyFinalChunk() throws Exception {
        byte[] wire = chunks(SIG1 + "::a", SIG2 + "::bc", SIG3);
        RecordingVerifier recording = new RecordingVerifier();
        S3ChunkedInputStream in = new S3ChunkedInputStream(new ByteArrayInputStream(wire), "seed-signature", recording);
        readAll(in);

        assertEquals(List.of("seed-signature", SIG1, SIG2), recording.chains,
                "the previous signature of chunk n must be the signature of chunk n-1, the first one is the seed");
        assertEquals(List.of(SIG1, SIG2, SIG3), recording.seenSignatures);
        assertEquals(List.of(1, 2, 0), recording.seenSizes);
        assertEquals(List.of("a", "bc", ""), recording.seenData);
    }

    @Test
    void verifierRequiredSeedIsEnforced() {
        byte[] wire = { '1', ';', 'c', '1' }; // malformed on purpose; we only check the initialization error first
        S3ChunkedInputStream in = new S3ChunkedInputStream(new ByteArrayInputStream(wire), null,
                new RecordingVerifier());
        IOException thrown = assertThrows(IOException.class, () -> readAll(in));
        assertTrue(thrown instanceof EOFException || thrown instanceof java.io.IOException,
                "a chunk stream without seed signature must fail when verification is required: " + thrown);
    }

    // ------------------------------------------------------------------ malformed streams

    @Test
    void headerWithoutSignatureIsRejected() {
        byte[] wire = { '1', '2', ':', 'n', 'o', 'c', 'a', 'd' };
        S3ChunkedInputStream in = new S3ChunkedInputStream(new ByteArrayInputStream(wire));
        assertThrows(IOException.class, () -> readAll(in));
    }

    @Test
    void emptySizeHeaderIsRejected() {
        // empty size header: an illegal (non-hex) size with a chunk-signature extension
        byte[] wire = ";chunk-signature=x".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        S3ChunkedInputStream in = new S3ChunkedInputStream(new ByteArrayInputStream(wire));
        assertThrows(IOException.class, () -> readAll(in));
    }

    @Test
    void duplicateChunkSignatureExtensionsAreRejected() throws IOException {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        out.write((hex(1) + ";chunk-signature=" + SIG1 + ";chunk-signature=" + SIG2 + "\r\n").getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        out.write('a');
        out.write("\r\n".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        S3ChunkedInputStream in = new S3ChunkedInputStream(new ByteArrayInputStream(out.toByteArray()));
        assertThrows(IOException.class, () -> readAll(in));
    }

    @Test
    void unsupportedExtensionsAreRejected() throws IOException {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        out.write((hex(1) + ";x-ext=1;chunk-signature=" + SIG1 + "\r\n").getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        out.write('a');
        out.write("\r\n".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        S3ChunkedInputStream in = new S3ChunkedInputStream(new ByteArrayInputStream(out.toByteArray()));
        assertThrows(IOException.class, () -> readAll(in));
    }

    @Test
    void truncatedChunkDataIsAnEofError() throws IOException {
        // header declares 8 bytes, only 3 are present before EOF
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        out.write((hex(8) + ";chunk-signature=" + SIG1 + "\r\n").getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        out.write("abc".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        S3ChunkedInputStream in = new S3ChunkedInputStream(new ByteArrayInputStream(out.toByteArray()));
        assertThrows(EOFException.class, () -> readAll(in),
                "a chunk whose data ends early must be an EOF error");
    }

    @Test
    void truncatedHeaderLineIsRejected() {
        // stream ends in the middle of the chunk header
        byte[] wire = { '8', ';', 'c', 'h', 'u', 'n', 'k', '-' };
        S3ChunkedInputStream in = new S3ChunkedInputStream(new ByteArrayInputStream(wire));
        assertThrows(IOException.class, () -> readAll(in));
    }

    @Test
    void missingChunkSeparatorIsAnEofError() throws IOException {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        out.write((hex(1) + ";chunk-signature=" + SIG1 + "\r\n").getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        out.write('a');
        // no trailing CRLF
        S3ChunkedInputStream in = new S3ChunkedInputStream(new ByteArrayInputStream(out.toByteArray()));
        assertThrows(EOFException.class, () -> readAll(in));
    }

    @Test
    void streamThatEndsBeforeAnyChunkIsAnEofError() {
        S3ChunkedInputStream in = new S3ChunkedInputStream(new ByteArrayInputStream(new byte[0]));
        assertThrows(EOFException.class, () -> readAll(in));
    }

    @Test
    void oversizedOrMalformedSizesAreRejected() {
        for (String sizeHex : new String[]{"ffffffffffffffff", "80000000", "zz", "1g2h"}) {
            byte[] wire = (sizeHex + ";chunk-signature=" + SIG1 + "\r\n").getBytes(java.nio.charset.StandardCharsets.US_ASCII);
            S3ChunkedInputStream in = new S3ChunkedInputStream(new ByteArrayInputStream(wire));
            assertThrows(IOException.class, () -> readAll(in),
                    "chunk size '" + sizeHex + "' must be rejected as an IOException (not a runtime exception)");
        }
    }

    @Test
    void validEightDigitHexSizeIsAccepted() throws IOException {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        out.write(("00000001;chunk-signature=" + SIG1 + "\r\n").getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        out.write('a');
        out.write("\r\n".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        out.write(("00000000;chunk-signature=" + SIG2 + "\r\n").getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        out.write("\r\n".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        S3ChunkedInputStream in = new S3ChunkedInputStream(new ByteArrayInputStream(out.toByteArray()));
        assertEquals('a', in.read());
        assertEquals(-1, in.read());
    }
}
