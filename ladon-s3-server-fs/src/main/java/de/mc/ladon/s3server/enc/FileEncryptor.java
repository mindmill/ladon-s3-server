package de.mc.ladon.s3server.enc;


import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;

/**
 * AES stream encryption with methods to retrieve wrapped cipher streams to read and write encrypted files.
 *
 * @author Christian Breitsprecher on 21.02.20.
 */
public class FileEncryptor {

    public static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";
    public static final String ALGORITHM = "AES";
    private static final int KEYSIZE = 16;
    public final byte[] secret;

    public FileEncryptor(byte[] secret) {
        this.secret = get128BitKey(secret);
    }

    /**
     * Creates a {@link CipherOutputStream} targeting the given file. First writes the IV and then the file content.
     * All Exceptions are wrapped as {@link RuntimeException}
     *
     * @param path the file to store the encrypted content
     * @return a new {@link CipherOutputStream }. Remember to close resources.
     */
    public OutputStream getEncryptedOutputStream(Path path) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            OutputStream fileOut = Files.newOutputStream(path);
            byte[] ivBytes = generateRandom128bits();
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret, ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivSpec);
            fileOut.write(ivBytes);
            return new CipherOutputStream(fileOut, cipher);
        } catch (InvalidKeyException
                | IOException
                | InvalidAlgorithmParameterException
                | NoSuchAlgorithmException
                | NoSuchPaddingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates a {@link CipherInputStream} reading and decrypting the given file.
     * All Exceptions are wrapped as {@link RuntimeException}
     *
     * @param path the file to read from. Needs to be encrypted.
     * @return a new {@link CipherInputStream} . Remember to close resources.
     */
    public InputStream getDecryptedInputStream(Path path) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            InputStream fileIn = Files.newInputStream(path);
            byte[] ivBytes = new byte[KEYSIZE];
            fileIn.read(ivBytes);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret, ALGORITHM);
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivSpec);
            return new CipherInputStream(fileIn, cipher);
        } catch (IOException
                | InvalidKeyException
                | InvalidAlgorithmParameterException
                | NoSuchAlgorithmException
                | NoSuchPaddingException e) {
            throw new RuntimeException(e);
        }

    }

    private byte[] get128BitKey(byte[] key) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        return md.digest(key);
    }


    private byte[] generateRandom128bits() {
        SecureRandom sr = new SecureRandom();
        byte[] bytes = new byte[KEYSIZE];
        sr.nextBytes(bytes);
        return bytes;
    }
}
