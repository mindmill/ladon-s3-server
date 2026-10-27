package de.mc.ladon.s3server.auth;

import org.junit.jupiter.api.Test;

import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class TestAuthKeyGen {

    @Test
    public void testNoMinusNoUnderscore() throws NoSuchAlgorithmException {
        for (int i = 0; i < 1000; i++) {
            AuthKeyGen.AwsKeyPair keyPair = AuthKeyGen.generateKeypair();
            assertFalse(keyPair.awsAccessKeyId.contains("-"));
            assertFalse(keyPair.awsAccessKeyId.contains("_"));
            assertFalse(keyPair.awsSecretAccessKey.contains("-"));
            assertFalse(keyPair.awsSecretAccessKey.contains("_"));
        }
    }
}
