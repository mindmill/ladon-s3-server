/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

import de.mc.s3server.entities.api.S3RequestId;

/**
 * Created by Ralf Ulrich on 20.02.16.
 */
public class InvalidBucketStateException extends S3ServerException {
    public InvalidBucketStateException(String resource, S3RequestId requestId) {
        super("The request is not valid with the current state of the bucket", resource, requestId);
    }

}
