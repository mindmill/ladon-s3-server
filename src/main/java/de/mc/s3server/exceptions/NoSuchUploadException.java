/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

/**
 * Created by Ralf Ulrich on 20.02.16.
 */
public class NoSuchUploadException extends S3ServerException{
    public NoSuchUploadException() {
        super("The specified multipart upload does not exist.");
    }

    public NoSuchUploadException(String message) {
        super(message);
    }

    public NoSuchUploadException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoSuchUploadException(Throwable cause) {
        super(cause);
    }

    public NoSuchUploadException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
