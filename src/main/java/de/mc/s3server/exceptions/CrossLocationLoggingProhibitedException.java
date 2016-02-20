/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

/**
 * Created by Ralf Ulrich on 20.02.16.
 */
public class CrossLocationLoggingProhibitedException extends S3ServerException {
    public CrossLocationLoggingProhibitedException() {
        super("Cross-location logging not allowed. Buckets in one geographic location " +
                "cannot log information to a bucket in another location.");
    }

    public CrossLocationLoggingProhibitedException(String message) {
        super(message);
    }

    public CrossLocationLoggingProhibitedException(String message, Throwable cause) {
        super(message, cause);
    }

    public CrossLocationLoggingProhibitedException(Throwable cause) {
        super(cause);
    }

    public CrossLocationLoggingProhibitedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
