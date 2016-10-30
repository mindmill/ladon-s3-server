/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.ladon.s3server.entities.impl;

import de.mc.ladon.s3server.common.S3Constants;
import de.mc.ladon.s3server.entities.api.S3RequestParams;

import java.util.HashMap;
import java.util.Map;

import static de.mc.ladon.s3server.common.S3Constants.*;

/**
 * @author Ralf Ulrich on 20.02.16.
 */
public class S3RequestParamsImpl implements S3RequestParams {

    private String delimiter;
    private String encodingType;
    private String marker;
    private Integer maxKeys;
    private String keyMarker;
    private String versionIdMarker;
    private String prefix;
    private Map<String, String> allParams;

    public S3RequestParamsImpl(Map<String, String[]> requestParams) {
        allParams = new HashMap<>(requestParams.size());
        for (String p : requestParams.keySet()) {
            allParams.put(p, requestParams.get(p)[0]);
        }

        this.delimiter = getFirstOrNull(DELIMITER, requestParams);
        this.encodingType = getFirstOrNull(ENCODING_TYPE, requestParams);
        this.marker = getFirstOrNull(MARKER, requestParams);
        this.keyMarker = getFirstOrNull(KEY_MARKER, requestParams);
        this.versionIdMarker = getFirstOrNull(VERSION_ID_MARKER, requestParams);
        try {
            this.maxKeys = Integer.parseInt(getFirstOrNull(MAX_KEYS, requestParams));
        } catch (NumberFormatException e) {
            //hack
        }
        this.prefix = getFirstOrNull(PREFIX, requestParams);
    }

    private String getFirstOrNull(String key, Map<String, String[]> requestParams) {
        String[] values = requestParams.get(key);
        if (values != null && values.length > 0) {
            return values[0];
        }
        return null;
    }

    @Override
    public boolean listVersions() {
        return allParams.get(S3Constants.VERSIONS) != null;
    }

    @Override
    public boolean acl() {
        return allParams.get(S3Constants.ACL) != null;
    }

    @Override
    public String getDelimiter() {
        return delimiter;
    }

    @Override
    public String getEncodingType() {
        return encodingType;
    }

    @Override
    public String getMarker() {
        return marker;
    }

    @Override
    public String getKeyMarker() {
        return keyMarker;
    }

    @Override
    public String getVersionIdMarker() {
        return versionIdMarker;
    }

    @Override
    public int getMaxKeys() {
        return maxKeys == null ? 1000 : maxKeys > 1000 ? 1000 : maxKeys;
    }

    @Override
    public String getPrefix() {
        return prefix;
    }

    public Map<String, String> getAllParams() {
        return allParams;
    }

    @Override
    public String toString() {
        return "S3RequestParams{" +
                "delimiter='" + delimiter + '\'' +
                ", encodingType='" + encodingType + '\'' +
                ", marker='" + marker + '\'' +
                ", maxKeys=" + maxKeys +
                ", prefix='" + prefix + '\'' +
                '}';
    }
}
