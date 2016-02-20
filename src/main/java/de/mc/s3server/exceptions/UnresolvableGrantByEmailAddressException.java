/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

/**
 * Created by Ralf Ulrich on 20.02.16.
 */
public class UnresolvableGrantByEmailAddressException extends S3ServerException{
    public UnresolvableGrantByEmailAddressException() {
        super("The email address you provided does not match any account on record.");
    }

    public UnresolvableGrantByEmailAddressException(String message) {
        super(message);
    }

    public UnresolvableGrantByEmailAddressException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnresolvableGrantByEmailAddressException(Throwable cause) {
        super(cause);
    }

    public UnresolvableGrantByEmailAddressException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
