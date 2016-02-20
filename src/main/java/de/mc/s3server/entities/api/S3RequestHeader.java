/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.entities.api;

/**
 * Created by Ralf Ulrich on 20.02.16.
 */
public interface S3RequestHeader {

    String getContentLength();

    String getContentType();

    String getContentMD5();

    String getXamzContentSha256();

    String getConnection();

    String getDate();

    String getXamzDate();

    String getXamzSecurityToken();

    String getEtag();

    String getServer();

    String getXamzDeleteMarker();

    String getXamzRequestId();

    String getXamzVersionId();

    String getAuthorization();

    String getExpect();

    String getHost();

}
