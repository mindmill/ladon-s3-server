/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

import de.mc.s3server.entities.api.S3RequestId;

import java.net.HttpURLConnection;

/**
 * @author Ralf Ulrich on 20.02.16.
 */
public class PermanentRedirectException extends S3ServerException {
    public PermanentRedirectException(String resource, S3RequestId requestId) {
        super("The bucket you are attempting to access must be addressed using the specified endpoint." +
                " Send all future requests to this endpoint.", resource, requestId, HttpURLConnection.HTTP_MOVED_PERM);
    }


}
