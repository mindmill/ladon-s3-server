/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.ladon.s3server.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Helpers for AWS Signature Version 4 (SigV4) calculations, pure JDK 21.
 *
 * <p>The canonicalization rules follow the AWS Signature Version 4 specifications, see
 * <a href="https://docs.aws.amazon.com/IAM/latest/UserGuide/create-signed-request.html">
 * Canonical request</a> for the general format and the Amazon S3 developer guide sections
 * "Signature Calculations for the Authorization Header" (sig-v4-header-based-auth),
 * "Transferring Payload in Multiple Chunks (Chunked Upload)" (sigv4-streaming) and
 * "Using Query Parameters (Presigned URLs)" (sigv4-query-string-auth) for S3 specifics.</p>
 */
public final class SigV4 {

    public static final String ALGORITHM = "AWS4-HMAC-SHA256";
    public static final String TERMINATOR = "aws4_request";

    /**
     * Marker used as the payload hash for presigned (query string) requests.
     * See AWS S3 "Using Query Parameters (AWS Signature Version 4)".
     */
    public static final String UNSIGNED_PAYLOAD = "UNSIGNED-PAYLOAD";

    /**
     * Marker used as the payload hash for streaming chunked uploads.
     * See AWS S3 "Signature Calculations for the Authorization Header:
     * Transferring Payload in Multiple Chunks (Chunked Upload)".
     */
    public static final String STREAMING_PAYLOAD = "STREAMING-AWS4-HMAC-SHA256-PAYLOAD";

    /**
     * Maximum allowed deviation of the request date from the server's clock (15 minutes),
     * as enforced by Amazon S3.
     */
    public static final long MAX_CLOCK_SKEW_MILLIS = 15L * 60L * 1000L;

    private SigV4() {
    }

    public static byte[] hmacSha256(byte[] key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            // HmacSHA256 is always available in a JDK.
            throw new IllegalStateException("HmacSHA256 is not available", e);
        }
    }

    public static byte[] hmacSha256(byte[] key, byte[] data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new IllegalStateException("HmacSHA256 is not available", e);
        }
    }

    public static String sha256Hex(String data) {
        return sha256Hex(data.getBytes(StandardCharsets.UTF_8));
    }

    public static String sha256Hex(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(data));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    /**
     * Derives the signing key scoped to date, region and service.
     *
     * <pre>
     * DateKey              = HMAC-SHA256("AWS4" + SecretKey, Date)
     * DateRegionKey        = HMAC-SHA256(DateKey, Region)
     * DateRegionServiceKey = HMAC-SHA256(DateRegionKey, Service)
     * SigningKey           = HMAC-SHA256(DateRegionServiceKey, "aws4_request")
     * </pre>
     */
    public static byte[] signingKey(String secretAccessKey, String dateStamp, String region, String serviceName) {
        byte[] key = ("AWS4" + secretAccessKey).getBytes(StandardCharsets.UTF_8);
        key = hmacSha256(key, dateStamp);
        key = hmacSha256(key, region);
        key = hmacSha256(key, serviceName);
        return hmacSha256(key, TERMINATOR);
    }

    public static String hex(byte[] data) {
        return HexFormat.of().formatHex(data);
    }

    /**
     * URI-encoding as required by the SigV4 specification: every byte is encoded except the
     * unreserved characters A–Z, a–z, 0–9, '-', '.', '_' and '~' (note: '/' <em>is</em>
     * percent-encoded — unreserved characters only pass through). Space is encoded as
     * %20 (never as '+'), hex digits are uppercase.
     *
     * <p>Encoding of canonical <em>paths</em> is the responsibility of the caller (paths
     * arrive pre-encoded in the request URI and are signed as-is).</p>
     */
    public static String uriEncode(String value) {
        if (value == null) {
            return "";
        }
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        StringBuilder result = new StringBuilder(bytes.length);
        for (byte b : bytes) {
            char ch = (char) (b & 0xFF);
            if ((ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9')
                    || ch == '-' || ch == '_' || ch == '.' || ch == '~') {
                result.append(ch);
            } else {
                result.append('%').append(String.format(Locale.ROOT, "%02X", b & 0xFF));
            }
        }
        return result.toString();
    }

    /**
     * Builds the canonical query string: parameters sorted by (decoded) name, each name and value
     * URI-encoded, joined with '&amp;'. A parameter without a value is rendered as {@code name=} unless
     * the provided map stores it as {@code null}.
     */
    public static String canonicalQueryString(Map<String, String> parameters) {
        TreeMap<String, String> sorted = new TreeMap<>(parameters);
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : sorted.entrySet()) {
            if (!first) {
                result.append('&');
            }
            first = false;
            result.append(uriEncode(entry.getKey()));
            String value = entry.getValue();
            if (value != null) {
                result.append('=').append(uriEncode(value));
            }
        }
        return result.toString();
    }

    /**
     * Builds the canonical query string from <em>multi-valued</em> parameters, as required
     * by the SigV4 specification: sorted by (decoded) name and — for duplicated names —
     * by value, each name and value URI-encoded, joined with {@code =&amp;}.
     */
    public static String canonicalQueryStringMulti(Map<String, List<String>> parameters) {
        TreeMap<String, List<String>> sorted = new TreeMap<>(parameters);
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, List<String>> entry : sorted.entrySet()) {
            List<String> values = entry.getValue() == null ? List.of() : entry.getValue();
            for (String value : new TreeSet<String>(values)) {
                if (value == null) {
                    continue;
                }
                if (!first) {
                    result.append('&');
                }
                first = false;
                result.append(uriEncode(entry.getKey()))
                        .append('=')
                        .append(uriEncode(value));
            }
        }
        return result.toString();
    }

    /**
     * Normalizes a request header value for the canonical header list, following the
     * SigV4 specification: multiple values (delivered comma-separated by the servlet
     * container) are normalized individually and joined with a single comma, and for
     * every value the leading/trailing whitespace is trimmed and consecutive whitespace
     * characters are collapsed to a single space.
     */
    public static String canonicalHeaderValue(String rawValue) {
        if (rawValue == null) {
            return "";
        }
        StringBuilder result = new StringBuilder(rawValue.length());
        boolean first = true;
        for (String value : rawValue.split(",")) {
            if (!first) {
                result.append(',');
            }
            first = false;
            result.append(value.trim().replaceAll("\\s{2,}", " "));
        }
        return result.toString();
    }

    /**
     * Case-insensitive lookup in a header name → value map.
     */
    public static String headerIgnoreCase(Map<String, String> headers, String name) {
        if (headers == null) {
            return null;
        }
        String value = headers.get(name);
        if (value != null) {
            return value;
        }
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(name)) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * Constant-time comparison of two hex signatures.
     */
    public static boolean signaturesEqual(String expected, String provided) {
        if (provided == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                provided.getBytes(StandardCharsets.UTF_8));
    }

    public static boolean isStreamingPayload(String xamzContentSha256) {
        return STREAMING_PAYLOAD.equalsIgnoreCase(xamzContentSha256);
    }

    public static boolean isUnsignedPayload(String xamzContentSha256) {
        return UNSIGNED_PAYLOAD.equalsIgnoreCase(xamzContentSha256);
    }
}
