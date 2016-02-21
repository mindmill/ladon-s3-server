/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

import de.mc.s3server.entities.api.S3RequestId;

/**
 * @author Ralf Ulrich on 20.02.16.
 */
public class MissingAttachmentException extends S3ServerException {
    public MissingAttachmentException(String resource, S3RequestId requestId) {
        super("A SOAP attachment was expected, but none where found.", resource, requestId);
    }


}
