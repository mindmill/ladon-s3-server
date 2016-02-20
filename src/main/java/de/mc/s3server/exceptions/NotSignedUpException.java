/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

/**
 * Created by Ralf Ulrich on 20.02.16.
 */
public class NotSignedUpException extends S3ServerException {
    public NotSignedUpException() {
        super("You must sign up before you can use S3.");
    }

    public NotSignedUpException(String message) {
        super(message);
    }

    public NotSignedUpException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotSignedUpException(Throwable cause) {
        super(cause);
    }

    public NotSignedUpException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
