/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

/**
 * Created by Ralf Ulrich on 20.02.16.
 */
public class RequestIsNotMultiPartContentException extends S3ServerException{
    public RequestIsNotMultiPartContentException() {
        super("Bucket POST must be of the enclosure-type multipart/form-data.");
    }

    public RequestIsNotMultiPartContentException(String message) {
        super(message);
    }

    public RequestIsNotMultiPartContentException(String message, Throwable cause) {
        super(message, cause);
    }

    public RequestIsNotMultiPartContentException(Throwable cause) {
        super(cause);
    }

    public RequestIsNotMultiPartContentException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
