/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.entities.impl;

import de.mc.s3server.common.StreamUtils;
import de.mc.s3server.entities.api.*;
import de.mc.s3server.exceptions.InternalErrorException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.UUID;

/**
 * @author Ralf Ulrich on 17.02.16.
 */
public class S3CallContextImpl implements S3CallContext {


    private HttpServletResponse response;
    private HttpServletRequest request;
    private S3RequestHeader header;
    private S3RequestParams params;
    private String requestId = UUID.randomUUID().toString();
    private S3User user;

    public S3CallContextImpl(HttpServletRequest request, HttpServletResponse response, S3User user, Map<String, String[]> params) {
        this.header = new S3RequestHeaderImpl(request);
        this.params = new S3RequestParamsImpl(params);
        this.request = request;
        this.response = response;
        this.user = user;

    }

    @Override
    public void setResponseHeader(S3ResponseHeader responseHeader) {
        S3ResponseHeaderImpl.appendHeaderToResponse(response, (S3ResponseHeaderImpl) responseHeader);
    }

    @Override
    public S3RequestId getRequestId() {
        return () -> requestId;
    }

    @Override
    public void setContent(InputStream inputStream) {
        try (InputStream in = inputStream) {
            StreamUtils.copy(in, response.getOutputStream());
            response.flushBuffer();
        } catch (IOException | InterruptedException e) {
            throw new InternalErrorException("", getRequestId());
        }
    }

    @Override
    public S3User getUser() {
        return user;
    }

    @Override
    public InputStream getContent() throws IOException {
        return request.getInputStream();
    }


    @Override
    public S3RequestHeader getHeader() {
        return header;
    }

    @Override
    public S3RequestParams getParams() {
        return params;
    }

}
