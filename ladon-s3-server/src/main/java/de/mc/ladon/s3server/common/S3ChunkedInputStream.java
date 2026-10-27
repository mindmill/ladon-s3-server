/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.ladon.s3server.common;

import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Input stream for S3 chunked (streaming) uploads, see
 * <a href="https://docs.aws.amazon.com/AmazonS3/latest/developerguide/sigv4-streaming.html">
 * Transferring Payload in Multiple Chunks (Chunked Upload) (AWS Signature Version 4)</a>.
 *
 * <p>Each chunk on the wire has the form:</p>
 *
 * <pre>
 * string(IntHexBase(chunk-size)) + ";chunk-signature=" + signature + \r\n + chunk-data + \r\n
 * </pre>
 *
 * <p>This stream strips the framing and, if a
 * {@link ChunkSignatureVerifier} is provided, verifies the signed chunk chain. The last
 * chunk has a size of 0.</p>
 */
public class S3ChunkedInputStream extends FilterInputStream {

    /**
     * Hook to verify the (chained) chunk signatures.
     *
     * @throws IOException if a chunk signature is invalid
     */
    /**
     * Hook to verify the (chained) chunk signatures.
     */
    public interface ChunkSignatureVerifier {
        /**
         * @throws de.mc.ladon.s3server.exceptions.S3ServerException if a chunk signature is invalid
         */
        void verify(String previousSignature, int chunkSize, byte[] chunkData, String signature);
    }

    private static final int MAX_HEADER_LENGTH = 1024;

    private String prevSignature;
    private String seedSignature;
    private final ChunkSignatureVerifier verifier;

    private boolean finished = false;
    private byte[] chunk = new byte[0];
    private int chunkLength = 0;
    private int chunkIndex = 0;

    public S3ChunkedInputStream(InputStream in) {
        this(in, null, null);
    }

    /**
     * @param in               the raw request input stream
     * @param seedSignature    the signature of the request itself (first line of the chunk chain),
     *                         required when {@code verifier} is provided
     * @param verifier         optional chain signature verifier, may be null
     */
    public S3ChunkedInputStream(InputStream in, String seedSignature, ChunkSignatureVerifier verifier) {
        super(in);
        this.seedSignature = seedSignature;
        this.verifier = verifier;
    }

    @Override
    public int read() throws IOException {
        if (finished || (chunkIndex >= chunkLength && !loadNextChunk())) {
            return -1;
        }
        return chunk[chunkIndex++] & 0xFF;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (len == 0) {
            return 0;
        }
        if (finished || (chunkIndex >= chunkLength && !loadNextChunk())) {
            return -1;
        }
        int toCopy = Math.min(len, chunkLength - chunkIndex);
        System.arraycopy(chunk, chunkIndex, b, off, toCopy);
        chunkIndex += toCopy;
        return toCopy;
    }

    @Override
    public int available() throws IOException {
        if (finished) {
            return 0;
        }
        return Math.max(0, chunkLength - chunkIndex) + in.available();
    }

    /**
     * Returns true when a fresh chunk (with data) was loaded.
     */
    private boolean loadNextChunk() throws IOException {
        String headerLine = readHeaderLine();
        if (headerLine == null) {
            throw new EOFException("unexpected end of chunked S3 payload");
        }
        String[] header = parseHeader(headerLine);
        int size = parseChunkSize(header[0]);
        byte[] chunkData = readFully(size);

        if (verifier != null) {
            String previous = prevSignature != null ? prevSignature : seedSignature;
            if (previous == null) {
                throw new EOFException("chunk stream not initialized with a seed signature");
            }
            verifier.verify(previous, size, chunkData, header[1]);
        }
        prevSignature = header[1];

        readFrame();

        this.chunk = chunkData;
        this.chunkLength = size;
        this.chunkIndex = 0;

        if (size == 0) {
            finished = true;
            return false;
        }
        return true;
    }

    private static String[] parseHeader(String headerLine) throws IOException {
        int semi = headerLine.indexOf(';');
        if (semi <= 0) {
            throw new IOException("malformed chunk header: " + headerLine);
        }
        String sizeHex = headerLine.substring(0, semi).trim();
        if (sizeHex.isEmpty()) {
            throw new IOException("malformed chunk header: " + headerLine);
        }
        String signature = null;
        for (int from = semi + 1; from <= headerLine.length();) {
            int next = headerLine.indexOf(';', from);
            String extension = next < 0
                    ? headerLine.substring(from).trim()
                    : headerLine.substring(from, next).trim();
            from = next < 0 ? headerLine.length() + 1 : next + 1;
            if (extension.startsWith("chunk-signature=")) {
                if (signature != null) {
                    throw new IOException("multiple chunk-signature extensions in header: " + headerLine);
                }
                signature = extension.substring("chunk-signature=".length()).trim();
                if (signature.isEmpty()) {
                    throw new IOException("missing chunk signature in header: " + headerLine);
                }
            } else if (!extension.isEmpty()) {
                throw new IOException("unsupported chunk extension: " + extension);
            }
        }
        if (signature == null) {
            throw new IOException("chunk header without chunk-signature: " + headerLine);
        }
        return new String[]{sizeHex, signature};
    }

    /**
     * Parses the hexadecimal chunk size. The size is bounded by {@link Integer#MAX_VALUE};
     * anything else (non-hex digits, out of range) is rejected with an {@link IOException}
     * rather than leaking a runtime exception.
     */
    private static int parseChunkSize(String hex) throws IOException {
        if (hex.length() == 0 || hex.length() > 8) {
            throw new IOException("invalid chunk size: " + hex);
        }
        long size = 0;
        for (int i = 0; i < hex.length(); i++) {
            char c = hex.charAt(i);
            int digit;
            if (c >= '0' && c <= '9') {
                digit = c - '0';
            } else if (c >= 'a' && c <= 'f') {
                digit = c - 'a' + 10;
            } else if (c >= 'A' && c <= 'F') {
                digit = c - 'A' + 10;
            } else {
                throw new IOException("invalid chunk size: " + hex);
            }
            size = size * 16L + digit;
        }
        if (size > Integer.MAX_VALUE) {
            throw new IOException("chunk size too large: " + size);
        }
        return (int) size;
    }

    private void readFrame() throws IOException {
        int first = in.read();
        int second = in.read();
        if (first != '\r' || second != '\n') {
            throw new EOFException("missing chunk separator (expected CRLF)");
        }
    }

    /**
     * Reads the given number of bytes, throwing when the stream ended early.
     */
    private byte[] readFully(int size) throws IOException {
        if (size == 0) {
            return new byte[0];
        }
        byte[] data = new byte[size];
        int offset = 0;
        while (offset < size) {
            int read = in.read(data, offset, size - offset);
            if (read < 0) {
                throw new EOFException("unexpected end of chunk data (expected " + size + " bytes, got " + offset + ")");
            }
            offset += read;
        }
        return data;
    }

    /**
     * Reads until LF. Returns null when the stream ended before any content.
     */
    private String readHeaderLine() throws IOException {
        StringBuilder builder = new StringBuilder(64);
        int r;
        boolean any = false;
        while ((r = in.read()) != -1) {
            if (any && r == '\n') {
                if (builder.length() > 0 && builder.charAt(builder.length() - 1) == '\r') {
                    builder.setLength(builder.length() - 1);
                }
                return builder.toString();
            }
            if (r != '\r') {
                builder.append((char) r);
            }
            any = true;
            if (builder.length() > MAX_HEADER_LENGTH) {
                throw new IOException("chunk header too long");
            }
        }
        return any ? builder.toString() : null;
    }
}
