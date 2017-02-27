/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.ladon.s3server.logging;

import de.mc.ladon.s3server.entities.api.*;
import de.mc.ladon.s3server.repository.api.S3Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Repository delegate to log all repository calls
 *
 * @author Ralf Ulrich on 18.03.16.
 */
public class LoggingRepository implements S3Repository {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final S3Repository delegate;

    public LoggingRepository(S3Repository delegate) {
        this.delegate = delegate;
    }


    @Override
    public List<S3Bucket> listAllBuckets(S3CallContext callContext) {
        logger.info("LIST BUCKETS :: " + callContext);
        return delegate.listAllBuckets(callContext);
    }

    @Override
    public void createBucket(S3CallContext callContext, String bucketName, String locationConstraint) {
        logger.info("CREATE BUCKET :: " + bucketName + " - " + locationConstraint + " " + callContext);
        delegate.createBucket(callContext, bucketName, locationConstraint);
    }

    @Override
    public void deleteBucket(S3CallContext callContext, String bucketName) {
        logger.info("DELETE BUCKET :: " + bucketName + " " + callContext);
        delegate.deleteBucket(callContext, bucketName);
    }

    @Override
    public void createObject(S3CallContext callContext, String bucketName, String objectKey) {
        logger.info("CREATE OBJECT :: " + bucketName + " / " + objectKey + " " + callContext);
        delegate.createObject(callContext, bucketName, objectKey);
    }

    @Override
    public S3Object copyObject(S3CallContext callContext, String srcBucket, String srcObjectKey, String destBucket, String destObjectKey, boolean copyMetadata) {
        logger.info("COPY OBJECT :: " + srcBucket + " / " + srcObjectKey + " -> " + destBucket + " / " + destObjectKey + " , meta " + copyMetadata + " " + callContext);
        return delegate.copyObject(callContext, srcBucket, srcObjectKey, destBucket, destObjectKey, copyMetadata);
    }

    @Override
    public void getObject(S3CallContext callContext, String bucketName, String objectKey, boolean head) {
        logger.info("GET OBJECT :: " + bucketName + " / " + objectKey + " " + callContext);
        delegate.getObject(callContext, bucketName, objectKey, head);
    }

    @Override
    public S3ListBucketResult listBucket(S3CallContext callContext, String bucketName) {
        logger.info("LIST BUCKET :: " + bucketName + " " + callContext);
        return delegate.listBucket(callContext, bucketName);
    }

    @Override
    public void deleteObject(S3CallContext callContext, String bucketName, String objectKey) {
        logger.info("DELETE OBJECT :: " + bucketName + " / " + objectKey + " " + callContext);
        delegate.deleteObject(callContext, bucketName, objectKey);
    }

    @Override
    public void getBucket(S3CallContext callContext, String bucketName) {
        logger.info("GET BUCKET :: " + bucketName + " " + callContext);
        delegate.getBucket(callContext, bucketName);
    }

    @Override
    public S3User getUser(S3CallContext callContext, String accessKey) {
        logger.info("GET USER :: " + accessKey + " " + callContext);
        return delegate.getUser(callContext, accessKey);
    }
}
