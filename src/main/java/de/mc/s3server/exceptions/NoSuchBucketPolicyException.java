/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

/**
 * Created by Ralf Ulrich on 20.02.16.
 */
public class NoSuchBucketPolicyException extends S3ServerException {
    public NoSuchBucketPolicyException() {
        super("The specified bucket does not have a bucket policy");
    }

    public NoSuchBucketPolicyException(String message) {
        super(message);
    }

    public NoSuchBucketPolicyException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoSuchBucketPolicyException(Throwable cause) {
        super(cause);
    }

    public NoSuchBucketPolicyException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
