/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.entities.impl;

import de.mc.s3server.entities.api.S3CallContext;
import org.springframework.security.core.context.SecurityContext;

/**
 * Created by max on 17.02.16.
 */
public class S3CallContextImpl implements S3CallContext {

    private SecurityContext securityContext;

    public S3CallContextImpl(SecurityContext securityContext) {
        this.securityContext = securityContext;
    }

    public SecurityContext getSecurityContext() {
        return securityContext;
    }
}
