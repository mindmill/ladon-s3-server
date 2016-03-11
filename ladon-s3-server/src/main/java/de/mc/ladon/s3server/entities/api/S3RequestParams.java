/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.ladon.s3server.entities.api;

import java.util.Map;

/**
 * @author Ralf Ulrich on 20.02.16.
 */
public interface S3RequestParams {


    /**
     * A delimiter is a character you use to group keys.
     * All keys that contain the same string between the prefix , if specified, and
     * the first occurrence of the delimiter after the prefix are grouped under a single
     * result element, CommonPrefixes . If you don't specify the prefix parameter,
     * then the substring starts at the beginning of the key. The keys that are grouped
     * under CommonPrefixes result element are not returned elsewhere in the re-
     * sponse.
     * Default: None
     *
     * @return the delimiter to group keys or null
     */
    String getDelimiter();


    /**
     * Requests Amazon S3 to encode the response and specifies the encoding method to use.
     * An object key can contain any Unicode character; however, XML 1.0 parser
     * cannot parse some characters, such as characters with an ASCII value from
     * 0 to 10. For characters that are not supported in XML 1.0, you can add this
     * parameter to request that Amazon S3 encode the keys in the response.
     * Default: None
     *
     * @return the encoding method to use or null
     */
    String getEncodingType();


    /**
     * Specifies the key to start with when listing objects in a bucket. Amazon S3
     * returns object keys in alphabetical order, starting with key after the marker in
     * order.
     * Default : None
     *
     * @return the key to start with or null
     */
    String getMarker();


    /**
     * Sets the maximum number of keys returned in the response body. You can
     * add this to your request if you want to retrieve fewer than the default 1000
     * keys.
     * The response might contain fewer keys but will never contain more. If there
     * are additional keys that satisfy the search criteria but were not returned because
     * max-keys was exceeded, the response contains <IsTruncated>true</IsTruncated> .
     * To return the additional keys, see getMarker() .
     * Default: 1000
     *
     * @return the max number of keys to return
     */
    Integer getMaxKeys();


    /**
     * Limits the response to keys that begin with the specified prefix. You can use
     * prefixes to separate a bucket into different groupings of keys. (You can think
     * of using prefix to make groups in the same way you'd use a folder in a file
     * system.)
     * Default: None
     *
     * @return the prefix to use or null
     */
    String getPrefix();

    /**
     * Get all params as a simple Map. Notice: only the first value is added, multivalue is not supported
     *
     * @return Map with all param keys and the first param value
     */
    Map<String, String> getAllParams();
}
