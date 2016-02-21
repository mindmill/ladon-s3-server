/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.repository.api;

import de.mc.s3server.entities.api.*;

import java.util.List;

/**
 * Created by Ralf Ulrich on 17.02.16.
 */
public interface S3Repository {

    List<S3Bucket> listAllBuckets(S3CallContext callContext);

    void createBucket(S3CallContext callContext, S3Bucket bucket);

    void updateBucket(S3CallContext callContext, S3Bucket bucket);

    void deleteBucket(S3CallContext callContext, String bucketName);

    void createObject(S3CallContext callContext, String bucketName, S3Object object);

    void updateObject(S3CallContext callContext, String bucketName, S3Object object);

    S3Object getObject(S3CallContext callContext, String bucketName, String objectKey);

    S3ListBucketResult listBucket(S3CallContext callContext, String bucketName);

    void deleteObject(S3CallContext callContext, String bucketName, String objectKey);

    S3Metadata getObjectMetadata(S3CallContext callContext, String bucketName, String objectKey);

}
