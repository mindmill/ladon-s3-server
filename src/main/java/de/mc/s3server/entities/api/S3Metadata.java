/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.entities.api;

import javax.servlet.http.HttpServletResponse;

/**
 * @author Ralf Ulrich on 17.02.16.
 */
public interface S3Metadata {

    String get(String key);

    void putInResponse(HttpServletResponse response);

}
