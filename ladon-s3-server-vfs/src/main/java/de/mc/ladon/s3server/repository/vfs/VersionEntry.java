/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.ladon.s3server.repository.vfs;

/**
 * A single object version stored on disk. The on-disk format is one line:
 *
 * <pre>
 * V1 + TAB + sha256Hex + TAB + md5Hex + TAB + createdMs + TAB + size + TAB + 0|1 + TAB + contentType
 * </pre>
 *
 * where field 6 is {@code 1} for a delete marker and {@code 0} for a normal
 * object; for delete markers the hash fields are empty. Malformed lines are
 * rejected with an {@link IllegalArgumentException}.
 */
public final class VersionEntry {

    public static final String FORMAT = "V1";

    private final String sha256;
    private final String md5;
    private final long createdMs;
    private final long size;
    private final boolean deleteMarker;
    private final String contentType;

    private VersionEntry(String sha256, String md5, long createdMs, long size,
                         boolean deleteMarker, String contentType) {
        this.sha256 = sha256;
        this.md5 = md5;
        this.createdMs = createdMs;
        this.size = size;
        this.deleteMarker = deleteMarker;
        this.contentType = contentType == null ? "" : contentType;
    }

    /**
     * Creates a delete marker version entry.
     *
     * @param createdMs the creation timestamp in epoch milliseconds
     * @return the entry
     */
    public static VersionEntry marker(long createdMs) {
        return new VersionEntry("", "", createdMs, 0L, true, "");
    }

    /**
     * Creates a normal object version entry.
     *
     * @param sha256        hex encoded SHA-256 of the blob
     * @param md5           hex encoded MD5 of the content
     * @param createdMs     the creation timestamp in epoch milliseconds
     * @param size          the content size in bytes
     * @param contentType   the content type (may be empty)
     * @return the entry
     */
    public static VersionEntry object(String sha256, String md5, long createdMs, long size, String contentType) {
        if (sha256 == null || sha256.trim().isEmpty()) {
            throw new IllegalArgumentException("sha256 is required for object versions");
        }
        return new VersionEntry(sha256, md5 == null ? "" : md5, createdMs, size, false, contentType);
    }

    public String getSha256() {
        return sha256;
    }

    public String getMd5() {
        return md5;
    }

    public long getCreatedMs() {
        return createdMs;
    }

    public long getSize() {
        return size;
    }

    public boolean isDeleteMarker() {
        return deleteMarker;
    }

    public String getContentType() {
        return contentType;
    }

    /**
     * Quoted ETag ({@code "md5-hex"}), or {@code null} for delete markers.
     *
     * @return the etag value
     */
    public String etag() {
        return deleteMarker ? null : "\"" + md5 + "\"";
    }

    @Override
    public String toString() {
        return FORMAT + "\t" + sha256 + "\t" + md5 + "\t" + createdMs + "\t" + size
                + "\t" + (deleteMarker ? "1" : "0") + "\t" + contentType;
    }

    /**
     * Parses a version entry line.
     *
     * @param line the line as stored in {@code versions/<versionId>}
     * @return the parsed entry
     * @throws IllegalArgumentException if the line is malformed
     */
    public static VersionEntry parse(String line) {
        String[] parts = line.split("\t", -1);
        if (parts.length != 7) {
            throw new IllegalArgumentException("malformed version entry: expected 7 tab separated fields");
        }
        if (!FORMAT.equals(parts[0])) {
            throw new IllegalArgumentException("unsupported version entry format: " + parts[0]);
        }
        long createdMs = Long.parseLong(parts[3]);
        long size = Long.parseLong(parts[4]);
        boolean marker = "1".equals(parts[5]);
        return new VersionEntry(parts[1], parts[2], createdMs, size, marker, parts[6]);
    }
}
