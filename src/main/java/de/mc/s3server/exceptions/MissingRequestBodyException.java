/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

/**
 * Created by Ralf Ulrich on 20.02.16.
 */
public class MissingRequestBodyException extends S3ServerException{
    public MissingRequestBodyException() {
        super("Request body is empty.");
    }

    public MissingRequestBodyException(String message) {
        super(message);
    }

    public MissingRequestBodyException(String message, Throwable cause) {
        super(message, cause);
    }

    public MissingRequestBodyException(Throwable cause) {
        super(cause);
    }

    public MissingRequestBodyException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
