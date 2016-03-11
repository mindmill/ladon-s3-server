/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.ladon.s3server.exceptions;

import de.mc.ladon.s3server.entities.api.S3RequestId;

import java.net.HttpURLConnection;

/**
 * @author Ralf Ulrich on 20.02.16.
 */
public class NotSignedUpException extends S3ServerException {
    public NotSignedUpException(String resource, S3RequestId requestId) {
        super("You must sign up before you can use S3.", resource, requestId, HttpURLConnection.HTTP_FORBIDDEN);
    }


}
