/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

import de.mc.s3server.entities.api.S3RequestId;

/**
 * @author Ralf Ulrich on 17.02.16.
 */
public class InvalidBucketName extends S3ServerException {

    public InvalidBucketName(String resource, S3RequestId requestId) {
        super("The specified bucket is not valid.", resource, requestId);
    }


}
