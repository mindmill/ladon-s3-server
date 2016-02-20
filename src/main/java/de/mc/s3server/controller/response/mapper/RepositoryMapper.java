/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.controller.response.mapper;

import de.mc.s3server.controller.response.entities.Bucket;
import de.mc.s3server.controller.response.entities.ListAllMyBucketsResult;
import de.mc.s3server.controller.response.entities.Owner;
import de.mc.s3server.entities.api.S3Bucket;
import de.mc.s3server.entities.api.S3User;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Ralf Ulrich on 17.02.16.
 */
public class RepositoryMapper {

    public static ListAllMyBucketsResult listAllMyBucketsResult(S3User user, List<S3Bucket> buckets) {
        ListAllMyBucketsResult result = new ListAllMyBucketsResult(new Owner(user.getUserID(), user.getUserName()));
        result.setBucketList(buckets.stream().map(b -> new Bucket(b.getBucketName(), b.getCreationDate())).collect(Collectors.toList()));
        return result;

    }


}
