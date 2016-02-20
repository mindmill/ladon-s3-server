/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

/**
 * Created by Ralf Ulrich on 20.02.16.
 */
public class OperationAbortedException extends S3ServerException {
    public OperationAbortedException() {
        super("A conflicting conditional operation is currently in progress against this resource. Try again.");
    }

    public OperationAbortedException(String message) {
        super(message);
    }

    public OperationAbortedException(String message, Throwable cause) {
        super(message, cause);
    }

    public OperationAbortedException(Throwable cause) {
        super(cause);
    }

    public OperationAbortedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
