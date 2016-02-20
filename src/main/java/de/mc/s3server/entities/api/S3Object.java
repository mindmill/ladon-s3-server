/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.entities.api;

import java.io.InputStream;

/**
 * Created by Ralf Ulrich on 17.02.16.
 */
public interface S3Object {

    String getKey();

    S3Bucket getBucket();

    S3Metadata getMetadata();

    InputStream getContent();

}
