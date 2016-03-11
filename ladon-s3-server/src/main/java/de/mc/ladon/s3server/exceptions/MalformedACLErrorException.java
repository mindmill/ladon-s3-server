/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.ladon.s3server.exceptions;

import de.mc.ladon.s3server.entities.api.S3RequestId;

import java.net.HttpURLConnection;

/**
 * @author Ralf Ulrich on 20.02.16.
 */
public class MalformedACLErrorException extends S3ServerException {
    public MalformedACLErrorException(String resource, S3RequestId requestId) {
        super("The XML you provided was not well formed or did not validate against our published schema.", resource, requestId, HttpURLConnection.HTTP_BAD_REQUEST);
    }


}
