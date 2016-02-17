/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.entities.api;

import java.util.Date;

/**
 * Created by max on 17.02.16.
 */
public interface S3Bucket {

    String getBucketName();

    Date getCreationDate();
}
