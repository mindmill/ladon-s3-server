/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.ladon.s3server.auth;

import de.mc.ladon.s3server.entities.api.S3CallContext;
import de.mc.ladon.s3server.exceptions.InvalidSecurityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper class for AWS Signature Version 4 (authorization header based).
 *
 * <p>The canonicalization follows the AWS SigV4 specification (see the static documentation
 * links in {@link SigV4}).</p>
 */
public class AwsSignatureVersion4 {

    private static final Logger logger = LoggerFactory.getLogger(AwsSignatureVersion4.class);

    protected static final Pattern AWS_AUTH4_PATTERN =
            Pattern.compile("AWS4-HMAC-SHA256\\s+Credential=([^/,]+)/([^/]+)/([^/]+)/([^/]+)/aws4_request,\\s*"
                    + "SignedHeaders=([^,]+),\\s*Signature=([0-9a-fA-F]+)");

    public AwsSignatureVersion4() {
    }

    public boolean isV4(S3CallContext callContext) {
        return buildMatcher(callContext) != null;
    }

    private Matcher buildMatcher(S3CallContext callContext) {
        String authorization = callContext.getHeader().getAuthorization();
        if (authorization == null) {
            return null;
        }
        Matcher matcher = AWS_AUTH4_PATTERN.matcher(authorization.trim());
        return matcher.matches() ? matcher : null;
    }

    /**
     * The access key ID encoded in the credential scope.
     */
    public String getAccessKey(S3CallContext callContext) {
        Matcher m = buildMatcher(callContext);
        return m != null ? m.group(1) : null;
    }

    /**
     * The date of the credential scope (format yyyyMMdd).
     */
    public String getDate(S3CallContext callContext) {
        Matcher m = buildMatcher(callContext);
        return m != null ? m.group(2) : null;
    }

    /**
     * The region of the credential scope.
     */
    public String getRegion(S3CallContext callContext) {
        Matcher m = buildMatcher(callContext);
        return m != null ? m.group(3) : null;
    }

    /**
     * The service of the credential scope (for S3 requests: {@code s3}).
     */
    public String getService(S3CallContext callContext) {
        Matcher m = buildMatcher(callContext);
        return m != null ? m.group(4) : null;
    }

    /**
     * The list of lowercase header names included in the signature (signed headers).
     */
    public List<String> getSignedHeaders(S3CallContext callContext) {
        Matcher m = buildMatcher(callContext);
        if (m == null) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (String name : m.group(5).split(";")) {
            String trimmed = name.trim().toLowerCase(Locale.ROOT);
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        result.sort(Comparator.naturalOrder());
        return result;
    }

    /**
     * The client provided signature.
     */
    public String getSignature(S3CallContext callContext) {
        Matcher m = buildMatcher(callContext);
        return m != null ? m.group(6) : null;
    }

    /**
     * Computes the server-side signature for the given request, using the canonical
     * construction rules of the SigV4 specification.
     */
    public String computeV4(S3CallContext callContext) {
        Matcher m = buildMatcher(callContext);
        if (m == null) {
            throw new InvalidSecurityException(callContext.getUri(), callContext.getRequestId());
        }
        String date = m.group(2);
        String region = m.group(3);
        String service = getService(callContext);
        byte[] signingKey = SigV4.signingKey(
                callContext.getUser().getSecretKey(), date, region, service);
        String stringToSign = buildStringToSign(callContext, date, region, service);
        return SigV4.hex(SigV4.hmacSha256(signingKey, stringToSign));
    }

    private String buildStringToSign(S3CallContext callContext, String date, String region, String service) {
        String xamzDate = callContext.getHeader().getXamzDate();
        if (xamzDate == null) {
            throw new InvalidSecurityException(callContext.getUri(), callContext.getRequestId());
        }
        return SigV4.ALGORITHM + "\n"
                + xamzDate + "\n"
                + date + "/" + region + "/" + service + "/aws4_request\n"
                + SigV4.sha256Hex(buildCanonicalRequest(callContext).toString());
    }

    private StringBuilder buildCanonicalRequest(S3CallContext callContext) {
        List<String> signedHeaders = getSignedHeaders(callContext);
        if (!signedHeaders.contains("host")) {
            // The host header is required to be signed for SigV4 requests.
            throw new InvalidSecurityException(callContext.getUri(), callContext.getRequestId());
        }

        StringBuilder canonicalRequest = new StringBuilder(callContext.getMethod());
        canonicalRequest.append('\n');
        canonicalRequest.append(callContext.getUri()).append('\n');

        if (callContext.getQueryString() != null) {
            canonicalRequest.append(SigV4.canonicalQueryStringMulti(callContext.getParams().getAllParamsMulti()));
        }
        canonicalRequest.append('\n');

        for (String name : signedHeaders) {
            String value = SigV4.headerIgnoreCase(callContext.getHeader().getFullHeader(), name);
            if (value == null) {
                // The client signed a header that is not present in the request.
                logger.debug("signed header '{}' missing from request", name);
                throw new InvalidSecurityException(callContext.getUri(), callContext.getRequestId());
            }
            canonicalRequest.append(name).append(':')
                    .append(SigV4.canonicalHeaderValue(value)).append('\n');
        }
        canonicalRequest.append('\n');
        canonicalRequest.append(String.join(";", signedHeaders)).append('\n');

        String payloadHash = callContext.getHeader().getXamzContentSha256();
        if (payloadHash == null) {
            // x-amz-content-sha256 is required for all SigV4 requests to S3.
            throw new InvalidSecurityException(callContext.getUri(), callContext.getRequestId());
        }
        canonicalRequest.append(payloadHash);
        return canonicalRequest;
    }
}
