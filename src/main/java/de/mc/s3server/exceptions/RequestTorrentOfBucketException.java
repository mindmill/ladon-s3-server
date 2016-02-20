/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.exceptions;

/**
 * Created by Ralf Ulrich on 20.02.16.
 */
public class RequestTorrentOfBucketException extends S3ServerException{
    public RequestTorrentOfBucketException() {
        super("Buckets don't have torrent files.");
    }

    public RequestTorrentOfBucketException(String message) {
        super(message);
    }

    public RequestTorrentOfBucketException(String message, Throwable cause) {
        super(message, cause);
    }

    public RequestTorrentOfBucketException(Throwable cause) {
        super(cause);
    }

    public RequestTorrentOfBucketException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
