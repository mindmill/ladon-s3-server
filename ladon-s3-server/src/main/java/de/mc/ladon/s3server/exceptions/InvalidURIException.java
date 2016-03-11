/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.ladon.s3server.exceptions;

import de.mc.ladon.s3server.entities.api.S3RequestId;

import java.net.HttpURLConnection;

/**
 * @author Ralf Ulrich on 20.02.16.
 */
public class InvalidURIException extends S3ServerException {

    public InvalidURIException(String resource, S3RequestId requestId) {
        super("Couldn't parse the specified URI.", resource, requestId, HttpURLConnection.HTTP_BAD_REQUEST);
    }


}
