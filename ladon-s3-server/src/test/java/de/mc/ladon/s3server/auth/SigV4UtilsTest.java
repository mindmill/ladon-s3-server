/*
 * T19: Specification vectors for the pure SigV4 utility primitives
 * (URI-encoding, canonical query construction, header value normalization,
 * signing-key derivation, SHA-256 and hex helpers).
 *
 * The expected values are the published SigV4 reference values (or well known
 * digests) and were computed with an independent implementation (Python
 * standard library) — they are hard-coded on purpose so that a regression in
 * any of the primitives is caught immediately.
 */
package de.mc.ladon.s3server.auth;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SigV4UtilsTest {

    // ------------------------------------------------------------------ uriEncode

    @Test
    void uriEncodePassesUnreservedCharactersThrough() {
        assertEquals("A-Za-z0-9._~", SigV4.uriEncode("A-Za-z0-9._~"));
    }

    @Test
    void uriEncodeEncodesEveryOtherCharacter() {
        assertEquals("%20", SigV4.uriEncode(" "));
        assertEquals("%2F", SigV4.uriEncode("/"));
        assertEquals("%3A", SigV4.uriEncode(":"));
        assertEquals("%23", SigV4.uriEncode("#"));
        assertEquals("%2B", SigV4.uriEncode("+"));
        assertEquals("%3F", SigV4.uriEncode("?"));
        assertEquals("%26", SigV4.uriEncode("&"));
        assertEquals("%3D", SigV4.uriEncode("="));
        assertEquals("%22", SigV4.uriEncode("\""));
        assertEquals("%25", SigV4.uriEncode("%"));
    }

    @Test
    void uriEncodeUsesUtf8Bytes() {
        assertEquals("%C3%BC", SigV4.uriEncode("ü"));
        assertEquals("%E1%88%B4", SigV4.uriEncode("ሴ"));
        assertEquals("a%C3%BC", SigV4.uriEncode("aü"));
    }

    @Test
    void uriEncodeHandlesNullAndEmpty() {
        assertEquals("", SigV4.uriEncode(null));
        assertEquals("", SigV4.uriEncode(""));
    }

    // ------------------------------------------------------------------ sha256Hex

    @Test
    void sha256HexMatchesKnownDigests() {
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                SigV4.sha256Hex(""));
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                SigV4.sha256Hex("abc"));
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                SigV4.sha256Hex("abc".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }

    // ------------------------------------------------------------------ signingKey

    @Test
    void signingKeyDerivationMatchesReferenceVector() {
        // Reference values (secret wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY, date 20150830,
        // region us-east-1) computed with an independent implementation.
        assertEquals("938127b5336810ddb6a5d6af445fcac9e371f9ed418ed386b022aed82901be75",
                SigV4.hex(SigV4.signingKey("wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY",
                        "20150830", "us-east-1", "service")));
        assertEquals("32f78051dcde24c552811d654f4a769112bb834b03975cdd6b1fd7d16248c269",
                SigV4.hex(SigV4.signingKey("wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY",
                        "20150830", "us-east-1", "s3")));
    }

    @Test
    void signingKeyDependsOnAllScopeComponents() {
        String secret = "secret";
        byte[] base = SigV4.signingKey(secret, "20150830", "us-east-1", "s3");
        assertFalse(java.util.Arrays.equals(base, SigV4.signingKey(secret, "20150831", "us-east-1", "s3")),
                "a different date must yield a different key");
        assertFalse(java.util.Arrays.equals(base, SigV4.signingKey(secret, "20150830", "eu-west-1", "s3")),
                "a different region must yield a different key");
        assertFalse(java.util.Arrays.equals(base, SigV4.signingKey(secret, "20150830", "us-east-1", "service")),
                "a different service must yield a different key");
    }

    // ------------------------------------------------- canonicalQueryString (single value)

    @Test
    void canonicalQueryStringEncodesAndKeepsSinglePairs() {
        assertEquals("Param1=value1", SigV4.canonicalQueryString(Map.of("Param1", "value1")));
        assertEquals("Param1=%20value%201", SigV4.canonicalQueryString(Map.of("Param1", " value 1")));
        assertEquals("Param1=a%3Db", SigV4.canonicalQueryString(Map.of("Param1", "a=b")));
        assertEquals("Param1=", SigV4.canonicalQueryString(Map.of("Param1", "")));
    }

    @Test
    void canonicalQueryStringSortsByNameEncodesSeparators() {
        assertEquals("a=1&b=2", SigV4.canonicalQueryString(Map.of("b", "2", "a", "1")));
        assertEquals("param-name=value%2Fwith%2Fslash",
                SigV4.canonicalQueryString(Map.of("param-name", "value/with/slash")));
    }

    // ------------------------------------------------- canonicalQueryStringMulti

    @Test
    void canonicalQueryStringMultiSortsValuesPerSpecification() {
        // duplicated parameter name: every value is signed, sorted case-sensitively
        // (ASCII: uppercase sorts before lowercase) — exactly the AWS reference behaviour.
        assertEquals("Param1=Value1&Param1=value2",
                SigV4.canonicalQueryStringMulti(Map.of("Param1", List.of("value2", "Value1"))));
        assertEquals("Param1=value1&Param1=value2",
                SigV4.canonicalQueryStringMulti(Map.of("Param1", List.of("value2", "value1"))));
    }

    @Test
    void canonicalQueryStringMultiHandlesEmptyValueListsAndMixedKeys() {
        assertEquals("", SigV4.canonicalQueryStringMulti(Map.of("acl", List.of())));
        assertEquals("acl=value&x-param=another",
                SigV4.canonicalQueryStringMulti(Map.of("x-param", List.of("another"), "acl", List.of("value"))));
        assertEquals("tagging=%26", SigV4.canonicalQueryStringMulti(Map.of("tagging", List.of("&"))));
    }

    // ------------------------------------------------------------ canonicalHeaderValue

    @Test
    void canonicalHeaderValueTrimsLeadingAndTrailingWhitespace() {
        assertEquals("x-y", SigV4.canonicalHeaderValue("  x-y  "));
        assertEquals("v1,v2,v3", SigV4.canonicalHeaderValue(" v1 , v2 , v3 "));
    }

    @Test
    void canonicalHeaderValueCollapsesConsecutiveWhitespace() {
        // SigV4 spec: "convert consecutive whitespace to a single space"
        assertEquals("\"a b c\"", SigV4.canonicalHeaderValue("\"a   b   c\""));
        assertEquals("a b", SigV4.canonicalHeaderValue("a \t b"));
    }

    @Test
    void canonicalHeaderValueJoinsMultiValuesWithSingleCommas() {
        assertEquals("value1,value2,value3", SigV4.canonicalHeaderValue("value1,value2,value3"));
        assertEquals("value1,value2,value3", SigV4.canonicalHeaderValue(" value1 , value2 , value3 "));
        // values containing several spaces after collapsing
        assertEquals("a b,c", SigV4.canonicalHeaderValue("a   b,  c  "));
    }

    @Test
    void canonicalHeaderValueHandlesNullAndEmpty() {
        assertEquals("", SigV4.canonicalHeaderValue(null));
        assertEquals("", SigV4.canonicalHeaderValue(""));
    }

    // ------------------------------------------------------------ headerIgnoreCase

    @Test
    void headerIgnoreCaseResolvesCaseInsensitively() {
        Map<String, String> headers = Map.of("X-Amz-Date", "20150830T123600Z");
        assertEquals("20150830T123600Z", SigV4.headerIgnoreCase(headers, "x-amz-date"));
        assertEquals("20150830T123600Z", SigV4.headerIgnoreCase(headers, "X-AMZ-DATE"));
        assertEquals(null, SigV4.headerIgnoreCase(headers, "x-amz-content-sha256"));
        assertEquals(null, SigV4.headerIgnoreCase(null, "x-amz-date"));
    }

    // ------------------------------------------------------------ signaturesEqual

    @Test
    void signaturesEqualComparesCaseSensitivelyAndRejectsNull() {
        String sig = "abc123def456";
        assertTrue(SigV4.signaturesEqual(sig, sig));
        assertFalse(SigV4.signaturesEqual(sig, sig + "00"));
        assertFalse(SigV4.signaturesEqual(sig, ""));
        assertFalse(SigV4.signaturesEqual(sig, null));
    }

    // ------------------------------------------------- payload markers

    @Test
    void payloadMarkerChecksAreCaseInsensitive() {
        assertTrue(SigV4.isStreamingPayload("STREAMING-AWS4-HMAC-SHA256-PAYLOAD"));
        assertTrue(SigV4.isStreamingPayload("streaming-aws4-hmac-sha256-payload"));
        assertFalse(SigV4.isStreamingPayload("UNSIGNED-PAYLOAD"));
        assertFalse(SigV4.isStreamingPayload(null));

        assertTrue(SigV4.isUnsignedPayload("UNSIGNED-PAYLOAD"));
        assertFalse(SigV4.isUnsignedPayload("STREAMING-AWS4-HMAC-SHA256-PAYLOAD"));
        assertFalse(SigV4.isUnsignedPayload(null));
    }

    // ------------------------------------------------------------------ hex

    @Test
    void hexFormatsLowercase() {
        assertArrayEquals("deadbeef".getBytes(), SigV4.hex(new byte[]{(byte) 0xde, (byte) 0xad, (byte) 0xbe, (byte) 0xef}).getBytes());
        assertEquals("00", SigV4.hex(new byte[]{0}));
        assertEquals("", SigV4.hex(new byte[0]));
    }
}
