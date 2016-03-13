/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.ladon.s3server.auth;

import de.mc.ladon.s3server.entities.api.S3CallContext;
import de.mc.ladon.s3server.entities.api.S3User;
import de.mc.ladon.s3server.entities.impl.S3CallContextImpl;
import de.mc.ladon.s3server.exceptions.InvalidAccessKeyIdException;
import de.mc.ladon.s3server.exceptions.InvalidSecurityException;
import de.mc.ladon.s3server.exceptions.MissingSecurityHeaderException;
import de.mc.ladon.s3server.exceptions.S3ServerException;
import de.mc.ladon.s3server.repository.api.S3Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ralf Ulrich on 13.03.16.
 */
public class Authorization {
    private static final Logger logger = LoggerFactory.getLogger(AwsSignatureVersion2.class);

    /**
     * Verify the provided credentials from the authentication header.
     * Also gets the users credentials from the repository and puts them into the S3CallContext.
     *
     * @param callContext the S3CallContext
     * @throws S3ServerException if authentication failsl
     */
    public static void checkAuthHeader(S3CallContext callContext, S3Repository repository) throws S3ServerException {
        String auth = callContext.getHeader().getAuthorization();
        if (auth == null) {
            throw new MissingSecurityHeaderException("", callContext.getRequestId());
        }

        AwsSignatureVersion4 s4 = new AwsSignatureVersion4();
        if (s4.isV4(callContext)) {
            logger.debug("found v4");
            String accessKey = s4.getAccessKey(callContext);
            putUserIntoCallContext(callContext, repository, accessKey);
            String signature = s4.computeV4(callContext);
            if (!signature.equals(s4.getSignature(callContext))) {
                throw new InvalidSecurityException("", callContext.getRequestId());
            }
        } else {
            logger.debug("found v2");
            AwsSignatureVersion2 s2 = new AwsSignatureVersion2();
            String accessKey = s2.getAccessKey(callContext);
            putUserIntoCallContext(callContext, repository, accessKey);
            String signature = s2.computeV2(callContext, "");
            if (!signature.equals(s2.getSignature(callContext))) {
                throw new InvalidSecurityException("", callContext.getRequestId());
            }
        }


    }

    private static void putUserIntoCallContext(S3CallContext callContext, S3Repository repository, String accessKey) {
        S3User user = repository.getUser(callContext, accessKey);
        if (user == null) {
            throw new InvalidAccessKeyIdException("", callContext.getRequestId());
        }
        ((S3CallContextImpl) callContext).setUser(user);
    }

}
