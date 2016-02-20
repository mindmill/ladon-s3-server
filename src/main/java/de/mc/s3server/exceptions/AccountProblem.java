/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

/**
 * Created by Ralf Ulrich on 20.02.16.
 */
public class AccountProblem extends AccessDeniedException{

    public AccountProblem() {
        super("There is a problem with your AWS account that prevents the operation from completing successfully.");
    }

    public AccountProblem(String message) {
        super(message);
    }

    public AccountProblem(String message, Throwable cause) {
        super(message, cause);
    }

    public AccountProblem(Throwable cause) {
        super(cause);
    }

    public AccountProblem(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
