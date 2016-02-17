/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.repository.api;

import de.mc.s3server.entities.api.*;

import java.io.InputStream;
import java.util.List;

/**
 * Created by max on 17.02.16.
 */
public interface Repository {

    List<S3Bucket> listAllBuckets(S3CallContext callContext);

    void createBucket(S3CallContext callContext, S3Bucket bucket);

    void updateBucket(S3CallContext callContext, S3Bucket bucket);

    S3Bucket getBucket(S3CallContext callContext, String bucketName);

    void deleteBucket(S3CallContext callContext, String bucketName);

    void createObject(S3CallContext callContext, String bucketName, S3Object object);

    void updateObject(S3CallContext callContext, String bucketName, S3Object object);

    void putContent(S3CallContext callContext, String bucketNAme, String objectKey, InputStream contentStream);

    S3Object getObject(S3CallContext callContext, String bucketName, String objectKey);

    void deleteObject(S3CallContext callContext, String bucketName, String objectKey);

    S3Metadata getObjectMetadata(S3CallContext callContext, String bucketName, String objectKey);

    void setBucketACL(S3CallContext callContext, String buckeName, S3ACL bucketACL);

    S3ACL getBucketACL(S3CallContext callContext, String bucketName);

    void setObjectACL(S3CallContext callContext, String bucketName, String objectKey);

    S3ACL getObjectACL(S3CallContext callContext, String bucketName, String objectKey);


}
