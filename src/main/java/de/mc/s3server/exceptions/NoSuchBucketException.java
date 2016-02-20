/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

/**
 * Created by Ralf Ulrich on 20.02.16.
 */
public class NoSuchBucketException extends S3ServerException{
    public NoSuchBucketException() {
        super("The specified bucket does not exist.");
    }

    public NoSuchBucketException(String message) {
        super(message);
    }

    public NoSuchBucketException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoSuchBucketException(Throwable cause) {
        super(cause);
    }

    public NoSuchBucketException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
