/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

/**
 * Created by Ralf Ulrich on 20.02.16.
 */
public class IncorrectNumberOfFilesInPostRequestException extends S3ServerException{

    public IncorrectNumberOfFilesInPostRequestException() {
        super("POST requires exactly one file upload per request");
    }

    public IncorrectNumberOfFilesInPostRequestException(String message) {
        super(message);
    }

    public IncorrectNumberOfFilesInPostRequestException(String message, Throwable cause) {
        super(message, cause);
    }

    public IncorrectNumberOfFilesInPostRequestException(Throwable cause) {
        super(cause);
    }

    public IncorrectNumberOfFilesInPostRequestException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
