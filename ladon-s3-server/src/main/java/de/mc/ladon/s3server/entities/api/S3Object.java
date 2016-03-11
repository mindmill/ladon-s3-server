/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.ladon.s3server.entities.api;

import java.io.InputStream;
import java.util.Date;

/**
 * @author Ralf Ulrich on 17.02.16.
 */
public interface S3Object {

    String getKey();

    Date getLastModified();

    S3User getOwner();

    String getETag();

    Long getSize();

    String getMimeType();

    String getStorageClass();

    String getBucket();

    S3Metadata getMetadata();

    InputStream getContent();

}
