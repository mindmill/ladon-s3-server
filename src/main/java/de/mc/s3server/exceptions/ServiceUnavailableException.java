/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

import de.mc.s3server.entities.api.S3RequestId;

/**
 * @author Ralf Ulrich on 20.02.16.
 */
public class ServiceUnavailableException extends S3ServerException {
    public ServiceUnavailableException(String resource, S3RequestId requestId) {
        super("Reduce your request rate.", resource, requestId);
    }


}
