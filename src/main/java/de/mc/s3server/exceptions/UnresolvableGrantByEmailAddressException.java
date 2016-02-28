/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

import de.mc.s3server.entities.api.S3RequestId;

import java.net.HttpURLConnection;

/**
 * @author Ralf Ulrich on 20.02.16.
 */
public class UnresolvableGrantByEmailAddressException extends S3ServerException {
    public UnresolvableGrantByEmailAddressException(String resource, S3RequestId requestId) {
        super("The email address you provided does not match any account on record.", resource, requestId, HttpURLConnection.HTTP_BAD_REQUEST);
    }


}
