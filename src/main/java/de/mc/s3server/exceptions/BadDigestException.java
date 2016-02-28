/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

import de.mc.s3server.entities.api.S3RequestId;

import java.net.HttpURLConnection;

/**
 * @author Ralf Ulrich on 20.02.16.
 */
public class BadDigestException extends S3ServerException {

    public BadDigestException(String resource, S3RequestId requestId) {
        super("The Content-MD5 you specified did not match what we received.", resource, requestId, HttpURLConnection.HTTP_BAD_REQUEST);
    }

    public BadDigestException(String message, String resource, S3RequestId requestId) {
        super(message, resource, requestId, HttpURLConnection.HTTP_BAD_REQUEST);
    }


}
