/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.ladon.s3server.entities.impl;

import de.mc.ladon.s3server.entities.api.S3ListBucketResult;
import de.mc.ladon.s3server.entities.api.S3Object;

import java.util.List;

/**
 * @author Ralf Ulrich on 21.02.16.
 */
public class S3ListBucketResultImpl implements S3ListBucketResult {

    private boolean truncated;
    private String bucketName;
    private List<S3Object> objects;

    public S3ListBucketResultImpl(boolean truncated, String bucketName, List<S3Object> objects) {
        this.truncated = truncated;
        this.bucketName = bucketName;
        this.objects = objects;
    }

    @Override
    public String getBucketName() {
        return bucketName;
    }

    @Override
    public boolean isTruncated() {
        return truncated;
    }

    @Override
    public List<S3Object> getObjects() {
        return objects;
    }
}
