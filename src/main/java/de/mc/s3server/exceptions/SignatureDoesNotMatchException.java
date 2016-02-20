/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

/**
 * Created by Ralf Ulrich on 20.02.16.
 */
public class SignatureDoesNotMatchException extends S3ServerException {
    public SignatureDoesNotMatchException() {
        super("The request signature we calculated " +
                "does not match the signature you provided.");
    }

    public SignatureDoesNotMatchException(String message) {
        super(message);
    }

    public SignatureDoesNotMatchException(String message, Throwable cause) {
        super(message, cause);
    }

    public SignatureDoesNotMatchException(Throwable cause) {
        super(cause);
    }

    public SignatureDoesNotMatchException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
