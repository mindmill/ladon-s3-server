/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

import de.mc.s3server.entities.api.S3RequestId;

/**
 * @author Ralf Ulrich on 20.02.16.
 */
public class RequestIsNotMultiPartContentException extends S3ServerException {
    public RequestIsNotMultiPartContentException(String resource, S3RequestId requestId) {
        super("Bucket POST must be of the enclosure-type multipart/form-data.", resource, requestId);
    }

}
