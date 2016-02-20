/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

/**
 * Created by Ralf Ulrich on 20.02.16.
 */
public class BadDigestException extends S3ServerException {

    public BadDigestException() {
        super("The Content-MD5 you specified did not match what we received.");
    }

    public BadDigestException(String message) {
        super(message);
    }

    public BadDigestException(String message, Throwable cause) {
        super(message, cause);
    }

    public BadDigestException(Throwable cause) {
        super(cause);
    }

    public BadDigestException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
