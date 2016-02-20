/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

/**
 * Created by Ralf Ulrich on 20.02.16.
 */
public class InlineDataTooLargeException extends S3ServerException{
    public InlineDataTooLargeException() {
        super("Inline data exceeds the maximum allowed size.");
    }

    public InlineDataTooLargeException(String message) {
        super(message);
    }

    public InlineDataTooLargeException(String message, Throwable cause) {
        super(message, cause);
    }

    public InlineDataTooLargeException(Throwable cause) {
        super(cause);
    }

    public InlineDataTooLargeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
