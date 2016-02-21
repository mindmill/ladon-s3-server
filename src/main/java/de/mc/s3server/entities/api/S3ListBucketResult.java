/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.entities.api;

import java.util.List;

/**
 * Created by Ralf Ulrich on 21.02.16.
 */
public interface S3ListBucketResult {

    List<S3Object> getObjects();

    String getBucketName();

    boolean isTruncated();





}
