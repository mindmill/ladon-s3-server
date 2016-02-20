/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

/**
 * Created by Ralf Ulrich on 20.02.16.
 */
public class AmbiguousGrantByEmailAddressException extends S3ServerException {

    public AmbiguousGrantByEmailAddressException() {
        super("The email address you provided is associated with more than one account.");
    }

    public AmbiguousGrantByEmailAddressException(String message) {
        super(message);
    }

    public AmbiguousGrantByEmailAddressException(String message, Throwable cause) {
        super(message, cause);
    }

    public AmbiguousGrantByEmailAddressException(Throwable cause) {
        super(cause);
    }

    public AmbiguousGrantByEmailAddressException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
