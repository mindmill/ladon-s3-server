/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

/**
 * Created by Ralf Ulrich on 20.02.16.
 */
public class MissingSecurityElementException extends S3ServerException {
    public MissingSecurityElementException() {
        super("The SOAP 1.1 request is missing a security element.");
    }

    public MissingSecurityElementException(String message) {
        super(message);
    }

    public MissingSecurityElementException(String message, Throwable cause) {
        super(message, cause);
    }

    public MissingSecurityElementException(Throwable cause) {
        super(cause);
    }

    public MissingSecurityElementException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
