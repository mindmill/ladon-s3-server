/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

/**
 * Created by Ralf Ulrich on 20.02.16.
 */
public class InvalidPartException extends S3ServerException {
    public InvalidPartException() {
        super("One or more of the specified parts could not be found. " +
                "The part might not have been uploaded, " +
                "or the specified entity tag might not have matched the part's entity tag.");
    }

    public InvalidPartException(String message) {
        super(message);
    }

    public InvalidPartException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidPartException(Throwable cause) {
        super(cause);
    }

    public InvalidPartException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
