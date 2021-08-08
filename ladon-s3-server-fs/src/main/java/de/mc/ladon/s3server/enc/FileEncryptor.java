package de.mc.ladon.s3server.enc;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;

public interface FileEncryptor {

    OutputStream getEncryptedOutputStream(Path path);

    InputStream getDecryptedInputStream(Path path);
}
