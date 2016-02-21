/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

import de.mc.s3server.entities.api.S3RequestId;

/**
 * @author Ralf Ulrich on 20.02.16.
 */
public class OperationAbortedException extends S3ServerException {
    public OperationAbortedException(String resource, S3RequestId requestId) {
        super("A conflicting conditional operation is currently in progress against this resource. Try again.", resource, requestId);
    }


}
