/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

/**
 * Created by Ralf Ulrich on 20.02.16.
 */
public class MalformedXMLException extends S3ServerException {
    public MalformedXMLException() {
        super("The XML you provided was not well-formed or did not validate against our published schema.");
    }

    public MalformedXMLException(String message) {
        super(message);
    }

    public MalformedXMLException(String message, Throwable cause) {
        super(message, cause);
    }

    public MalformedXMLException(Throwable cause) {
        super(cause);
    }

    public MalformedXMLException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
