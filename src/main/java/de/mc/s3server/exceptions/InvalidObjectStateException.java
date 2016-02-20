/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

/**
 * Created by Ralf Ulrich on 20.02.16.
 */
public class InvalidObjectStateException extends S3ServerException {
    public InvalidObjectStateException() {
        super("The operation is not valid for the current state of the object.");
    }

    public InvalidObjectStateException(String message) {
        super(message);
    }

    public InvalidObjectStateException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidObjectStateException(Throwable cause) {
        super(cause);
    }

    public InvalidObjectStateException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
