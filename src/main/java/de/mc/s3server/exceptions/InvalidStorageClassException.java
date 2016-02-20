/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

/**
 * Created by Ralf Ulrich on 20.02.16.
 */
public class InvalidStorageClassException extends S3ServerException {
    public InvalidStorageClassException() {
        super("The storage class you specified is not valid.");
    }

    public InvalidStorageClassException(String message) {
        super(message);
    }

    public InvalidStorageClassException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidStorageClassException(Throwable cause) {
        super(cause);
    }

    public InvalidStorageClassException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
