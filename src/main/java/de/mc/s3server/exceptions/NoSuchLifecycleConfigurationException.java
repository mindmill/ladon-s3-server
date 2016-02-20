/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

/**
 * Created by Ralf Ulrich on 20.02.16.
 */
public class NoSuchLifecycleConfigurationException  extends S3ServerException{

    public NoSuchLifecycleConfigurationException() {
        super("The lifecycle configuration does not exist.");
    }

    public NoSuchLifecycleConfigurationException(String message) {
        super(message);
    }

    public NoSuchLifecycleConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoSuchLifecycleConfigurationException(Throwable cause) {
        super(cause);
    }

    public NoSuchLifecycleConfigurationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
