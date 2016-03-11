/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.ladon.s3server.exceptions;

import de.mc.ladon.s3server.entities.api.S3RequestId;

import java.net.HttpURLConnection;

/**
 * @author Ralf Ulrich on 20.02.16.
 */
public class InvalidPartException extends S3ServerException {
    public InvalidPartException(String resource, S3RequestId requestId) {
        super("One or more of the specified parts could not be found. " +
                "The part might not have been uploaded, " +
                "or the specified entity tag might not have matched the part's entity tag.", resource, requestId, HttpURLConnection.HTTP_BAD_REQUEST);
    }


}
