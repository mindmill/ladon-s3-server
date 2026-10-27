/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.ladon.s3server.repository.vfs;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * The on-disk layout of the {@link VFSRepository}. All content is stored
 * content-addressed (SHA-256) in a shared {@code data} area, which makes
 * deduplication automatic. Buckets keep per-object version metadata in
 * {@code buckets/<bucket>/objects/<keyhash>}:
 *
 * <pre>
 * <root>/
 *   data/<sha256hex>                                     content blobs (immutable, shared)
 *   locks/<bucket>/<keyhash>                             lock files (FileChannel.lock)
 *   buckets/<bucket>/objects/<keyhash>/key               the original object key
 *   buckets/<bucket>/objects/<keyhash>/head              id of the current (latest) version
 *   buckets/<bucket>/objects/<keyhash>/versions/&lt;id&gt;    one version entry per line format
 *   userdb.txt                                           user database (tab separated)
 * </pre>
 *
 * where <code>keyhash = hex(SHA-256(bucket + "\n" + key))</code>.
 */
public final class VFSLayout {

    public static final String DATA = "data";
    public static final String LOCKS = "locks";
    public static final String BUCKETS = "buckets";
    public static final String OBJECTS = "objects";
    public static final String KEY_FILE = "key";
    public static final String HEAD_FILE = "head";
    public static final String VERSIONS = "versions";
    public static final String USERDB_FILE = "userdb.txt";

    private final Path root;

    public VFSLayout(Path root) {
        if (root == null) {
            throw new IllegalArgumentException("root must not be null");
        }
        this.root = root.toAbsolutePath().normalize();
    }

    public Path root() {
        return root;
    }

    public Path dataDir() {
        return root.resolve(DATA);
    }

    public Path bucketsDir() {
        return root.resolve(BUCKETS);
    }

    public Path bucketDir(String bucket) {
        return bucketsDir().resolve(bucket);
    }

    public Path objectsDir(String bucket) {
        return bucketDir(bucket).resolve(OBJECTS);
    }

    public Path objectDir(String bucket, String keyHash) {
        return objectsDir(bucket).resolve(keyHash);
    }

    public Path keyFile(String bucket, String keyHash) {
        return objectDir(bucket, keyHash).resolve(KEY_FILE);
    }

    public Path headFile(String bucket, String keyHash) {
        return objectDir(bucket, keyHash).resolve(HEAD_FILE);
    }

    public Path versionsDir(String bucket, String keyHash) {
        return objectDir(bucket, keyHash).resolve(VERSIONS);
    }

    public Path versionFile(String bucket, String keyHash, String versionId) {
        return versionsDir(bucket, keyHash).resolve(versionId);
    }

    public Path lockFile(String bucket, String keyHash) {
        return root.resolve(LOCKS).resolve(bucket).resolve(keyHash);
    }

    public Path userdbFile() {
        return root.resolve(USERDB_FILE);
    }

    public Path blob(String sha256Hex) {
        return dataDir().resolve(sha256Hex);
    }

    /**
     * Derives the stable on-disk name for an object key.
     *
     * @param bucket the bucket name
     * @param key    the object key
     * @return {@code hex(SHA-256(bucket + "\n" + key))}
     */
    public static String keyHash(String bucket, String key) {
        return sha256Hex((bucket + "\n" + key).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Hex encodes the given bytes.
     *
     * @param bytes the bytes
     * @return the hex representation
     */
    public static String hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }

    /**
     * The SHA-256 digest of the given bytes, hex encoded.
     *
     * @param bytes the bytes
     * @return the hex encoded digest
     */
    public static String sha256Hex(byte[] bytes) {
        try {
            return hex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    /**
     * Reads the full content of a file as a string.
     *
     * @param path the file
     * @return the content
     * @throws IOException if the file could not be read
     */
    public static String readSingleLine(Path path) throws IOException {
        String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        int i = content.indexOf('\n');
        return i >= 0 ? content.substring(0, i) : content;
    }

    /**
     * Validates a version id supplied by a client (or stored in this repository).
     * The id must be a plain file name: no path separators, limited length.
     *
     * @param versionId the candidate
     * @return true if the id is safe to use as a file name
     */
    public static boolean isValidVersionId(String versionId) {
        if (versionId == null || versionId.length() < 3 || versionId.length() > 64) {
            return false;
        }
        for (int i = 0; i < versionId.length(); i++) {
            char c = versionId.charAt(i);
            if (!(c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9'
                    || c == '-' || c == '_')) {
                return false;
            }
        }
        return !versionId.contains("..");
    }
}
