/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.ladon.s3server;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.S3ClientOptions;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3VersionSummary;
import com.amazonaws.services.s3.model.VersionListing;
import de.mc.ladon.s3server.repository.api.S3Repository;
import de.mc.ladon.s3server.repository.vfs.VFSRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * T39: end-to-end integration test for the virtual file system repository.
 * The Spring context of this test uses the {@link VFSRepository} as the
 * {@code @Primary} S3Repository (the file-system repository from the production
 * configuration is backed out via {@code @ConditionalOnMissingBean}) and drives
 * the complete AWS versioning flow through the real HTTP stack with the
 * AWS SDK v1:
 *
 * <pre>
 * put v1 → put v2 → head is v2, v1 addressable → listVersions (order, isLatest)
 * → deduplication (shared blob) → delete (marker) → 404 → permanent delete of
 * the marker (revival) → permanent version deletes walking the history →
 * garbage collection → bucket deletion
 * </pre>
 *
 * @author Ralf Ulrich
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {S3ServerApplication.class, VFSStorageTests.VfsTestConfig.class},
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = "server.port=8081")
public class VFSStorageTests {

    static Path dataRoot;

    /**
     * Test configuration replacing the file system repository with a VFS
     * repository on a fresh temporary root.
     */
    @TestConfiguration
    public static class VfsTestConfig {

        @Bean
        @Primary
        public S3Repository vfsRepository() {
            try {
                dataRoot = Files.createTempDirectory("ladon-vfs-boot-test");
            } catch (Exception e) {
                throw new IllegalStateException("could not create temp data root", e);
            }
            return new VFSRepository(dataRoot);
        }
    }

    private AmazonS3Client client;
    private static List<AmazonS3Client> clients = new ArrayList<>();

    @BeforeEach
    public void setUp() {
        client = getClient();
    }

    @AfterEach
    public void tearDown() {
        clients.forEach(AmazonS3Client::shutdown);
        clients.clear();
    }

    @Test
    public void testVersionedVfsFlow() throws Exception {
        String bucket = UUID.randomUUID().toString();
        String key = "document.txt";
        client.createBucket(bucket);

        // ---- two versions of the same key ---------------------------------
        String versionOne = "version one";
        String versionTwo = "version two";
        String v1 = client.putObject(bucket, key, byteStream(versionOne), null).getVersionId();
        String v2 = client.putObject(bucket, key, byteStream(versionTwo), null).getVersionId();
        assertNotNull(v1, "the VFS repository must report the created version id");
        assertFalse(v2.equals(v1), "each put must produce its own version");

        // head (current) is v2; the older version stays addressable
        assertEquals(versionTwo, read(bucket, key, null));
        assertEquals(versionOne, read(bucket, key, v1));
        assertEquals(versionTwo, read(bucket, key, v2));

        // ---- listVersions: newest first, isLatest on the current one ------
        VersionListing listing = client.listVersions(bucket, key);
        assertEquals(2, listing.getVersionSummaries().size(), "both versions must be listed");
        S3VersionSummary newest = listing.getVersionSummaries().get(0);
        assertEquals(key, newest.getKey());
        assertEquals(v2, newest.getVersionId());
        assertTrue(newest.isLatest(), "the newest version is the current one");
        assertFalse(newest.isDeleteMarker());
        S3VersionSummary older = listing.getVersionSummaries().get(1);
        assertEquals(v1, older.getVersionId());
        assertFalse(older.isLatest());

        // ---- deduplication: same payload under a second key ----------------
        client.putObject(bucket, "twin.txt", byteStream(versionOne), null);
        assertEquals(2, blobCount(), "3 objects / 2 payloads → exactly two content blobs");

        // ---- plain delete = delete marker ---------------------------------
        client.deleteObject(bucket, key);
        AmazonS3Exception notFound = assertThrows(AmazonS3Exception.class,
                () -> client.getObject(new com.amazonaws.services.s3.model.GetObjectRequest(bucket, key)),
                "a marker-covered key answers 404");
        assertEquals("NoSuchKey", notFound.getErrorCode());

        VersionListing withMarker = client.listVersions(bucket, key);
        assertEquals(3, withMarker.getVersionSummaries().size(), "the delete marker is a new version");
        S3VersionSummary marker = withMarker.getVersionSummaries().get(0);
        assertTrue(marker.isDeleteMarker(), "the first (latest) entry is a delete marker");
        assertTrue(marker.isLatest(), "the marker is the current version");
        String markerId = marker.getVersionId();

        // ---- permanent delete of the marker revives the latest object -----
        client.deleteVersion(bucket, key, markerId);
        assertEquals(versionTwo, read(bucket, key, null), "the previous version is current again");

        // ---- permanent deletes walk the version history --------------------
        client.deleteVersion(bucket, key, v2);
        assertEquals(versionOne, read(bucket, key, null), "v1 is the new current version");

        client.deleteVersion(bucket, key, v1);
        AmazonS3Exception gone = assertThrows(AmazonS3Exception.class,
                () -> client.getObject(new com.amazonaws.services.s3.model.GetObjectRequest(bucket, key)),
                "no version left → NoSuchKey");
        assertEquals("NoSuchKey", gone.getErrorCode());

        // v2's blob was swept by the collector, the shared blob survives the twin
        assertEquals(1, blobCount(), "v2's blob must be swept, the shared blob must survive the twin");

        // ---- twin: marker, then full removal -------------------------------
        S3VersionSummary twin = findEntry(client.listVersions(bucket, null), "twin.txt");
        assertNotNull(twin, "the twin must still be listed");
        client.deleteObject(bucket, "twin.txt");
        S3VersionSummary twinMarker = latest(client.listVersions(bucket, null));
        assertTrue(twinMarker.isDeleteMarker());
        client.deleteVersion(bucket, "twin.txt", twinMarker.getVersionId());
        assertEquals(versionOne, read(bucket, "twin.txt", null), "removing the marker revives the twin");
        client.deleteVersion(bucket, "twin.txt", twin.getVersionId());
        assertTrue(client.listVersions(bucket, null).getVersionSummaries().isEmpty(),
                "all versions of the twin are permanently deleted");
        assertEquals(0, blobCount(), "the last unreferenced blob must be collected");

        // ---- bucket lifecycle ----------------------------------------------
        client.deleteBucket(bucket);
        AmazonS3Exception noBucket = assertThrows(AmazonS3Exception.class,
                () -> client.headBucket(
                        new com.amazonaws.services.s3.model.HeadBucketRequest(bucket)));
        assertEquals(404, noBucket.getStatusCode(), "HEAD has no body → the status code carries the fact");
        List<String> names = new ArrayList<>();
        for (Bucket b : client.listBuckets()) {
            names.add(b.getName());
        }
        assertFalse(names.contains(bucket), "the bucket must be gone from the account");
    }

    // ------------------------------------------------------------------ helpers

    private static S3VersionSummary latest(VersionListing listing) {
        for (S3VersionSummary s : listing.getVersionSummaries()) {
            if (s.isLatest()) {
                return s;
            }
        }
        throw new AssertionError("expected a latest version entry, got " + listing.getVersionSummaries());
    }

    private static S3VersionSummary findEntry(VersionListing listing, String key) {
        for (S3VersionSummary s : listing.getVersionSummaries()) {
            if (key.equals(s.getKey())) {
                return s;
            }
        }
        return null;
    }

    private String read(String bucket, String key, String versionId) throws Exception {
        com.amazonaws.services.s3.model.GetObjectRequest request =
                new com.amazonaws.services.s3.model.GetObjectRequest(bucket, key);
        if (versionId != null) {
            request.withVersionId(versionId);
        }
        S3Object obj = client.getObject(request);
        try (InputStream in = obj.getObjectContent()) {
            return new String(in.readAllBytes());
        }
    }

    private static ByteArrayInputStream byteStream(String content) {
        return new ByteArrayInputStream(content.getBytes());
    }

    private static long blobCount() throws Exception {
        Path data = dataRoot.resolve("data");
        if (!Files.isDirectory(data)) {
            return 0;
        }
        long n;
        try (var s = Files.list(data)) {
            n = s.filter(Files::isRegularFile).count();
        }
        return n;
    }

    public static AmazonS3Client getClient() {
        AWSCredentials credentials = new BasicAWSCredentials(
                "rHUYeAk58Ilhg6iUEFtr",
                "IVimdW7BIQLq9PLyVpXzZUq8zS4nLfrsoiZSJanu");
        AmazonS3Client newClient = new AmazonS3Client(credentials, new ClientConfiguration());
        newClient.setS3ClientOptions(new S3ClientOptions().withPathStyleAccess(true));
        newClient.setEndpoint("http://localhost:8081/api/s3");
        clients.add(newClient);
        return newClient;
    }

}
