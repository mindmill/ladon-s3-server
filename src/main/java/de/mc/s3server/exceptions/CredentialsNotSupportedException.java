/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

/**
 * Created by Ralf Ulrich on 20.02.16.
 */
public class CredentialsNotSupportedException extends S3ServerException {
    public CredentialsNotSupportedException() {
        super("This request does not support credentials");
    }

    public CredentialsNotSupportedException(String message) {
        super(message);
    }

    public CredentialsNotSupportedException(String message, Throwable cause) {
        super(message, cause);
    }

    public CredentialsNotSupportedException(Throwable cause) {
        super(cause);
    }

    public CredentialsNotSupportedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
