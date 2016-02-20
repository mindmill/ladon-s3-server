/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

/**
 * Created by Ralf Ulrich on 20.02.16.
 */
public class MissingAttachmentException extends S3ServerException {
    public MissingAttachmentException() {
        super("A SOAP attachment was expected, but none where found.");
    }

    public MissingAttachmentException(String message) {
        super(message);
    }

    public MissingAttachmentException(String message, Throwable cause) {
        super(message, cause);
    }

    public MissingAttachmentException(Throwable cause) {
        super(cause);
    }

    public MissingAttachmentException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
