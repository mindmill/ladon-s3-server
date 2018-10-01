package de.mc.ladon.s3server.common;

import de.mc.ladon.s3server.entities.api.S3CallContext;

import java.net.URLEncoder;

public class EncodingUtil {
    private EncodingUtil() {
    }

    public static String getEncoded(S3CallContext callContext, String value) {
        if (value == null) return null;
        if (callContext.getParams().getEncodingType() != null) {
            return URLEncoder.encode(value);
        } else {
            return value;
        }
    }
}
