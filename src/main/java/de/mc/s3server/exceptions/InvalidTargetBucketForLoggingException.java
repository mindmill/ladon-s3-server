/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

/**
 * Created by Ralf Ulrich on 20.02.16.
 */
public class InvalidTargetBucketForLoggingException extends S3ServerException {
    public InvalidTargetBucketForLoggingException() {
        super("The target bucket for logging does not exist");
    }

    public InvalidTargetBucketForLoggingException(String message) {
        super(message);
    }

    public InvalidTargetBucketForLoggingException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidTargetBucketForLoggingException(Throwable cause) {
        super(cause);
    }

    public InvalidTargetBucketForLoggingException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
