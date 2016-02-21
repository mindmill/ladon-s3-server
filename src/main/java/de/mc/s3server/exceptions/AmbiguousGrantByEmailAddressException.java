/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

import de.mc.s3server.entities.api.S3RequestId;

/**
 * @author Ralf Ulrich on 20.02.16.
 */
public class AmbiguousGrantByEmailAddressException extends S3ServerException {

    public AmbiguousGrantByEmailAddressException(String resource, S3RequestId requestId) {
        super("The email address you provided is associated with more than one account.", resource, requestId);
    }

    public AmbiguousGrantByEmailAddressException(String message, String resource, S3RequestId requestId) {
        super(message, resource, requestId);
    }


}
