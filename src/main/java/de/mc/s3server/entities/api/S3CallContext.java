/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.entities.api;

import org.springframework.security.core.context.SecurityContext;

/**
 * Created by Ralf Ulrich on 17.02.16.
 */
public interface S3CallContext {

    S3RequestHeader getHeader();

    S3RequestParams getParams();

    void setResponseHeader(S3ResponseHeader responseHeader);

    SecurityContext getSecurityContext();
}
