/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */
package de.mc.ladon.s3server.auth;

import com.google.common.io.BaseEncoding;

import javax.crypto.KeyGenerator;
import java.security.NoSuchAlgorithmException;

/**
 * Generates a new KeyPair
 * @author Ralf Ulrich 21.10.16.
 */
public class AuthKeyGen {


    public static AwsKeyPair generateKeypair() throws NoSuchAlgorithmException {
        KeyGenerator generator = KeyGenerator.getInstance(AwsSignatureVersion2.HMAC_SHA_1);
        generator.init(120);
        byte[] awsAccessKeyId = generator.generateKey().getEncoded();
        generator.init(240);
        byte[] awsSecretAccessKey = generator.generateKey().getEncoded();
        return new AwsKeyPair(
                BaseEncoding.base64Url().encode(awsSecretAccessKey),
                BaseEncoding.base64Url().encode(awsAccessKeyId)
        );

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
