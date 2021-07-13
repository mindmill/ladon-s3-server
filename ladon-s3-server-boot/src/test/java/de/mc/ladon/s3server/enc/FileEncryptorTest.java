package de.mc.ladon.s3server.enc;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class FileEncryptorTest {

    @Test
    void getEncryptedOutputStream() throws IOException {
        FileEncryptor enc = new FileEncryptor("test".getBytes());
        File testfile = org.assertj.core.util.Files.newTemporaryFile();
        System.out.println(testfile.getAbsolutePath());
        byte[] testContent = "this is a test".getBytes();
        try (OutputStream encOut = enc.getEncryptedOutputStream(testfile.toPath())) {
            encOut.write(testContent);
        }

        byte[] readBytes = Files.readAllBytes(testfile.toPath());

        try (InputStream decInputStream = enc.getDecryptedInputStream(testfile.toPath());) {
            byte[] bytes = new byte[testContent.length];
            decInputStream.read(bytes);
            System.out.println(new String(bytes));
            assertArrayEquals(testContent, bytes);
        }


    }
}
