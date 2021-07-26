package de.mc.ladon.s3server.common;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by ralfulrich on 01.03.16.
 */
public class StreamUtils {


    /**
     * Copy the contents of the given InputStream to the given OutputStream.
     * Leaves both streams open when done.
     *
     * @param in  the InputStream to copy from
     * @param out the OutputStream to copy to
     * @return the number of bytes copied
     * @throws IOException          in case of I/O errors
     * @throws InterruptedException if thread is interrupted
     */
    public static long copy(InputStream in, OutputStream out) throws IOException, InterruptedException {
        long byteCount = 0;
        byte[] buffer = new byte[4096];
        int bytesRead = -1;
        while ((bytesRead = in.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
            byteCount += bytesRead;
            Thread.sleep(0);
        }
        out.flush();
        return byteCount;
    }

    /**
     * Read the given byte[] from the stream. Leaves the stream open when done.
     *
     * @param in    {@link InputStream} to read from
     * @param bytes byte[] to put the read data into
     * @throws IOException in case of I/O errors
     */
    public static void readByteArray(InputStream in, byte[] bytes) throws IOException {
        int size = bytes.length;
        int index = 0;
        int read;
        while (index < size && (read = in.read()) != -1) {
            bytes[index++] = (byte) read;
        }
        if (index != size) throw new IOException("stream ended before reading full byte[]");
    }

}
