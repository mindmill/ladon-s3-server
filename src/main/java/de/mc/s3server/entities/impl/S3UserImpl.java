/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.entities.impl;

import de.mc.s3server.entities.api.S3User;

/**
 * @author Ralf Ulrich on 17.02.16.
 */
public class S3UserImpl implements S3User {

    private String userID;
    private String userName;

    public S3UserImpl() {
    }

    public S3UserImpl(String userID, String userName) {
        this.userID = userID;
        this.userName = userName;
    }


    @Override
    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    @Override
    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
