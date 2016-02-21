/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

import de.mc.s3server.entities.api.S3RequestId;

/**
 * @author Ralf Ulrich on 20.02.16.
 */
public class InvalidAccessKeyIdException extends S3ServerException {
    public InvalidAccessKeyIdException(String resource, S3RequestId requestId) {
        super("The AWS access key Id you provided does not exist in our records", resource, requestId);
    }


}
