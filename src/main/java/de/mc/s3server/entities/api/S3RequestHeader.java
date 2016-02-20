/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.entities.api;

/**
 * Created by Ralf Ulrich on 20.02.16.
 */
public interface S3RequestHeader {

    /**
     * Length of the message (without the headers) according to RFC
     * 2616. This header is required for PUTs and operations that load
     * XML, such as logging and ACLs.
     */
    String getContentLength();

    /**
     * The content type of the resource in case the request content in the
     * body. Example: text/plain
     */
    String getContentType();

    /**
     * The base64 encoded 128-bit MD5 digest of the message (without
     * the headers) according to RFC 1864. This header can be used as
     * a message integrity check to verify that the data is the same data
     * that was originally sent. Although it is optional, we recommend using
     * the Content-MD5 mechanism as an end-to-end integrity check. For
     * more information about REST request authentication, go to REST
     * Authentication in the Amazon Simple Storage Service Developer
     * Guide.
     */
    String getContentMD5();

    /**
     * When using signature version 4 to authenticate request, this header
     * provides a hash of the request payload. For more information see
     * Signature Calculations for the Authorization Header: Transferring
     * Payload in a Single Chunk (AWS Signature Version 4) (p. 20). When
     * uploading object in chunks, you set the value to STREAMING-
     * AWS4-HMAC-SHA256-PAYLOAD to indicate that the signature
     * covers only headers and that there is no payload. For more inform-
     * ation, see Signature Calculations for the Authorization Header:
     * Transferring Payload in Multiple Chunks (Chunked Upload) (AWS
     * Signature Version 4) (p. 32).
     */
    String getXamzContentSha256();

    String getConnection();

    /**
     * The current date and time according to the requester. Example:
     * Wed, 01 Mar 2006 12:00:00 GMT . When you specify the Au-
     * thorization header, you must specify either the x-amz-date or
     * the Date header
     */
    String getDate();

    /**
     * The current date and time according to the requester. Example:
     * Wed, 01 Mar 2006 12:00:00 GMT . When you specify the Au-
     * thorization header, you must specify either the x-amz-date or
     * the Date header. If you specify both, the value specified for the x-
     * amz-date header takes precedence.
     */
    String getXamzDate();

    /**
     * This header can be used in the following scenarios:
     * <ul>
     * <li>Provide security tokens for Amazon DevPay operations—Each
     * request that uses Amazon DevPay requires two x-amz-secur-
     * ity-token headers: one for the product token and one for the
     * user token. When Amazon S3 receives an authenticated request,
     * it compares the computed signature with the provided signature.
     * Improperly formatted multi-value headers used to calculate a
     * signature can cause authentication issues</li>
     * <li>Provide security token when using temporary security cre-
     * dentials—When making requests using temporary security cre-
     * dentials you obtained from IAM you must provide a security token
     * using this header.</li>
     * </ul>
     * This header is required for requests that use Amazon DevPay and
     * requests that are signed using temporary security credentials.
     */
    String getXamzSecurityToken();

    /**
     * The entity tag is a hash of the object. The ETag reflects changes only to the
     * contents of an object, not its metadata. The ETag may or may not be an MD5
     * digest of the object data. Whether or not it is depends on how the object was
     * created and how it is encrypted as described below:
     * <ul>
     * <li>Objects created by the PUT Object, POST Object, or Copy operation, or through
     * the AWS Management Console, and are encrypted by SSE-S3 or plaintext,
     * have ETags that are an MD5 digest of their object data.</li>
     * <li>Objects created by the PUT Object, POST Object, or Copy operation, or through
     * the AWS Management Console, and are encrypted by SSE-C or SSE-KMS,
     * have ETags that are not an MD5 digest of their object data.</li>
     * <li>If an object is created by either the Multipart Upload or Part Copy operation,
     * the ETag is not an MD5 digest, regardless of the method of encryption.</li>
     * </ul>
     * Type: String
     */
    String getEtag();

    /**
     * The name of the server that created the response.
     * Type: String
     * Default: AmazonS3
     */
    String getServer();

    /**
     * Specifies whether the object returned was (true) or was not (false) a delete
     * marker.
     * Type: Boolean
     * Valid Values: true | false
     * Default: false
     */
    String getXamzDeleteMarker();

    /**
     * A value created by Amazon S3 that uniquely identifies the request. In the unlikely
     * event that you have problems with Amazon S3, AWS can use this value to
     * troubleshoot the problem.
     * Type: String
     * Default: None
     */
    String getXamzRequestId();

    /**
     * The version of the object. When you enable versioning, Amazon S3 generates a
     * random number for objects added to a bucket. The value is UTF-8 encoded and
     * URL ready. When you PUT an object in a bucket where versioning has been
     * suspended, the version ID is always null .
     * Type: String
     * Valid Values: null | any URL-ready, UTF-8 encoded string
     * Default: null
     */
    String getXamzVersionId();

    /**
     * The information required for request authentication. For more inform-
     * ation, go to The Authentication Header in the Amazon Simple Stor-
     * age Service Developer Guide. For anonymous requests this header
     * is not required.
     */
    String getAuthorization();

    /**
     * When your application uses 100-continue, it does not send the re-
     * quest body until it receives an acknowledgment. If the message is
     * rejected based on the headers, the body of the message is not sent.
     * This header can be used only if you are sending a body.
     * Valid Values: 100-continue
     */
    String getExpect();

    /**
     * For path-style requests, the value is s3.amazonaws.com . For vir-
     * tual-style requests, the value is BucketName.s3.amazonaws.com .
     * For more information, go to Virtual Hosting in the Amazon Simple
     * Storage Service Developer Guide.
     * This header is required for HTTP 1.1 (most toolkits add this header
     * automatically); optional for HTTP/1.0 requests.
     */
    String getHost();

}
