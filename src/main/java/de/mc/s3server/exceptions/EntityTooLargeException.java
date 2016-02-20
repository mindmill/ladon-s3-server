/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

/**
 * Created by Ralf Ulrich on 20.02.16.
 */
public class EntityTooLargeException extends S3ServerException {

    public EntityTooLargeException() {
        super("Your proposed upload exceeds the maximum allowed object size.");
    }

    public EntityTooLargeException(String message) {
        super(message);
    }

    public EntityTooLargeException(String message, Throwable cause) {
        super(message, cause);
    }

    public EntityTooLargeException(Throwable cause) {
        super(cause);
    }

    public EntityTooLargeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
