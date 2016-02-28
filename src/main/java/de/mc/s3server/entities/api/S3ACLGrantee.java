/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.entities.api;

import java.util.List;

/**
 * @author Ralf Ulrich on 27.02.16.
 */
public interface S3ACLGrantee {

    /**
     * if value specified is the email address of an AWS account
     */
    List<String> getEmail();

    /**
     * if value specified is the canonical user ID of an AWS account
     */
    List<String> getId();

    /**
     * if granting permission to a predefined group.
     */
    List<String> getUri();

}
