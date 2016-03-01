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
import java.util.Random;
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
@SpringApplicationConfiguration(classes = S3ServerApplication.class)
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


        ObjectMetadata meta1 = new ObjectMetadata();
        meta1.addUserMetadata("test", "1111111111");
        Bucket b = getClient().createBucket(UUID.randomUUID().toString());

        for (int j = 0; j < 10; j++) {
            final int finalJ = j;
            executeOnBucket(meta1, b, finalJ,"1111111111");
            executeOnBucket(meta1, b, finalJ,"2222222222");
            executeOnBucket(meta1, b, finalJ,"3333333333");
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
            Random random  = new Random(System.currentTimeMillis());
            for (int i = 0; i < 10; i++) {
                c.putObject(b.getName(), "test"+ finalJ +".txt", new ByteArrayInputStream(content.getBytes()), meta1);
                try {
                    Thread.sleep(random.nextInt(10));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                try {
                    if (i > 0)
                        assertEquals(10, Streams.asString(c.getObject(b.getName(), "test"+ finalJ +".txt").getObjectContent()).chars().count());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
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
