/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.ladon.s3server.auth;

import de.mc.ladon.s3server.entities.api.S3CallContext;
import de.mc.ladon.s3server.entities.api.S3User;
import de.mc.ladon.s3server.entities.impl.S3CallContextImpl;
import de.mc.ladon.s3server.exceptions.AccessDeniedException;
import de.mc.ladon.s3server.exceptions.InvalidAccessKeyIdException;
import de.mc.ladon.s3server.exceptions.InvalidSecurityException;
import de.mc.ladon.s3server.exceptions.MissingSecurityHeaderException;
import de.mc.ladon.s3server.exceptions.RequestTimeTooSkewedException;
import de.mc.ladon.s3server.exceptions.S3ServerException;
import de.mc.ladon.s3server.repository.api.S3Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Verifies the credentials of a request. Two authentication schemes are supported,
 * each as header based (Authorization) and query parameter based (presigned URL) authentication:
 *
 * <ul>
 *     <li>AWS Signature Version 4 (header based and presigned/query based)</li>
 *     <li>AWS Signature Version 2 (header based and presigned/query based)</li>
 * </ul>
 *
 * In addition to the signature itself the request date is checked against the server's clock
 * (Amazon S3 rejects a request whose date deviates by more than 15 minutes). Presigned URLs
 * additionally carry an {@code X-Amz-Expires} value in seconds which is enforced here.
 */
public class Authorization {
    private static final Logger logger = LoggerFactory.getLogger(Authorization.class);

    private static final DateTimeFormatter XAMZ_DATE = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");

    private Authorization() {
    }

    public static void checkAuthHeader(S3CallContext callContext, S3Repository repository) throws S3ServerException {
        String auth = callContext.getHeader().getAuthorization();
        if (auth != null) {
            AwsSignatureVersion4 s4 = new AwsSignatureVersion4();
            if (s4.isV4(callContext)) {
                logger.debug("auth by v4 header");
                verifyHeaderV4(callContext, repository, s4);
            } else {
                logger.debug("auth by v2 header");
                verifyHeaderV2(callContext, repository);
            }
            return;
        }

        Map<String, String> params = lowerCasedParams(callContext);
        if (params.containsKey("x-amz-signature") && params.containsKey("x-amz-algorithm")
                && params.containsKey("x-amz-credential")) {
            logger.debug("auth by v4 presigned url");
            verifyQueryV4(callContext, repository, params);
            return;
        }
        if (params.containsKey("signature") && params.containsKey("awsaccesskeyid")) {
            logger.debug("auth by v2 presigned url");
            verifyQueryV2(callContext, repository, params);
            return;
        }
        throw new MissingSecurityHeaderException("", callContext.getRequestId());
    }

    private static void verifyHeaderV4(S3CallContext ctx, S3Repository repository, AwsSignatureVersion4 s4) throws S3ServerException {
        String accessKey = s4.getAccessKey(ctx);
        S3User user = putUserIntoCallContext(ctx, repository, accessKey);
        checkClockSkew(ctx, ctx.getHeader().getXamzDate());
        String signature = s4.computeV4(ctx);
        if (!SigV4.signaturesEqual(signature, s4.getSignature(ctx))) {
            throw new InvalidSecurityException("", ctx.getRequestId());
        }
    }

    private static void verifyHeaderV2(S3CallContext ctx, S3Repository repository) throws S3ServerException {
        AwsSignatureVersion2 s2 = new AwsSignatureVersion2();
        S3User user = putUserIntoCallContext(ctx, repository, s2.getAccessKey(ctx));
        // SigV2: the x-amz-date header takes precedence over the Date header when both
        // are present (AWS SigV2 spec).
        String date = ctx.getHeader().getXamzDate();
        if (date == null) {
            date = ctx.getHeader().getDate();
        }
        if (date == null) {
            throw new InvalidSecurityException("", ctx.getRequestId());
        }
        checkClockSkew(ctx, date, true);
        String signature = s2.computeV2(ctx, null);
        if (!signature.equals(s2.getSignature(ctx))) {
            throw new InvalidSecurityException("", ctx.getRequestId());
        }
    }

    private static void verifyQueryV4(S3CallContext ctx, S3Repository repository, Map<String, String> params) throws S3ServerException {
        String algorithm = params.get("x-amz-algorithm");
        if (!SigV4.ALGORITHM.equalsIgnoreCase(algorithm)) {
            throw new AccessDeniedException("", ctx.getRequestId());
        }

        String credential = params.get("x-amz-credential");
        String[] scope = credential.split("/");
        if (scope.length != 5 || !"s3".equals(scope[3])) {
            throw new InvalidSecurityException("", ctx.getRequestId());
        }
        String accessKey = scope[0];
        String date = scope[1];
        String region = scope[2];

        String xamzDate = params.get("x-amz-date");
        if (xamzDate == null) {
            xamzDate = ctx.getHeader().getXamzDate();
        }
        if (xamzDate == null) {
            throw new InvalidSecurityException("", ctx.getRequestId());
        }

        // X-Amz-Expires must be present and in [1..604800] seconds (seven days maximum).
        long expires;
        try {
            expires = Long.parseLong(params.get("x-amz-expires") == null ? "" : params.get("x-amz-expires"));
        } catch (NumberFormatException e) {
            throw new InvalidSecurityException("", ctx.getRequestId());
        }
        if (expires < 1 || expires > 604800L) {
            throw new AccessDeniedException("", ctx.getRequestId());
        }

        S3User user = putUserIntoCallContext(ctx, repository, accessKey);

        long requestTime;
        try {
            requestTime = parseIsoDate(xamzDate);
        } catch (RuntimeException e) {
            // an unparseable request time is a broken credential, not a server error
            throw new InvalidSecurityException("", ctx.getRequestId());
        }
        long now = System.currentTimeMillis();
        if (now < requestTime - SigV4.MAX_CLOCK_SKEW_MILLIS
                || now > requestTime + expires * 1000L) {
            throw new RequestTimeTooSkewedException("", ctx.getRequestId());
        }

        String signedHeaders = params.get("x-amz-signedheaders");
        if (signedHeaders == null) {
            throw new InvalidSecurityException("", ctx.getRequestId());
        }
        List<String> headerNames = new ArrayList<>();
        for (String name : signedHeaders.split(";")) {
            String trimmed = name.trim().toLowerCase(Locale.ROOT);
            if (!trimmed.isEmpty()) {
                headerNames.add(trimmed);
            }
        }
        headerNames.sort(Comparator.naturalOrder());
        if (!headerNames.contains("host")) {
            throw new InvalidSecurityException("", ctx.getRequestId());
        }

        StringBuilder canonicalRequest = new StringBuilder(ctx.getMethod());
        canonicalRequest.append('\n');
        canonicalRequest.append(ctx.getUri()).append('\n');

        // The canonical query string contains every query parameter except X-Amz-Signature.
        java.util.Map<String, java.util.List<String>> canonicalParams = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, java.util.List<String>> entry : ctx.getParams().getAllParamsMulti().entrySet()) {
            if (entry.getKey() == null || "x-amz-signature".equalsIgnoreCase(entry.getKey())) {
                continue;
            }
            canonicalParams.put(entry.getKey(), entry.getValue());
        }
        canonicalRequest.append(SigV4.canonicalQueryStringMulti(canonicalParams)).append('\n');

        for (String name : headerNames) {
            String value = SigV4.headerIgnoreCase(ctx.getHeader().getFullHeader(), name);
            if (value == null) {
                throw new InvalidSecurityException("", ctx.getRequestId());
            }
            canonicalRequest.append(name).append(':').append(SigV4.canonicalHeaderValue(value)).append('\n');
        }
        canonicalRequest.append('\n');
        canonicalRequest.append(String.join(";", headerNames)).append('\n');
        canonicalRequest.append(SigV4.UNSIGNED_PAYLOAD);

        String stringToSign = SigV4.ALGORITHM + "\n"
                + xamzDate + "\n"
                + date + "/" + region + "/s3/aws4_request\n"
                + SigV4.sha256Hex(String.valueOf(canonicalRequest));

        byte[] signingKey = SigV4.signingKey(user.getSecretKey(), date, region, "s3");
        String signature = SigV4.hex(SigV4.hmacSha256(signingKey, stringToSign));

        if (!SigV4.signaturesEqual(signature, params.get("x-amz-signature"))) {
            throw new InvalidSecurityException("", ctx.getRequestId());
        }
    }

    private static void verifyQueryV2(S3CallContext ctx, S3Repository repository, Map<String, String> params) throws S3ServerException {
        String accessKey = params.get("awsaccesskeyid");
        S3User user = putUserIntoCallContext(ctx, repository, accessKey);

        String expires = params.get("expires");
        if (expires == null) {
            throw new InvalidSecurityException("", ctx.getRequestId());
        }
        // Clock check for presigned URLs.
        long now = System.currentTimeMillis();
        long expiresMillis;
        try {
            expiresMillis = Long.parseLong(expires) * 1000L;
        } catch (NumberFormatException e) {
            throw new InvalidSecurityException("", ctx.getRequestId());
        }
        // A presigned URL is invalid once the current time is past Expires
        // (AWS SigV2 spec: "A request received after this time (according to the server)
        // will be rejected."). No upper limit on the remaining lifetime is applied.
        if (now > expiresMillis) {
            throw new RequestTimeTooSkewedException("", ctx.getRequestId());
        }

        String signature = AwsSignatureVersion2.computeV2(ctx, expires);
        if (signature == null || !signature.equals(params.get("signature"))) {
            throw new InvalidSecurityException("", ctx.getRequestId());
        }
    }

    private static S3User putUserIntoCallContext(S3CallContext callContext, S3Repository repository, String accessKey) throws S3ServerException {
        S3User user = repository.getUser(callContext, accessKey);
        if (user == null) {
            throw new InvalidAccessKeyIdException("", callContext.getRequestId());
        }
        ((S3CallContextImpl) callContext).setUser(user);
        return user;
    }

    /**
     * Checks whether the given request date (ISO8601 basic for SigV4, RFC 1123 for SigV2)
     * deviates by at most the allowed clock skew from the server's clock.
     */
    private static void checkClockSkew(S3CallContext ctx, String xamzDate) throws S3ServerException {
        checkClockSkew(ctx, parseIsoDate(xamzDate));
    }

    private static void checkClockSkew(S3CallContext ctx, String rfc1123Date, boolean rfc1123) throws S3ServerException {
        long requestTime;
        try {
            requestTime = rfc1123 ? parseRfc1123(rfc1123Date) : parseIsoDate(rfc1123Date);
        } catch (RuntimeException e) {
            throw new InvalidSecurityException("", ctx.getRequestId());
        }
        checkClockSkew(ctx, requestTime);
    }

    private static void checkClockSkew(S3CallContext ctx, long requestTimeMillis) {
        long now = System.currentTimeMillis();
        if (Math.abs(now - requestTimeMillis) > SigV4.MAX_CLOCK_SKEW_MILLIS) {
            throw new RequestTimeTooSkewedException("", ctx.getRequestId());
        }
    }

    static long parseIsoDate(String xamzDate) {
        if (xamzDate == null || xamzDate.length() < 15) {
            throw new IllegalArgumentException("invalid x-amz-date: " + xamzDate);
        }
        LocalDateTime dateTime = LocalDateTime.parse(xamzDate.substring(0, 15), XAMZ_DATE);
        return dateTime.toInstant(ZoneOffset.UTC).toEpochMilli();
    }

    static long parseRfc1123(String date) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ROOT);
            Date parsed = format.parse(date);
            if (parsed == null) {
                throw new IllegalArgumentException("invalid date: " + date);
            }
            return parsed.getTime();
        } catch (Exception e) {
            throw new IllegalArgumentException("invalid date: " + date, e);
        }
    }

    /**
     * Returns the query parameters as a map with lowercased keys (first value wins).
     */
    private static Map<String, String> lowerCasedParams(S3CallContext ctx) {
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : ctx.getParams().getAllParams().entrySet()) {
            if (entry.getKey() != null) {
                result.putIfAbsent(entry.getKey().toLowerCase(Locale.ROOT), entry.getValue());
            }
        }
        return result;
    }
}
