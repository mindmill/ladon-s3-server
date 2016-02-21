/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

import de.mc.s3server.entities.api.S3RequestId;

/**
 * Created by Ralf Ulrich on 20.02.16.
 */
public class InternalErrorException extends S3ServerException {

    public InternalErrorException(String resource, S3RequestId requestId) {
        super("We encountered an internal error. Please try again.", resource, requestId);
    }


}
