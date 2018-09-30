/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.ladon.s3server.entities.api;

import java.util.List;

/**
 * @author Ralf Ulrich on 21.02.16.
 */
public interface S3ListBucketResult {

    List<S3Object> getObjects();

    List<String> getCommonPrefixes();

    String getBucketName();

    boolean isTruncated();

    String nextKeyMarker();

    String nextVersionIdMarker();





}
