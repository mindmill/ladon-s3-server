/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.entities.api;

/**
 * @author Ralf Ulrich on 21.02.16.
 */
@FunctionalInterface
public interface S3RequestId {

    String get();

}
