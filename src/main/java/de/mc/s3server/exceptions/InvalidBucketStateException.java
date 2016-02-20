/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

/**
 * Created by Ralf Ulrich on 20.02.16.
 */
public class InvalidBucketStateException extends S3ServerException{
    public InvalidBucketStateException() {
        super("The request is not valid with the current state of the bucket");
    }

    public InvalidBucketStateException(String message) {
        super(message);
    }

    public InvalidBucketStateException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidBucketStateException(Throwable cause) {
        super(cause);
    }

    public InvalidBucketStateException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
