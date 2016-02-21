/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

import de.mc.s3server.entities.api.S3RequestId;

/**
 * Created by Ralf Ulrich on 20.02.16.
 */
public class AccountProblem extends AccessDeniedException {

    public AccountProblem(String resource, S3RequestId requestId) {
        super("There is a problem with your AWS account that prevents the operation from completing successfully.", resource, requestId);
    }

    public AccountProblem(String message, String resource, S3RequestId requestId) {
        super(message, resource, requestId);
    }


}
