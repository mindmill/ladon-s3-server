/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.ladon.s3server.exceptions;

import de.mc.ladon.s3server.entities.api.S3RequestId;

import java.net.HttpURLConnection;

/**
 * @author Ralf Ulrich on 20.02.16.
 */
public class NoLoggingStatusForKeyException extends S3ServerException {
    public NoLoggingStatusForKeyException(String resource, S3RequestId requestId) {
        super("There is no such thing as a logging\n" +
                "status subresource for a key.", resource, requestId, HttpURLConnection.HTTP_BAD_REQUEST);
    }


}
