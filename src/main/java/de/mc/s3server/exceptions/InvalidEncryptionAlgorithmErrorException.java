/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

import de.mc.s3server.entities.api.S3RequestId;

import java.net.HttpURLConnection;

/**
 * @author Ralf Ulrich on 20.02.16.
 */
public class InvalidEncryptionAlgorithmErrorException extends S3ServerException {
    public InvalidEncryptionAlgorithmErrorException(String resource, S3RequestId requestId) {
        super("The encryption request you specified is not valid. The valid value is AES256", resource, requestId, HttpURLConnection.HTTP_BAD_REQUEST);
    }


}
