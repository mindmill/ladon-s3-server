/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

/**
 * Created by max on 20.02.16.
 */
public class MalformedPOSTRequestException extends S3ServerException{
    public MalformedPOSTRequestException() {
        super("The body of your POST request is not well-formed multipart/form-data.");
    }

    public MalformedPOSTRequestException(String message) {
        super(message);
    }

    public MalformedPOSTRequestException(String message, Throwable cause) {
        super(message, cause);
    }

    public MalformedPOSTRequestException(Throwable cause) {
        super(cause);
    }

    public MalformedPOSTRequestException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
