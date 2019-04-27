/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.ladon.s3server.common;

/**
 * Constants
 *
 * @author Ralf Ulrich on 17.02.16.
 */
public interface S3Constants {

    /**
     * Length of the message (without the headers) according to RFC
     * 2616. This header is required for PUTs and operations that load
     * XML, such as logging and ACLs.
     */
    String CONTENT_LENGTH = "Content-Length";
    /**
     * The content type of the resource in case the request content in the
     * body. Example: text/plain
     */
    String CONTENT_TYPE = "Content-Type";
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
    String CONTENT_MD5 = "Content-MD5";

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
    String X_AMZ_CONTENT_SHA256 = "x-amz-content-sha256";

    String CONNECTION = "Connection";
    /**
     * The current date and time according to the requester. Example:
     * Wed, 01 Mar 2006 12:00:00 GMT . When you specify the Au-
     * thorization header, you must specify either the x-amz-date or
     * the Date header
     */
    String DATE = "Date";

    /**
     * The current date and time according to the requester. Example:
     * Wed, 01 Mar 2006 12:00:00 GMT . When you specify the Au-
     * thorization header, you must specify either the x-amz-date or
     * the Date header. If you specify both, the value specified for the x-
     * amz-date header takes precedence.
     */
    String X_AMZ_DATE = "x-amz-date";

    /**
     * This header can be used in the following scenarios:
     * • Provide security tokens for Amazon DevPay operations—Each
     * request that uses Amazon DevPay requires two x-amz-secur-
     * ity-token headers: one for the product token and one for the
     * user token. When Amazon S3 receives an authenticated request,
     * it compares the computed signature with the provided signature.
     * Improperly formatted multi-value headers used to calculate a
     * signature can cause authentication issues
     * • Provide security token when using temporary security cre-
     * dentials—When making requests using temporary security cre-
     * dentials you obtained from IAM you must provide a security token
     * using this header. To learn more about temporary security creden-
     * tials, go to Making Requests.
     * This header is required for requests that use Amazon DevPay and
     * requests that are signed using temporary security credentials.
     */
    String X_AMZ_SECURITY_TOKEN = "x-amz-security-token";

    /**
     * The entity tag is a hash of the object. The ETag reflects changes only to the
     * contents of an object, not its metadata. The ETag may or may not be an MD5
     * digest of the object data. Whether or not it is depends on how the object was
     * created and how it is encrypted as described below:
     * • Objects created by the PUT Object, POST Object, or Copy operation, or through
     * the AWS Management Console, and are encrypted by SSE-S3 or plaintext,
     * have ETags that are an MD5 digest of their object data.
     * • Objects created by the PUT Object, POST Object, or Copy operation, or through
     * the AWS Management Console, and are encrypted by SSE-C or SSE-KMS,
     * have ETags that are not an MD5 digest of their object data.
     * • If an object is created by either the Multipart Upload or Part Copy operation,
     * the ETag is not an MD5 digest, regardless of the method of encryption.
     * Type: String
     */
    String ETAG = "ETag";

    /**
     * The name of the server that created the response.
     * Type: String
     * Default: AmazonS3
     */
    String SERVER = "Server";

    /**
     * Specifies whether the object returned was (true) or was not (false) a delete
     * marker.
     * Type: Boolean
     * Valid Values: true | false
     * Default: false
     */
    String X_AMZ_DELETE_MARKER = "x-amz-delete-marker";

    /**
     * A value created by Amazon S3 that uniquely identifies the request. In the unlikely
     * event that you have problems with Amazon S3, AWS can use this value to
     * troubleshoot the problem.
     * Type: String
     * Default: None
     */
    String X_AMZ_REQUEST_ID = "x-amz-request-id";

    /**
     * The version of the object. When you enable versioning, Amazon S3 generates a
     * random number for objects added to a bucket. The value is UTF-8 encoded and
     * URL ready. When you PUT an object in a bucket where versioning has been
     * suspended, the version ID is always null .
     * Type: String
     * Valid Values: null | any URL-ready, UTF-8 encoded string
     * Default: null
     */
    String X_AMZ_VERSION_ID = "x-amz-version-id";
    /**
     * The information required for request authentication. For more inform-
     * ation, go to The Authentication Header in the Amazon Simple Stor-
     * age Service Developer Guide. For anonymous requests this header
     * is not required.
     */
    String AUTHORIZAZION = "Authorization";

    /**
     * When your application uses 100-continue, it does not send the re-
     * quest body until it receives an acknowledgment. If the message is
     * rejected based on the headers, the body of the message is not sent.
     * This header can be used only if you are sending a body.
     * Valid Values: 100-continue
     */
    String EXPECT = "Expect";

    /**
     * For path-style requests, the value is s3.amazonaws.com . For vir-
     * tual-style requests, the value is BucketName.s3.amazonaws.com .
     * For more information, go to Virtual Hosting in the Amazon Simple
     * Storage Service Developer Guide.
     * This header is required for HTTP 1.1 (most toolkits add this header
     * automatically); optional for HTTP/1.0 requests.
     */
    String HOST = "Host";

    enum Connection {
        open, close
    }

    /**
     * A delimiter is a character you use to group keys.
     * All keys that contain the same string between the prefix , if specified, and
     * the first occurrence of the delimiter after the prefix are grouped under a single
     * result element, CommonPrefixes . If you don't specify the prefix parameter,
     * then the substring starts at the beginning of the key. The keys that are grouped
     * under CommonPrefixes result element are not returned elsewhere in the re-
     * sponse.
     */
    String DELIMITER = "delimiter";


    /**
     * Requests Amazon S3 to encode the response and specifies the encoding method to use.
     * An object key can contain any Unicode character; however, XML 1.0 parser
     * cannot parse some characters, such as characters with an ASCII value from
     * 0 to 10. For characters that are not supported in XML 1.0, you can add this
     * parameter to request that Amazon S3 encode the keys in the response.
     */
    String ENCODING_TYPE = "encoding-type";

    /**
     * Specifies the key to start with when listing objects in a bucket. Amazon S3
     * returns object keys in alphabetical order, starting with key after the marker in
     * order.
     */
    String MARKER = "marker";

    /**
     * Sets the maximum number of keys returned in the response body. You can
     * add this to your request if you want to retrieve fewer than the default 1000
     * keys.
     */
    String MAX_KEYS = "max-keys";

    /**
     * Limits the response to keys that begin with the specified prefix. You can use
     * prefixes to separate a bucket into different groupings of keys. (You can think
     * of using prefix to make groups in the same way you'd use a folder in a file
     * system.)
     */
    String PREFIX = "prefix";

    /**
     * Version ID of an object
     */
    String VERSION_ID = "versionId";

    /**
     * Query param on version enabled requests
     */
    String VERSIONS = "versions";

    /**
     *  Query param for acl requests
     */
    String ACL = "acl";
    /**
     * Specifies whether the object is (true) or is not (false) the current version of an object.
     */
    String IS_LATEST = "IsLatest";

    /**
     * Specifies the object version you want to start listing from. Also, see key-marker.
     */
    String VERSION_ID_MARKER = "version-id-marker";

    /**
     * Specifies the key in the bucket that you want to start listing from. Also, see version-id-marker.
     */
    String KEY_MARKER = "key-marker";

    /**
     * Can be used to specify caching behavior along the request/reply chain.
     */
    String CACHE_CONTROL = "Cache-Control";


    /**
     * Specifies presentational information for the object. e.g.
     * Content-Disposition: attachment; filename="fname.ext"
     */
    String CONTENT_DISPOSITION = "Content-Disposition";


    /**
     * Specifies what content encodings have been applied to the object and thus
     * what decoding mechanisms must be applied to obtain
     * the media-type referenced by the Content-Type header field.
     */
    String CONTENT_ENCODING = "Content-Encoding";


    /**
     * This implementation of the PUT operation creates a copy of an object that is already stored in Amazon S3.
     * A PUT copy operation is the same as performing a GET and then a PUT. Adding the request header,
     * x-amz-copy-source, makes the PUT operation copy the source object into the destination bucket.
     */
    String X_AMZ_COPY_SOURCE = "x-amz-copy-source";


    /**
     * Specifies whether the metadata is copied from the source object or replaced with metadata provided in the request.
     * Type: String
     * Default: COPY
     * Valid values: COPY | REPLACE
     */
    String X_AMZ_METADATA_DIRECTIVE = "x-amz-metadata-directive";
    String X_AMZ_METADATA_DIRECTIVE_REPLACE = "replace";
    String X_AMZ_METADATA_DIRECTIVE_COPY = "copy";

    /**
     * The date and time at which the object is no longer cacheable.
     */
    String EXPIRES = "Expires";

    /**
     * The date and time at which the object is no longer cacheable.
     */
    String X_AMZ_EXPIRATION = "x-amz-expiration";

    /**
     * If you don't specify, Standard is the default storage class. Amazon
     * S3 supports other storage classes.
     */
    String X_AMZ_STORAGE_CLASS = "x-amz-storage-class";

    /**
     * Headers starting with this prefix are user-defined metadata.
     * No
     * Within the PUT request header, the user-defined metadata is
     * limited to 2 KB in size. User-defined metadata is a set of key-
     * value pairs. The size of user-defined metadata is measured by
     * taking the sum of the number of bytes in the UTF-8 encoding of
     * each key and value. Amazon S3 doesn't validate or interpret
     * user-defined metadata.
     */
    String X_AMZ_META_PREFIX = "x-amz-meta-";


    /**
     * Prefix for general Amazon headers: x-amz-
     */
    String X_AMZ_PREFIX = "x-amz-";

    /**
     * If the bucket is configured as a website, redirects requests for
     * this object to another object in the same bucket or to an external
     * URL.
     */
    String X_AMZ_WEBSITE_REDIRECT_LOCATION = "x-amz-website-redirect-location";

    /**
     * The canned ACL to apply to the object.
     * Type: String
     * Default: private
     * Valid Values: private | public-read | public-read-
     * write | aws-exec-read | authenticated-read |
     * bucket-owner-read | bucket-owner-full-control
     */
    String X_AMZ_ACL = "x-amz-acl";

    /**
     * Allows grantee to read the object/bucket data and its metadata.
     * Type: String
     * Default: None
     * Constraints: None
     */
    String X_AMZ_GRANT_READ = "x-amz-grant-read";

    /**
     * This applies only when granting permission on a
     * bucket.
     * Type: String
     * Default: None
     * Constraints: None
     */
    String X_AMZ_GRANT_WRITE = "x-amz-grant-write";

    /**
     * Allows grantee to read the object/bucket ACL.
     * Type: String
     * Default: None
     * Constraints: None
     */
    String X_AMZ_GRANT_READ_ACP = "x-amz-grant-read-acp";

    /**
     * Allows grantee to write the ACL for the applicable object.
     * Type: String
     * Default: None
     * Constraints: None
     */
    String X_AMZ_GRANT_WRITE_ACP = "x-amz-grant-write-acp";

    /**
     * Allows grantee the READ, READ_ACP, and WRITE_ACP permissions on the object.
     * Type: String
     * Default: None
     * Constraints: None
     */
    String X_AMZ_GRANT_FULL_CONTROL = "x-amz-grant-full-control";

    /**
     * The canned ACL to apply to the object.
     */
    enum CannedACL {
        PRIVATE("private"),
        PUBLIC_READ("public-read"),
        PUBLIC_READ_WRITE("public-read-write"),
        AWS_EXEC_READ("aws-exec-read"),
        AUTHENTICATED_READ("authenticated-read"),
        BUCKET_OWNER_READ("bucket-owner-read"),
        BUCKET_OWNER_FULL_CONTROL("bucket-owner-full-control");

        private String value;

        CannedACL(String value) {
            this.value = value;
        }


    }
}
