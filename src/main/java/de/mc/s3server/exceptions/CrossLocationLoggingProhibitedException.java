/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

import de.mc.s3server.entities.api.S3RequestId;

/**
 * Created by Ralf Ulrich on 20.02.16.
 */
public class CrossLocationLoggingProhibitedException extends S3ServerException {
    public CrossLocationLoggingProhibitedException(String resource, S3RequestId requestId) {
        super("Cross-location logging not allowed. Buckets in one geographic location " +
                "cannot log information to a bucket in another location.", resource, requestId);
    }

}
