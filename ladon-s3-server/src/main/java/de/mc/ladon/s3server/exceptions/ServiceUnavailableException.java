/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.ladon.s3server.exceptions;

import de.mc.ladon.s3server.entities.api.S3RequestId;

import java.net.HttpURLConnection;

/**
 * @author Ralf Ulrich on 20.02.16.
 */
public class ServiceUnavailableException extends S3ServerException {
    public ServiceUnavailableException(String resource, S3RequestId requestId) {
        super("Reduce your request rate.", resource, requestId, HttpURLConnection.HTTP_UNAVAILABLE);
    }


}
