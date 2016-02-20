/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

/**
 * Created by Ralf Ulrich on 20.02.16.
 */
public class EntityTooSmallException extends S3ServerException {

    public EntityTooSmallException() {
        super("Your proposed upload is smaller than the minimum allowed object size.");
    }

    public EntityTooSmallException(String message) {
        super(message);
    }

    public EntityTooSmallException(String message, Throwable cause) {
        super(message, cause);
    }

    public EntityTooSmallException(Throwable cause) {
        super(cause);
    }

    public EntityTooSmallException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
