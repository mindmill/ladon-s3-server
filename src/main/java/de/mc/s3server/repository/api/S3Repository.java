/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.repository.api;

import de.mc.s3server.jaxb.entities.CreateBucketConfiguration;
import de.mc.s3server.entities.api.*;

import java.util.List;

/**
 * This is the abstraction interface to implement your own repository
 *
 * @author Ralf Ulrich on 17.02.16.
 */
public interface S3Repository {

    List<S3Bucket> listAllBuckets(S3CallContext callContext);

    void createBucket(S3CallContext callContext, String bucketName, CreateBucketConfiguration configuration);

    void updateBucket(S3CallContext callContext, S3Bucket bucket);

    void deleteBucket(S3CallContext callContext, String bucketName);

    void createObject(S3CallContext callContext, String bucketName, String objectKey);

    void updateObject(S3CallContext callContext, String bucketName, S3Object object);

    S3Object getObject(S3CallContext callContext, String bucketName, String objectKey);

    S3ListBucketResult listBucket(S3CallContext callContext, String bucketName);

    void deleteObject(S3CallContext callContext, String bucketName, String objectKey);

    S3UserMetadata getObjectMetadata(S3CallContext callContext, String bucketName, String objectKey);

}
