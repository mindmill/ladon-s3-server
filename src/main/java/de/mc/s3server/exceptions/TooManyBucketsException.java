/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

import de.mc.s3server.entities.api.S3RequestId;

/**
 * @author Ralf Ulrich on 20.02.16.
 */
public class TooManyBucketsException extends S3ServerException {
    public TooManyBucketsException(String resource, S3RequestId requestId) {
        super("You have attempt to create more buckets than allowed.", resource, requestId);
    }


}
