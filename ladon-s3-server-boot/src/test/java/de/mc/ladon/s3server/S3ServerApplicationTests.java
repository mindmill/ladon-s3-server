/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.ladon.s3server;

import com.amazonaws.AmazonClientException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.S3ClientOptions;
import com.amazonaws.services.s3.model.*;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.tomcat.util.http.fileupload.util.Streams;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Example Integration Test
 *
 * @author Ralf Ulrich
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = S3ServerApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class S3ServerApplicationTests {

    @BeforeEach
    public void setUp() {
        AmazonS3Client client = getClient();
        client.createBucket("test");
        if (client.listObjects("test").getObjectSummaries().size() == 0) {
            for (int i = 0; i < 50; i++) {
                client.putObject("test", "test" + i + ".txt", new ByteArrayInputStream(("test" + i).getBytes()), new ObjectMetadata());
            }
        }
    }

    public AmazonS3Client getClient() {
        AWSCredentials credentials = new BasicAWSCredentials("rHUYeAk58Ilhg6iUEFtr", "IVimdW7BIQLq9PLyVpXzZUq8zS4nLfrsoiZSJanu");
        AmazonS3Client newClient = new AmazonS3Client(credentials,
                new ClientConfiguration());
        newClient.setS3ClientOptions(new S3ClientOptions().withPathStyleAccess(true));
        newClient.setEndpoint("http://localhost:8080/api/s3");
        return newClient;
    }

    @Test
    public void contextLoads() {

    }


    @Test
    public void testListBuckets() {
        List<Bucket> buckets = getClient().listBuckets();
        assertTrue(buckets.size() >= 1);
    }

    @Test
    public void testListEmptyBucket() {
        AmazonS3Client client = getClient();
        Bucket b = client.createBucket(UUID.randomUUID().toString());
        ObjectListing listing = client.listObjects(b.getName());
        assertEquals(0, listing.getObjectSummaries().size());
    }

    @Test
    public void testListNonEmptyBucket() {
        AmazonS3Client client = getClient();
        ObjectListing listing = client.listObjects("test");
        assertEquals(50, listing.getObjectSummaries().size());
    }

    @Test
    public void testDeleteEmptyBucket() {
        AmazonS3Client client = getClient();
        Bucket b = client.createBucket(UUID.randomUUID().toString());
        int count = client.listBuckets().size();
        ObjectListing listing = client.listObjects(b.getName());
        assertEquals(0, listing.getObjectSummaries().size());
        client.deleteBucket(b.getName());
        assertEquals(count - 1, client.listBuckets().size());
    }


    @Test
    public void testDeleteNonEmptyBucket() {
        AmazonS3Client client = getClient();
        int count = client.listBuckets().size();
        try {
            client.deleteBucket("test");
        } catch (AmazonS3Exception e) {
            assertEquals(409, e.getStatusCode());
        }
        assertEquals(count, client.listBuckets().size());

    }

    @Test
    public void testGetObject() throws IOException {
        AmazonS3Client client = getClient();
        InputStream in = client.getObject("test", "test1.txt").getObjectContent();
        String bucketName =  client.listObjects("test", "test1.txt").getObjectSummaries().get(0).getBucketName();
        assertEquals("test1", Streams.asString(in));
        assertEquals("test", bucketName);
    }

    @Test
    public void testPutObjectNoMd5NoLength() throws IOException {
        AmazonS3Client client = getClient();
        Bucket b = client.createBucket(UUID.randomUUID().toString());
        ObjectMetadata meta = new ObjectMetadata();
        client.putObject(b.getName(), "test.txt", new ByteArrayInputStream("test".getBytes()), meta);

    }

    @Test
    @Disabled
    public void testPutBigObjectAndVerifyGet() throws IOException {
        final long TEST_LENGTH = 1024 * 1024 * 1024 * 6L; // 6 GB

        AmazonS3Client client = getClient();
        Bucket b = client.createBucket(UUID.randomUUID().toString());
        ObjectMetadata meta = new ObjectMetadata();
        meta.setContentLength(TEST_LENGTH);

        class BigInput extends InputStream {
            final long l;
            long counter = 0L;

            BigInput(long length) {
                l = length;
            }

            @Override
            public int read() throws IOException {
                return counter++ >= l ? -1 : 'M';
            }
        }
        client.putObject(b.getName(), "test.txt", new BigInput(TEST_LENGTH), meta);

        try (S3ObjectInputStream content = client.getObject(b.getName(), "test.txt").getObjectContent()) {
            long readCounter = 0L;
            int byteRead = 0;

            while ((byteRead = content.read()) != -1) {
                assertEquals('M', byteRead);
                readCounter++;
            }
            assertEquals(TEST_LENGTH, readCounter);
        } catch (AmazonClientException e) {
            e.printStackTrace();
        }

    }

    @Test
    public void testCopyObject() {
        AmazonS3Client client = getClient();
        Bucket b = client.createBucket(UUID.randomUUID().toString());
        ObjectMetadata meta = new ObjectMetadata();
        client.putObject(b.getName(), "test.txt", new ByteArrayInputStream("test".getBytes()), meta);
        client.copyObject(b.getName(), "test.txt", b.getName(), "test2.txt");

    }

    @Test
    public void testPutObjectWrongMd5() {
        Assertions.assertThrows(AmazonS3Exception.class, () -> {
            AmazonS3Client client = getClient();
            Bucket b = client.createBucket(UUID.randomUUID().toString());
            String md5 = Base64.encodeBase64String(DigestUtils.md5("test1"));
            ObjectMetadata meta = new ObjectMetadata();
            meta.setContentMD5(md5);
            client.putObject(b.getName(), "test.txt", new ByteArrayInputStream("test".getBytes()), meta);
        });

    }

    @Test
    public void testPutObjectWrongLengthRightMd5() throws IOException {
        Assertions.assertThrows(AmazonClientException.class, () -> {
            AmazonS3Client client = getClient();
            Bucket b = client.createBucket(UUID.randomUUID().toString());
            String md5 = Base64.encodeBase64String(DigestUtils.md5("test"));
            ObjectMetadata meta = new ObjectMetadata();
            meta.setContentMD5(md5);
            meta.setContentLength(5);
            client.putObject(b.getName(), "test.txt", new ByteArrayInputStream("test".getBytes()), meta);

        });
    }

    @Test
    public void testPutObjectWithMd5AndLength() throws IOException {
        AmazonS3Client client = getClient();
        Bucket b = client.createBucket(UUID.randomUUID().toString());
        ObjectMetadata meta = new ObjectMetadata();
        String md5 = Base64.encodeBase64String(DigestUtils.md5("test"));
        meta.setContentMD5(md5);
        meta.setContentLength(4);
        client.putObject(b.getName(), "test.txt", new ByteArrayInputStream("test".getBytes()), meta);
    }

    @Test
    public void testPutObjectWithoutAndLength() throws IOException {
        AmazonS3Client client = getClient();
        Bucket b = client.createBucket(UUID.randomUUID().toString());
        ObjectMetadata meta = new ObjectMetadata();
        meta.setContentLength(4);
        client.putObject(b.getName(), "test.txt", new ByteArrayInputStream("test".getBytes()), meta);
    }

    @Test
    public void testPutObjectMeta() throws IOException {
        AmazonS3Client client = getClient();
        Bucket b = client.createBucket(UUID.randomUUID().toString());
        ObjectMetadata meta = new ObjectMetadata();
        meta.addUserMetadata("peter", "Lustig");
        client.putObject(b.getName(), "test.txt", new ByteArrayInputStream("test".getBytes()), meta);
        InputStream in = client.getObject(b.getName(), "test.txt").getObjectContent();
        assertEquals("test", Streams.asString(in));
        assertEquals("Lustig", client.getObject(b.getName(), "test.txt").getObjectMetadata().getUserMetadata().get("peter"));
    }

    @Test
    public void testMaxKeys() {
        assertEquals(10, getClient().listObjects(new ListObjectsRequest("test", null, null, null, 10)).getObjectSummaries().size());
        assertEquals(20, getClient().listObjects(new ListObjectsRequest("test", null, null, null, 20)).getObjectSummaries().size());
    }

    @Test
    public void testPrefix() {
        List<S3ObjectSummary> objectSummaries = getClient().listObjects(new ListObjectsRequest("test", "test10.txt", null, null, null)).getObjectSummaries();
        S3ObjectSummary txt10 = objectSummaries.get(0);
        assertEquals("test10.txt", txt10.getKey());
        assertEquals(1, objectSummaries.size());
    }

    @Test
    public void testMarker() {
        List<S3ObjectSummary> objectSummaries = getClient().listObjects(new ListObjectsRequest("test", null, "test10.txt", null, null)).getObjectSummaries();
        S3ObjectSummary txt11 = objectSummaries.get(0);
        S3ObjectSummary txt19 = objectSummaries.get(8);
        assertEquals("test11.txt", txt11.getKey());
        assertEquals("test19.txt", txt19.getKey());

    }


    @Test
    public void testDelimiter() {
        AmazonS3Client client = getClient();
        String bucket = UUID.randomUUID().toString();
        client.createBucket(bucket);
        List<S3ObjectSummary> objectSummaries = client.listObjects(new ListObjectsRequest(bucket, null, null, null, null)).getObjectSummaries();
        assertEquals(0, objectSummaries.size());

        ObjectMetadata meta = new ObjectMetadata();
        client.putObject(bucket, "one/one.txt", new ByteArrayInputStream("test".getBytes()), meta);
        client.putObject(bucket, "two/two.txt", new ByteArrayInputStream("test".getBytes()), meta);
        client.putObject(bucket, "three/four/four.txt", new ByteArrayInputStream("test".getBytes()), meta);

        objectSummaries = client.listObjects(new ListObjectsRequest(bucket, null, null, null, null)).getObjectSummaries();
        assertEquals(3, objectSummaries.size());


        ObjectListing ol = client.listObjects(new ListObjectsRequest(bucket, null, null, "/", null));
        assertEquals(0, ol.getObjectSummaries().size());
        System.out.println(ol.getCommonPrefixes());
        assertEquals(3, ol.getCommonPrefixes().size());

    }


}
