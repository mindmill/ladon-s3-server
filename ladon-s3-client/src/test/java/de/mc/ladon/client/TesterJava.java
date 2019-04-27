package de.mc.ladon.client;

import java.util.UUID;

public class TesterJava {

    public static void main(String[] args) {
        LadonS3Client client = new LadonS3Client("http://localhost:8080/api/s3/", "system", "system");

        String b = UUID.randomUUID().toString();
        client.createBucket(b);
      //  System.out.println(client.listBuckets());
      //  System.out.println(client.listObjects(b));
       // System.out.println(client.listVersions(b));

        client.deleteBucket(b);
    }
}
