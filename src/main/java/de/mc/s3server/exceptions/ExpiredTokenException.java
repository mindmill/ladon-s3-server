/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

import de.mc.s3server.entities.api.S3RequestId;

/**
 * Created by max on 20.02.16.
 */
public class ExpiredTokenException extends S3ServerException {
    public ExpiredTokenException(String resource, S3RequestId requestId) {
        super("The provided token has expired.", resource, requestId);
    }


}
