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
    private String nextKeyMarker;
    private String nextVersionIdMarker;

    public S3ListBucketResultImpl(List<S3Object> objects,
                                  boolean truncated,
                                  String bucketName,
                                  String nextKeyMarker,
                                  String nextVersionIdMarker) {
        this.truncated = truncated;
        this.bucketName = bucketName;
        this.nextKeyMarker = nextKeyMarker;
        this.nextVersionIdMarker = nextVersionIdMarker;
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
    public String nextKeyMarker() {
        return nextKeyMarker;
    }

    @Override
    public String nextVersionIdMarker() {
        return nextVersionIdMarker;
    }

    @Override
    public List<S3Object> getObjects() {
        return objects;
    }
}
