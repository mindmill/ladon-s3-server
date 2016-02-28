/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

import de.mc.s3server.entities.api.S3RequestId;

import java.net.HttpURLConnection;

/**
 * @author Ralf Ulrich on 20.02.16.
 */
public class PreconditionFailedException extends S3ServerException {
    public PreconditionFailedException(String resource, S3RequestId requestId) {
        super("At least one of the preconditions you specified did not hold.", resource, requestId, HttpURLConnection.HTTP_PRECON_FAILED);
    }


}
