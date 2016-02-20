/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

/**
 * Created by Ralf Ulrich on 20.02.16.
 */
public class TokenRefreshRequiredException extends  S3ServerException {
    public TokenRefreshRequiredException() {
        super("The provided token must be refreshed.");
    }

    public TokenRefreshRequiredException(String message) {
        super(message);
    }

    public TokenRefreshRequiredException(String message, Throwable cause) {
        super(message, cause);
    }

    public TokenRefreshRequiredException(Throwable cause) {
        super(cause);
    }

    public TokenRefreshRequiredException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
