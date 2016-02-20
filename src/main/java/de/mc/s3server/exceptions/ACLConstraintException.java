/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

/**
 * Created by Ralf Ulrich on 17.02.16.
 */
public class ACLConstraintException extends AccessDeniedException {

    public ACLConstraintException() {
    }

    public ACLConstraintException(String message) {
        super(message);
    }

    public ACLConstraintException(String message, Throwable cause) {
        super(message, cause);
    }

    public ACLConstraintException(Throwable cause) {
        super(cause);
    }

    public ACLConstraintException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
