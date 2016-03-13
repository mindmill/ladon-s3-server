/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.ladon.s3server.authorization;

import com.google.common.base.Charsets;
import com.google.common.io.BaseEncoding;
import de.mc.ladon.s3server.entities.api.S3CallContext;
import de.mc.ladon.s3server.exceptions.InternalErrorException;
import de.mc.ladon.s3server.exceptions.S3ServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper class for aws signature v2
 */
public class AwsSignatureVersion2 {

    private static final Logger logger = LoggerFactory.getLogger(AwsSignatureVersion2.class);
    public static final String HMAC_SHA_1 = "HmacSHA1";
    public static final Pattern AWS_AUTH_PATTERN = Pattern.compile("AWS ([^:]+):(.*)");

    public String computeV2(S3CallContext callContext, String pathPrefix) throws S3ServerException {

        String canonicalString = RestUtils.makeS3CanonicalString(callContext.getMethod(), callContext.getUri(), callContext, null);
        SecretKeySpec keySpec = new SecretKeySpec(callContext.getUser().getSecretKey().getBytes(), HMAC_SHA_1);

        try {
            Mac mac = Mac.getInstance(HMAC_SHA_1);
            mac.init(keySpec);
            byte[] result = mac.doFinal(canonicalString.getBytes(Charsets.UTF_8.name()));
            return BaseEncoding.base64().encode(result);
        } catch (UnsupportedEncodingException e) {
            // UTF8 should be supported
            logger.error("utf-8 not supported", e);
        } catch (NoSuchAlgorithmException e) {
            logger.error("sha1 not found", e);
        } catch (InvalidKeyException e) {
            logger.error("invalid key", e);
        }
        throw new InternalErrorException(pathPrefix, callContext.getRequestId());
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
