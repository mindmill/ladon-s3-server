/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.ladon.s3server.exceptions;

import de.mc.ladon.s3server.entities.api.S3RequestId;

import java.net.HttpURLConnection;

/**
 * @author Ralf Ulrich on 20.02.16.
 */
public class InvalidRequestException extends S3ServerException {
    public InvalidRequestException(String resource, S3RequestId requestId) {
        super("SOAP requests must be made over an HTTPS connection.", resource, requestId, HttpURLConnection.HTTP_BAD_REQUEST);
    }


}
