/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.ladon.s3server.repository.vfs;

import de.mc.ladon.s3server.auth.AwsSignatureVersion4;
import de.mc.ladon.s3server.auth.SigV4;
import de.mc.ladon.s3server.auth.SigV4ChunkVerifier;
import de.mc.ladon.s3server.common.DelimiterUtil;
import de.mc.ladon.s3server.common.Encoding;
import de.mc.ladon.s3server.common.S3ChunkedInputStream;
import de.mc.ladon.s3server.common.S3Constants;
import de.mc.ladon.s3server.common.StreamUtils;
import de.mc.ladon.s3server.entities.api.S3Bucket;
import de.mc.ladon.s3server.entities.api.S3CallContext;
import de.mc.ladon.s3server.entities.api.S3ListBucketResult;
import de.mc.ladon.s3server.entities.api.S3Object;
import de.mc.ladon.s3server.entities.api.S3User;
import de.mc.ladon.s3server.entities.impl.S3BucketImpl;
import de.mc.ladon.s3server.entities.impl.S3ListBucketResultImpl;
import de.mc.ladon.s3server.entities.impl.S3ObjectImpl;
import de.mc.ladon.s3server.entities.impl.S3ResponseHeaderImpl;
import de.mc.ladon.s3server.entities.impl.S3UserImpl;
import de.mc.ladon.s3server.exceptions.BucketNotEmptyException;
import de.mc.ladon.s3server.exceptions.InvalidAccessKeyIdException;
import de.mc.ladon.s3server.exceptions.InvalidBucketName;
import de.mc.ladon.s3server.exceptions.InvalidDigestException;
import de.mc.ladon.s3server.exceptions.InternalErrorException;
import de.mc.ladon.s3server.exceptions.NoSuchBucketException;
import de.mc.ladon.s3server.exceptions.NoSuchKeyException;
import de.mc.ladon.s3server.exceptions.NoSuchVersionException;
import de.mc.ladon.s3server.exceptions.S3ServerException;
import de.mc.ladon.s3server.repository.api.S3Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Virtual file system repository with <b>content addressing</b>, <b>object
 * versioning</b> and <b>duplicate elimination</b>:
 *
 * <ul>
 *     <li>content is stored once per unique payload (SHA-256) in
 *         {@code data/<sha256>}, so identical objects — across keys and
 *         buckets — share a single blob;</li>
 *     <li>every key keeps a full version history ({@code versions/<versionId>},
 *         one line per {@link VersionEntry}) plus a {@code head} pointer to the
 *         current version, which may also be a <em>delete marker</em>;</li>
 *     <li>mutating operations are coordinated across processes with
 *         {@code flock(2)} locks ({@link VFSLockManager}), released by the OS
 *         if a process dies;</li>
 *     <li>unreferenced blobs are removed by a mark-and-sweep collector
 *         ({@link VFSBlobGC#sweep()}), triggered automatically after
 *         permanent deletes and bucket deletion, or manually via {@link #gc()}.</li>
 * </ul>
 *
 * <p>S3 semantics implemented on top of this layout (see {@link S3Repository}):</p>
 * <ul>
 *     <li>{@code PUT Object} appends a version; {@code GET} returns the current
 *         version or the version named in {@code ?versionId=} (delete markers
 *         answer with {@code NoSuchKey}, unknown version ids with
 *         {@code NoSuchVersion});</li>
 *     <li>{@code DELETE} without a version id writes a delete marker; with
 *         {@code ?versionId=} the version (or marker) is permanently removed
 *         and the previous version becomes current again;</li>
 *     <li>{@code GET Bucket&versions} lists the full version history in S3
 *         order (keys ascending, newest version first) with pagination
 *         ({@code key-marker}/{@code version-id-marker}, {@code max-keys});</li>
 *     <li>{@code PUT Copy} shares the source blob (no data copy) and appends a
 *         new version to the destination key.</li>
 * </ul>
 */
public class VFSRepository implements S3Repository {

    private static final Logger logger = LoggerFactory.getLogger(VFSRepository.class);
    private static final String VERSION_ID_ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final Random VERSION_ID_RANDOM = new Random();
    private static final AtomicLong VERSION_ID_SEQ = new AtomicLong();

    private final VFSLayout layout;
    private final VFSLockManager lockManager;
    private final VFSBlobGC blobGC;
    private final VFSUserDb userDb;

    public VFSRepository(Path root) {
        this.layout = new VFSLayout(root);
        this.lockManager = new VFSLockManager(layout);
        this.blobGC = new VFSBlobGC(layout);
        this.userDb = new VFSUserDb(layout.userdbFile());
    }

    public VFSRepository(String root) {
        this(Paths.get(root));
    }

    /**
     * The repository root directory.
     *
     * @return the root
     */
    public Path root() {
        return layout.root();
    }

    /**
     * Adds (or replaces) a user in the repository database.
     *
     * @param userId    the user id
     * @param userName  the user name
     * @param accessKey the access key
     * @param secretKey the secret signing key
     */
    public void addUser(String userId, String userName, String accessKey, String secretKey) {
        userDb.addUser(userId, userName, accessKey, secretKey);
    }

    /**
     * Removes unreferenced content blobs. Also runs automatically after
     * permanent deletes and {@link #deleteBucket(S3CallContext, String)}.
     *
     * @return the number of removed blobs
     */
    public int gc() {
        return blobGC.sweep();
    }

    // ------------------------------------------------------------------ buckets

    @Override
    public List<S3Bucket> listAllBuckets(S3CallContext callContext) {
        List<S3Bucket> result = new ArrayList<>();
        Path bucketsDir = layout.bucketsDir();
        if (!Files.isDirectory(bucketsDir)) {
            return result;
        }
        for (String name : listSubDirs(bucketsDir)) {
            Date created = new Date();
            try {
                created = new Date(Files.getLastModifiedTime(layout.bucketDir(name)).toMillis());
            } catch (IOException e) {
                logger.warn("could not determine creation date of bucket {}", name, e);
            }
            result.add(new S3BucketImpl(name, created, new S3UserImpl()));
        }
        return result;
    }

    @Override
    public void createBucket(S3CallContext callContext, String bucketName, String locationConstraint) {
        validateBucketName(bucketName, callContext);
        try {
            Files.createDirectories(layout.bucketDir(bucketName));
        } catch (IOException e) {
            logger.error("could not create bucket {}", bucketName, e);
            throw new InternalErrorException(bucketName, callContext.getRequestId());
        }
    }

    @Override
    public void deleteBucket(S3CallContext callContext, String bucketName) {
        validateBucketName(bucketName, callContext);
        Path bucketDir = layout.bucketDir(bucketName);
        if (!Files.exists(bucketDir)) {
            throw new NoSuchBucketException(bucketName, callContext.getRequestId());
        }
        Path objectsDir = layout.objectsDir(bucketName);
        if (Files.isDirectory(objectsDir) && !isDirectoryEmpty(objectsDir)) {
            throw new BucketNotEmptyException(bucketName, callContext.getRequestId());
        }
        try {
            deleteRecursively(bucketDir);
        } catch (IOException e) {
            logger.error("could not delete bucket {}", bucketName, e);
            throw new InternalErrorException(bucketName, callContext.getRequestId());
        }
        blobGC.sweep();
    }

    @Override
    public void getBucket(S3CallContext callContext, String bucketName) {
        validateBucketName(bucketName, callContext);
        if (!Files.isDirectory(layout.bucketDir(bucketName))) {
            throw new NoSuchBucketException(bucketName, callContext.getRequestId());
        }
    }

    // ------------------------------------------------------------------ objects

    @Override
    public void createObject(S3CallContext callContext, String bucketName, String objectKey) {
        ensureBucketExists(bucketName, callContext);
        String keyHash = VFSLayout.keyHash(bucketName, objectKey);
        long createdMs = System.currentTimeMillis();

        VFSLockManager.LockHandle lock = lockManager.acquireExclusive(callContext, bucketName, keyHash);
        try {
            PutResult content;
            try {
                content = writeContent(callContext, objectKey);
            } catch (S3ServerException e) {
                throw e;
            } catch (IOException | NoSuchAlgorithmException | InterruptedException e) {
                logger.error("could not write object {} into bucket {}", objectKey, bucketName, e);
                throw new InternalErrorException(objectKey, callContext.getRequestId());
            }

            String versionId = newVersionId();
            Files.createDirectories(layout.versionsDir(bucketName, keyHash));
            Files.write(layout.keyFile(bucketName, keyHash), objectKey.getBytes(StandardCharsets.UTF_8));
            Files.write(layout.versionFile(bucketName, keyHash, versionId),
                    VersionEntry.object(content.sha256, content.md5, createdMs, content.size,
                            contentTypeOf(callContext)).toString().getBytes(StandardCharsets.UTF_8));
            Files.write(layout.headFile(bucketName, keyHash), versionId.getBytes(StandardCharsets.UTF_8));

            S3ResponseHeaderImpl responseHeader = new S3ResponseHeaderImpl();
            // S3 PUT Object answers 200 with no body (Content-Length: 0 on real
            // S3); declaring the object's size would make clients wait for a
            // body that never comes. Same convention as the FSRepository.
            responseHeader.setDate(new Date());
            responseHeader.setLastModified(new Date(createdMs));
            responseHeader.setEtag("\"" + content.md5 + "\"");
            responseHeader.setXamzVersionId(versionId);
            callContext.setResponseHeader(responseHeader);
        } catch (S3ServerException e) {
            throw e;
        } catch (IOException e) {
            logger.error("could not write object {} into bucket {}", objectKey, bucketName, e);
            throw new InternalErrorException(objectKey, callContext.getRequestId());
        } finally {
            lock.close();
        }
    }

    /**
     * Reads the request payload (chunked + signed if requested), verifies
     * {@code Content-MD5}/{@code Content-Length} and stores the content as a
     * content-addressed blob, deduplicated against existing blobs.
     *
     * @return the stored blob and its digests, or throws a client error
     */
    private PutResult writeContent(S3CallContext callContext, String objectKey)
            throws IOException, NoSuchAlgorithmException, InterruptedException {
        Long contentLength = callContext.getHeader().getContentLength();
        String decodedContentLength = callContext.getHeader().getFullHeader().get(S3Constants.X_AMZ_DECODED_CONTENT_LENGTH);
        String clientMd5 = callContext.getHeader().getContentMD5();
        boolean isChunked = SigV4.isStreamingPayload(callContext.getHeader().getXamzContentSha256());

        Files.createDirectories(layout.dataDir());
        final Path[] tempHolder = new Path[1];
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            try (InputStream content = callContext.getContent()) {
                InputStream in = content;
                if (isChunked) {
                    in = new S3ChunkedInputStream(in, chunkSeedSignature(callContext), buildChunkVerifier(callContext));
                }
                InputStream md5In = new DigestInputStream(in, md5);
                InputStream shaIn = new DigestInputStream(md5In, sha256);
                Path tempBlob = Files.createTempFile(layout.dataDir(), "vfs-", ".tmp");
                tempHolder[0] = tempBlob;
                try (OutputStream out = Files.newOutputStream(tempBlob)) {
                    StreamUtils.copy(shaIn, out);
                }
                long size = Files.size(tempBlob);

                byte[] md5Bytes = md5.digest();
                String storageMd5Base64 = Base64.getEncoder().encodeToString(md5Bytes);
                boolean lengthOk = isChunked
                        ? decodedContentLength == null || decodedContentLength.equals(Long.toString(size))
                        : contentLength == null || contentLength.longValue() == size;
                if (!lengthOk || (clientMd5 != null && !clientMd5.equals(storageMd5Base64))) {
                    throw new InvalidDigestException(objectKey, callContext.getRequestId());
                }

                byte[] sha256Bytes = sha256.digest();
                String sha256Hex = Encoding.toHex(sha256Bytes);
                Path target = layout.blob(sha256Hex);
                if (Files.exists(target)) {
                    // identical content is stored already -> deduplicate, keep the existing blob
                    deleteQuietly(tempBlob);
                } else {
                    try {
                        Files.move(tempBlob, target, StandardCopyOption.ATOMIC_MOVE);
                    } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                        Files.move(tempBlob, target, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
                tempHolder[0] = null;
                return new PutResult(target, sha256Hex, Encoding.toHex(md5Bytes), size);
            }
        } finally {
            deleteQuietly(tempHolder[0]);
        }
    }

    @Override
    public S3Object copyObject(S3CallContext callContext, String srcBucket, String srcObjectKey,
                               String destBucket, String destObjectKey, boolean copyMetadata) {
        ensureBucketExists(srcBucket, callContext);
        ensureBucketExists(destBucket, callContext);
        String srcHash = VFSLayout.keyHash(srcBucket, srcObjectKey);
        String dstHash = VFSLayout.keyHash(destBucket, destObjectKey);

        // a missing source object (including delete markers) is a client error
        VersionEntry source = resolveVersion(callContext, srcBucket, srcObjectKey, srcHash, null);

        long createdMs = System.currentTimeMillis();
        VFSLockManager.LockHandle lock = lockManager.acquireExclusive(callContext, destBucket, dstHash);
        try {
            String versionId = newVersionId();
            Files.createDirectories(layout.versionsDir(destBucket, dstHash));
            Files.write(layout.keyFile(destBucket, dstHash), destObjectKey.getBytes(StandardCharsets.UTF_8));
            Files.write(layout.versionFile(destBucket, dstHash, versionId),
                    VersionEntry.object(source.getSha256(), source.getMd5(), createdMs, source.getSize(),
                            source.getContentType()).toString().getBytes(StandardCharsets.UTF_8));
            Files.write(layout.headFile(destBucket, dstHash), versionId.getBytes(StandardCharsets.UTF_8));

            String contentType = source.getContentType().isEmpty() ? null : source.getContentType();
            return new S3ObjectImpl(destObjectKey,
                    new Date(createdMs),
                    destBucket,
                    source.getSize(),
                    new S3UserImpl(),
                    null,
                    null,
                    contentType,
                    "\"" + source.getMd5() + "\"",
                    versionId,
                    false,
                    true);
        } catch (IOException e) {
            logger.error("could not copy object {} to {}/{}", srcObjectKey, destBucket, destObjectKey, e);
            throw new InternalErrorException(destObjectKey, callContext.getRequestId());
        } finally {
            lock.close();
        }
    }

    @Override
    public void getObject(S3CallContext callContext, String bucketName, String objectKey, boolean head) {
        ensureBucketExists(bucketName, callContext);
        String keyHash = VFSLayout.keyHash(bucketName, objectKey);
        String clientVersionId = versionIdParam(callContext);

        VersionEntry entry = resolveVersion(callContext, bucketName, objectKey, keyHash, clientVersionId);
        String versionId = clientVersionId != null ? clientVersionId
                : readHead(callContext, bucketName, keyHash, objectKey);

        S3ResponseHeaderImpl responseHeader = new S3ResponseHeaderImpl();
        responseHeader.setContentType(entry.getContentType().isEmpty()
                ? "application/octet-stream" : entry.getContentType());
        responseHeader.setContentLength(entry.getSize());
        responseHeader.setDate(new Date());
        responseHeader.setLastModified(new Date(entry.getCreatedMs()));
        responseHeader.setEtag(entry.etag());
        responseHeader.setXamzVersionId(versionId);
        applyResponseOverrides(responseHeader, callContext);
        callContext.setResponseHeader(responseHeader);

        if (head) {
            return;
        }
        Path blob = layout.blob(entry.getSha256());
        if (!Files.isRegularFile(blob)) {
            // the version metadata survived but the blob is gone (storage damage)
            throw new InternalErrorException(objectKey, callContext.getRequestId());
        }
        try (InputStream in = Files.newInputStream(blob)) {
            callContext.setContent(in);
        } catch (IOException e) {
            throw new InternalErrorException(objectKey, callContext.getRequestId());
        }
    }

    @Override
    public void deleteObject(S3CallContext callContext, String bucketName, String objectKey) {
        ensureBucketExists(bucketName, callContext);
        Path objectDir = layout.objectDir(bucketName, VFSLayout.keyHash(bucketName, objectKey));
        if (!Files.isDirectory(objectDir)) {
            throw new NoSuchKeyException(objectKey, callContext.getRequestId());
        }
        String keyHash = objectDir.getFileName().toString();

        String clientVersionId = versionIdParam(callContext);
        if (clientVersionId != null && !clientVersionId.isEmpty() && !VFSLayout.isValidVersionId(clientVersionId)) {
            // reject anything that is not a plain file name (path traversal hardening)
            throw new NoSuchVersionException(objectKey, callContext.getRequestId());
        }

        boolean permanent = clientVersionId != null && !clientVersionId.isEmpty();
        long now = System.currentTimeMillis();
        VFSLockManager.LockHandle lock = lockManager.acquireExclusive(callContext, bucketName, keyHash);
        try {
            if (!permanent) {
                // S3 semantics: append a delete marker as the new current version
                String markerId = newVersionId();
                Files.createDirectories(layout.versionsDir(bucketName, keyHash));
                Files.write(layout.versionFile(bucketName, keyHash, markerId),
                        VersionEntry.marker(now).toString().getBytes(StandardCharsets.UTF_8));
                Files.write(layout.headFile(bucketName, keyHash), markerId.getBytes(StandardCharsets.UTF_8));

                S3ResponseHeaderImpl responseHeader = new S3ResponseHeaderImpl();
                responseHeader.setXamzDeleteMarker(Boolean.TRUE);
                responseHeader.setXamzVersionId(markerId);
                callContext.setResponseHeader(responseHeader);
                return;
            }

            Path versionFile = layout.versionFile(bucketName, keyHash, clientVersionId);
            if (!Files.isRegularFile(versionFile)) {
                throw new NoSuchVersionException(objectKey, callContext.getRequestId());
            }
            VersionEntry removed;
            try {
                removed = VersionEntry.parse(VFSLayout.readSingleLine(versionFile));
            } catch (IllegalArgumentException e) {
                throw new NoSuchVersionException(objectKey, callContext.getRequestId());
            }
            Files.delete(versionFile);

            // the previous version (if any) becomes current again
            List<VersionTuple> remaining = readAllVersions(bucketName, keyHash);
            if (remaining.isEmpty()) {
                deleteRecursively(objectDir);
            } else {
                Files.write(layout.headFile(bucketName, keyHash),
                        remaining.get(0).versionId.getBytes(StandardCharsets.UTF_8));
            }

            S3ResponseHeaderImpl responseHeader = new S3ResponseHeaderImpl();
            responseHeader.setXamzDeleteMarker(removed.isDeleteMarker());
            responseHeader.setXamzVersionId(clientVersionId);
            callContext.setResponseHeader(responseHeader);

            // the blob may be unreferenced now -> run the collector
            blobGC.sweep();
        } catch (S3ServerException e) {
            throw e;
        } catch (IOException e) {
            logger.error("could not delete object {} from bucket {}", objectKey, bucketName, e);
            throw new InternalErrorException(objectKey, callContext.getRequestId());
        } finally {
            lock.close();
        }
    }

    // ------------------------------------------------------------------ listing

    @Override
    public S3ListBucketResult listBucket(S3CallContext callContext, String bucketName) {
        ensureBucketExists(bucketName, callContext);
        Path objectsDir = layout.objectsDir(bucketName);
        String prefix = callContext.getParams().getPrefix() == null ? "" : callContext.getParams().getPrefix();
        int maxKeys = callContext.getParams().getMaxKeys();

        if (callContext.getParams().listVersions()) {
            return listVersions(callContext, bucketName, objectsDir, prefix, maxKeys);
        }

        String marker = callContext.getParams().getMarker();
        String delimiter = callContext.getParams().getDelimiter();
        if (delimiter != null && !delimiter.equals("/")) {
            throw new de.mc.ladon.s3server.exceptions.InvalidTokenException(delimiter, callContext.getRequestId());
        }

        List<KeyHead> heads = new ArrayList<>();
        for (String keyHash : listSubDirs(objectsDir)) {
            KeyHead head = readKeyMeta(bucketName, keyHash);
            if (head == null) {
                continue;
            }
            if (head.entry == null || head.entry.isDeleteMarker()) {
                // keys whose current version is a marker do not appear in a normal listing
                continue;
            }
            if (!head.key.startsWith(prefix)) {
                continue;
            }
            if (marker != null && !marker.isEmpty() && head.key.compareTo(marker) <= 0) {
                continue;
            }
            heads.add(head);
        }
        heads.sort(Comparator.comparing((KeyHead h) -> h.key));

        boolean truncated = heads.size() > maxKeys;
        List<S3Object> objects = new ArrayList<>();
        Set<String> prefixes = new TreeSet<>();
        String lastKey = null;
        for (int i = 0; i < heads.size() && i < maxKeys; i++) {
            KeyHead head = heads.get(i);
            if (delimiter != null) {
                String commonPrefix = DelimiterUtil.getCommonPrefix(head.key, prefix, delimiter);
                if (commonPrefix != null) {
                    prefixes.add(commonPrefix);
                    continue;
                }
            }
            VersionEntry v = head.entry;
            objects.add(new S3ObjectImpl(head.key,
                    new Date(v.getCreatedMs()),
                    bucketName,
                    v.getSize(),
                    new S3UserImpl(),
                    null,
                    null,
                    v.getContentType().isEmpty() ? null : v.getContentType(),
                    v.etag(),
                    head.headId,
                    false,
                    true));
            lastKey = head.key;
        }
        return new S3ListBucketResultImpl(objects, new ArrayList<>(prefixes), truncated,
                bucketName, truncated ? lastKey : null, null);
    }

    private S3ListBucketResult listVersions(S3CallContext callContext, String bucketName,
                                            Path objectsDir, String prefix, int maxKeys) {
        String markerKey = callContext.getParams().getKeyMarker();
        if (markerKey == null || markerKey.isEmpty()) {
            markerKey = callContext.getParams().getMarker();
        }
        String versionIdMarker = callContext.getParams().getVersionIdMarker();

        // every version of every key, in S3 order: keys ascending,
        // newest version first (createdMs, then versionId as tie break)
        List<VersionTuple> all = new ArrayList<>();
        for (String keyHash : listSubDirs(objectsDir)) {
            KeyHead meta = readKeyMeta(bucketName, keyHash);
            if (meta == null || meta.key == null || !meta.key.startsWith(prefix)) {
                continue;
            }
            for (VersionTuple tuple : readAllVersions(bucketName, keyHash)) {
                tuple.isLatest = tuple.versionId.equals(meta.headId);
                all.add(tuple);
            }
        }
        all.sort(Comparator
                .comparing((VersionTuple t) -> t.key)
                .thenComparing(Comparator.comparing((VersionTuple t) -> t.entry.getCreatedMs()).reversed())
                .thenComparing(Comparator.comparing((VersionTuple t) -> t.versionId, Comparator.reverseOrder())));

        // AWS ListObjectVersions marker semantics: continue with the first tuple strictly
        // after (markerKey, versionIdMarker). Versions within one key are ordered newest
        // first, and version ids grow with newness — so "already delivered" for the marker
        // key means: all versions with an id >= the marker id (newer or the marker
        // itself). Continue with the first older version (id < marker); with only a key
        // marker the whole marker key is skipped.
        int from = 0;
        if (markerKey != null && !markerKey.isEmpty()) {
            if (versionIdMarker == null || versionIdMarker.isEmpty()) {
                while (from < all.size() && all.get(from).key.compareTo(markerKey) <= 0) {
                    from++;
                }
            } else {
                while (from < all.size()) {
                    VersionTuple t = all.get(from);
                    int keyCmp = t.key.compareTo(markerKey);
                    if (keyCmp > 0) {
                        break;
                    }
                    if (keyCmp == 0 && t.versionId.compareTo(versionIdMarker) < 0) {
                        break;
                    }
                    from++;
                }
            }
        }
        int count = all.size() - from;
        boolean truncated = count > maxKeys;
        int shown = Math.min(count, maxKeys);

        List<S3Object> objects = new ArrayList<>();
        String lastKey = null;
        String lastVersionId = null;
        for (int i = 0; i < shown; i++) {
            VersionTuple t = all.get(from + i);
            if (t.entry.isDeleteMarker()) {
                objects.add(new S3ObjectImpl(t.key,
                        new Date(t.entry.getCreatedMs()), bucketName, 0L,
                        new S3UserImpl(), null, null, null, null,
                        t.versionId, true, t.isLatest));
            } else {
                objects.add(new S3ObjectImpl(t.key,
                        new Date(t.entry.getCreatedMs()), bucketName, t.entry.getSize(),
                        new S3UserImpl(), null, null,
                        t.entry.getContentType().isEmpty() ? null : t.entry.getContentType(),
                        t.entry.etag(), t.versionId, false, t.isLatest));
            }
            lastKey = t.key;
            lastVersionId = t.versionId;
        }
        return new S3ListBucketResultImpl(objects, new ArrayList<String>(), truncated, bucketName,
                truncated ? lastKey : null, truncated ? lastVersionId : null);
    }

    // ------------------------------------------------------------------ users

    @Override
    public S3User getUser(S3CallContext callContext, String accessKey) {
        if (accessKey == null) {
            throw new InvalidAccessKeyIdException("", callContext.getRequestId());
        }
        S3User user = userDb.getByAccessKey(accessKey);
        if (user == null) {
            throw new InvalidAccessKeyIdException("", callContext.getRequestId());
        }
        return user;
    }

    // ------------------------------------------------------------------ internals

    private static void validateBucketName(String bucketName, S3CallContext callContext) {
        if (bucketName == null || bucketName.isEmpty() || bucketName.indexOf('/') >= 0
                || bucketName.indexOf('\0') >= 0 || "..".equals(bucketName)) {
            throw new InvalidBucketName(String.valueOf(bucketName), callContext.getRequestId());
        }
    }

    private void ensureBucketExists(String bucketName, S3CallContext callContext) {
        validateBucketName(bucketName, callContext);
        if (!Files.isDirectory(layout.bucketDir(bucketName))) {
            throw new NoSuchBucketException(bucketName, callContext.getRequestId());
        }
    }

    private static String versionIdParam(S3CallContext callContext) {
        Map<String, String> params = callContext.getParams().getAllParams();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (S3Constants.VERSION_ID.equalsIgnoreCase(entry.getKey())) {
                String value = entry.getValue() == null ? null : entry.getValue().trim();
                return value == null || value.isEmpty() ? null : value;
            }
        }
        return null;
    }

    /**
     * Generates a version id: {@code v} + base36(epoch-millis) + `-` + a small
     * per-call sequence + random characters. The sequence makes same-millisecond
     * calls unique per repository instance, the random part makes collisions
     * across processes negligible.
     */
    private static String newVersionId() {
        long seq = VERSION_ID_SEQ.incrementAndGet();
        StringBuilder sb = new StringBuilder("v");
        sb.append(Long.toString(System.currentTimeMillis(), 36));
        sb.append('-');
        sb.append(Long.toString(seq & 0xFFFF, 36));
        for (int i = 0; i < 3; i++) {
            sb.append(VERSION_ID_ALPHABET.charAt(VERSION_ID_RANDOM.nextInt(VERSION_ID_ALPHABET.length())));
        }
        return sb.toString();
    }

    /**
     * Resolves a version entry by head or by an explicit version id.
     *
     * @param clientVersionId an explicit version id, or {@code null} for the current (head) version
     * @return the version entry (never a delete marker; those answer with {@code NoSuchKey})
     */
    private VersionEntry resolveVersion(S3CallContext callContext, String bucketName, String objectKey,
                                        String keyHash, String clientVersionId) {
        String versionId = clientVersionId != null
                ? clientVersionId
                : readHead(callContext, bucketName, keyHash, objectKey);
        Path versionFile = layout.versionFile(bucketName, keyHash, versionId);
        if (!Files.isRegularFile(versionFile)) {
            throw new NoSuchVersionException(objectKey, callContext.getRequestId());
        }
        try {
            VersionEntry entry = VersionEntry.parse(VFSLayout.readSingleLine(versionFile));
            if (entry.isDeleteMarker()) {
                throw new NoSuchKeyException(objectKey, callContext.getRequestId());
            }
            return entry;
        } catch (IOException | IllegalArgumentException e) {
            throw new NoSuchVersionException(objectKey, callContext.getRequestId());
        }
    }

    private String readHead(S3CallContext callContext, String bucketName, String keyHash, String objectKey) {
        try {
            String head = VFSLayout.readSingleLine(layout.headFile(bucketName, keyHash)).trim();
            if (head.isEmpty() || !VFSLayout.isValidVersionId(head)) {
                throw new NoSuchKeyException(objectKey, callContext.getRequestId());
            }
            return head;
        } catch (IOException e) {
            // no readable head -> the object has no current version
            throw new NoSuchKeyException(objectKey, callContext.getRequestId());
        }
    }

    private static String contentTypeOf(S3CallContext callContext) {
        String contentType = callContext.getHeader().getContentType();
        return contentType == null ? "" : contentType;
    }

    /**
     * Applies the S3 response header overrides ({@code response-*}) from the
     * request parameters, e.g. {@code response-content-type},
     * {@code response-content-disposition}, {@code response-expires} ...
     */
    private static void applyResponseOverrides(S3ResponseHeaderImpl header, S3CallContext callContext) {
        Map<String, String> params = callContext.getParams().getAllParams();
        String v;
        if ((v = SigV4.headerIgnoreCase(params, "response-content-type")) != null) {
            header.setContentType(v);
        }
        if ((v = SigV4.headerIgnoreCase(params, "response-content-disposition")) != null) {
            header.setContentDisposition(v);
        }
        if ((v = SigV4.headerIgnoreCase(params, "response-content-encoding")) != null) {
            header.setContentEncoding(v);
        }
        if ((v = SigV4.headerIgnoreCase(params, "response-content-language")) != null) {
            header.setContentLanguage(v);
        }
        if ((v = SigV4.headerIgnoreCase(params, "response-cache-control")) != null) {
            header.setCacheControl(v);
        }
        if ((v = SigV4.headerIgnoreCase(params, "response-expires")) != null) {
            try {
                header.setExpires(Long.parseLong(v.trim()));
            } catch (NumberFormatException e) {
                // ignore invalid value, keep the server default
            }
        }
    }

    /**
     * The seed of the chunk signature chain for a chunked upload. The request
     * signature was verified already, so the seed equals the request signature.
     */
    private static String chunkSeedSignature(S3CallContext callContext) {
        return new AwsSignatureVersion4().getSignature(callContext);
    }

    private static SigV4ChunkVerifier buildChunkVerifier(S3CallContext callContext) {
        AwsSignatureVersion4 sig = new AwsSignatureVersion4();
        String date = sig.getDate(callContext);
        String region = sig.getRegion(callContext);
        String xamzDate = callContext.getHeader().getXamzDate();
        byte[] signingKey = SigV4.signingKey(callContext.getUser().getSecretKey(), date, region, "s3");
        String scope = date + "/" + region + "/s3/aws4_request";
        return new SigV4ChunkVerifier(xamzDate, scope, signingKey, callContext.getUri(), callContext.getRequestId());
    }

    /**
     * Reads the key, head id and head entry of one object.
     *
     * @return the meta data, or {@code null} if the object is unreadable
     */
    private KeyHead readKeyMeta(String bucketName, String keyHash) {
        Path objectDir = layout.objectDir(bucketName, keyHash);
        if (!Files.isDirectory(objectDir)) {
            return null;
        }
        KeyHead head = new KeyHead();
        try {
            head.key = new String(Files.readAllBytes(layout.keyFile(bucketName, keyHash)), StandardCharsets.UTF_8);
            head.headId = VFSLayout.readSingleLine(layout.headFile(bucketName, keyHash)).trim();
            Path versionFile = layout.versionFile(bucketName, keyHash, head.headId);
            if (Files.isRegularFile(versionFile)) {
                head.entry = VersionEntry.parse(VFSLayout.readSingleLine(versionFile));
            }
        } catch (IOException | IllegalArgumentException e) {
            logger.warn("could not read metadata of object {} in bucket {}", keyHash, bucketName, e);
            return null;
        }
        return head;
    }

    /**
     * All versions of one object, newest first.
     */
    private List<VersionTuple> readAllVersions(String bucketName, String keyHash) {
        List<VersionTuple> result = new ArrayList<>();
        String key = null;
        try {
            key = new String(Files.readAllBytes(layout.keyFile(bucketName, keyHash)), StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.warn("could not read key of object {} in bucket {}", keyHash, bucketName, e);
        }
        Path versionsDir = layout.versionsDir(bucketName, keyHash);
        if (Files.isDirectory(versionsDir)) {
            for (Path versionFile : listFiles(versionsDir)) {
                String id = versionFile.getFileName().toString();
                try {
                    VersionEntry v = VersionEntry.parse(VFSLayout.readSingleLine(versionFile));
                    result.add(new VersionTuple(key, id, v, false));
                } catch (IOException | IllegalArgumentException e) {
                    logger.warn("skipping unreadable version entry {} in bucket {}", versionFile, bucketName, e);
                }
            }
        }
        result.sort(Comparator
                .comparing((VersionTuple t) -> t.entry.getCreatedMs()).reversed()
                .thenComparing(Comparator.comparing((VersionTuple t) -> t.versionId, Comparator.reverseOrder())));
        return result;
    }

    private static List<String> listSubDirs(Path dir) {
        List<String> result = new ArrayList<>();
        if (!Files.isDirectory(dir)) {
            return result;
        }
        try {
            Files.list(dir).forEach(p -> {
                if (Files.isDirectory(p)) {
                    result.add(p.getFileName().toString());
                }
            });
        } catch (IOException e) {
            throw new IllegalStateException("could not list " + dir, e);
        }
        result.sort(Comparator.naturalOrder());
        return result;
    }

    private static List<Path> listFiles(Path dir) {
        List<Path> result = new ArrayList<>();
        if (!Files.isDirectory(dir)) {
            return result;
        }
        try {
            Files.list(dir).forEach(p -> {
                if (Files.isRegularFile(p)) {
                    result.add(p);
                }
            });
        } catch (IOException e) {
            throw new IllegalStateException("could not list " + dir, e);
        }
        return result;
    }

    private static boolean isDirectoryEmpty(Path dir) {
        return listFiles(dir).isEmpty() && listSubDirs(dir).isEmpty();
    }

    private static void deleteRecursively(Path dir) throws IOException {
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path d, IOException exc) throws IOException {
                Files.delete(d);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            logger.warn("could not delete {}", path, e);
        }
    }

    /**
     * Stored blob plus digests, the result of a content upload.
     */
    private static final class PutResult {
        final Path blob;
        final String sha256;
        final String md5;
        final long size;

        PutResult(Path blob, String sha256, String md5, long size) {
            this.blob = blob;
            this.sha256 = sha256;
            this.md5 = md5;
            this.size = size;
        }
    }

    /**
     * The identity, head and head entry of one object.
     */
    private static final class KeyHead {
        String key;
        String headId;
        VersionEntry entry;
    }

    /**
     * A single version for listing, with its current-version flag
     * (mutable on purpose: the flag is filled in by the listing).
     */
    private static final class VersionTuple {
        final String key;
        final String versionId;
        final VersionEntry entry;
        boolean isLatest;

        VersionTuple(String key, String versionId, VersionEntry entry, boolean isLatest) {
            this.key = key;
            this.versionId = versionId;
            this.entry = entry;
            this.isLatest = isLatest;
        }
    }
}
