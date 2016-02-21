/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

import de.mc.s3server.entities.api.S3RequestId;

/**
 * Created by Ralf Ulrich on 20.02.16.
 */
public class IncompleteBodyException extends S3ServerException {

    public IncompleteBodyException(String resource, S3RequestId requestId) {
        super("You did not provide the number of bytes specified by the Content-Length HTTP header", resource, requestId);
    }

}
