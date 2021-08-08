package de.mc.ladon.s3server.enc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;


/**
 * Does not encrypt anything
 */
public class PlaintextEncryptor implements FileEncryptor {

    @Override
    public OutputStream getEncryptedOutputStream(Path path) {
        try {
            return Files.newOutputStream(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public InputStream getDecryptedInputStream(Path path) {
        try {
            return Files.newInputStream(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
