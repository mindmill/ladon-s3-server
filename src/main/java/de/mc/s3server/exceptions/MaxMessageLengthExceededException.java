/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

/**
 * Created by Ralf Ulrich on 20.02.16.
 */
public class MaxMessageLengthExceededException extends S3ServerException {
    public MaxMessageLengthExceededException() {
        super("Your request was too big.");
    }

    public MaxMessageLengthExceededException(String message) {
        super(message);
    }

    public MaxMessageLengthExceededException(String message, Throwable cause) {
        super(message, cause);
    }

    public MaxMessageLengthExceededException(Throwable cause) {
        super(cause);
    }

    public MaxMessageLengthExceededException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
