/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.ladon.s3server.auth;

import de.mc.ladon.s3server.common.S3ChunkedInputStream;
import de.mc.ladon.s3server.entities.api.S3RequestId;
import de.mc.ladon.s3server.exceptions.InvalidRequestException;

/**
 * Verifies the chained chunk signatures of a SigV4 streaming (chunked) upload.
 *
 * <p>The string to sign of a chunk (see "Signature Calculations for the Authorization
 * Header: Transferring Payload in Multiple Chunks (Chunked Upload)" in the Amazon S3
 * Developer Guide) is:</p>
 *
 * <pre>
 * AWS4-HMAC-SHA256-PAYLOAD
 * &lt;request date, x-amz-date&gt;
 * &lt;scope: date/region/service/aws4_request&gt;
 * &lt;signature of the previous chunk, or of the request itself for the first chunk&gt;
 * &lt;hash of the (non-signed) extension headers, empty here&gt;
 * &lt;hash of the chunk data&gt;
 * </pre>
 */
public final class SigV4ChunkVerifier implements S3ChunkedInputStream.ChunkSignatureVerifier {

    private static final String CHUNK_STRING_TO_SIGN_PREFIX = "AWS4-HMAC-SHA256-PAYLOAD";

    private final String requestDate;
    private final String scope;
    private final byte[] signingKey;
    private final String resource;
    private final S3RequestId requestId;

    /**
     * @param requestDate  the x-amz-date of the request (ISO 8601 basic, e.g. 20130524T000000Z)
     * @param scope        the credential scope from the request (date/region/service/aws4_request)
     * @param signingKey   the derived signing key for secret key, scope date, region and service "s3"
     * @param resourceId   resource identifier for error messages
     * @param requestId    request identifier for error messages
     */
    public SigV4ChunkVerifier(String requestDate, String scope, byte[] signingKey,
                              String resourceId, S3RequestId requestId) {
        this.requestDate = requestDate;
        this.scope = scope;
        this.signingKey = signingKey;
        this.resource = resourceId;
        this.requestId = requestId;
    }

    /**
     * Verifies the signature of the given chunk data.
     *
     * @param previousSignature the signature of the previous chunk, or of the request
     *                          itself (seed signature) for the first chunk; must not be null
     * @param chunkData         the raw data of the chunk (without framing)
     * @param providedSignature the chunk-signature from the chunk header
     * @throws InvalidRequestException if the signature does not match
     */
    /**
     * {@inheritDoc}. {@code chunkSize} is informational; the chunk data itself is signed.
     */
    @Override
    public void verify(String previousSignature, int chunkSize, byte[] chunkData, String providedSignature) {
        String stringToSign = CHUNK_STRING_TO_SIGN_PREFIX + "\n"
                + requestDate + "\n"
                + scope + "\n"
                + previousSignature + "\n"
                + SigV4.sha256Hex("") + "\n"
                + SigV4.sha256Hex(chunkData);
        String expected = SigV4.hex(SigV4.hmacSha256(signingKey, stringToSign));
        if (!SigV4.signaturesEqual(expected, providedSignature)) {
            throw new InvalidRequestException(resource, requestId);
        }
    }
}
