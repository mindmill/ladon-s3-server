/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.entities.impl;

import de.mc.s3server.entities.api.S3CallContext;
import de.mc.s3server.entities.api.S3RequestHeader;
import org.springframework.security.core.context.SecurityContext;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by Ralf Ulrich on 17.02.16.
 */
public class S3CallContextImpl implements S3CallContext {

    private SecurityContext securityContext;
    private S3RequestHeader header;

    public S3CallContextImpl(SecurityContext securityContext, HttpServletRequest request) {
        this.securityContext = securityContext;
        this.header = new S3RequestHeaderImpl(request);
    }

    @Override
    public S3RequestHeader getHeader() {
        return header;
    }

    public SecurityContext getSecurityContext() {
        return securityContext;
    }
}
