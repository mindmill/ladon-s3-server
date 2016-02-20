/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

/**
 * Created by Ralf Ulrich on 20.02.16.
 */
public class IncompleteBodyException extends S3ServerException {

    public IncompleteBodyException() {
        super("You did not provide the number of bytes specified by the Content-Length HTTP header");
    }

    public IncompleteBodyException(String message) {
        super(message);
    }

    public IncompleteBodyException(String message, Throwable cause) {
        super(message, cause);
    }

    public IncompleteBodyException(Throwable cause) {
        super(cause);
    }

    public IncompleteBodyException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
