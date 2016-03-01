/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server;

import com.amazonaws.AmazonClientException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.S3ClientOptions;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.tomcat.util.http.fileupload.util.Streams;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Example Integration Test
 *
 * @author Ralf Ulrich
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = SimpleS3ServerApplication.class)
@WebAppConfiguration
@IntegrationTest("server.port:8080")
public class SimpleS3ServerApplicationTests {

    @Test
    public void contextLoads() {

    }


    @Test
    public void testListBuckets() {
        AmazonS3Client client = getClient();
        client.createBucket("test");
        List<Bucket> buckets = client.listBuckets();
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
        Bucket b = client.createBucket(UUID.randomUUID().toString());
        client.putObject(b.getName(), "test.txt", new ByteArrayInputStream("test".getBytes()), new ObjectMetadata());
        ObjectListing listing = client.listObjects(b.getName());
        assertEquals(1, listing.getObjectSummaries().size());
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
        Bucket b = client.createBucket(UUID.randomUUID().toString());
        client.putObject(b.getName(), "test.txt", new ByteArrayInputStream("test".getBytes()), new ObjectMetadata());
        int count = client.listBuckets().size();
        try {
            client.deleteBucket(b.getName());
        } catch (AmazonS3Exception e) {
            assertEquals(409, e.getStatusCode());
        }
        assertEquals(count, client.listBuckets().size());

    }

    @Test
    public void testGetObject() throws IOException {
        AmazonS3Client client = getClient();
        Bucket b = client.createBucket(UUID.randomUUID().toString());
        client.putObject(b.getName(), "test.txt", new ByteArrayInputStream("test".getBytes()), new ObjectMetadata());
        InputStream in = client.getObject(b.getName(), "test.txt").getObjectContent();
        assertEquals("test", Streams.asString(in));
    }

    @Test
    public void testPutObjectNoMd5NoLength() throws IOException {
        AmazonS3Client client = getClient();
        Bucket b = client.createBucket(UUID.randomUUID().toString());
        ObjectMetadata meta = new ObjectMetadata();
        client.putObject(b.getName(), "test.txt", new ByteArrayInputStream("test".getBytes()), meta);

    }

    @Test(expected = AmazonS3Exception.class)
    public void testPutObjectWrongMd5() throws IOException {
        AmazonS3Client client = getClient();
        Bucket b = client.createBucket(UUID.randomUUID().toString());
        String md5 = Base64.encodeBase64String(DigestUtils.md5("test1"));
        ObjectMetadata meta = new ObjectMetadata();
        meta.setContentMD5(md5);
        client.putObject(b.getName(), "test.txt", new ByteArrayInputStream("test".getBytes()), meta);

    }

    @Test(expected = AmazonClientException.class)
    public void testPutObjectWrongLengthRightMd5() throws IOException {
        AmazonS3Client client = getClient();
        Bucket b = client.createBucket(UUID.randomUUID().toString());
        String md5 = Base64.encodeBase64String(DigestUtils.md5("test"));
        ObjectMetadata meta = new ObjectMetadata();
        meta.setContentMD5(md5);
        meta.setContentLength(5);
        client.putObject(b.getName(), "test.txt", new ByteArrayInputStream("test".getBytes()), meta);

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


    public AmazonS3Client getClient() {
        AWSCredentials credentials = new BasicAWSCredentials("TESTING", "TESTING");
        AmazonS3Client newClient = new AmazonS3Client(credentials,
                new ClientConfiguration());
        newClient.setS3ClientOptions(new S3ClientOptions().withPathStyleAccess(true));
        newClient.setEndpoint("http://localhost:8080/api/s3");
        return newClient;
    }

}
