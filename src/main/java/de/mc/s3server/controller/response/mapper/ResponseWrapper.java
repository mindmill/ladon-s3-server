/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.controller.response.mapper;

import de.mc.s3server.controller.response.entities.*;
import de.mc.s3server.entities.api.S3Bucket;
import de.mc.s3server.entities.api.S3CallContext;
import de.mc.s3server.entities.api.S3Object;
import de.mc.s3server.entities.api.S3User;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Ralf Ulrich on 17.02.16.
 */
public class ResponseWrapper {

    public static ListAllMyBucketsResult listAllMyBucketsResult(S3User user, List<S3Bucket> buckets) {
        ListAllMyBucketsResult result = new ListAllMyBucketsResult(new Owner(user.getUserID(), user.getUserName()));
        result.setBucketList(buckets.stream().map(b -> new Bucket(b.getBucketName(), b.getCreationDate())).collect(Collectors.toList()));
        return result;

    }

    public static ListBucketResult listBucketResult(S3CallContext callContext, String bucketName, List<S3Object> objects, boolean isTruncated) {
        return new ListBucketResult(callContext, bucketName,
                objects.stream().map(o -> new Contents(new Owner(o.getOwner().getUserID(), o.getOwner().getUserName()),
                        o.getKey(), o.getLastModified(), o.getETag(), o.getSize(), o.getStorageClass()))
                        .collect(Collectors.toList()), isTruncated);
    }


}
