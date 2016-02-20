/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

/**
 * Created by Ralf Ulrich on 17.02.16.
 */
public class BucketAlreadyExistsException extends S3ServerException {

    public BucketAlreadyExistsException() {
        super("The requested bucket name is not available.");
    }

    public BucketAlreadyExistsException(String message) {
        super(message);
    }

    public BucketAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }

    public BucketAlreadyExistsException(Throwable cause) {
        super(cause);
    }

    public BucketAlreadyExistsException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
