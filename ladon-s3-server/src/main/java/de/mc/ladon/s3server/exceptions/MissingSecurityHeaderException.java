/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.ladon.s3server.exceptions;

import de.mc.ladon.s3server.entities.api.S3RequestId;

import java.net.HttpURLConnection;

/**
 * @author Ralf Ulrich on 20.02.16.
 */
public class MissingSecurityHeaderException extends S3ServerException {
    public MissingSecurityHeaderException(String resource, S3RequestId requestId) {
        super("Your request is missing a required header.", resource, requestId, HttpURLConnection.HTTP_BAD_REQUEST);
    }

}
