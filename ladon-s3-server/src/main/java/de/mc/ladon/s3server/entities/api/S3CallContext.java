/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.ladon.s3server.entities.api;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Ralf Ulrich on 17.02.16.
 */
public interface S3CallContext {

    void setContent(InputStream inputStream);

    S3User getUser();

    S3RequestHeader getHeader();

    S3RequestParams getParams();

    S3RequestId getRequestId();

    String getMethod();

    String getUri();

    String getQueryString();

    HttpServletRequest getRawRequest();

    InputStream getContent() throws IOException;

    void setResponseHeader(S3ResponseHeader responseHeader);

}
