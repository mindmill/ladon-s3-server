/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.ladon.s3server.authorization;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.io.BaseEncoding;
import de.mc.ladon.s3server.entities.api.S3CallContext;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AwsSignatureVersion2 {

    private static final List<String> SIGNED_PARAMETERS = Arrays.asList("acl",
            "torrent",
            "logging",
            "location",
            "policy",
            "requestPayment",
            "versioning",
            "versions",
            "versionId",
            "notification",
            "uploadId",
            "uploads",
            "partNumber",
            "website",
            "delete",
            "lifecycle",
            "tagging",
            "cors",
            "restore");
    public static final String HMAC_SHA_1 = "HmacSHA1";


    public String computeV2(S3CallContext callContext, String pathPrefix) throws Exception {
        StringBuilder stringToSign = new StringBuilder(callContext.getMethod());
        stringToSign.append("\n");
        stringToSign.append(callContext.getHeader().getContentMD5());
        stringToSign.append("\n");
        stringToSign.append(callContext.getHeader().getContentType());
        stringToSign.append("\n");

        String expires = callContext.getHeader().getExpires();
        String date = expires == null ? callContext.getHeader().getDate() : expires;
        if (callContext.getHeader().getXamzDate() == null) {
            stringToSign.append(date);
        }
        stringToSign.append("\n");

        Map<String, String> requestHeaders = callContext.getHeader().getFullHeader();
        List<String> headers = requestHeaders.keySet()
                .stream()
                .filter(this::relevantAmazonHeader)
                .map(name -> toHeaderStringRepresentation(name, requestHeaders))
                .sorted()
                .collect(Collectors.toList());

        for (String header : headers) {
            stringToSign.append(header);
            stringToSign.append("\n");
        }

        stringToSign.append(pathPrefix).append(callContext.getUri().substring(3));

        char separator = '?';
        for (String parameterName : callContext.getParams().getAllParams().keySet().stream().sorted().collect(Collectors.toList())) {
            if (SIGNED_PARAMETERS.contains(parameterName)) {
                stringToSign.append(separator).append(parameterName);
                String parameterValue = callContext.getParams().getAllParams().get(parameterName);
                if (!Strings.isNullOrEmpty(parameterValue)) {
                    stringToSign.append("=").append(parameterValue);
                }
                separator = '&';
            }
        }

        SecretKeySpec keySpec = new SecretKeySpec(callContext.getUser().getSecretKey().getBytes(), HMAC_SHA_1);
        Mac mac = Mac.getInstance(HMAC_SHA_1);
        mac.init(keySpec);
        byte[] result = mac.doFinal(stringToSign.toString().getBytes(Charsets.UTF_8.name()));
        return BaseEncoding.base64().encode(result);
    }

    private boolean relevantAmazonHeader(final String name) {
        return name.toLowerCase().startsWith("x-amz-");
    }

    private String toHeaderStringRepresentation(final String headerName, Map<String, String> requestHeaders) {
        return headerName.toLowerCase().trim() + ":" + requestHeaders.get(headerName).trim();
    }
}
