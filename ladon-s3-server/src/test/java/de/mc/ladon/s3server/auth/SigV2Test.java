/*
 * T17: AWS Signature Version 2 — the legacy S3 scheme.
 *
 * <ul>
 *     <li>{@link RestUtils#makeS3CanonicalString} must build exactly the
 *         spec's StringToSign (hand-computed reference values are asserted).</li>
 *     <li>{@link AwsSignatureVersion2} must produce the base64(HMAC-SHA1) of that
 *         StringToSign (independently computed signatures are asserted) and
 *         parse the {@code AWS accesskey:signature} header.</li>
 *     <li>{@link Authorization#checkAuthHeader} must accept a correctly signed
 *         (header + presigned query) request and reject wrong signatures,
 *         unknown access keys and unparsable/missing security data.</li>
 * </ul>
 */
package de.mc.ladon.s3server.auth;

import de.mc.ladon.s3server.entities.api.S3CallContext;
import de.mc.ladon.s3server.entities.impl.S3CallContextImpl;
import de.mc.ladon.s3server.exceptions.InvalidAccessKeyIdException;
import de.mc.ladon.s3server.exceptions.InvalidSecurityException;
import de.mc.ladon.s3server.exceptions.MissingSecurityHeaderException;
import de.mc.ladon.s3server.repository.api.S3Repository;
import org.junit.jupiter.api.Test;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SigV2Test {

    private static final String ACCESS = "V2EXAMPLEACCESSKEY";
    private static final String SECRET = "v2-test-secret-key";
    /** The 2011 vector date (the canonical-string tests are clock-independent). */
    private static final String VECTOR_DATE = "Thu, 24 Feb 2011 00:00:00 GMT";

    private static S3Repository repository() {
        return TestSignContexts.repository(TestSignContexts.user(ACCESS, SECRET));
    }

    // ------------------------------------------------------------- canonical string

    @Test
    void canonicalStringMatchesTheSpecVector() {
        S3CallContext ctx = context("GET", "/my-bucket/my-object", Map.of("acl", "public-read"),
                Map.of(
                        "Date", VECTOR_DATE,
                        "Content-Type", "text/plain",
                        "Content-MD5", "eB5eJF1ptWaXm4bijSPxNg==",
                        "x-amz-meta-author", "ralf",
                        "x-amz-security-token", "token123"),
                null);

        // Reference StringToSign per the S3 SigV2 spec:
        //   GET \n <md5> \n <type> \n <date> \n <x-amz-* sorted by name> \n <canonical resource>
        String expected = "GET\n"
                + "eB5eJF1ptWaXm4bijSPxNg==\n"
                + "text/plain\n"
                + VECTOR_DATE + "\n"
                + "x-amz-meta-author:ralf\n"
                + "x-amz-security-token:token123\n"
                + "/my-bucket/my-object?acl=public-read";

        assertEquals(expected, RestUtils.makeS3CanonicalString(ctx, ""),
                "the SigV2 StringToSign must follow the S3 specification exactly");
    }

    @Test
    void canonicalStringUsesExpiresAndSortsSignedSubResources() {
        S3CallContext ctx = context("GET", "/uploads/file.bin",
                Map.of("uploadId", "1234567890", "foo", "bar", "partNumber", "7"),
                Map.of("Host", "example.amazonaws.com"),
                null);

        // no md5/type/date lines, expires replaces the date line;
        // "foo" is not a signed sub-resource and is therefore not signed
        String expected = "GET\n\n\n"
                + "1893456000\n"
                + "/uploads/file.bin?partNumber=7&uploadId=1234567890";

        assertEquals(expected, RestUtils.makeS3CanonicalString(ctx, "1893456000"),
                "presigned URLs must sign with Expires, unsigned sub-resources must be skipped");
    }

    @Test
    void xamzDateTakesPrecedenceOverDateHeader() {
        // AWS SigV2 spec: when an x-amz-date header is present, the system ignores the
        // Date header when computing the request signature — the date line must carry
        // the x-amz-date value.
        S3CallContext ctx = context("PUT", "/b/k", Map.of("object-lock", ""),
                Map.of(
                        "Content-Type", "application/octet-stream",
                        "Date", "Tue, 27 Mar 2007 21:20:27 +0000",
                        "x-amz-date", "Tue, 27 Mar 2007 21:20:26 +0000",
                        "x-amz-security-token", "tok123"),
                null);

        String expected = "PUT\n"
                + "\n"
                + "application/octet-stream\n"
                + "Tue, 27 Mar 2007 21:20:26 +0000\n"
                + "x-amz-security-token:tok123\n"
                + "/b/k?object-lock";
        assertEquals(expected, RestUtils.makeS3CanonicalString(ctx, null),
                "the x-amz-date header must take precedence over the Date header");

        ((S3CallContextImpl) ctx).setUser(TestSignContexts.user(ACCESS, SECRET));
        // independently computed: base64(HMAC-SHA1(secret, StringToSign))
        assertEquals("NXTxfpJ57/EWdnx0XI3OYwshZYs=", AwsSignatureVersion2.computeV2(ctx, null));
    }

    // ------------------------------------------------------------------ signature

    @Test
    void signatureIsBase64HmacSha1OfTheCanonicalString() {
        S3CallContext ctx = context("GET", "/my-bucket/my-object", Map.of("acl", "public-read"),
                Map.of(
                        "Date", VECTOR_DATE,
                        "Content-Type", "text/plain",
                        "Content-MD5", "eB5eJF1ptWaXm4bijSPxNg==",
                        "x-amz-meta-author", "ralf",
                        "x-amz-security-token", "token123"),
                null);
        ((S3CallContextImpl) ctx).setUser(TestSignContexts.user(ACCESS, SECRET));

        // independently computed: base64(HMAC-SHA1(secret, StringToSign))
        assertEquals("V4Dd8WsCQpayfLQF30IWx75UqaA=", AwsSignatureVersion2.computeV2(ctx, ""));

        S3CallContext ctx2 = context("GET", "/uploads/file.bin", Map.of("uploadId", "1234567890", "partNumber", "7"),
                Map.of(), null);
        ((S3CallContextImpl) ctx2).setUser(TestSignContexts.user(ACCESS, SECRET));
        assertEquals("6odoAkf/c+0vfQ6QX6jX2JRh3W4=", AwsSignatureVersion2.computeV2(ctx2, "1893456000"));
    }

    @Test
    void parsesAccessKeyAndSignatureFromTheAuthorizationHeader() {
        S3CallContext ctx = context("GET", "/x", Map.of(),
                Map.of("Authorization", "AWS " + ACCESS + ":abc123signature=="), null);
        AwsSignatureVersion2 s2 = new AwsSignatureVersion2();
        assertEquals(ACCESS, s2.getAccessKey(ctx));
        assertEquals("abc123signature==", s2.getSignature(ctx));

        S3CallContext other = context("GET", "/x", Map.of(),
                Map.of("Authorization", "AWS OTHERKEY:sig="), null);
        assertEquals("OTHERKEY", new AwsSignatureVersion2().getAccessKey(other));
        assertEquals("sig=", new AwsSignatureVersion2().getSignature(other));

        S3CallContext none = context("GET", "/x", Map.of(), Map.of("Host", "example.amazonaws.com"), null);
        assertNull(new AwsSignatureVersion2().getAccessKey(nonAuthCtx(none)), "no V4-style data must yield null");
    }

    /**
     * The v2 parser must not match on a v4-style Authorization header.
     */
    private static S3CallContext nonAuthCtx(S3CallContext ctx) {
        return context("GET", "/x", Map.of(),
                Map.of("Authorization", "AWS4-HMAC-SHA256 Credential=AK/20150830/us-east-1/s3/aws4_request, SignedHeaders=host, Signature=xx"), null);
    }

    // ----------------------------------------------------------------- full protocol

    @Test
    void acceptsCorrectlySignedHeaderRequest() {
        String date = rfc1123(new Date()); // within the allowed clock skew; identical for signing and request
        Map<String, String> headers = new LinkedHashMap<>(Map.of(
                "Content-Type", "text/plain",
                "Date", date,
                "x-amz-security-token", "token123"));

        S3CallContext signCtx = context("GET", "/my-bucket/my-object", Map.of("acl", "public-read"),
                new LinkedHashMap<>(headers), null);
        ((S3CallContextImpl) signCtx).setUser(TestSignContexts.user(ACCESS, SECRET));
        String signature = AwsSignatureVersion2.computeV2(signCtx, "");

        headers.put("Authorization", "AWS " + ACCESS + ":" + signature);
        S3CallContext request = context("GET", "/my-bucket/my-object", Map.of("acl", "public-read"),
                new LinkedHashMap<>(headers), null);

        assertDoesNotThrow(() -> Authorization.checkAuthHeader(request, repository()),
                "a correctly signed SigV2 header request must be accepted");
    }

    @Test
    void rejectsWrongSignatureAndUnknownAccessKey() {
        String date = rfc1123(new Date());
        S3CallContext request = context("GET", "/o", Map.of(),
                new LinkedHashMap<>(Map.of(
                        "Date", date,
                        "Authorization", "AWS " + ACCESS + ":wrongsignature=")),
                null);
        assertThrows(InvalidSecurityException.class,
                () -> Authorization.checkAuthHeader(request, repository()),
                "a wrong SigV2 signature must be rejected");

        S3CallContext unknown = context("GET", "/o", Map.of(),
                new LinkedHashMap<>(Map.of(
                        "Date", date,
                        "Authorization", "AWS SOMEONEELSE:sig=")),
                null);
        assertThrows(InvalidAccessKeyIdException.class,
                () -> Authorization.checkAuthHeader(unknown, repository()),
                "an unknown access key must be rejected with InvalidAccessKeyId");
    }

    @Test
    void acceptsPresignedQueryRequest() {
        String expires = Long.toString(System.currentTimeMillis() / 1000 + 3600);
        S3CallContext signCtx = context("GET", "/presigned/object", Map.of(), Map.of(), null);
        ((S3CallContextImpl) signCtx).setUser(TestSignContexts.user(ACCESS, SECRET));
        String signature = AwsSignatureVersion2.computeV2(signCtx, expires);

        S3CallContext request = requestWith("AWSAccessKeyId", ACCESS, "Expires", expires, "Signature", signature);
        assertDoesNotThrow(() -> Authorization.checkAuthHeader(request, repository()),
                "a correctly signed SigV2 presigned URL must be accepted");
    }

    @Test
    void rejectsWrongQuerySignature() {
        String expires = Long.toString(System.currentTimeMillis() / 1000 + 3600);
        S3CallContext request = requestWith("AWSAccessKeyId", ACCESS, "Expires", expires, "Signature", "tampered=");
        assertThrows(InvalidSecurityException.class,
                () -> Authorization.checkAuthHeader(request, repository()),
                "a wrong SigV2 query signature must be rejected");
    }

    @Test
    void rejectsExpiredPresignedUrl() {
        String expired = Long.toString(System.currentTimeMillis() / 1000 - 7200);
        S3CallContext signCtx = context("GET", "/presigned/object", Map.of(), Map.of(), null);
        ((S3CallContextImpl) signCtx).setUser(TestSignContexts.user(ACCESS, SECRET));
        // the signature is correct for the (already past) expiry — the expiry check must reject it
        String signature = AwsSignatureVersion2.computeV2(signCtx, expired);

        S3CallContext request = requestWith("AWSAccessKeyId", ACCESS, "Expires", expired, "Signature", signature);
        assertThrows(de.mc.ladon.s3server.exceptions.RequestTimeTooSkewedException.class,
                () -> Authorization.checkAuthHeader(request, repository()),
                "an expired SigV2 presigned URL must be rejected");
    }

    @Test
    void rejectsPresignedUrlWithoutExpiresOrUnknownKey() {
        S3CallContext noExpires = requestWith("AWSAccessKeyId", ACCESS, "Signature", "sig=");
        assertThrows(InvalidSecurityException.class,
                () -> Authorization.checkAuthHeader(noExpires, repository()),
                "a SigV2 presigned URL without Expires must be rejected");

        S3CallContext unknownKey = requestWith("AWSAccessKeyId", "SOMEONEELSE",
                "Expires", Long.toString(System.currentTimeMillis() / 1000 + 3600), "Signature", "sig=");
        assertThrows(InvalidAccessKeyIdException.class,
                () -> Authorization.checkAuthHeader(unknownKey, repository()),
                "an unknown access key in a presigned URL must be rejected");
    }

    @Test
    void requestWithoutAnySecurityDataIsRejected() {
        S3CallContext noAuth = context("GET", "/o", Map.of(), Map.of("Host", "example.amazonaws.com"), null);
        assertThrows(MissingSecurityHeaderException.class,
                () -> Authorization.checkAuthHeader(noAuth, repository()));
    }

    // ------------------------------------------------------------------ helpers

    private static S3CallContext requestWith(String k1, String v1, String k2, String v2, String k3, String v3) {
        Map<String, String[]> params = new LinkedHashMap<>();
        params.put(k1, new String[]{v1});
        params.put(k2, new String[]{v2});
        params.put(k3, new String[]{v3});
        return TestSignContexts.newContext("GET", "/presigned/object", null,
                Map.of("Host", "example.amazonaws.com"), params, null);
    }

    private static S3CallContext requestWith(String k1, String v1, String k2, String v2) {
        Map<String, String[]> params = new LinkedHashMap<>();
        params.put(k1, new String[]{v1});
        params.put(k2, new String[]{v2});
        return TestSignContexts.newContext("GET", "/presigned/object", null,
                Map.of("Host", "example.amazonaws.com"), params, null);
    }

    private static S3CallContext context(String method, String uri,
                                         Map<String, String> params,
                                         Map<String, String> headers,
                                         String unusedMarker) {
        Map<String, String[]> multi = new LinkedHashMap<>();
        params.forEach((k, v) -> multi.put(k, new String[]{v}));
        return TestSignContexts.newContext(method, uri, null, new LinkedHashMap<>(headers), multi, null);
    }

    private static String rfc1123(Date date) {
        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ROOT);
        format.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
        return format.format(date);
    }
}
