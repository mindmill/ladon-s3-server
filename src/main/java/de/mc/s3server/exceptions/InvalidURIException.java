/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

/**
 * Created by Ralf Ulrich on 20.02.16.
 */
public class InvalidURIException extends S3ServerException {

    public InvalidURIException() {
        super("Couldn't parse the specified URI.");
    }

    public InvalidURIException(String message) {
        super(message);
    }

    public InvalidURIException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidURIException(Throwable cause) {
        super(cause);
    }

    public InvalidURIException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
