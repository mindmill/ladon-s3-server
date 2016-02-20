/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

/**
 * Created by Ralf Ulrich on 20.02.16.
 */
public class NoSuchVersionException extends S3ServerException {
    public NoSuchVersionException() {
        super("No version with this id exists.");
    }

    public NoSuchVersionException(String message) {
        super(message);
    }

    public NoSuchVersionException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoSuchVersionException(Throwable cause) {
        super(cause);
    }

    public NoSuchVersionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
