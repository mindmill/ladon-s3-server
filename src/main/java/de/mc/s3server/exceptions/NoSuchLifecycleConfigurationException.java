/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

import de.mc.s3server.entities.api.S3RequestId;

/**
 * Created by Ralf Ulrich on 20.02.16.
 */
public class NoSuchLifecycleConfigurationException extends S3ServerException {

    public NoSuchLifecycleConfigurationException(String resource, S3RequestId requestId) {
        super("The lifecycle configuration does not exist.", resource, requestId);
    }


}
