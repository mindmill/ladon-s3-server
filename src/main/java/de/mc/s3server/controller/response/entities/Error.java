/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.controller.response.entities;

import de.mc.s3server.exceptions.S3ServerException;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Simple error response
 *
 * Created by Ralf Ulrich on 20.02.16.
 */
@XmlRootElement(name = "Error")
public class Error {
    private String code;
    private String message;
    private String resource;
    private String requestId;

    public Error() {
    }

    public Error(S3ServerException exception) {
        this.code = exception.getCode();
        this.message = exception.getMessage();
        this.resource = exception.getResource();
        this.requestId = exception.getRequestId();
    }

    @XmlElement(name = "Code")
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @XmlElement(name = "Message")
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @XmlElement(name = "Resource")
    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    @XmlElement(name = "RequestId")
    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
}
