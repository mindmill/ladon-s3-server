/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.ladon.s3server.exceptions;

import de.mc.ladon.s3server.entities.api.S3RequestId;

/**
 * @author Ralf Ulrich on 20.02.16.
 */
public class SlowDownException extends S3ServerException {
    public SlowDownException(String resource, S3RequestId requestId) {
        super("Reduce your request rate.", resource, requestId, 429);
    }


}
