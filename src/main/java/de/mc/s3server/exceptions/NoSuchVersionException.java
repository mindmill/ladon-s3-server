/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

import de.mc.s3server.entities.api.S3RequestId;

import java.net.HttpURLConnection;

/**
 * @author Ralf Ulrich on 20.02.16.
 */
public class NoSuchVersionException extends S3ServerException {
    public NoSuchVersionException(String resource, S3RequestId requestId) {
        super("No version with this id exists.", resource, requestId, HttpURLConnection.HTTP_NOT_FOUND);
    }


}
