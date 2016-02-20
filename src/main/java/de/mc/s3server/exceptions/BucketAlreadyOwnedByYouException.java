/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

/**
 * Created by max on 20.02.16.
 */
public class BucketAlreadyOwnedByYouException extends BucketAlreadyExistsException {

    public BucketAlreadyOwnedByYouException() {
        super("Your previous request to create the named bucket succeeded and you already own it.");
    }

    public BucketAlreadyOwnedByYouException(String message) {
        super(message);
    }

    public BucketAlreadyOwnedByYouException(String message, Throwable cause) {
        super(message, cause);
    }

    public BucketAlreadyOwnedByYouException(Throwable cause) {
        super(cause);
    }

    public BucketAlreadyOwnedByYouException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
