/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.entities.api;

import de.mc.s3server.common.S3Constants;

/**
 * @author Ralf Ulrich on 17.02.16.
 */
public interface S3ACL {

    /**
     * The canned ACL to apply to the object.
     */
    S3Constants.CannedACL getACL();


    /**
     * Allows grantee to read the object data and its metadata.
     * Type: String
     * Default: None
     * Constraints: None
     */
    S3ACLGrantee getGrantRead();

    /**
     * This applies only when granting permission on a
     * bucket.
     * Type: String
     * Default: None
     * Constraints: None
     */
    S3ACLGrantee getGrantWrite();


    /**
     * Allows grantee to read the object ACL.
     * Type: String
     * Default: None
     * Constraints: None
     */
    S3ACLGrantee getGrantReadACP();

    /**
     * Allows grantee to write the ACL for the applicable object.
     * Type: String
     * Default: None
     * Constraints: None
     */
    S3ACLGrantee getGrantWriteACP();

    /**
     * Allows grantee the READ, READ_ACP, and WRITE_ACP permis- No
     * sions on the object.
     * Type: String
     * Default: None
     * Constraints: None
     */
    S3ACLGrantee getGrantFullControl();

}
