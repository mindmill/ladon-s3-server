/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.controller;

import de.mc.s3server.controller.response.entities.ListAllMyBucketsResult;
import de.mc.s3server.controller.response.entities.ListBucketResult;
import de.mc.s3server.controller.response.mapper.ResponseWrapper;
import de.mc.s3server.entities.api.S3CallContext;
import de.mc.s3server.entities.api.S3Object;
import de.mc.s3server.entities.impl.S3ResponseHeaderImpl;
import de.mc.s3server.entities.impl.S3UserImpl;
import de.mc.s3server.repository.api.S3Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.HandlerMapping;

import javax.servlet.http.HttpServletRequest;

/**
 * Main Controller of the s3server
 *
 * @author Ralf Ulrich on 17.02.16.
 */
@RestController
@RequestMapping("${s3server.api.base.url}")
public class S3Controller {

    private S3Repository repository;

    @Autowired
    public S3Controller(S3Repository repository) {
        this.repository = repository;
    }

    /**
     * List all my buckets
     *
     * @param callContext S3CallContext
     * @return List of all my buckets
     */
    @RequestMapping(value = {"", "/"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_XML_VALUE)
    public ListAllMyBucketsResult listAllMyBucketsResult(S3CallContext callContext) {
        return ResponseWrapper.listAllMyBucketsResult(new S3UserImpl("test", "test"), repository.listAllBuckets(callContext));
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
     * This implementation of the PUT operation creates a new bucket. Not every string is an acceptable bucket name.
     *
     * @param callContext S3CallContext
     * @param bucketName  the bucket to create
     */
    @ResponseStatus(HttpStatus.CREATED)
    @RequestMapping(value = "/{bucketName}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_XML_VALUE)
    public void createBucket(S3CallContext callContext, @PathVariable("bucketName") String bucketName) {
        repository.createBucket(callContext, bucketName);
    }

    /**
     * This implementation of the GET operation returns some or all (up to 1000) of the objects in a bucket. You
     * can use the request parameters as selection criteria to return a subset of the objects in a bucket. A 200
     * OK response can contain valid or invalid xml. Make sure to design your application to parse the contents
     * of the response and handle it appropriately.
     * To use this implementation of the operation, you must have READ access to the bucket.
     *
     * @param callContext S3CallContext
     * @param bucketName  name of the bucket
     * @return a list of objects ( up to 1000 )
     */
    @ResponseStatus(HttpStatus.OK)
    @RequestMapping(value = "/{bucketName}", method = RequestMethod.GET, produces = MediaType.APPLICATION_XML_VALUE)
    public ListBucketResult listBucketsResult(S3CallContext callContext, @PathVariable("bucketName") String bucketName) {
        return ResponseWrapper.listBucketResult(callContext, repository.listBucket(callContext, bucketName));
    }


    /**
     * This implementation of the GET operation retrieves objects from Amazon S3. To use GET , you must have
     * READ access to the object.
     * <p>
     * An Amazon S3 bucket has no directory hierarchy such as you would find in a typical computer file system.
     * You can, however, create a logical hierarchy by using object key names that imply a folder structure. For
     * example, instead of naming an object sample.jpg , you can name it
     * photos/2006/February/sample.jpg .
     *
     * @param callContext S3CallContext
     * @param bucketName  name of the bucket
     * @param request     the HttpRequest
     */
    @ResponseStatus(HttpStatus.OK)
    @RequestMapping(value = "/{bucketName}/**", method = RequestMethod.GET, produces = {MediaType.APPLICATION_XML_VALUE})
    public void getObject(S3CallContext callContext, @PathVariable("bucketName") String bucketName, HttpServletRequest request) {
        String objectKey = getObjectKey(request, bucketName);
        S3ResponseHeaderImpl header = new S3ResponseHeaderImpl();
        S3Object s3Object = repository.getObject(callContext, bucketName, objectKey);
        header.setContentLength(s3Object.getSize());
        header.setContentType(s3Object.getMimeType());
        callContext.setResponseHeader(header);
        callContext.setContent(s3Object.getContent());
    }

    /**
     * @param callContext
     * @param bucketName
     * @param request
     */
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequestMapping(value = "/{bucketName}/**", method = RequestMethod.DELETE, produces = {MediaType.APPLICATION_XML_VALUE})
    public void deleteObject(S3CallContext callContext, @PathVariable("bucketName") String bucketName, HttpServletRequest request) {
        String objectKey = getObjectKey(request, bucketName);
        repository.deleteObject(callContext, bucketName, objectKey);
    }

    /*
     * get rest of the path after the bucketname
     */
    private String getObjectKey(HttpServletRequest request, String bucketName) {
        String fullPath = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        int bucketIndex = fullPath.indexOf(bucketName);
        return fullPath.substring(bucketIndex + bucketName.length() + 1);
    }


}
