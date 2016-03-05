/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.jaxb.fsmeta;

import de.mc.s3server.entities.api.S3User;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Ralf Ulrich on 05.03.16.
 */
@XmlRootElement(name = "FsUserMeta")
public class FSUserData {

    private String publicKey;
    private String secretKey;
    private String userId;
    private String userName;
    private String email;

    public FSUserData() {
    }

    public FSUserData(S3User user) {
        this.publicKey = user.getPublicKey();
        this.secretKey = user.getSecretKey();
        this.userId = user.getUserId();
        this.userName = user.getUserName();
        this.email = user.getEmail();
    }

    @XmlElement(name = "PublicKey")
    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    @XmlElement(name = "SecretKey")
    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    @XmlElement(name = "UserId")
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @XmlElement(name = "UserName")
    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    @XmlElement(name = "Email")
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
