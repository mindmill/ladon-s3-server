/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.S3ClientOptions;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import org.apache.tomcat.util.http.fileupload.util.Streams;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

/**
 * Example Integration Test
 *
 * @author Ralf Ulrich
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = SimpleS3ServerApplication.class)
@WebAppConfiguration
@IntegrationTest("server.port:8080")
public class FSStorageTests {

    private ExecutorService service;

    @Before
    public void setUp() {
        service = Executors.newFixedThreadPool(3);
    }


    @Test
    public void testFSLockConcurrentAccess() throws IOException, InterruptedException {
        AmazonS3Client client1 = getClient();
        AmazonS3Client client2 = getClient();
        AmazonS3Client client3 = getClient();

        ObjectMetadata meta1 = new ObjectMetadata();
        meta1.addUserMetadata("test", "1111111111");
        ObjectMetadata meta2 = new ObjectMetadata();
        meta1.addUserMetadata("test", "2222222222");
        ObjectMetadata meta3 = new ObjectMetadata();
        meta1.addUserMetadata("test", "3333333333");
        Bucket b = client1.createBucket(UUID.randomUUID().toString());

        service.execute(() -> {
            for (int i = 0; i < 100; i++) {
                client1.putObject(b.getName(), "test.txt", new ByteArrayInputStream("1111111111".getBytes()), meta1);
                try {
                    if (i > 0)
                        assertEquals(10, Streams.asString(client1.getObject(b.getName(), "test.txt").getObjectContent()).chars().count());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
//        service.execute(() -> {
//            for (int i = 0; i < 100; i++) {
//                client2.putObject(b.getName(), "test.txt", new ByteArrayInputStream("2222222222".getBytes()), meta2);
//                try {
//                    if (i > 0)
//                        assertEquals(10, Streams.asString(client2.getObject(b.getName(), "test.txt").getObjectContent()).chars().count());
//                } catch (IOException e) {
//                    throw new RuntimeException(e);
//                }
//            }
//        });
//
//        service.execute(() -> {
//            for (int i = 0; i < 100; i++) {
//                client3.putObject(b.getName(), "test.txt", new ByteArrayInputStream("3333333333".getBytes()), meta3);
//                try {
//                    if (i > 0)
//                        assertEquals(10, Streams.asString(client3.getObject(b.getName(), "test.txt").getObjectContent()).chars().count());
//                } catch (IOException e) {
//                    throw new RuntimeException(e);
//                }
//            }
//        });
        service.shutdown();
        service.awaitTermination(1, TimeUnit.MINUTES);
        S3Object object1 = client1.getObject(b.getName(), "test.txt");
        String content = Streams.asString(object1.getObjectContent());
        String meta = object1.getObjectMetadata().getUserMetadata().get("test");

        assertEquals(1, content.chars().distinct().count());
        assertEquals(1, meta.chars().distinct().count());
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
