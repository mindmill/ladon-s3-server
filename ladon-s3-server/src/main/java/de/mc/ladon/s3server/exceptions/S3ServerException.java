/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.ladon.s3server.exceptions;

import de.mc.ladon.s3server.entities.api.S3RequestId;

/**
 * @author Ralf Ulrich on 20.02.16.
 */
public abstract class S3ServerException extends RuntimeException {

    private String resource;
    private String requestId;
    private int responseStatus;

    public S3ServerException() {
    }

    public S3ServerException(String message, String resource, S3RequestId requestId, int responseStatus) {
        super(message);
        this.requestId = requestId.get();
        this.resource = resource;
        this.responseStatus = responseStatus;
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

    public int getResponseStatus() {
        return responseStatus;
    }


}
