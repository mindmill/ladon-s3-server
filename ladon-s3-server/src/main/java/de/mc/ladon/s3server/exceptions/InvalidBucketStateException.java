/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.ladon.s3server.exceptions;

import de.mc.ladon.s3server.entities.api.S3RequestId;

import java.net.HttpURLConnection;

/**
 * @author Ralf Ulrich on 20.02.16.
 */
public class InvalidBucketStateException extends S3ServerException {
    public InvalidBucketStateException(String resource, S3RequestId requestId) {
        super("The request is not valid with the current state of the bucket", resource, requestId, HttpURLConnection.HTTP_CONFLICT);
    }

}
