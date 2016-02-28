/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.repository.impl;

import de.mc.s3server.entities.api.S3User;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

/**
 * Simple file system lock object based on properties
 *
 * @author Ralf Ulrich on 28.02.16.
 */
public class FSLock {

    public static final String LOCK_TIME = "time";
    public static final String LOCK_TYPE = "type";
    public static final String LOCK_USER = "user";
    public static final String LOCK_FILE_EXTENSION = ".lock";
    private String type;
    private String user;
    private String time;
    private SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");


    public enum LockType {
        read, write
    }


    public static FSLock load(Path metaPath, String objectKey) throws IOException {
        Properties p = new Properties();
        Path lockPath = Paths.get(metaPath.toString(), objectKey + LOCK_FILE_EXTENSION);
        try (InputStream in = Files.newInputStream(lockPath)) {
            p.loadFromXML(in);
        }
        return new FSLock(p);

    }

    public void delete(S3User requestingUser, Path metaPath, String objectKey) throws IOException {
        if (isUnlockAllowed(requestingUser)) {
            Path lockPath = Paths.get(metaPath.toString(), objectKey + LOCK_FILE_EXTENSION);
            Files.delete(lockPath);
        }
    }


    public void save(Path metaPath, String objectKey) throws IOException {
        Path lockPath = Paths.get(metaPath.toString(), objectKey + LOCK_FILE_EXTENSION);
        try (OutputStream out = Files.newOutputStream(lockPath)) {
            toProperties().storeToXML(out, null);
        }
    }

    public FSLock(LockType type, S3User user) {
        this.type = type.name();
        this.user = user.getUserID();
        this.time = format.format(new Date());
    }

    public FSLock(Properties p) {
        if (!p.isEmpty()) {
            this.type = p.getProperty(LOCK_TYPE);
            this.user = p.getProperty(LOCK_USER);
            this.time = p.getProperty(LOCK_TIME);
        }
    }

    public Properties toProperties() {
        Properties p = new Properties();
        p.setProperty(LOCK_TIME, time);
        p.setProperty(LOCK_TYPE, type);
        p.setProperty(LOCK_USER, user);
        return p;
    }


    public void setUser(S3User user) {
        this.user = user.getUserID();
    }

    public void updateTime() {
        this.time = format.format(new Date());
    }

    public boolean isObsolete() {
        try {
            return format.parse(time).getTime() < System.currentTimeMillis() - 3600 * 1000;
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isSharingAllowed(LockType requestedLock) {
        return requestedLock == LockType.read && type.equals(LockType.read.name()) || isObsolete();
    }

    public boolean isUnlockAllowed(S3User requestingUser) {
        return user.equals(requestingUser.getUserID()) || isObsolete();
    }


}
