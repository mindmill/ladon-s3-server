/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.controller;

import de.mc.s3server.controller.response.entities.ListAllMyBucketsResult;
import de.mc.s3server.controller.response.entities.ListBucketResult;
import de.mc.s3server.controller.response.mapper.ResponseWrapper;
import de.mc.s3server.entities.api.S3CallContext;
import de.mc.s3server.entities.impl.S3UserImpl;
import de.mc.s3server.repository.api.Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

/**
 * Created by Ralf Ulrich on 17.02.16.
 */
@RestController
@RequestMapping("${s3.api.base.url}")
public class S3Controller {

    private Repository repository;

    @Autowired
    public S3Controller(Repository repository) {
        this.repository = repository;
    }


    /**
     * List all my buckets
     *
     * @param callContext
     * @return
     */
    @RequestMapping(value = {"" , "/"} , method = RequestMethod.GET, produces = MediaType.APPLICATION_XML_VALUE)
    public ListAllMyBucketsResult listAllMyBucketsResult(S3CallContext callContext) {
        return ResponseWrapper.listAllMyBucketsResult(new S3UserImpl("maxid", "max"), repository.listAllBuckets(callContext));
    }

    /**
     * This implementation of the DELETE operation deletes the bucket named in the URI. All objects (including
     * all object versions and delete markers) in the bucket must be deleted before the bucket itself can be
     * deleted.
     *
     * @param callContext S3CallContext
     * @param bucketName  name of the bucket
     */
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequestMapping(value = "/{bucketName}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_XML_VALUE)
    public void deleteBucket(S3CallContext callContext, @PathVariable("bucketName") String bucketName) {
        repository.deleteBucket(callContext, bucketName);
    }

    /**
     * This implementation of the GET operation returns some or all (up to 1000) of the objects in a bucket. You
     * can use the request parameters as selection criteria to return a subset of the objects in a bucket. A 200
     * OK response can contain valid or invalid xml. Make sure to design your application to parse the contents
     * of the response and handle it appropriately.
     * To use this implementation of the operation, you must have READ access to the bucket.
     *
     * @param callContext S3CallContext
     * @param bucketName name of the bucket
     * @return a list of objects ( up to 1000 )
     */
    @ResponseStatus(HttpStatus.OK)
    @RequestMapping(value = "/{bucketName}", method = RequestMethod.GET, produces = MediaType.APPLICATION_XML_VALUE)
    public ListBucketResult listBucketsResult(S3CallContext callContext, @PathVariable("bucketName") String bucketName) {
        return ResponseWrapper.listBucketResult(callContext,bucketName,repository.listBucket(callContext,bucketName),false);
    }


}
