/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

/**
 * Created by Ralf Ulrich on 20.02.16.
 */
public class InvalidPartOrderException extends S3ServerException {
    public InvalidPartOrderException() {
        super("The list of parts was not in ascending order." +
                "Parts list must specified in order by part number.");
    }

    public InvalidPartOrderException(String message) {
        super(message);
    }

    public InvalidPartOrderException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidPartOrderException(Throwable cause) {
        super(cause);
    }

    public InvalidPartOrderException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
