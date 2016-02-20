/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

/**
 * Created by Ralf Ulrich on 20.02.16.
 */
public class InvalidPolicyDocumentException extends S3ServerException {
    public InvalidPolicyDocumentException() {
        super("The content of the form does not meet the conditions specified in the policy document.");
    }

    public InvalidPolicyDocumentException(String message) {
        super(message);
    }

    public InvalidPolicyDocumentException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidPolicyDocumentException(Throwable cause) {
        super(cause);
    }

    public InvalidPolicyDocumentException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
