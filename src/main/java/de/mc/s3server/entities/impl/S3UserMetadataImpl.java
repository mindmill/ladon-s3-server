/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.entities.impl;

import de.mc.s3server.common.S3Constants;
import de.mc.s3server.entities.api.S3UserMetadata;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Properties;

/**
 * @author Ralf Ulrich on 21.02.16.
 */
public class S3UserMetadataImpl extends HashMap<String, String> implements S3UserMetadata {

    public S3UserMetadataImpl() {
    }

    public S3UserMetadataImpl(Properties properties) {
        properties.entrySet().stream()
                .filter(e -> ((String) e.getKey()).startsWith(S3Constants.X_AMZ_META_PREFIX))
                .forEach(e -> put((String) e.getKey(), (String) e.getValue()));
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
