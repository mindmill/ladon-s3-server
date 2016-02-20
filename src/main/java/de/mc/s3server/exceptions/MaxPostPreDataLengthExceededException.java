/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

/**
 * Created by Ralf Ulrich on 20.02.16.
 */
public class MaxPostPreDataLengthExceededException extends S3ServerException {
    public MaxPostPreDataLengthExceededException() {
        super("our POST request fields preceding the upload file were too large.");
    }

    public MaxPostPreDataLengthExceededException(String message) {
        super(message);
    }

    public MaxPostPreDataLengthExceededException(String message, Throwable cause) {
        super(message, cause);
    }

    public MaxPostPreDataLengthExceededException(Throwable cause) {
        super(cause);
    }

    public MaxPostPreDataLengthExceededException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
