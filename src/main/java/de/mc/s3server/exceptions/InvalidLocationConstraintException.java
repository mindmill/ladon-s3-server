/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

/**
 * Created by Ralf Ulrich on 20.02.16.
 */
public class InvalidLocationConstraintException extends S3ServerException{

    public InvalidLocationConstraintException() {
        super("The specified location constraint is not valid");
    }

    public InvalidLocationConstraintException(String message) {
        super(message);
    }

    public InvalidLocationConstraintException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidLocationConstraintException(Throwable cause) {
        super(cause);
    }

    public InvalidLocationConstraintException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
