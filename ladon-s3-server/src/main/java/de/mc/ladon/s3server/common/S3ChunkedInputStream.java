
package de.mc.ladon.s3server.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;


/**
 * AWS Chunked Inputstream
 */
public class S3ChunkedInputStream extends InputStream {
    private byte[] chunk;
    private int index;
    private int chunkSize;
    private String sig;
    protected volatile InputStream in;
    private BufferedReader reader;

    public S3ChunkedInputStream(InputStream is) {
        in = is;
    }

    @Override
    public int read() throws IOException {
        while (index == chunkSize) {
            String meta = readChunkHeader();
            if (meta.isEmpty()) return -1;
            String[] parts = meta.split(";");
            chunkSize = Integer.parseInt(parts[0], 16);
            sig = parts[1];
            chunk = new byte[chunkSize];
            index = 0;
            StreamUtils.readByteArray(in, chunk);
            if (chunkSize == 0) return -1;
            readChunkHeader();
        }
        return chunk[index++] & 0xFF;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int i;
        for (i = 0; i < len; ++i) {
            int r = read();
            if (r == -1) break;
            b[off + i] = (byte) r;
        }
        if (i == 0) return -1;
        return i;
    }

    public int available() throws IOException {
        return this.in.available();
    }

    public void close() throws IOException {
        this.in.close();
    }

    /**
     * The header is separated with a linebreak, so we can just read a line
     * https://docs.aws.amazon.com/AmazonS3/latest/API/sigv4-streaming.html#sigv4-chunked-body-definition
     */
    private String readChunkHeader() throws IOException {
        StringBuilder builder = new StringBuilder();
        while (true) {
            int r = in.read();
            if (r == '\r' && in.read() == '\n' || r == -1) {
                break;
            } else {
                builder.append((char) r);
            }
        }
        return builder.toString();
    }

}

