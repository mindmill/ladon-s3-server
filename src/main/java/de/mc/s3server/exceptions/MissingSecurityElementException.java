/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

import de.mc.s3server.entities.api.S3RequestId;

/**
 * @author Ralf Ulrich on 20.02.16.
 */
public class MissingSecurityElementException extends S3ServerException {
    public MissingSecurityElementException(String resource, S3RequestId requestId) {
        super("The SOAP 1.1 request is missing a security element.", resource, requestId);
    }


}