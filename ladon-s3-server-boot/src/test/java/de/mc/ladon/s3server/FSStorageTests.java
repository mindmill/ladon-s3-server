/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.ladon.s3server;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.S3ClientOptions;
import com.amazonaws.services.s3.model.*;
import org.apache.tomcat.util.http.fileupload.util.Streams;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * Example Integration Test
 *
 * @author Ralf Ulrich
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = S3ServerApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class FSStorageTests {

    private ExecutorService service;

    @BeforeEach
    public void setUp() {
        service = Executors.newFixedThreadPool(3);
    }


    @Test
    public void testFSLockConcurrentAccess() throws IOException, InterruptedException {


        ObjectMetadata meta1 = new ObjectMetadata();
        meta1.addUserMetadata("test", "1111111111");
        Bucket b = getClient().createBucket(UUID.randomUUID().toString());

        for (int j = 0; j < 10; j++) {
            final int finalJ = j;
            executeOnBucket(meta1, b, finalJ, "1111111111");
            executeOnBucket(meta1, b, finalJ, "2222222222");
            executeOnBucket(meta1, b, finalJ, "3333333333");
        }


        service.shutdown();
        service.awaitTermination(1, TimeUnit.MINUTES);
        S3Object object1 = getClient().getObject(b.getName(), "test1.txt");
        String content = Streams.asString(object1.getObjectContent());
        String meta = object1.getObjectMetadata().getUserMetadata().get("test");

        assertEquals(1, content.chars().distinct().count());
        assertEquals(1, meta.chars().distinct().count());
    }

    private void executeOnBucket(ObjectMetadata meta1, Bucket b, int finalJ, String content) {
        service.execute(() -> {
            AmazonS3Client c = getClient();
            Random random = new Random(System.currentTimeMillis());
            for (int i = 0; i < 10; i++) {
                c.putObject(b.getName(), "test" + finalJ + ".txt", new ByteArrayInputStream(content.getBytes()), meta1);
                try {
                    Thread.sleep(random.nextInt(10));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                try {
                    if (i > 0)
                        assertEquals(10, Streams.asString(c.getObject(b.getName(), "test" + finalJ + ".txt").getObjectContent()).chars().count());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }


    public static void main(String[] args) {
        AmazonS3Client client = getClient();
        int count = 0;
        ObjectListing listing = client.listObjects("test");
        count += listing.getObjectSummaries().size();
        printListing(listing);
        while (listing.isTruncated()) {
            listing = client.listNextBatchOfObjects(listing);
            count += listing.getObjectSummaries().size();
            printListing(listing);
        }

        System.out.println(count);

    }

    private static void printListing(ObjectListing listing) {
        for (S3ObjectSummary next : listing.getObjectSummaries()) {
            System.out.println(next.getKey());
        }
    }

    public static AmazonS3Client getClient() {
        AWSCredentials credentials = new BasicAWSCredentials(
                "rHUYeAk58Ilhg6iUEFtr",
                "IVimdW7BIQLq9PLyVpXzZUq8zS4nLfrsoiZSJanu");
        AmazonS3Client newClient = new AmazonS3Client(credentials,
                new ClientConfiguration());
        newClient.setS3ClientOptions(new S3ClientOptions().withPathStyleAccess(true));
        newClient.setEndpoint("http://localhost:8080/api/s3");
        return newClient;
    }

}
