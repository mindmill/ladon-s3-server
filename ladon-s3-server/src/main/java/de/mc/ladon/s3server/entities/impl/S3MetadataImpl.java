/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.ladon.s3server.entities.impl;

import de.mc.ladon.s3server.common.S3Constants;
import de.mc.ladon.s3server.entities.api.S3Metadata;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Ralf Ulrich on 21.02.16.
 */
public class S3MetadataImpl extends HashMap<String, String> implements S3Metadata {

    public S3MetadataImpl() {
    }

    public S3MetadataImpl(Map<String, String> metadata) {
        metadata.entrySet().stream()
                .filter(e -> e.getKey().startsWith(S3Constants.X_AMZ_META_PREFIX)
                        || e.getKey().equals(S3Constants.X_AMZ_DECODED_CONTENT_LENGTH))
                .forEach(e -> put(e.getKey(), e.getValue()));
        String etag = metadata.get(S3Constants.ETAG);
        if (etag != null) put(S3Constants.ETAG, etag);
    }

    @Override
    public String get(String key) {
        return super.get(key);
    }

    @Override
    public void putInResponse(HttpServletResponse response) {
        for (Entry<String, String> e : entrySet()) {
            response.setHeader(e.getKey(), e.getValue());
        }
    }
}
