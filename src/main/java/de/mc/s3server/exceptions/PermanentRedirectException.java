/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

/**
 * Created by Ralf Ulrich on 20.02.16.
 */
public class PermanentRedirectException extends S3ServerException{
    public PermanentRedirectException() {
        super("The bucket you are attempting to access must be addressed using the specified endpoint." +
                " Send all future requests to this endpoint.");
    }

    public PermanentRedirectException(String message) {
        super(message);
    }

    public PermanentRedirectException(String message, Throwable cause) {
        super(message, cause);
    }

    public PermanentRedirectException(Throwable cause) {
        super(cause);
    }

    public PermanentRedirectException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
