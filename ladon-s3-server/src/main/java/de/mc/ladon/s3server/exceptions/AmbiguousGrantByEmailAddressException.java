/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.ladon.s3server.exceptions;

import de.mc.ladon.s3server.entities.api.S3RequestId;

import java.net.HttpURLConnection;

/**
 * @author Ralf Ulrich on 20.02.16.
 */
public class AmbiguousGrantByEmailAddressException extends S3ServerException {

    public AmbiguousGrantByEmailAddressException(String resource, S3RequestId requestId) {
        super("The email address you provided is associated with more than one account.", resource, requestId, HttpURLConnection.HTTP_BAD_REQUEST);
    }

    public AmbiguousGrantByEmailAddressException(String message, String resource, S3RequestId requestId) {
        super(message, resource, requestId, HttpURLConnection.HTTP_BAD_REQUEST);
    }


}
