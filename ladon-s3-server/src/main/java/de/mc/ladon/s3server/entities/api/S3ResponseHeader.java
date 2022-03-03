/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.ladon.s3server.entities.api;

import de.mc.ladon.s3server.common.S3Constants;

import java.util.Date;

/**
 * @author Ralf Ulrich on 20.02.16.
 */
public interface S3ResponseHeader {

    S3Metadata getMetadata();
    /**
     * The length in bytes of the body in the response.
     * Default: None
     *
     * @param contentLength The length in bytes of the body in the response.
     */
    void setContentLength(Long contentLength);
    Long getContentLength();

    /**
     * The MIME type of the content. For example, Content-Type: text/html; charset=utf-8
     * Default: None
     *
     * @param contentType The MIME type of the content. For example, Content-Type: text/html; charset=utf-8
     */
    void setContentType(String contentType);
    String getContentType();


    /**
     * specifies whether the connection to the server is open or closed.
     * Default: None
     *
     * @param connection specifies whether the connection to the server is open or closed.
     */
    void setConnection(S3Constants.Connection connection);
    S3Constants.Connection getConnection();


    /**
     * The date and time Amazon S3 responded, for example, Wed, 01 Mar 2006 12:00:00 GMT.
     * Default: None
     *
     * @param date The date and time Amazon S3 responded, for example, Wed, 01 Mar 2006 12:00:00 GMT.
     */
    void setDate(Date date);
    Date getDate();


    /**
     * Sets the cache control and expires header
     *
     * @param expires the wanted cache duration in seconds, null if no cache
     */
    void setExpires(Long expires);
    Long getExpires();

    /**
     * Used to set the Last-Modified header of the response
     *
     * @param lastModified date of the last modification
     */
    void setLastModified(Date lastModified);
    Date getLastModified();

    /**
     * The entity tag is a hash of the object. The ETag reflects changes only to the
     * contents of an object, not its metadata. The ETag may or may not be an MD5
     * digest of the object data. Whether or not it is depends on how the object was
     * created and how it is encrypted as described below:
     * <ul>
     * <li> Objects created by the PUT Object, POST Object, or Copy operation, or through
     * the AWS Management Console, and are encrypted by SSE-S3 or plaintext,
     * have ETags that are an MD5 digest of their object data.</li>
     * <li> Objects created by the PUT Object, POST Object, or Copy operation, or through
     * the AWS Management Console, and are encrypted by SSE-C or SSE-KMS,
     * have ETags that are not an MD5 digest of their object data.</li>
     * <li>If an object is created by either the Multipart Upload or Part Copy operation,
     * the ETag is not an MD5 digest, regardless of the method of encryption.</li>
     * </ul>
     *
     * @param etag a hash of the objects content
     */
    void setEtag(String etag);
    String getEtag();


    /**
     * The name of the server that created the response.
     * Default: AmazonS3
     *
     * @param server The name of the server that created the response.
     */
    void setServer(String server);
    String getServer();


    /**
     * Specifies whether the object returned was (true) or was not (false) a delete marker.
     * Default : false
     *
     * @param deleteMarker Specifies whether the object returned was (true) or was not (false) a delete marker.
     */
    void setXamzDeleteMarker(Boolean deleteMarker);
    Boolean getXamzDeleteMarker();


    /**
     * A value created by Amazon S3 that uniquely identifies the request. In the unlikely
     * event that you have problems with Amazon S3, AWS can use this value to
     * troubleshoot the problem.
     * Default: None
     *
     * @param requestId unique request id
     */
    void setXamzRequestId(String requestId);
    String getXamzRequestId();


    /**
     * The version of the object. When you enable versioning, Amazon S3 generates a
     * random number for objects added to a bucket. The value is UTF-8 encoded and
     * URL ready. When you PUT an object in a bucket where versioning has been
     * suspended, the version ID is always null .
     * Default: null
     *
     * @param versionId null | any URL-ready, UTF-8 encoded string
     */
    void setXamzVersionId(String versionId);
    String getXamzVersionId();


    /**
     * If the object expiration is configured, the response
     * includes this header. It includes the expiry-date and rule-id key-value pairs
     * providing object expiration information. The value of the rule-id is URL encoded.
     * Type: String
     *
     * @param expiration String with expiry-date and rule-id
     */
    void setXamzExpiration(String expiration);
    String getXamzExpiration();

}
