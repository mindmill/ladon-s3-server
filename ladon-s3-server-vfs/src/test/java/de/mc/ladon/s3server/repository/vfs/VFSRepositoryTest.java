/*
 * T38: repository-level unit tests for the VFS layout — the whole S3 versioning
 * lifecycle (put v1 -> v2, get head, get by versionId, listVersions ordering and
 * pagination, delete marker flow, permanent version delete, delete bucket),
 * blob deduplication and garbage collection, copy semantics, user handling and
 * concurrent writes.
 */
package de.mc.ladon.s3server.repository.vfs;

import de.mc.ladon.s3server.common.Encoding;
import de.mc.ladon.s3server.entities.api.S3Bucket;
import de.mc.ladon.s3server.entities.api.S3ListBucketResult;
import de.mc.ladon.s3server.entities.api.S3Object;
import de.mc.ladon.s3server.entities.api.S3User;
import de.mc.ladon.s3server.entities.impl.S3ResponseHeaderImpl;
import de.mc.ladon.s3server.exceptions.BucketNotEmptyException;
import de.mc.ladon.s3server.exceptions.InvalidAccessKeyIdException;
import de.mc.ladon.s3server.exceptions.InvalidDigestException;
import de.mc.ladon.s3server.exceptions.NoSuchBucketException;
import de.mc.ladon.s3server.exceptions.NoSuchKeyException;
import de.mc.ladon.s3server.exceptions.NoSuchVersionException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VFSRepositoryTest {

    private Path root;
    private VFSRepository repository;

    @BeforeEach
    void setUp() throws Exception {
        root = Files.createTempDirectory("ladon-vfs-test");
        repository = new VFSRepository(root);
    }

    @AfterEach
    void tearDown() throws Exception {
        deleteRecursively(root);
    }

    // ------------------------------------------------------------------ buckets

    @Test
    void bucketLifecycleListsCreateHeadAndDelete() {
        createBucket("my-bucket");
        createBucket("my-bucket"); // idempotent per AWS (BucketAlreadyOwnedByYou 200)
        createBucket("other-bucket");

        List<S3Bucket> buckets = repo().listAllBuckets(ctx("GET", Collections.emptyMap(), null).callContext);
        List<String> names = new ArrayList<>();
        for (S3Bucket b : buckets) {
            names.add(b.getBucketName());
        }
        assertEquals("my-bucket,other-bucket", String.join(",", names));

        assertDoesNothing(() -> repo().getBucket(ctx("HEAD", Collections.emptyMap(), null).callContext, "my-bucket"));
        assertThrows(NoSuchBucketException.class,
                () -> repo().getBucket(ctx("HEAD", Collections.emptyMap(), null).callContext, "ghost-bucket"));
    }

    // ------------------------------------------------------------------ objects: put/get

    @Test
    void putCreatesVersionAndAnswersWithEtagAndVersionId() throws Exception {
        createBucket("b");
        byte[] body = "hello vfs".getBytes(StandardCharsets.UTF_8);
        TestContexts.Context ctx = ctx("PUT", Collections.emptyMap(), body);
        repo().createObject(ctx.callContext, "b", "greeting.txt");
        S3ResponseHeaderImpl header = responseHeader(ctx);

        assertNotNull(header.getXamzVersionId(), "x-amz-version-id must be set");
        assertEquals("\"cf4f4ef879a9c544d8e9fb740e8d1fce\"",
                header.getEtag(), "etag must be the quoted base16 md5 of the payload");
        // the GET must answer with the exact content
        TestContexts.Context get = ctx("GET", Collections.emptyMap(), null);
        repo().getObject(get.callContext, "b", "greeting.txt", false);
        assertEquals("hello vfs", new String(get.writtenContent, StandardCharsets.UTF_8));
        assertEquals(body.length, get.writtenContent.length);
        assertEquals(header.getXamzVersionId(), responseHeader(get).getXamzVersionId());
    }

    @Test
    void putTwiceKeepsBothVersionsAndHeadFollowsTheNewest() throws Exception {
        createBucket("b");
        put("b", "file", "version one");
        String v1 = headVersion("b", "file");
        Thread.sleep(5);
        put("b", "file", "version two");
        String v2 = headVersion("b", "file");

        assertFalse(v1.equals(v2), "each put must create a new version");
        assertEquals("version two", get("b", "file", null));
        assertEquals("version one", get("b", "file", v1));
        assertEquals("version two", get("b", "file", v2));
    }

    @Test
    void headObjectReturnsHeadersOnlyWithoutContent() {
        createBucket("b");
        put("b", "file", "payload");
        TestContexts.Context head = ctx("HEAD", Collections.emptyMap(), null);
        repo().getObject(head.callContext, "b", "file", true);
        assertNotNull(responseHeader(head).getEtag());
        assertNull(head.writtenContent, "a HEAD request must not write content");
    }

    @Test
    void getsAnswerNoSuchKeyAndNoSuchBucket() {
        assertThrows(NoSuchBucketException.class,
                () -> repo().getObject(ctx("GET", Collections.emptyMap(), null).callContext, "no-such-bucket", "k", false));
        createBucket("b");
        assertThrows(NoSuchKeyException.class,
                () -> repo().getObject(ctx("GET", Collections.emptyMap(), null).callContext, "b", "missing", false));
    }

    @Test
    void contentLengthAndMd5AreVerified() {
        createBucket("b");
        byte[] body = "payload".getBytes(StandardCharsets.UTF_8);

        TestContexts.Context len = new TestContexts.Context("PUT",
                Collections.singletonMap("Content-Length", "1000"), Collections.emptyMap(), body);
        assertThrows(InvalidDigestException.class,
                () -> repo().createObject(len.callContext, "b", "wrong-length"),
                "a lying Content-Length must be a 400-class client error");

        TestContexts.Context md5 = new TestContexts.Context("PUT",
                Collections.singletonMap("Content-MD5", "AAAAAAAAAAAAAAAAAAAAAA="), Collections.emptyMap(), body);
        assertThrows(InvalidDigestException.class,
                () -> repo().createObject(md5.callContext, "b", "wrong-md5"),
                "a wrong Content-MD5 must be rejected (invalid digest)");
    }

    // ------------------------------------------------------------------ deletes

    @Test
    void deleteWithoutVersionIdWritesMarkerAndHidesTheObject() {
        createBucket("b");
        put("b", "file", "still here");
        TestContexts.Context del = ctx("DELETE", Collections.emptyMap(), null);
        repo().deleteObject(del.callContext, "b", "file");
        S3ResponseHeaderImpl header = responseHeader(del);

        assertEquals(Boolean.TRUE, header.getXamzDeleteMarker(), "a plain DELETE answers x-amz-delete-marker: true");
        assertNotNull(header.getXamzVersionId());

        assertThrows(NoSuchKeyException.class,
                () -> repo().getObject(ctx("GET", Collections.emptyMap(), null).callContext, "b", "file", false),
                "the current version must be the delete marker -> GET is a 404");

        S3ListBucketResult versions = repo().listBucket(
                ctx("GET", Collections.singletonMap("versions", ""), null).callContext, "b");
        assertEquals(2, versions.getObjects().size());
        // newest first: the delete marker, then the (no longer current) version
        assertEquals(Boolean.TRUE, versions.getObjects().get(0).isDeleted(), "the marker is the newest entry");
        assertEquals(Boolean.TRUE, versions.getObjects().get(0).isLatest(), "the marker is the current version");
        assertEquals(Boolean.FALSE, versions.getObjects().get(1).isDeleted(), "the object version is not a marker");
        assertEquals(Boolean.FALSE, versions.getObjects().get(1).isLatest());
    }

    @Test
    void permanentDeleteOfVersionRestoresPreviousVersion() throws Exception {
        createBucket("b");
        put("b", "file", "v1");
        String v1 = headVersion("b", "file");
        Thread.sleep(5);
        put("b", "file", "v2");
        String v2 = headVersion("b", "file");

        repo().deleteObject(ctx("DELETE", Collections.singletonMap("versionId", v2), null).callContext, "b", "file");

        assertEquals("v1", get("b", "file", null), "the previous version becomes current again");
        S3ListBucketResult versions = listVersions("b");
        assertEquals(1, versions.getObjects().size());
        assertEquals(v1, versions.getObjects().get(0).getVersionId());
        assertEquals(Boolean.TRUE, versions.getObjects().get(0).isLatest());
    }

    @Test
    void permanentDeleteOfMarkerRestoresTheObject() throws Exception {
        createBucket("b");
        put("b", "file", "v1");
        TestContexts.Context del = ctx("DELETE", Collections.emptyMap(), null);
        repo().deleteObject(del.callContext, "b", "file");
        String marker = responseHeader(del).getXamzVersionId();

        repo().deleteObject(ctx("DELETE", Collections.singletonMap("versionId", marker), null).callContext, "b", "file");

        assertEquals("v1", get("b", "file", null), "deleting the marker revives the object");
    }

    @Test
    void permanentDeleteOfLastVersionRemovesTheKey() throws Exception {
        createBucket("b");
        put("b", "file", "v1");
        String v1 = headVersion("b", "file");
        repo().deleteObject(ctx("DELETE", Collections.singletonMap("versionId", v1), null).callContext, "b", "file");

        assertThrows(NoSuchKeyException.class,
                () -> repo().getObject(ctx("GET", Collections.emptyMap(), null).callContext, "b", "file", false));
        assertEquals(0, listVersions("b").getObjects().size(), "no versions remain");
        // the whole object directory is gone
        assertFalse(Files.isDirectory(root.resolve("buckets/b/objects/" + keyHash("b", "file"))),
                "the object directory must be removed");
    }

    @Test
    void deleteOfUnknownVersionOrMalformedIdIsNoSuchVersion() {
        createBucket("b");
        put("b", "file", "v1");
        assertThrows(NoSuchVersionException.class,
                () -> repo().deleteObject(ctx("DELETE", Collections.singletonMap("versionId", "v-nope"), null).callContext, "b", "file"));
        // path traversal ids must be rejected as well (a 404, not a 500)
        assertThrows(NoSuchVersionException.class,
                () -> repo().deleteObject(ctx("DELETE", Collections.singletonMap("versionId", "../escape"), null).callContext, "b", "file"));
        assertThrows(NoSuchVersionException.class,
                () -> repo().getObject(ctx("GET", Collections.singletonMap("versionID", "../../etc/passwd"), null).callContext,
                        "b", "file", false));
    }

    // ------------------------------------------------------------------ dedup & gc

    @Test
    void identicalContentSharesOneBlobAcrossKeysAndBuckets() throws Exception {
        createBucket("b1");
        createBucket("b2");
        byte[] same = "same content everywhere".getBytes(StandardCharsets.UTF_8);
        put("b1", "a", new String(same, StandardCharsets.UTF_8));
        put("b1", "b", new String(same, StandardCharsets.UTF_8));
        put("b2", "c", new String(same, StandardCharsets.UTF_8));
        Thread.sleep(5);
        put("b1", "a", "different content");

        assertEquals(2, blobCount(), "three identical payloads -> one blob, one different -> two");
    }

    @Test
    void garbageCollectionRemovesUnreferencedBlobs() throws Exception {
        createBucket("b");
        put("b", "file", "content-A");
        Thread.sleep(5);
        put("b", "file", "content-B");
        String v2 = headVersion("b", "file");
        String v1 = secondNewestVersion("b", "file");

        assertEquals(2, blobCount());
        // delete the newest version -> its blob is unreferenced -> gc runs automatically
        repo().deleteObject(ctx("DELETE", Collections.singletonMap("versionId", v2), null).callContext, "b", "file");
        assertEquals(1, blobCount(), "the unreferenced blob must be swept, the referenced one must survive");
        assertEquals("content-A", get("b", "file", null));

        // delete the last version -> everything is gone
        repo().deleteObject(ctx("DELETE", Collections.singletonMap("versionId", v1), null).callContext, "b", "file");
        assertEquals(0, blobCount());
    }

    @Test
    void manualGcReturnsRemovedCount() throws Exception {
        createBucket("b");
        put("b", "file", "x");
        String v1 = headVersion("b", "file");
        repo().deleteObject(ctx("DELETE", Collections.singletonMap("versionId", v1), null).callContext, "b", "file");
        // manual run on an already clean store removes nothing
        assertEquals(0, repository.gc());
    }

    @Test
    void deletingBucketRemovesItsDataAndIsRejectedWhileNotEmpty() throws Exception {
        createBucket("b");
        put("b", "file", "x");
        assertThrows(BucketNotEmptyException.class, () -> repo().deleteBucket(ctx("DELETE", Collections.emptyMap(), null).callContext, "b"));

        String v1 = headVersion("b", "file");
        repo().deleteObject(ctx("DELETE", Collections.singletonMap("versionId", v1), null).callContext, "b", "file");
        repo().deleteBucket(ctx("DELETE", Collections.emptyMap(), null).callContext, "b");

        List<S3Bucket> buckets = repo().listAllBuckets(ctx("GET", Collections.emptyMap(), null).callContext);
        assertTrue(buckets.isEmpty(), "the bucket must be gone");
        assertFalse(Files.exists(root.resolve("buckets/b")), "the bucket dir must be removed");
    }

    // ------------------------------------------------------------------ copy

    @Test
    void copySharesTheBlobAndAppendsAVersion() throws Exception {
        createBucket("b");
        put("b", "src", "copy-me");
        String srcV = headVersion("b", "src");

        S3Object copied = repo().copyObject(ctx("PUT",
                Collections.singletonMap("x-amz-copy-source", "src"), null).callContext, "b", "src", "b", "dst", true);

        assertEquals("dst", copied.getKey());
        assertFalse(srcV.equals(copied.getVersionId()), "the copy is a new version");
        assertEquals("copy-me", get("b", "dst", null));
        assertEquals(1, blobCount(), "the copy must not duplicate the blob");

        assertThrows(NoSuchBucketException.class,
                () -> repo().copyObject(ctx("PUT", Collections.emptyMap(), null).callContext, "ghost", "x", "b", "dst2", true),
                "copying from a bucket that does not exist is a NoSuchBucket error");
    }

    @Test
    void copyFromDeleteMarkerIsNoSuchKey() {
        createBucket("b");
        put("b", "src", "x");
        repo().deleteObject(ctx("DELETE", Collections.emptyMap(), null).callContext, "b", "src");
        assertThrows(NoSuchKeyException.class,
                () -> repo().copyObject(ctx("PUT", Collections.emptyMap(), null).callContext, "b", "src", "b", "dst", true));
    }

    // ------------------------------------------------------------------ listing

    @Test
    void listVersionsReturnsS3OrderAndIsLatestFlags() throws Exception {
        createBucket("b");
        put("b", "key-a", "a1");
        String a1 = headVersion("b", "key-a");
        Thread.sleep(5);
        put("b", "key-a", "a2");
        String a2 = headVersion("b", "key-a");
        Thread.sleep(5);
        put("b", "key-b", "b1");
        String b1 = headVersion("b", "key-b");
        Thread.sleep(5);
        put("b", "key-b", "b2");
        String b2 = headVersion("b", "key-b");

        S3ListBucketResult versions = listVersions("b");
        assertEquals(4, versions.getObjects().size(), "one entry per version: a1, a2, b1, b2");

        List<S3Object> objects = versions.getObjects();
        // keys ascending, then newest first (S3 ListObjectVersions order)
        assertEquals("key-a", objects.get(0).getKey());
        assertEquals(a2, objects.get(0).getVersionId());
        assertEquals(Boolean.TRUE, objects.get(0).isLatest());
        assertEquals("key-a", objects.get(1).getKey());
        assertEquals(a1, objects.get(1).getVersionId());
        assertEquals(Boolean.FALSE, objects.get(1).isLatest());
        assertEquals("key-b", objects.get(2).getKey());
        assertEquals(b2, objects.get(2).getVersionId());
        assertEquals(Boolean.TRUE, objects.get(2).isLatest());
        assertEquals("key-b", objects.get(3).getKey());
        assertEquals(b1, objects.get(3).getVersionId());
        assertEquals(Boolean.FALSE, objects.get(3).isLatest());
        // etag and size must come from the stored version entry
        assertEquals(2L, objects.get(0).getSize().longValue(), "size of \"a2\"");
        assertNotNull(objects.get(0).getETag(), "object versions carry an etag");
    }

    @Test
    void listVersionsMarksDeleteMarkersAndSkipsMarkeredKeysInNormalListings() throws Exception {
        createBucket("b");
        put("b", "key", "v1");
        repo().deleteObject(ctx("DELETE", Collections.emptyMap(), null).callContext, "b", "key");

        S3ListBucketResult versions = listVersions("b");
        assertEquals(2, versions.getObjects().size());

        S3Object current = versions.getObjects().get(0);
        assertEquals(Boolean.TRUE, current.isDeleted(), "the current entry is the delete marker");
        assertEquals(Boolean.TRUE, current.isLatest());
        assertEquals(Boolean.FALSE, versions.getObjects().get(1).isDeleted());
        assertEquals(Boolean.FALSE, versions.getObjects().get(1).isLatest());

        S3ListBucketResult normal = repo().listBucket(ctx("GET", Collections.emptyMap(), null).callContext, "b");
        assertTrue(normal.getObjects().isEmpty(), "a marker-covered key must not appear in a plain listing");
    }

    @Test
    void listVersionsPaginationContinuesAfterTheMarker() throws Exception {
        createBucket("b");
        put("b", "k1", "a");
        Thread.sleep(5);
        put("b", "k1", "b");
        Thread.sleep(5);
        put("b", "k2", "c");
        Thread.sleep(5);
        put("b", "k3", "d");

        S3ListBucketResult first = listVersions("b", Collections.singletonMap("max-keys", "2"));
        assertTrue(first.isTruncated());
        assertEquals(2, first.getObjects().size());
        String nextKey = first.nextKeyMarker();
        String nextVersion = first.nextVersionIdMarker();
        assertNotNull(nextKey);
        assertNotNull(nextVersion);

        S3ListBucketResult second = repo().listBucket(new TestContexts.Context("GET",
                Collections.emptyMap(), pageParams(nextKey, nextVersion), null).callContext, "b");
        assertEquals(2, second.getObjects().size());
        List<String> all = new ArrayList<>();
        appendIds(all, first.getObjects());
        appendIds(all, second.getObjects());
        assertEquals(4, all.size());
        for (int i = 1; i < all.size(); i++) {
            assertFalse(all.get(i - 1).equals(all.get(i)), "no duplicates across pages: " + all);
        }
    }

    @Test
    void normalListingSkipsMarkeredKeysPrefixesAndDelimiters() throws Exception {
        createBucket("b");
        put("b", "a.txt", "1");
        put("b", "dir/a.txt", "2");
        put("b", "dir/b.txt", "3");
        put("b", "gone", "4");
        repo().deleteObject(ctx("DELETE", Collections.emptyMap(), null).callContext, "b", "gone");

        S3ListBucketResult all = repo().listBucket(ctx("GET", Collections.emptyMap(), null).callContext, "b");
        List<String> keys = new ArrayList<>();
        for (S3Object o : all.getObjects()) {
            keys.add(o.getKey());
        }
        assertEquals("[a.txt, dir/a.txt, dir/b.txt]", keys.toString());
        S3ListBucketResult prefixed = repo().listBucket(ctx("GET",
                Collections.singletonMap("prefix", "dir"), null).callContext, "b");
        assertEquals(2, prefixed.getObjects().size());

        S3ListBucketResult delimited = repo().listBucket(ctx("GET",
                Collections.singletonMap("delimiter", "/"), null).callContext, "b");
        List<String> flat = new ArrayList<>();
        for (S3Object o : delimited.getObjects()) {
            flat.add(o.getKey());
        }
        assertTrue(flat.contains("a.txt"));
        assertFalse(flat.contains("dir/a.txt"));
        assertTrue(delimited.getCommonPrefixes().contains("dir"));
    }

    @Test
    void normalListingHonorsTheMarker() throws Exception {
        createBucket("b");
        put("b", "a", "1");
        put("b", "b", "2");
        put("b", "c", "3");

        S3ListBucketResult after = repo().listBucket(ctx("GET",
                Collections.singletonMap("marker", "a"), null).callContext, "b");
        List<String> keys = new ArrayList<>();
        for (S3Object o : after.getObjects()) {
            keys.add(o.getKey());
        }
        assertEquals("[b, c]", keys.toString());
    }

    @Test
    void responseOverridesAreApplied() {
        createBucket("b");
        put("b", "file", "x");
        java.util.LinkedHashMap<String, String> params = new java.util.LinkedHashMap<>();
        params.put("response-content-type", "text/csv");
        params.put("response-content-disposition", "attachment; filename=export.csv");
        TestContexts.Context realGet = ctx("GET", params, null);
        repo().getObject(realGet.callContext, "b", "file", false);
        S3ResponseHeaderImpl header = responseHeader(realGet);
        assertEquals("text/csv", header.getContentType());
        assertEquals("attachment; filename=export.csv", header.getContentDisposition());
    }

    // ------------------------------------------------------------------ users

    @Test
    void defaultUserIsAvailableAndUnknownUsersAreRejected() {
        S3User user = repo().getUser(ctx("GET", Collections.emptyMap(), null).callContext, TestContexts.ACCESS_KEY);
        assertNotNull(user, "the default demo user must resolve");
        assertEquals("IVimdW7BIQLq9PLyVpXzZUq8zS4nLfrsoiZSJanu", user.getSecretKey());

        assertThrows(InvalidAccessKeyIdException.class,
                () -> repo().getUser(ctx("GET", Collections.emptyMap(), null).callContext, "UNKNOWN-ACCESS-KEY"));
        assertThrows(InvalidAccessKeyIdException.class,
                () -> repo().getUser(ctx("GET", Collections.emptyMap(), null).callContext, null));

        repo().addUser("alice", "Alice A", "alice-access-key", "alice-secret-key");
        S3User alice = repo().getUser(ctx("GET", Collections.emptyMap(), null).callContext, "alice-access-key");
        assertEquals("alice", alice.getUserId());
        assertEquals("Alice A", alice.getUserName());
        assertEquals("alice-secret-key", alice.getSecretKey());
    }

    // ------------------------------------------------------------------ concurrency

    @Test
    void concurrentPutsAreSerializedAndNoVersionIsLost() throws Exception {
        createBucket("b");
        int threads = 5;
        int putsPerThread = 12;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger errors = new AtomicInteger();
        List<String> contents = Collections.synchronizedList(new ArrayList<>());
        for (int t = 0; t < threads; t++) {
            int thread = t;
            pool.execute(() -> {
                try {
                    start.await();
                    for (int p = 0; p < putsPerThread; p++) {
                        String content = "t" + thread + "-p" + p;
                        put("b", "hotkey", content);
                        contents.add(content);
                    }
                } catch (Exception e) {
                    errors.incrementAndGet();
                    e.printStackTrace();
                }
            });
        }
        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(60, TimeUnit.SECONDS), "all puts must finish");
        assertEquals(0, errors.get(), "no thread may fail");

        S3ListBucketResult versions = listVersions("b");
        assertEquals(threads * putsPerThread, versions.getObjects().size(), "every put must become a version");

        Set<String> ids = new HashSet<>();
        for (S3Object o : versions.getObjects()) {
            assertTrue(ids.add(o.getVersionId()), "version ids must be unique");
        }
        // the newest version must be readable
        S3Object latest = versions.getObjects().get(0);
        assertTrue(latest.isLatest());
        assertNotNull(get("b", "hotkey", latest.getVersionId()));
    }

    // ------------------------------------------------------------------ helpers

    private VFSRepository repo() {
        return repository;
    }

    private TestContexts.Context ctx(String method, java.util.Map<String, String> params, byte[] body) {
        return new TestContexts.Context(method, Collections.emptyMap(), params, body);
    }

    private S3ResponseHeaderImpl responseHeader(TestContexts.Context ctx) {
        return (S3ResponseHeaderImpl) ctx.responseHeader;
    }

    private void createBucket(String name) {
        repo().createBucket(ctx("PUT", Collections.emptyMap(), null).callContext, name, null);
    }

    private void put(String bucket, String key, String content) {
        TestContexts.Context put = ctx("PUT", Collections.emptyMap(), content.getBytes(StandardCharsets.UTF_8));
        repo().createObject(put.callContext, bucket, key);
    }

    private String get(String bucket, String key, String versionId) throws IOException {
        java.util.Map<String, String> params = new java.util.LinkedHashMap<>();
        if (versionId != null) {
            params.put("versionId", versionId);
        }
        TestContexts.Context get = ctx("GET", params, null);
        repo().getObject(get.callContext, bucket, key, false);
        return new String(get.writtenContent, StandardCharsets.UTF_8);
    }

    private S3ListBucketResult listVersions(String bucket) {
        return listVersions(bucket, Collections.singletonMap("versions", ""));
    }

    private S3ListBucketResult listVersions(String bucket, java.util.Map<String, String> params) {
        java.util.Map<String, String> all = new java.util.LinkedHashMap<>(params);
        all.putIfAbsent("versions", "");
        return repo().listBucket(ctx("GET", all, null).callContext, bucket);
    }

    private String headVersion(String bucket, String key) throws IOException {
        Path head = root.resolve("buckets").resolve(bucket).resolve("objects").resolve(keyHash(bucket, key)).resolve("head");
        return new String(Files.readAllBytes(head), StandardCharsets.UTF_8).trim();
    }

    private String secondNewestVersion(String bucket, String key) throws IOException {
        List<String> versions = versionIds(bucket, key);
        assertEquals(2, versions.size());
        // ids are monotonic: ascending sort == oldest first, so index 0 is the second newest
        return versions.get(0);
    }

    private List<String> versionIds(String bucket, String key) throws IOException {
        Path versionsDir = root.resolve("buckets").resolve(bucket).resolve("objects").resolve(keyHash(bucket, key)).resolve("versions");
        try (Stream<Path> stream = Files.list(versionsDir)) {
            return stream.map(p -> p.getFileName().toString()).sorted().collect(Collectors.toList());
        }
    }

    private static String keyHash(String bucket, String key) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return Encoding.toHex(md.digest((bucket + "\n" + key).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private long blobCount() throws IOException {
        Path data = root.resolve("data");
        if (!Files.isDirectory(data)) {
            return 0;
        }
        try (Stream<Path> stream = Files.list(data)) {
            return stream.filter(Files::isRegularFile).count();
        }
    }

    private List<String> files(String dir) {
        return Collections.singletonList(dir);
    }

    private java.util.Map<String, String> pageParams(String keyMarker, String versionIdMarker) {
        java.util.LinkedHashMap<String, String> params = new java.util.LinkedHashMap<>();
        params.put("versions", "");
        params.put("key-marker", keyMarker);
        params.put("version-id-marker", versionIdMarker);
        return params;
    }

    private void appendIds(List<String> out, List<S3Object> objects) {
        for (S3Object o : objects) {
            out.add(o.getKey() + ":" + o.getVersionId());
        }
    }

    private static void assertDoesNothing(Runnable r) {
        r.run();
    }

    private static void deleteRecursively(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
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
}
