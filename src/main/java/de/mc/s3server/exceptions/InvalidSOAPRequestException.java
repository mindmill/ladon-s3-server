/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

/**
 * Created by Ralf Ulrich on 20.02.16.
 */
public class InvalidSOAPRequestException extends S3ServerException {
    public InvalidSOAPRequestException() {
        super("The SOAP request body is invalid.");
    }

    public InvalidSOAPRequestException(String message) {
        super(message);
    }

    public InvalidSOAPRequestException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidSOAPRequestException(Throwable cause) {
        super(cause);
    }

    public InvalidSOAPRequestException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
