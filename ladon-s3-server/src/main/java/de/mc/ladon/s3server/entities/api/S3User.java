/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.ladon.s3server.entities.api;

import java.util.Set;

/**
 * @author Ralf Ulrich on 17.02.16.
 */
public interface S3User {

    String getUserId();

    String getUserName();

    String getSecretKey();

    String getPublicKey();

    Set<String> getRoles();

}
