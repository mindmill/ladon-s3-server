/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

import de.mc.s3server.entities.api.S3RequestId;

/**
 * Created by max on 20.02.16.
 */
public class MalformedPOSTRequestException extends S3ServerException {
    public MalformedPOSTRequestException(String resource, S3RequestId requestId) {
        super("The body of your POST request is not well-formed multipart/form-data.", resource, requestId);
    }


}
