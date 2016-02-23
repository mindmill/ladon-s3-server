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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.util.List;

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
        System.out.println(client.listBuckets());

        client.createBucket("test");

        List<Bucket> buckets = client.listBuckets();
        buckets.forEach(b ->
                System.out.println(client.listObjects(b.getName()))
        );
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
