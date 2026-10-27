/*
 * T15: AWS SigV4 presigned URLs (query parameter authentication) — the protocol
 * semantics of {@link Authorization#checkAuthHeader} for S3 query string auth:
 *
 * <ul>
 *     <li>X-Amz-Algorithm must be AWS4-HMAC-SHA256 (otherwise AccessDenied),</li>
 *     <li>X-Amz-Credential must carry the s3 service scope,</li>
 *     <li>X-Amz-Expires in 1..604800 seconds, unparseable/missing is InvalidSecurity,</li>
 *     <li>the request time must be within the allowed clock skew and its validity window.</li>
 * </ul>
 *
 * In addition a static cross-implementation vector (computed with an independent
 * Python reference implementation) pins the canonical request / string to sign
 * construction of the query signer itself.
 */
package de.mc.ladon.s3server.auth;

import de.mc.ladon.s3server.entities.api.S3CallContext;
import de.mc.ladon.s3server.exceptions.AccessDeniedException;
import de.mc.ladon.s3server.exceptions.InvalidAccessKeyIdException;
import de.mc.ladon.s3server.exceptions.InvalidSecurityException;
import de.mc.ladon.s3server.exceptions.MissingSecurityHeaderException;
import de.mc.ladon.s3server.exceptions.RequestTimeTooSkewedException;
import de.mc.ladon.s3server.repository.api.S3Repository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PresignedUrlTest {

    private static final String ACCESS = "PRESIGNEDACCESSKEY";
    private static final String SECRET = "presigned-url-secret-key";
    private static final String REGION = "us-east-1";
    private static final String PLACEHOLDER =
            "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    private static S3Repository repository() {
        return TestSignContexts.repository(TestSignContexts.user(ACCESS, SECRET));
    }

    // ------------------------------------------------------------------ static vector

    /**
     * Independently computed reference vector (Python standard library):
     * GET /presigned/object against host example.amazonaws.com, request date
     * 20150830T120000Z, scope 20150830/us-east-1/s3/aws4_request, signed headers: host.
     * The vector pins the canonical query construction (case, sorting, encoding)
     * and the UNSIGNED-PAYLOAD signature the server must accept.
     */
    @Test
    void oracleMatchesIndependentlyComputedVector() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Host", "example.amazonaws.com");

        String signature = computeQuerySignature("GET", "/presigned/object",
                "20150830T120000Z", params(ACCESS, "20150830T120000Z"),
                "host", headers);
        assertEquals("8c4695f32dbcac50f004c1a82b189d581bc5fb3b1be58fd3a7ab1452bbca0fef", signature,
                "the query signature must be reproducible against the independent reference implementation");
    }

    // ------------------------------------------------------------------ protocol (happy path)

    @Test
    void acceptsCorrectlySignedPresignedUrl() {
        String date = isoDate(Instant.now());
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Host", "example.amazonaws.com");
        Map<String, String[]> params = params(ACCESS, date);
        params.put("X-Amz-Signature", new String[]{computeQuerySignature("GET", "/presigned/object",
                date, params, "host", headers)});

        S3CallContext ctx = context("GET", "/presigned/object", headers, params);
        assertDoesNotThrow(() -> Authorization.checkAuthHeader(ctx, repository()),
                "a correctly signed presigned URL must be accepted");
    }

    // ------------------------------------------------------------------ protocol (rejects)

    @Test
    void unknownAccessKeyIdIsRejected() {
        S3CallContext ctx = context("GET", "/o",
                header("Host", "example.amazonaws.com"),
                params("SOMEONEELSE", isoDate(Instant.now())));
        assertThrows(InvalidAccessKeyIdException.class,
                () -> Authorization.checkAuthHeader(ctx, repository()),
                "an unknown access key id in the credential scope must be rejected");
    }

    @Test
    void algorithmMismatchIsAccessDenied() {
        Map<String, String[]> params = params(ACCESS, isoDate(Instant.now()));
        params.put("X-Amz-Algorithm", new String[]{"AWS4-HMAC-SHA512"});
        assertThrows(AccessDeniedException.class,
                () -> Authorization.checkAuthHeader(context("GET", "/o",
                        header("Host", "example.amazonaws.com"), params), repository()),
                "a mismatching X-Amz-Algorithm must be rejected with AccessDenied");
    }

    @Test
    void credentialScopeWithoutS3ServiceIsRejected() {
        Map<String, String[]> params = params(ACCESS, isoDate(Instant.now()));
        String date = params.get("X-Amz-Date")[0];
        params.put("X-Amz-Credential", new String[]{ACCESS + "/" + date + "/us-east-1/other/aws4_request"});
        assertThrows(InvalidSecurityException.class,
                () -> Authorization.checkAuthHeader(context("GET", "/o",
                        header("Host", "example.amazonaws.com"), params), repository()),
                "the credential scope must designate the s3 service");
    }

    @Test
    void expiresOutOfRangeOrMissingIsRejected() {
        Map<String, String[]> zero = params(ACCESS, isoDate(Instant.now()));
        zero.put("X-Amz-Expires", new String[]{"0"});
        assertThrows(AccessDeniedException.class,
                () -> Authorization.checkAuthHeader(context("GET", "/o",
                        header("Host", "example.amazonaws.com"), zero), repository()),
                "X-Amz-Expires must be at least 1 second");

        Map<String, String[]> toLarge = params(ACCESS, isoDate(Instant.now()));
        toLarge.put("X-Amz-Expires", new String[]{"604801"});
        assertThrows(AccessDeniedException.class,
                () -> Authorization.checkAuthHeader(context("GET", "/o",
                        header("Host", "example.amazonaws.com"), toLarge), repository()),
                "X-Amz-Expires must be at most 604800 seconds (seven days)");

        Map<String, String[]> missing = params(ACCESS, isoDate(Instant.now()));
        missing.remove("X-Amz-Expires");
        assertThrows(InvalidSecurityException.class,
                () -> Authorization.checkAuthHeader(context("GET", "/o",
                        header("Host", "example.amazonaws.com"), missing), repository()),
                "a missing X-Amz-Expires must be rejected with InvalidSecurity");
    }

    @Test
    void outsideTheValidityWindowIsRequestTimeTooSkewed() {
        long oneHourMillis = 60L * 60L * 1000L;
        long tenHoursMillis = 10L * 60L * 60L * 1000L;

        // a request time far in the future (client clock running ahead beyond the
        // allowed skew) is rejected
        Map<String, String[]> inFuture = params(ACCESS, isoDate(Instant.now().plusMillis(tenHoursMillis)));
        inFuture.put("X-Amz-Expires", new String[]{"3600"});
        assertThrows(RequestTimeTooSkewedException.class,
                () -> Authorization.checkAuthHeader(context("GET", "/o",
                        header("Host", "example.amazonaws.com"), inFuture), repository()),
                "a presigned URL signed ten hours in the future must be rejected");

        // signed two hours ago with a one-hour validity window: the URL is expired
        Map<String, String[]> expired = params(ACCESS, isoDate(Instant.now().minusMillis(2 * oneHourMillis)));
        expired.put("X-Amz-Expires", new String[]{"3600"});
        assertThrows(RequestTimeTooSkewedException.class,
                () -> Authorization.checkAuthHeader(context("GET", "/o",
                        header("Host", "example.amazonaws.com"), expired), repository()),
                "a presigned URL past its X-Amz-Expires window must be rejected");
    }

    @Test
    void urlInsideItsValidityWindowIsAcceptedDespiteItsAge() {
        // signed one hour ago with a two-hour validity window: for query authentication the
        // X-Amz-Expires window is the validity criterion — the 15-minute skew bound of the
        // header-based flow must not invalidate a URL that is still inside its window
        String date = isoDate(Instant.now().minusMillis(60L * 60L * 1000L));
        Map<String, String> headers = header("Host", "example.amazonaws.com");
        Map<String, String[]> params = params(ACCESS, date);
        params.put("X-Amz-Expires", new String[]{"7200"});
        params.put("X-Amz-Signature", new String[]{computeQuerySignature("GET", "/presigned/object",
                date, params, "host", headers)});

        S3CallContext ctx = context("GET", "/presigned/object", headers, params);
        assertDoesNotThrow(() -> Authorization.checkAuthHeader(ctx, repository()),
                "a presigned URL still inside its X-Amz-Expires window must be accepted");
    }

    @Test
    void unparseableRequestTimeIsInvalidSecurity() {
        Map<String, String[]> params = params(ACCESS, "20150830Tgarbage");
        assertThrows(InvalidSecurityException.class,
                () -> Authorization.checkAuthHeader(context("GET", "/o",
                        header("Host", "example.amazonaws.com"), params), repository()),
                "an unparseable X-Amz-Date must be a 403 (InvalidSecurity), not a 500");
    }

    @Test
    void missingSignedHeadersIsRejected() {
        Map<String, String[]> params = params(ACCESS, isoDate(Instant.now()));
        params.remove("X-Amz-SignedHeaders");
        assertThrows(InvalidSecurityException.class,
                () -> Authorization.checkAuthHeader(context("GET", "/o",
                        header("Host", "example.amazonaws.com"), params), repository()),
                "a presigned URL must name its signed headers");
    }

    @Test
    void signedHeadersWithoutHostAreRejected() {
        Map<String, String[]> params = params(ACCESS, isoDate(Instant.now()));
        params.put("X-Amz-SignedHeaders", new String[]{"content-type"});
        assertThrows(InvalidSecurityException.class,
                () -> Authorization.checkAuthHeader(context("GET", "/o",
                        header("Host", "example.amazonaws.com"), params), repository()),
                "the host header must always be signed in a presigned URL");
    }

    @Test
    void signedHeaderAbsentFromRequestIsRejected() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Host", "example.amazonaws.com");
        Map<String, String[]> params = params(ACCESS, isoDate(Instant.now()));
        params.put("X-Amz-SignedHeaders", new String[]{"host;x-amz-meta-missing"});
        assertThrows(InvalidSecurityException.class,
                () -> Authorization.checkAuthHeader(context("GET", "/o", headers, params), repository()),
                "every signed header must be present in the request");
    }

    @Test
    void wrongSignatureIsRejected() {
        String date = isoDate(Instant.now());
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Host", "example.amazonaws.com");
        Map<String, String[]> params = params(ACCESS, date);
        params.put("X-Amz-Signature", new String[]{"f".repeat(PLACEHOLDER.length())});
        assertThrows(InvalidSecurityException.class,
                () -> Authorization.checkAuthHeader(context("GET", "/presigned/object", headers, params), repository()),
                "a wrong presigned URL signature must be rejected");
    }

    @Test
    void v4QueryWithoutSignatureIsMissingSecurityHeader() {
        Map<String, String[]> params = params(ACCESS, isoDate(Instant.now()));
        params.remove("X-Amz-Signature");
        assertThrows(MissingSecurityHeaderException.class,
                () -> Authorization.checkAuthHeader(context("GET", "/o",
                        header("Host", "example.amazonaws.com"), params), repository()),
                "the S3 auth route must not classify incomplete v4 queries as v4 requests");
    }

    // ------------------------------------------------------------------ helpers

    /**
     * Computes the SigV4 query string signature the way a spec-conformant signer
     * does (all query parameters except X-Amz-Signature are part of the canonical
     * query string; the payload is UNSIGNED-PAYLOAD).
     */
    private static String computeQuerySignature(String method, String uri, String date,
                                                Map<String, String[]> params,
                                                String signedHeaders,
                                                Map<String, String> headers) {
        List<String> names = new ArrayList<>();
        for (String name : signedHeaders.split(";")) {
            String trimmed = name.trim().toLowerCase(Locale.ROOT);
            if (!trimmed.isEmpty()) {
                names.add(trimmed);
            }
        }
        names.sort(String.CASE_INSENSITIVE_ORDER);

        StringBuilder canonicalRequest = new StringBuilder(method).append('\n');
        canonicalRequest.append(uri).append('\n');
        Map<String, List<String>> query = new LinkedHashMap<>();
        for (Map.Entry<String, String[]> entry : params.entrySet()) {
            if (!"X-Amz-Signature".equalsIgnoreCase(entry.getKey())) {
                query.put(entry.getKey(), List.of(entry.getValue()));
            }
        }
        canonicalRequest.append(SigV4.canonicalQueryStringMulti(query)).append('\n');
        for (String name : names) {
            canonicalRequest.append(name).append(':');
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                if (entry.getKey().toLowerCase(Locale.ROOT).equals(name)) {
                    canonicalRequest.append(SigV4.canonicalHeaderValue(entry.getValue()));
                    break;
                }
            }
            canonicalRequest.append('\n');
        }
        canonicalRequest.append('\n');
        canonicalRequest.append(String.join(";", names)).append('\n');
        canonicalRequest.append(SigV4.UNSIGNED_PAYLOAD);

        String dateStamp = date.substring(0, 8);
        String stringToSign = SigV4.ALGORITHM + '\n'
                + date + '\n'
                + dateStamp + "/" + REGION + "/s3/aws4_request\n"
                + SigV4.sha256Hex(String.valueOf(canonicalRequest));
        byte[] signingKey = SigV4.signingKey(SECRET, dateStamp, REGION, "s3");
        return SigV4.hex(SigV4.hmacSha256(signingKey, stringToSign));
    }

    private static Map<String, String[]> params(String accessKey, String date) {
        Map<String, String[]> params = new LinkedHashMap<>();
        params.put("X-Amz-Algorithm", new String[]{SigV4.ALGORITHM});
        params.put("X-Amz-Credential", new String[]{accessKey + "/" + date.substring(0, 8) + "/us-east-1/s3/aws4_request"});
        params.put("X-Amz-Date", new String[]{date});
        params.put("X-Amz-Expires", new String[]{"3600"});
        params.put("X-Amz-SignedHeaders", new String[]{"host"});
        params.put("X-Amz-Signature", new String[]{PLACEHOLDER});
        return params;
    }

    private static S3CallContext context(String method, String uri, Map<String, String> headers,
                                         Map<String, String[]> params) {
        return TestSignContexts.newContext(method, uri, null, headers, params, null);
    }

    private static Map<String, String> header(String name, String value) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(name, value);
        return headers;
    }

    private static String isoDate(Instant instant) {
        return DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC).format(instant);
    }
}
