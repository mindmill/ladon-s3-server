/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

import de.mc.s3server.entities.api.S3RequestId;

/**
 * @author Ralf Ulrich on 20.02.16.
 */
public abstract class S3ServerException extends RuntimeException {

    private String resource;
    private String requestId;

    public S3ServerException() {
    }

    public S3ServerException(String message, String resource, S3RequestId requestId) {
        super(message);
        this.requestId = requestId.get();
        this.resource = resource;
    }

    public String getCode() {
        return this.getClass().getSimpleName().replace("Exception", "");
    }

    public String getResource() {
        return resource;
    }


    public String getRequestId() {
        return requestId;
    }

}
