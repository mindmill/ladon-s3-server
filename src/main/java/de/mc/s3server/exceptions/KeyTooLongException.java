/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

/**
 * Created by Ralf Ulrich on 20.02.16.
 */
public class KeyTooLongException extends S3ServerException {
    public KeyTooLongException() {
        super("Your key is too long.");
    }

    public KeyTooLongException(String message) {
        super(message);
    }

    public KeyTooLongException(String message, Throwable cause) {
        super(message, cause);
    }

    public KeyTooLongException(Throwable cause) {
        super(cause);
    }

    public KeyTooLongException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
