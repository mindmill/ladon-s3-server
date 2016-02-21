/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.entities.impl;

import de.mc.s3server.entities.api.S3Bucket;
import de.mc.s3server.entities.api.S3User;

import java.util.Date;

/**
 * Created by Ralf Ulrich on 17.02.16.
 */
public class S3BucketImpl implements S3Bucket {

    private String bucketName;
    private Date creationDate;
    private S3User owner;


    public S3BucketImpl(String bucketName, Date creationDate, S3User owner) {
        this.bucketName = bucketName;
        this.creationDate = creationDate;
        this.owner = owner;
    }

    @Override
    public S3User getOwner() {
        return owner;
    }

    @Override
    public String getBucketName() {
        return bucketName;
    }


    @Override
    public Date getCreationDate() {
        return creationDate;
    }

}
