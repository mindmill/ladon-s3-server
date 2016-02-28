/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

import de.mc.s3server.entities.api.S3RequestId;

import java.net.HttpURLConnection;

/**
 * @author Ralf Ulrich on 20.02.16.
 */
public class RequestTorrentOfBucketException extends S3ServerException {
    public RequestTorrentOfBucketException(String resource, S3RequestId requestId) {
        super("Buckets don't have torrent files.", resource, requestId, HttpURLConnection.HTTP_BAD_REQUEST);
    }


}
