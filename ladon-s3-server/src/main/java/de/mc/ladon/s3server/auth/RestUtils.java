package de.mc.ladon.s3server.auth;

import de.mc.ladon.s3server.common.S3Constants;
import de.mc.ladon.s3server.entities.api.S3CallContext;

import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Utilities for the AWS Signature Version 2 canonical string (StringToSign), built strictly
 * following the S3 SigV2 specification:
 *
 * <pre>
 * StringToSign = HTTP-Verb + "\n"
 *              + Content-MD5 + "\n"
 *              + Content-Type + "\n"
 *              + Date + "\n"
 *              + CanonicalizedACLHeaders
 *              + CanonicalizedResource
 * </pre>
 *
 * where the {@code x-amz-*} headers are sorted by name, and the canonical resource is the
 * request path followed by the signed sub-resources sorted by name.
 */
public class RestUtils {

    /**
     * The set of sub-resources which are part of the canonical StringToSign (signed
     * sub-resources). Parameters outside of this set are not signed.
     *
     * <p>Keys are the lowercased parameter names, values are the <em>canonical</em>
     * sub-resource names exactly as they appear in the AWS SigV2 sub-resource list
     * (e.g. {@code partNumber}, {@code uploadId}) — the canonical names are what is
     * signed in the StringToSign.</p>
     */
    private static final Map<String, String> SIGNED_PARAMETERS = Map.ofEntries(
            Map.entry("acl", "acl"),
            Map.entry("torrent", "torrent"),
            Map.entry("logging", "logging"),
            Map.entry("location", "location"),
            Map.entry("policy", "policy"),
            Map.entry("requestpayment", "requestPayment"),
            Map.entry("versioning", "versioning"),
            Map.entry("versions", "versions"),
            Map.entry("versionid", "versionId"),
            Map.entry("notification", "notification"),
            Map.entry("uploadid", "uploadId"),
            Map.entry("uploads", "uploads"),
            Map.entry("partnumber", "partNumber"),
            Map.entry("website", "website"),
            Map.entry("delete", "delete"),
            Map.entry("lifecycle", "lifecycle"),
            Map.entry("tagging", "tagging"),
            Map.entry("cors", "cors"),
            Map.entry("restore", "restore"),
            Map.entry("replication", "replication"),
            Map.entry("object-lock", "object-lock"),
            Map.entry("retention", "retention"),
            Map.entry("legal-hold", "legal-hold"),
            Map.entry("x-amz-security-token", "x-amz-security-token"),
            Map.entry("x-id", "x-id"),
            Map.entry("seal", "seal"),
            Map.entry("response-content-type", "response-content-type"),
            Map.entry("response-content-language", "response-content-language"),
            Map.entry("response-expires", "response-expires"),
            Map.entry("response-cache-control", "response-cache-control"),
            Map.entry("response-content-disposition", "response-content-disposition"),
            Map.entry("response-content-encoding", "response-content-encoding"),
            Map.entry("security", "security"));

    private RestUtils() {
    }

    /**
     * Normalizes a value used in a SigV2 canonical StringToSign. According to the AWS SigV2
     * spec the header name is lowercased, values of headers with the same name are joined
     * with commas "without any spaces between values", and the surrounding whitespace of
     * each value is trimmed — but consecutive whitespace <em>inside</em> a value is left
     * intact (unlike SigV4).
     */
    private static String canonicalSigV2Value(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder result = new StringBuilder(value.length());
        boolean first = true;
        for (String part : value.split(",")) {
            if (!first) {
                result.append(',');
            }
            first = false;
            result.append(part.trim());
        }
        return result.toString();
    }

    public static String makeS3CanonicalString(S3CallContext context, String expires) {
        Map<String, String> fullHeader = context.getHeader().getFullHeader();

        String contentMd5 = SigV4.headerIgnoreCase(fullHeader, "content-md5");
        String contentType = SigV4.headerIgnoreCase(fullHeader, "content-type");

        // Date line, per the AWS SigV2 spec:
        //  - for presigned URLs the Expires parameter replaces the date (the Date and
        //    x-amz-date headers are not used at all),
        //  - for header-based requests the x-amz-date header takes precedence over the
        //    Date header when both are present (its value is signed literally in the
        //    date line and the header is <em>not</em> repeated in CanonicalizedAmzHeaders).
        String dateLine;
        if (expires != null && !expires.isEmpty()) {
            dateLine = expires;
        } else {
            String xamzDate = SigV4.headerIgnoreCase(fullHeader, "x-amz-date");
            if (xamzDate == null) {
                xamzDate = SigV4.headerIgnoreCase(fullHeader, "date");
            }
            dateLine = xamzDate == null ? "" : xamzDate.trim();
        }

        // Positional StringToSign lines are signed as sent — only surrounding whitespace
        // is trimmed; the internal whitespace of the date value must be preserved
        // (the AWS sigV2 spec examples sign "Tue, 27 Mar 2007 19:36:42 +0000" literally).
        StringBuilder buf = new StringBuilder(context.getMethod().length() + 128);
        buf.append(context.getMethod()).append("\n");
        buf.append(contentMd5 == null ? "" : contentMd5.trim()).append("\n");
        buf.append(contentType == null ? "" : contentType.trim()).append("\n");
        buf.append(dateLine).append("\n");

        // CanonicalizedAmzHeaders: all x-amz-* headers (request headers and query
        // parameters) sorted by name — except x-amz-date, which is a positional date
        // element (see above).
        Map<String, String> amzHeaders = new TreeMap<>();
        if (fullHeader != null) {
            for (Map.Entry<String, String> entry : fullHeader.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) {
                    continue;
                }
                String lower = entry.getKey().toLowerCase(Locale.ROOT);
                if (lower.startsWith(S3Constants.X_AMZ_PREFIX) && !"x-amz-date".equals(lower)) {
                    amzHeaders.put(lower, entry.getValue());
                }
            }
        }
        for (Map.Entry<String, String> entry : context.getParams().getAllParams().entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            String lower = entry.getKey().toLowerCase(Locale.ROOT);
            if (lower.startsWith(S3Constants.X_AMZ_PREFIX) && !"x-amz-date".equals(lower)) {
                amzHeaders.put(lower, entry.getValue());
            }
        }
        for (Map.Entry<String, String> entry : amzHeaders.entrySet()) {
            buf.append(entry.getKey()).append(':').append(canonicalSigV2Value(entry.getValue())).append("\n");
        }

        // CanonicalizedResource: the request path and the signed sub-resources, sorted by name.
        // Sub-resource names are signed in their canonical (camelCase) form, e.g. partNumber
        // and uploadId (AWS SigV2 spec: "subresources must be lexicographically sorted by
        // subresource name", names taken from the sub-resource list).
        buf.append(context.getUri());
        TreeMap<String, String> signedParams = new TreeMap<>();
        for (Map.Entry<String, String> entry : context.getParams().getAllParams().entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            String canonical = SIGNED_PARAMETERS.get(entry.getKey());
            if (canonical == null) {
                canonical = SIGNED_PARAMETERS.get(entry.getKey().toLowerCase(Locale.ROOT));
            }
            if (canonical != null && !signedParams.containsKey(canonical)) {
                signedParams.put(canonical, entry.getValue());
            }
        }
        if (!signedParams.isEmpty()) {
            buf.append("?");
            boolean first = true;
            for (Map.Entry<String, String> entry : signedParams.entrySet()) {
                if (!first) {
                    buf.append("&");
                }
                first = false;
                buf.append(entry.getKey());
                if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                    buf.append("=").append(entry.getValue());
                }
            }
        }
        return buf.toString();
    }
}
