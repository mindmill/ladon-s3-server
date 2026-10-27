/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */
package de.mc.ladon.s3server.auth;

import javax.crypto.KeyGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Generates a new KeyPair
 *
 * @author Ralf Ulrich 21.10.16.
 */
public class AuthKeyGen {


    public static AwsKeyPair generateKeypair() throws NoSuchAlgorithmException {
        String accessKey = "-";
        String secretKey = "-";
        while (accessKey.contains("-") || accessKey.contains("_")
                || secretKey.contains("-") || secretKey.contains("_")) {
            KeyGenerator generator = KeyGenerator.getInstance(AwsSignatureVersion2.HMAC_SHA_1);
            generator.init(120);
            byte[] awsAccessKeyId = generator.generateKey().getEncoded();
            accessKey = Base64.getUrlEncoder().encodeToString(awsAccessKeyId);
            generator.init(240);
            byte[] awsSecretAccessKey = generator.generateKey().getEncoded();
            secretKey = Base64.getUrlEncoder().encodeToString(awsSecretAccessKey);
        }
        return new AwsKeyPair(secretKey, accessKey);

    }

    public static class AwsKeyPair {
        public final String awsSecretAccessKey;
        public final String awsAccessKeyId;

        public AwsKeyPair(String awsSecretAccessKey, String awsAccessKeyId) {
            this.awsSecretAccessKey = awsSecretAccessKey;
            this.awsAccessKeyId = awsAccessKeyId;
        }

        @Override
        public String toString() {
            return "AwsKeyPair{" +
                    "awsSecretAccessKey='" + awsSecretAccessKey + '\'' +
                    ", awsAccessKeyId='" + awsAccessKeyId + '\'' +
                    '}';
        }
    }
}
