/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

/**
 * Created by Ralf Ulrich on 20.02.16.
 */
public abstract class S3ServerException extends RuntimeException {

    private String resource;
    private String requestId;

    public S3ServerException() {
    }

    public S3ServerException(String message) {
        super(message);
    }

    public S3ServerException(String message, Throwable cause) {
        super(message, cause);
    }

    public S3ServerException(Throwable cause) {
        super(cause);
    }

    public S3ServerException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public String getCode(){
        return this.getClass().getSimpleName().replace("Exception", "");
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
}
