/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.ladon.s3server.exceptions;

import de.mc.ladon.s3server.entities.api.S3RequestId;

import java.net.HttpURLConnection;

/**
 * @author Ralf Ulrich on 20.02.16.
 */
public class InvalidObjectStateException extends S3ServerException {
    public InvalidObjectStateException(String resource, S3RequestId requestId) {
        super("The operation is not valid for the current state of the object.", resource, requestId, HttpURLConnection.HTTP_FORBIDDEN);
    }


}
