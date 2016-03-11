/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.ladon.s3server.exceptions;

import de.mc.ladon.s3server.entities.api.S3RequestId;

/**
 * @author Ralf Ulrich on 20.02.16.
 */
public class InvalidRangeException extends S3ServerException {
    public InvalidRangeException(String resource, S3RequestId requestId) {
        // THERE IS NO REQUESTED_RANGE_NOT_SATISFIABLE in HttpURLConnection
        super("The requested range cannot be satisfied.", resource, requestId, 416);
    }


}
