/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.entities.impl;

import de.mc.s3server.common.S3Constants;
import de.mc.s3server.entities.api.S3ACL;
import de.mc.s3server.entities.api.S3ACLGrantee;

/**
 * @author Ralf Ulrich on 27.02.16.
 */
public class S3ACLImpl implements S3ACL {

    private S3Constants.CannedACL ACL;
    private S3ACLGrantee grantRead;
    private S3ACLGrantee grantWrite;
    private S3ACLGrantee grantReadACP;
    private S3ACLGrantee grantWriteACP;
    private S3ACLGrantee grantFullControl;

    public S3ACLImpl(String ACL, String grantRead, String grantWrite, String grantReadACP, String grantWriteACP, String grantFullControl) {
        this.ACL = S3Constants.CannedACL.valueOf(ACL);
        this.grantRead = new S3ACLGranteeImpl(grantRead);
        this.grantWrite = new S3ACLGranteeImpl(grantWrite);
        this.grantReadACP = new S3ACLGranteeImpl(grantReadACP);
        this.grantWriteACP = new S3ACLGranteeImpl(grantWriteACP);
        this.grantFullControl = new S3ACLGranteeImpl(grantFullControl);
    }

    @Override
    public S3Constants.CannedACL getACL() {
        return S3Constants.CannedACL.valueOf("");
    }


    @Override
    public S3ACLGrantee getGrantRead() {
        return grantRead;
    }

    @Override
    public S3ACLGrantee getGrantWrite() {
        return grantWrite;
    }

    @Override
    public S3ACLGrantee getGrantReadACP() {
        return grantReadACP;
    }

    @Override
    public S3ACLGrantee getGrantWriteACP() {
        return grantWriteACP;
    }

    @Override
    public S3ACLGrantee getGrantFullControl() {
        return grantFullControl;
    }
}
