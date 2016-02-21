/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

import de.mc.s3server.entities.api.S3RequestId;

/**
 * @author Ralf Ulrich on 20.02.16.
 */
public class InvalidPartOrderException extends S3ServerException {
    public InvalidPartOrderException(String resource, S3RequestId requestId) {
        super("The list of parts was not in ascending order." +
                "Parts list must specified in order by part number.", resource, requestId);
    }


}
