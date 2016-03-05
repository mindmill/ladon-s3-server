/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.entities.api;

/**
 * @author Ralf Ulrich on 17.02.16.
 */
public interface S3User {

    String getUserId();

    String getUserName();

    String getSecretKey();

    String getPublicKey();

    String getEmail();

}
