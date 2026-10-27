/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.ladon.s3server.auth;

import de.mc.ladon.s3server.entities.api.S3CallContext;
import de.mc.ladon.s3server.exceptions.InvalidSecurityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper class for AWS Signature Version 2.
 *
 * <p>SigV2 is a legacy scheme (deprecated by AWS) but is still accepted by this server for
 * backwards compatibility with older clients. See "Signature calculations for the
 * Authorization header (AWS Signature Version 2)" in the Amazon S3 Developer Guide for the
 * StringToSign format used here (see {@link RestUtils#makeS3CanonicalString}).</p>
 */
public class AwsSignatureVersion2 {

    private static final Logger logger = LoggerFactory.getLogger(AwsSignatureVersion2.class);
    public static final String HMAC_SHA_1 = "HmacSHA1";
    public static final Pattern AWS_AUTH_PATTERN = Pattern.compile("AWS ([^:]+):(.*)");

    /**
     * Computes the SigV2 signature (base64 HMAC-SHA1) for the given request.
     *
     * @param expires when non-null, used instead of the Date header (presigned URL)
     */
    public static String computeV2(S3CallContext callContext, String expires) {
        String canonicalString = RestUtils.makeS3CanonicalString(callContext, expires);
        byte[] key = callContext.getUser().getSecretKey().getBytes(StandardCharsets.UTF_8);
        try {
            Mac mac = Mac.getInstance(HMAC_SHA_1);
            mac.init(new SecretKeySpec(key, HMAC_SHA_1));
            return Base64.getEncoder().encodeToString(mac.doFinal(canonicalString.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            logger.error("sigv2 not available", e);
        }
        throw new InvalidSecurityException(callContext.getUri(), callContext.getRequestId());
    }

    public String getSignature(S3CallContext callContext) {
        Matcher m = AWS_AUTH_PATTERN.matcher(callContext.getHeader().getAuthorization());
        if (m.matches()) {
            return m.group(2);
        }
        return null;
    }

    public String getAccessKey(S3CallContext callContext) {
        Matcher m = AWS_AUTH_PATTERN.matcher(callContext.getHeader().getAuthorization());
        if (m.matches()) {
            return m.group(1);
        }
        return null;
    }

}
