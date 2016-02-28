/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.repository.api;

import de.mc.s3server.entities.api.*;
import de.mc.s3server.jaxb.entities.CreateBucketConfiguration;

import java.util.List;

/**
 * This is the abstraction interface to implement your own repository
 *
 * @author Ralf Ulrich on 17.02.16.
 */
public interface S3Repository {

    List<S3Bucket> listAllBuckets(S3CallContext callContext);

    void createBucket(S3CallContext callContext, String bucketName, CreateBucketConfiguration configuration);

    void deleteBucket(S3CallContext callContext, String bucketName);

    void createObject(S3CallContext callContext, String bucketName, String objectKey);

    void getObject(S3CallContext callContext, String bucketName, String objectKey, boolean head);

    S3ListBucketResult listBucket(S3CallContext callContext, String bucketName);

    void deleteObject(S3CallContext callContext, String bucketName, String objectKey);

    S3UserMetadata getObjectMetadata(S3CallContext callContext, String bucketName, String objectKey);

    void getBucket(S3CallContext callContext, String bucketName);
}
