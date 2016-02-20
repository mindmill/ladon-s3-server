/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

/**
 * Created by Ralf Ulrich on 20.02.16.
 */
public class UnexpectedContentException  extends S3ServerException{
    public UnexpectedContentException() {
        super("This request does not support content.");
    }

    public UnexpectedContentException(String message) {
        super(message);
    }

    public UnexpectedContentException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnexpectedContentException(Throwable cause) {
        super(cause);
    }

    public UnexpectedContentException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
