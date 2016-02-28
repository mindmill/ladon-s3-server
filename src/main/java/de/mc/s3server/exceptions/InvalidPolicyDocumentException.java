/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

import de.mc.s3server.entities.api.S3RequestId;

import java.net.HttpURLConnection;

/**
 * @author Ralf Ulrich on 20.02.16.
 */
public class InvalidPolicyDocumentException extends S3ServerException {
    public InvalidPolicyDocumentException(String resource, S3RequestId requestId) {
        super("The content of the form does not meet the conditions specified in the policy document.", resource, requestId, HttpURLConnection.HTTP_BAD_REQUEST);
    }


}
