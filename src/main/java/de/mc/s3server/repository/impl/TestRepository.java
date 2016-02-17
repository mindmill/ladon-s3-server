/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.repository.impl;

import de.mc.s3server.entities.api.*;
import de.mc.s3server.entities.impl.S3BucketImpl;
import de.mc.s3server.repository.api.Repository;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by max on 17.02.16.
 */
@org.springframework.stereotype.Repository
public class TestRepository implements Repository {

    private Map<String, List<S3Bucket>> buckets = new HashMap<>();

    public TestRepository() {
        List<S3Bucket> testBuckets = new ArrayList<>();
        testBuckets.add(new S3BucketImpl("testbucket"));
        testBuckets.add(new S3BucketImpl("testbucket2"));
        testBuckets.add(new S3BucketImpl("testbucket3"));


        buckets.put("max", testBuckets);
    }

    @Override
    public List<S3Bucket> listAllBuckets(S3CallContext callContext) {
        return buckets.get("max");
    }

    @Override
    public void createBucket(S3CallContext callContext, S3Bucket bucket) {

    }

    @Override
    public void updateBucket(S3CallContext callContext, S3Bucket bucket) {

    }

    @Override
    public S3Bucket getBucket(S3CallContext callContext, String bucketName) {
        return null;
    }

    @Override
    public void deleteBucket(S3CallContext callContext, String bucketName) {

    }

    @Override
    public void createObject(S3CallContext callContext, String bucketName, S3Object object) {

    }

    @Override
    public void updateObject(S3CallContext callContext, String bucketName, S3Object object) {

    }

    @Override
    public void putContent(S3CallContext callContext, String bucketNAme, String objectKey, InputStream contentStream) {

    }

    @Override
    public S3Object getObject(S3CallContext callContext, String bucketName, String objectKey) {
        return null;
    }

    @Override
    public void deleteObject(S3CallContext callContext, String bucketName, String objectKey) {

    }

    @Override
    public S3Metadata getObjectMetadata(S3CallContext callContext, String bucketName, String objectKey) {
        return null;
    }

    @Override
    public void setBucketACL(S3CallContext callContext, String buckeName, S3ACL bucketACL) {

    }

    @Override
    public S3ACL getBucketACL(S3CallContext callContext, String bucketName) {
        return null;
    }

    @Override
    public void setObjectACL(S3CallContext callContext, String bucketName, String objectKey) {

    }

    @Override
    public S3ACL getObjectACL(S3CallContext callContext, String bucketName, String objectKey) {
        return null;
    }
}
