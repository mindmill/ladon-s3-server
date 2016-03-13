/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.ladon.s3server.repository.api;

import de.mc.ladon.s3server.entities.api.S3Bucket;
import de.mc.ladon.s3server.entities.api.S3CallContext;
import de.mc.ladon.s3server.entities.api.S3ListBucketResult;
import de.mc.ladon.s3server.entities.api.S3User;

import java.util.List;

/**
 * This is the abstraction interface to implement your own repository
 *
 * @author Ralf Ulrich on 17.02.16.
 */
public interface S3Repository {

    /**
     * List all buckets of the current user
     *
     * @param callContext the S3CallContext
     * @return a list of all buckets of the user
     */
    List<S3Bucket> listAllBuckets(S3CallContext callContext);

    /**
     * This implementation of the PUT operation creates a new bucket. Not every string is an acceptable bucket name.
     *
     * @param callContext        the S3CallContext
     * @param bucketName         name of the bucket
     * @param locationConstraint configuration or null. Contains the location information
     */
    void createBucket(S3CallContext callContext, String bucketName, String locationConstraint);

    /**
     * This implementation of the DELETE operation deletes the bucket named in the URI. All objects (including
     * all object versions and delete markers) in the bucket must be deleted before the bucket itself can be
     * deleted.
     *
     * @param callContext the S3CallContext
     * @param bucketName  name of the bucket to delete
     */
    void deleteBucket(S3CallContext callContext, String bucketName);

    /**
     * This implementation of the PUT operation adds an object to a bucket. You must have WRITE permissions
     * on a bucket to add an object to it.
     * <p>
     * To ensure that data is not corrupted traversing the network, use the Content-MD5 header. When you
     * use this header, Amazon S3 checks the object against the provided MD5 value and, if they do not match,
     * returns an error. Additionally, you can calculate the MD5 while putting an object to Amazon S3 and
     * compare the returned ETag to the calculated MD5 value.
     *
     * @param callContext the S3CallContext
     * @param bucketName  name of the bucket
     * @param objectKey   key of the object to create (like filename in a file system, e.g. "test/readme.txt")
     */
    void createObject(S3CallContext callContext, String bucketName, String objectKey);

    /**
     * This implementation of the GET operation retrieves objects from Amazon S3. To use GET , you must have
     * READ access to the object.
     * <p>
     * An Amazon S3 bucket has no directory hierarchy such as you would find in a typical computer file system.
     * You can, however, create a logical hierarchy by using object key names that imply a folder structure. For
     * example, instead of naming an object sample.jpg , you can name it
     * photos/2006/February/sample.jpg .
     *
     * @param callContext the S3CallContext
     * @param bucketName  name of the bucket
     * @param objectKey   key of the object to get
     * @param head        if true, only head data is written to the response, if false, the content is written as well
     */
    void getObject(S3CallContext callContext, String bucketName, String objectKey, boolean head);

    /**
     * This implementation of the GET operation returns some or all (up to 1000) of the objects in a bucket. You
     * can use the request parameters as selection criteria to return a subset of the objects in a bucket. A 200
     * OK response can contain valid or invalid xml. Make sure to design your application to parse the contents
     * of the response and handle it appropriately.
     * To use this implementation of the operation, you must have READ access to the bucket.
     *
     * @param callContext the S3CallContext
     * @param bucketName  name of the bucket
     * @return a S3ListBucketResult object containing the S3Object elements
     */
    S3ListBucketResult listBucket(S3CallContext callContext, String bucketName);

    /**
     * The DELETE operation removes the null version (if there is one) of an object and inserts a delete marker,
     * which becomes the current version of the object. If there isn't a null version, Amazon S3 does not remove
     * any objects.
     * <p>
     * To remove a specific version, you must be the bucket owner and you must use the versionId
     * subresource. Using this subresource permanently deletes the version. If the object deleted is a delete
     * marker, Amazon S3 sets the response header, x-amz-delete-marker, to true .
     *
     * @param callContext the S3CallContext
     * @param bucketName  name of the bucket
     * @param objectKey   key of the object
     */
    void deleteObject(S3CallContext callContext, String bucketName, String objectKey);

    /**
     * Used for the HEAD request on a bucket.
     * Intention is only to check whether the bucket exists and the user has access rights.
     *
     * @param callContext the S3CallContext
     * @param bucketName  name of the bucket
     */
    void getBucket(S3CallContext callContext, String bucketName);

    /**
     * Used to load the user details for a given access key. Only needed if security is enabled.
     *
     * @param callContext the S3CallContext
     * @param accessKey   the access key of the request
     * @return the S3User object from the repository
     */
    S3User getUser(S3CallContext callContext, String accessKey);
}
