/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

import de.mc.s3server.entities.api.S3RequestId;

/**
 * @author Ralf Ulrich on 20.02.16.
 */
public class InvalidStorageClassException extends S3ServerException {
    public InvalidStorageClassException(String resource, S3RequestId requestId) {
        super("The storage class you specified is not valid.", resource, requestId);
    }


}
