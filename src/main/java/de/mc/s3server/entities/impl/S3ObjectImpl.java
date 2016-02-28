/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.entities.impl;

import de.mc.s3server.common.S3Constants;
import de.mc.s3server.entities.api.S3UserMetadata;
import de.mc.s3server.entities.api.S3Object;
import de.mc.s3server.entities.api.S3User;
import org.springframework.util.MimeType;

import java.io.InputStream;
import java.util.Date;

/**
 * @author Ralf Ulrich on 21.02.16.
 */
public class S3ObjectImpl implements S3Object {

    private String key;
    private Date lastModified;
    private String bucket;
    private Long size;
    private MimeType mimeType;
    private S3User owner;
    private S3UserMetadata metadata;
    private InputStream content;


    public S3ObjectImpl(String key, Date lastModified, String bucket, Long size, S3User owner, S3UserMetadata metadata, InputStream content, MimeType mimeType) {
        this.key = key;
        this.lastModified = lastModified;
        this.bucket = bucket;
        this.size = size;
        this.mimeType = mimeType;
        this.owner = owner;
        this.metadata = metadata;
        this.content = content;
    }

    @Override
    public S3User getOwner() {
        return owner;
    }

    @Override
    public Long getSize() {
        return size;
    }

    @Override
    public String getStorageClass() {
        return "STANDARD";
    }

    @Override
    public MimeType getMimeType() {
        return mimeType;
    }

    @Override
    public String getETag() {
        return metadata.get(S3Constants.ETAG);
    }

    @Override
    public Date getLastModified() {
        return lastModified;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public String getBucket() {
        return bucket;
    }


    @Override
    public S3UserMetadata getMetadata() {
        return metadata;
    }

    @Override
    public InputStream getContent() {
        return content;
    }

}
