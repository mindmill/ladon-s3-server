/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

/**
 * Created by Ralf Ulrich on 20.02.16.
 */
public class InvalidSecurityException extends S3ServerException {
    public InvalidSecurityException() {
        super("The provided security credentials are not valid.");
    }

    public InvalidSecurityException(String message) {
        super(message);
    }

    public InvalidSecurityException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidSecurityException(Throwable cause) {
        super(cause);
    }

    public InvalidSecurityException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
