/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

/**
 * Created by Ralf Ulrich on 20.02.16.
 */
public class NoSuchKeyException extends S3ServerException {
    public NoSuchKeyException() {
        super("The specified key does not exist.");
    }

    public NoSuchKeyException(String message) {
        super(message);
    }

    public NoSuchKeyException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoSuchKeyException(Throwable cause) {
        super(cause);
    }

    public NoSuchKeyException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
