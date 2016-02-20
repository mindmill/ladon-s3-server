/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

/**
 * Created by Ralf Ulrich on 20.02.16.
 */
public class RestoreAlreadyInProgressException extends S3ServerException {
    public RestoreAlreadyInProgressException() {
        super("Object restore is already in progress.");
    }

    public RestoreAlreadyInProgressException(String message) {
        super(message);
    }

    public RestoreAlreadyInProgressException(String message, Throwable cause) {
        super(message, cause);
    }

    public RestoreAlreadyInProgressException(Throwable cause) {
        super(cause);
    }

    public RestoreAlreadyInProgressException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
