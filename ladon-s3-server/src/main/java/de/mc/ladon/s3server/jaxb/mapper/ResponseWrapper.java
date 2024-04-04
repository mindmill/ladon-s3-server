/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.ladon.s3server.jaxb.mapper;

import de.mc.ladon.s3server.entities.api.*;
import de.mc.ladon.s3server.jaxb.entities.*;

import java.util.List;
import java.util.stream.Collectors;

import static de.mc.ladon.s3server.common.EncodingUtil.getEncoded;

/**
 * @author Ralf Ulrich on 17.02.16.
 */
public class ResponseWrapper {

    public static ListAllMyBucketsResult listAllMyBucketsResult(S3User user, List<S3Bucket> buckets) {
        ListAllMyBucketsResult result = new ListAllMyBucketsResult(new Owner(user.getUserId(), user.getUserName()));
        result.setBucketList(buckets.stream().map(b -> new Bucket(b.getBucketName(), b.getCreationDate())).collect(Collectors.toList()));
        return result;

    }

    public static ListBucketResult listBucketResult(S3CallContext callContext, S3ListBucketResult list) {
        return new ListBucketResult(callContext, list.getBucketName(),
                list.getObjects().stream().map(o -> new Contents(new Owner(o.getOwner().getUserId(), o.getOwner().getUserName()),
                        getEncoded(callContext, o.getKey()), o.getLastModified(), o.getETag(), o.getSize(), o.getStorageClass()))
                        .collect(Collectors.toList()),
                list.getCommonPrefixes() != null ? list.getCommonPrefixes().stream()
                        .map(CommonPrefixes::new).collect(Collectors.toList())  : null,
                list.isTruncated());
    }

    public static CopyObjectResult copyObjectResult(S3Object copiedObject) {
        return new CopyObjectResult(copiedObject.getLastModified(), copiedObject.getETag());
    }

    public static ListVersionsResult listVersionsResult(S3CallContext callContext, S3ListBucketResult list) {
        return new ListVersionsResult(callContext, list.getBucketName(),
                list.getObjects().stream().map(o -> {
                    if (o.isDeleted()) {
                        return new DeleteMarker(new Owner(o.getOwner().getUserId(), o.getOwner().getUserName()),
                                getEncoded(callContext, o.getKey()), o.getVersionId(), o.isLatest(), o.getLastModified(), o.getETag(), o.getSize(), o.getStorageClass());
                    } else {
                        return new Version(new Owner(o.getOwner().getUserId(), o.getOwner().getUserName()),
                                getEncoded(callContext, o.getKey()), o.getVersionId(), o.isLatest(), o.getLastModified(), o.getETag(), o.getSize(), o.getStorageClass());
                    }
                }).collect(Collectors.toList()), list.getCommonPrefixes() != null ?  list.getCommonPrefixes().stream()
                .map(CommonPrefixes::new).collect(Collectors.toList())  : null, list.isTruncated(),
                getEncoded(callContext, list.nextKeyMarker()),
                getEncoded(callContext, list.nextVersionIdMarker()));
    }


}
