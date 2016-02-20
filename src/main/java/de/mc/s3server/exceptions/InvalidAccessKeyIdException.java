/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

/**
 * Created by Ralf Ulrich on 20.02.16.
 */
public class InvalidAccessKeyIdException extends S3ServerException {
    public InvalidAccessKeyIdException() {
        super("The AWS access key Id you provided does not exist in our records");
    }

    public InvalidAccessKeyIdException(String message) {
        super(message);
    }

    public InvalidAccessKeyIdException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidAccessKeyIdException(Throwable cause) {
        super(cause);
    }

    public InvalidAccessKeyIdException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
