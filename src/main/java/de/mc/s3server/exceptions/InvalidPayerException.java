/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

/**
 * Created by Ralf Ulrich on 20.02.16.
 */
public class InvalidPayerException extends S3ServerException {
    public InvalidPayerException() {
        super("All access to this object has been disabled.");
    }

    public InvalidPayerException(String message) {
        super(message);
    }

    public InvalidPayerException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidPayerException(Throwable cause) {
        super(cause);
    }

    public InvalidPayerException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
