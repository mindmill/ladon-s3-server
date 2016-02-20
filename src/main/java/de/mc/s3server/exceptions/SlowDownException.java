/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

/**
 * Created by Ralf Ulrich on 20.02.16.
 */
public class SlowDownException extends S3ServerException {
    public SlowDownException() {
        super("Reduce your request rate.");
    }

    public SlowDownException(String message) {
        super(message);
    }

    public SlowDownException(String message, Throwable cause) {
        super(message, cause);
    }

    public SlowDownException(Throwable cause) {
        super(cause);
    }

    public SlowDownException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
