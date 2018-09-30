package de.mc.ladon.s3server.auth;

import org.junit.Test;

import java.security.NoSuchAlgorithmException;

import static org.junit.Assert.assertFalse;

public class TestAuthKeyGen {

    @Test
    public void testNoMinusNoUnderscore() throws NoSuchAlgorithmException {
       for (int i = 0 ; i < 1000; i++){
           AuthKeyGen.AwsKeyPair keyPair  = AuthKeyGen.generateKeypair();
           System.out.println(keyPair);
           assertFalse(keyPair.awsAccessKeyId.contains("-"));
           assertFalse(keyPair.awsAccessKeyId.contains("_"));
           assertFalse(keyPair.awsSecretAccessKey.contains("-"));
           assertFalse(keyPair.awsSecretAccessKey.contains("_"));
       }
    }
}
