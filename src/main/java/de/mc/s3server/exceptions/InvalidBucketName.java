/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

/**
 * Created by Ralf Ulrich on 17.02.16.
 */
public class InvalidBucketName extends S3ServerException {

    public InvalidBucketName() {
        super("The specified bucket is not valid.");
    }

    public InvalidBucketName(String message) {
        super(message);
    }

    public InvalidBucketName(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidBucketName(Throwable cause) {
        super(cause);
    }

    public InvalidBucketName(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
