/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

/**
 * Created by Ralf Ulrich on 20.02.16.
 */
public class NoLoggingStatusForKeyException extends S3ServerException {
    public NoLoggingStatusForKeyException() {
        super("There is no such thing as a logging\n" +
                "status subresource for a key.");
    }

    public NoLoggingStatusForKeyException(String message) {
        super(message);
    }

    public NoLoggingStatusForKeyException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoLoggingStatusForKeyException(Throwable cause) {
        super(cause);
    }

    public NoLoggingStatusForKeyException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
