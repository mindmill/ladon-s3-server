/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

/**
 * Created by Ralf Ulrich on 20.02.16.
 */
public class UserKeyMustBeSpecifiedException extends S3ServerException {
    public UserKeyMustBeSpecifiedException() {
        super("The bucket POST must contain the specified field name. If it is specified, check the order of the fields.");
    }

    public UserKeyMustBeSpecifiedException(String message) {
        super(message);
    }

    public UserKeyMustBeSpecifiedException(String message, Throwable cause) {
        super(message, cause);
    }

    public UserKeyMustBeSpecifiedException(Throwable cause) {
        super(cause);
    }

    public UserKeyMustBeSpecifiedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
