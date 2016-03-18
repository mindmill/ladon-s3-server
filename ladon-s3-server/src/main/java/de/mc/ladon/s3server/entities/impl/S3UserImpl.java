/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.ladon.s3server.entities.impl;

import de.mc.ladon.s3server.entities.api.S3User;

/**
 * @author Ralf Ulrich on 17.02.16.
 */
public class S3UserImpl implements S3User {

    private String userId;
    private String userName;
    private String secretKey;
    private String publicKey;
    private String email;

    public S3UserImpl() {
    }

    public S3UserImpl(String userId, String userName, String publicKey, String secretKey, String email) {
        this.userId = userId;
        this.userName = userName;
        this.secretKey = secretKey;
        this.publicKey = publicKey;
        this.email = email;
    }

    @Override
    public String getUserId() {
        return userId;
    }

    public void setUserID(String userID) {
        this.userId = userID;
    }

    @Override
    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    @Override
    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    @Override
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String toString() {
        return "S3User{" +
                "userId='" + userId + '\'' +
                ", userName='" + userName + '\'' +
                '}';
    }
}
