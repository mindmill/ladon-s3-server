/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.entities.impl;

import de.mc.s3server.entities.api.S3Bucket;

import java.util.Date;

/**
 * Created by Ralf Ulrich on 17.02.16.
 */
public class S3BucketImpl implements S3Bucket {

   private String bucketName;
    private Date creationDate;

    public S3BucketImpl() {
    }

    public S3BucketImpl(String bucketName, Date creationDate) {
        this.bucketName = bucketName;
        this.creationDate = creationDate;
    }

    @Override
    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    @Override
    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }
}
