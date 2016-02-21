/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

import de.mc.s3server.entities.api.S3RequestId;

/**
 * Created by Ralf Ulrich on 20.02.16.
 */
public class EntityTooSmallException extends S3ServerException {

    public EntityTooSmallException(String resource, S3RequestId requestId) {
        super("Your proposed upload is smaller than the minimum allowed object size.", resource, requestId);
    }


}
