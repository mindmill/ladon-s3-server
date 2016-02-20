/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

/**
 * Created by Ralf Ulrich on 20.02.16.
 */
public class InvalidEncryptionAlgorithmErrorException extends S3ServerException {
    public InvalidEncryptionAlgorithmErrorException() {
        super("The encryption request you specified is not valid. The valid value is AES256");
    }

    public InvalidEncryptionAlgorithmErrorException(String message) {
        super(message);
    }

    public InvalidEncryptionAlgorithmErrorException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidEncryptionAlgorithmErrorException(Throwable cause) {
        super(cause);
    }

    public InvalidEncryptionAlgorithmErrorException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
