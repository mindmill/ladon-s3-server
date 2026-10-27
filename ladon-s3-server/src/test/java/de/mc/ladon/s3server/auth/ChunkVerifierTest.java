/*
 * T16: SigV4ChunkVerifier — the chunk signature chain of a SigV4 streaming
 * (chunked) upload, cross-checked against the AWS sample implementation
 * {@link AWS4SignerForChunkedUpload} (originating from the AWS documentation):
 *
 * <ol>
 *     <li>The sample signer produces the wire bytes (chunk framing) and its
 *         internal signature chain is reproduced by the production verifier.</li>
 *     <li>A pure spec vector (independently computed) pins the chunk
 *         string-to-sign and HMAC result byte-for-byte.</li>
 *     <li>Tampered chunk data or chunk signatures are rejected with
 *         {@link de.mc.ladon.s3server.exceptions.InvalidRequestException}.</li>
 * </ol>
 */
package de.mc.ladon.s3server.auth;

import de.mc.ladon.s3server.common.S3ChunkedInputStream;
import de.mc.ladon.s3server.exceptions.InvalidRequestException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ChunkVerifierTest {

    private static final String SECRET = "chunk-verification-secret-key";
    private static final String ACCESS = "TESTACCESSKEY";
    private static final String REGION = "us-east-1";
    private static final String SERVICE = "s3";

    /**
     * Builds the wire payload with the AWS sample signer, splitting the payload
     * into the given chunk size, and returns the wire bytes plus the state a
     * conformant server needs to verify the chain.
     */
    private static WireAndState signWire(int chunkBytes, String payload) throws Exception {
        AWS4SignerForChunkedUpload signer =
                new AWS4SignerForChunkedUpload(new URL("https://example.amazonaws.com"), "PUT", SERVICE, REGION);

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Host", "example.amazonaws.com");
        headers.put("x-amz-content-sha256", AWS4SignerForChunkedUpload.STREAMING_BODY_SHA256);

        // the sample signer prints its canonical request / string-to-sign; keep the surefire output clean
        String authorization;
        PrintStream original = System.out;
        System.setOut(new PrintStream(new ByteArrayOutputStream(), true, "UTF-8"));
        try {
            authorization = signer.computeSignature(headers, new LinkedHashMap<>(),
                    AWS4SignerForChunkedUpload.STREAMING_BODY_SHA256, ACCESS, SECRET);
        } finally {
            System.setOut(original);
        }

        WireAndState state = new WireAndState();
        state.requestDate = headers.get("x-amz-date");
        state.scope = state.requestDate.substring(0, 8) + "/" + REGION + "/" + SERVICE + "/aws4_request";
        int seedStart = authorization.indexOf("Signature=") + "Signature=".length();
        state.seedSignature = authorization.substring(seedStart).trim().toLowerCase();

        byte[] data = payload.getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream wire = new ByteArrayOutputStream();
        int offset = 0;
        while (offset < data.length) {
            int len = Math.min(chunkBytes, data.length - offset);
            byte[] slice = new byte[len];
            System.arraycopy(data, offset, slice, 0, len);
            // the sample signer signs exactly the (sliced) user data passed in
            byte[] unsignedChunk = signer.constructSignedChunk(len, slice);
            wire.write(unsignedChunk, 0, unsignedChunk.length);
            offset += len;
        }
        // the final empty chunk terminates the stream
        byte[] finalChunk = signer.constructSignedChunk(0, new byte[0]);
        wire.write(finalChunk, 0, finalChunk.length);
        state.wire = wire.toByteArray();
        state.payload = data;
        return state;
    }

    private static final class WireAndState {
        String requestDate;
        String scope;
        String seedSignature;
        byte[] wire;
        byte[] payload;
    }

    private S3ChunkedInputStream streamingStream(WireAndState state) {
        byte[] signingKey = SigV4.signingKey(SECRET, state.requestDate.substring(0, 8), REGION, SERVICE);
        return new S3ChunkedInputStream(new ByteArrayInputStream(state.wire), state.seedSignature,
                new SigV4ChunkVerifier(state.requestDate, state.scope, signingKey, "/object", () -> "test-rid"));
    }

    @Test
    void verifiesChainProducedByTheAwsSampleSigner() throws Exception {
        WireAndState state = signWire(7, "The quick brown fox jumps over the lazy dog. Pack my box with five dozen liquor jugs.");

        byte[] payload = streamingStream(state).readAllBytes();
        assertArrayEquals(state.payload, payload,
                "the payload reassembled from the sample-signed chunks must be byte-identical");
    }

    @Test
    void rejectsTamperedChunkData() throws Exception {
        WireAndState state = signWire(5, "abcdefghijklmnop");
        // flip a data byte of the first chunk (two bytes after its header line)
        int firstHeaderCrlf = indexOf(state.wire, "\r\n");
        state.wire[firstHeaderCrlf + 2 + 2] ^= 0x01;

        assertThrows(InvalidRequestException.class, () -> streamingStream(state).readAllBytes(),
                "tampering with the chunk data must be detected by the chain verification");
    }

    @Test
    void rejectsTamperedChunkSignature() throws Exception {
        WireAndState state = signWire(5, "abcdefghijklmnop");
        // corrupt the first hex digit of the first chunk signature
        int sigIndex = indexOf(state.wire, "chunk-signature=") + "chunk-signature=".length();
        state.wire[sigIndex] = (byte) (state.wire[sigIndex] == 'a' ? 'b' : 'a');

        assertThrows(InvalidRequestException.class, () -> streamingStream(state).readAllBytes(),
                "tampering with the chunk signature must be detected");
    }

    @Test
    void specVectorPinsTheChunkSignatureComputation() {
        // Independently computed reference values (Python stdlib):
        //   secret             = chunk-test-secret      date  = 20150830T123600Z
        //   scope              = 20150830/us-east-1/s3/aws4_request
        //   previous signature = 000...0 (64 zeros)     chunk  = "hello world"
        //   expected           = bae3345f7bbc6bcc4dfeb3ada4a76a45e439630832289e1e25aa14a4c1afb387
        byte[] signingKey = SigV4.signingKey("chunk-test-secret", "20150830", "us-east-1", "s3");
        SigV4ChunkVerifier verifier = new SigV4ChunkVerifier(
                "20150830T123600Z", "20150830/us-east-1/s3/aws4_request", signingKey, "/object", () -> "test-rid");

        byte[] data = "hello world".getBytes(StandardCharsets.UTF_8);
        String zeros = "0".repeat(64);

        // correct signature: accepted
        verifier.verify(zeros, data.length, data,
                "bae3345f7bbc6bcc4dfeb3ada4a76a45e439630832289e1e25aa14a4c1afb387");

        // one-bit different signature: rejected
        assertThrows(InvalidRequestException.class, () ->
                verifier.verify(zeros, data.length, data,
                        "bae3345f7bbc6bcc4dfeb3ada4a76a45e439630832289e1e25aa14a4c1afb386"));

        // different previous-signature chain value: rejected (chain binding)
        assertThrows(InvalidRequestException.class, () ->
                verifier.verify("f".repeat(64), data.length, data,
                        "bae3345f7bbc6bcc4dfeb3ada4a76a45e439630832289e1e25aa14a4c1afb387"));
    }

    private static int indexOf(byte[] wire, String token) {
        String text = new String(wire, 0, wire.length, StandardCharsets.US_ASCII);
        int at = text.indexOf(token);
        if (at < 0) {
            throw new AssertionError("token not found: " + token);
        }
        return at;
    }
}
