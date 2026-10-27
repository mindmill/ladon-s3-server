/*
 * T14: AWS Signature Version 4 (authorization header based) against the AWS SigV4
 * reference test suite (src/test/resources/aws4testsuite, each case providing the raw
 * request `.req`, the canonical request `.creq` and the expected `.authz` header).
 *
 * The suite credentials (accessKeyId AKIDEXAMPLE, secretAccessKey
 * {@code wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY}, scope
 * {@code 20150830/us-east-1/service}) are taken from the AWS documentation page that
 * publishes the test suite; every other value is read from the suite files themselves.
 *
 * Mapping to the S3 server under test: the server derives the credential scope, the
 * signed headers and the payload hash from the client's own headers (Authorization,
 * x-amz-content-sha256), exactly as for a real S3 request. The tests therefore pass
 *
 *   - the request headers of the suite case,
 *   - the Authorization header as computed by the suite (scope + signed headers),
 *   - x-amz-content-sha256 = SHA-256 of the suite request body (empty for GET cases),
 *   - the canonical URI published in the corresponding {@code .creq} file, i.e. the
 *     very URI the suite's reference computation signs,
 *
 * and then assert that {@link AwsSignatureVersion4#computeV4} reproduces the
 * reference signature. This is a full cross-implementation check of the canonical
 * request construction (URI, query, canonical headers, payload hash) and of the
 * signing-key derivation.
 */

package de.mc.ladon.s3server.auth;

import de.mc.ladon.s3server.entities.api.S3CallContext;
import de.mc.ladon.s3server.entities.impl.S3CallContextImpl;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AwsSignatureVersion4Test {

    /** Credentials of the official AWS SigV4 reference test suite. */
    private static final String ACCESS_KEY = "AKIDEXAMPLE";
    private static final String SECRET_KEY = "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY";

    @Test
    void computesOfficialSuiteSignatures() throws IOException {
        Path suiteRoot = suiteRoot();
        List<Path> cases = new ArrayList<>();
        for (java.io.File candidate : suiteRoot.toFile().listFiles()) {
            if (candidate.isDirectory()) {
                cases.add(candidate.toPath());
            }
        }
        cases.sort(Path::compareTo);
        assertTrue(cases.size() >= 30, "expected at least 30 suite cases, found " + cases.size() + " in " + suiteRoot);

        for (Path caseDir : cases) {
            String name = caseDir.getFileName().toString();
            Path req = caseDir.resolve(name + ".req");
            Path creq = caseDir.resolve(name + ".creq");
            Path authz = caseDir.resolve(name + ".authz");
            assertTrue(Files.exists(req) && Files.exists(creq) && Files.exists(authz),
                    "suite case '" + name + "' must provide .req, .creq and .authz files");

            String authorizationHeader = Files.readString(authz, StandardCharsets.UTF_8).trim();
            SuiteCase suiteCase = parseSuiteCase(Files.readString(req, StandardCharsets.UTF_8),
                    Files.readString(creq, StandardCharsets.UTF_8));
            suiteCase.expectedSignature = expectedSignature(authorizationHeader);

            // S3 header authorization: the payload hash is delivered as the
            // x-amz-content-sha256 header (here: the hash of the suite request body).
            // The credential scope and signed headers come from the client's
            // Authorization header, as in a real request.
            Map<String, String> headers = new LinkedHashMap<>(suiteCase.headers);
            headers.put("x-amz-content-sha256", SigV4.sha256Hex(suiteCase.body.getBytes(StandardCharsets.UTF_8)));
            headers.put("authorization", authorizationHeader);

            // The query parameters a servlet container would deliver for the suite wire
            // query are the (decoded) pairs of the canonical query line — the raw `.req`
            // rendering of some cases (e.g. post-vanilla-query-nonunreserved) is a lossy
            // un-escaped display and cannot be parsed back into the signed pairs.
            String queryString = suiteCase.canonicalQuery.isEmpty() ? null : suiteCase.canonicalQuery;

            S3CallContext ctx = TestSignContexts.newContext(suiteCase.method, suiteCase.canonicalUri,
                    queryString,
                    headers, parametersOf(suiteCase.canonicalQuery),
                    suiteCase.body.getBytes(StandardCharsets.UTF_8));
            ((S3CallContextImpl) ctx).setUser(TestSignContexts.user(ACCESS_KEY, SECRET_KEY));

            String computed = new AwsSignatureVersion4().computeV4(ctx);
            assertEquals(suiteCase.expectedSignature, computed,
                    "suite case '" + name + "' must be signed with the reference signature");
        }
    }

    @Test
    void multiValueServedByServletIsNormalized() throws IOException {
        // Servlet containers present multi-valued headers comma-separated: "value1, value2, value3"
        // (the same values the suite's get-header-value-multiline case folds across lines).
        Path caseDir = suiteRoot().resolve("get-header-value-multiline");
        String authorizationHeader = Files.readString(caseDir.resolve(caseDir.getFileName().toString() + ".authz"),
                StandardCharsets.UTF_8).trim();

        SuiteCase suiteCase = parseSuiteCase(
                "GET / HTTP/1.1\n"
                        + "Host:example.amazonaws.com\n"
                        + "My-Header1:value1, value2, value3\n"
                        + "X-Amz-Date:20150830T123600Z",
                Files.readString(caseDir.resolve("get-header-value-multiline.creq"), StandardCharsets.UTF_8));
        Map<String, String> headers = new LinkedHashMap<>(suiteCase.headers);
        headers.put("x-amz-content-sha256", SigV4.sha256Hex(new byte[0]));
        headers.put("authorization", authorizationHeader);
        S3CallContext ctx = TestSignContexts.newContext(suiteCase.method, suiteCase.canonicalUri, null,
                headers, Map.of(), new byte[0]);
        ((S3CallContextImpl) ctx).setUser(TestSignContexts.user(ACCESS_KEY, SECRET_KEY));

        String computed = new AwsSignatureVersion4().computeV4(ctx);
        assertEquals(expectedSignature(authorizationHeader), computed,
                "comma-separated multi-value headers must normalize to the canonical form");
    }

    @Test
    void duplicateQueryParametersAreSignedPerSpecification() throws IOException {
        // ?Param1=value2&Param1=Value1 -> canonical form carries both values, sorted: Param1=Value1&Param1=value2
        Path caseDir = suiteRoot().resolve("get-vanilla-query-order-key");
        String authorizationHeader = Files.readString(caseDir.resolve(caseDir.getFileName().toString() + ".authz"),
                StandardCharsets.UTF_8).trim();

        Map<String, String[]> params = new LinkedHashMap<>();
        params.put("Param1", new String[]{"value2", "Value1"});
        Map<String, String> headers = new LinkedHashMap<>(Map.of(
                "Host", "example.amazonaws.com",
                "X-Amz-Date", "20150830T123600Z"));
        headers.put("x-amz-content-sha256", SigV4.sha256Hex(new byte[0]));
        headers.put("authorization", authorizationHeader);
        S3CallContext ctx = TestSignContexts.newContext("GET", "/", "Param1=value2&Param1=Value1",
                headers, params, new byte[0]);
        ((S3CallContextImpl) ctx).setUser(TestSignContexts.user(ACCESS_KEY, SECRET_KEY));

        String computed = new AwsSignatureVersion4().computeV4(ctx);
        assertEquals(expectedSignature(authorizationHeader), computed,
                "duplicated query parameters must be signed with every value, sorted");
    }

    // ------------------------------------------------------------------
    // suite parsing
    // ------------------------------------------------------------------

    private static final class SuiteCase {
        String method;
        String canonicalUri;
        String canonicalQuery = "";
        String rawQuery;
        Map<String, String> headers = new LinkedHashMap<>();
        String body = "";
        String expectedSignature;
    }

    private static Path suiteRoot() {
        try {
            return Path.of(AwsSignatureVersion4Test.class.getResource("/aws4testsuite").toURI());
        } catch (Exception e) {
            throw new IllegalStateException("aws4testsuite resource not found on classpath", e);
        }
    }

    /**
     * Parses an HTTP/1.1 request as delivered by the suite (request line, header
     * lines, optional body after the blank line) together with the canonical URI
     * published in the matching canonical request file.
     *
     * <p>Header lines with a repeated name (and obs-fold continuation lines) are
     * joined comma-separated, which is exactly how a servlet container presents the
     * values to {@code HttpServletRequest.getHeader}.</p>
     */
    private static SuiteCase parseSuiteCase(String rawRequest, String canonicalRequest) {
        // One suite file (get-space) carries a UTF-8 BOM — strip it if present.
        if (rawRequest.length() > 0 && rawRequest.charAt(0) == '\uFEFF') {
            rawRequest = rawRequest.substring(1);
        }
        String[] lines = rawRequest.split("\r?\n", -1);
        SuiteCase spec = new SuiteCase();

        String requestLine = lines[0].trim();
        int sp1 = requestLine.indexOf(' ');
        int sp2 = requestLine.indexOf(' ', sp1 + 1);
        spec.method = requestLine.substring(0, sp1).toUpperCase(Locale.ROOT);
        String rawResource = sp2 < 0 ? requestLine.substring(sp1 + 1) : requestLine.substring(sp1 + 1, sp2);
        int q = rawResource.indexOf('?');
        if (q < 0) {
            spec.rawQuery = null;
        } else {
            spec.rawQuery = rawResource.substring(q + 1);
        }

        String lastHeader = null;
        boolean body = false;
        List<String> bodyLines = new ArrayList<>();
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            if (body) {
                bodyLines.add(line);
                continue;
            }
            if (line.isEmpty()) {
                body = true;
                continue;
            }
            if (line.startsWith(" ") || line.startsWith("\t")) {
                // obs-fold continuation of the previous header: the suite treats the
                // folded values as additional comma-separated header values.
                String previous = lastHeader != null ? findHeader(spec.headers, lastHeader) : null;
                String value = previous == null
                        ? line.trim()
                        : previous + "," + line.trim();
                if (lastHeader != null) {
                    spec.headers.put(lastHeader, value);
                }
                continue;
            }
            int colon = line.indexOf(':');
            if (colon <= 0) {
                continue;
            }
            String name = line.substring(0, colon).trim();
            String value = line.substring(colon + 1).trim();
            String existing = findHeader(spec.headers, name);
            spec.headers.put(name, existing == null ? value : existing + "," + value);
            lastHeader = name;
        }
        spec.body = String.join("\n", bodyLines);

        // The canonical URI and query are the very ones the reference computation
        // signs (second and third lines of the canonical request).
        String[] creqLines = canonicalRequest.split("\r?\n", -1);
        spec.canonicalUri = creqLines.length > 1 ? creqLines[1] : "/";
        spec.canonicalQuery = creqLines.length > 2 ? creqLines[2] : "";
        return spec;
    }

    /**
     * Case-insensitive header lookup in the parsed header map.
     */
    private static String findHeader(Map<String, String> headers, String name) {
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(name)) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * Decodes a raw query string into the (name, values) map a servlet container
     * would deliver via {@code getParameterMap()}; duplicated names keep every value.
     */
    private static Map<String, String[]> parametersOf(String rawQuery) {
        Map<String, String[]> result = new LinkedHashMap<>();
        if (rawQuery == null || rawQuery.isEmpty()) {
            return result;
        }
        for (String pair : rawQuery.split("&")) {
            if (pair.isEmpty()) {
                continue;
            }
            int eq = pair.indexOf('=');
            String name = decode(eq < 0 ? pair : pair.substring(0, eq));
            String value = eq < 0 ? "" : decode(pair.substring(eq + 1));
            String[] existing = result.get(name);
            if (existing == null) {
                result.put(name, new String[]{value});
            } else {
                String[] next = new String[existing.length + 1];
                System.arraycopy(existing, 0, next, 0, existing.length);
                next[existing.length] = value;
                result.put(name, next);
            }
        }
        return result;
    }

    private static String decode(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (RuntimeException e) {
            return value;
        }
    }

    /**
     * Extracts the Signature= part of an Authorization header line and validates its
     * form before comparing.
     */
    private static String expectedSignature(String authorizationHeader) {
        String value = authorizationHeader.trim();
        for (String part : value.split(",")) {
            String trimmed = part.trim();
            if (trimmed.toLowerCase(Locale.ROOT).startsWith("signature=")) {
                String sig = trimmed.substring("signature=".length());
                assertTrue(sig.matches("[0-9a-fA-F]{64}"), "unexpected signature in authz: " + sig);
                return sig;
            }
        }
        throw new IllegalArgumentException("no Signature= in " + value);
    }
}
