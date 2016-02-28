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
    public static final int LOCK_TIMEOUT = 3600 * 1000;
    private String type;
    private String user;
    private String time;
    private SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");


    /**
     * Possible types of the lock
     */
    public enum LockType {
        read, write
    }


    /**
     * Create a lock object from reading an existing properties file.
     *
     * @param metaPath  meta data path of the bucket
     * @param objectKey key of the object this lock is for
     * @return a FSLock object loaded from the file
     * @throws IOException if there is no file or it couldn't be read
     */
    public static FSLock load(Path metaPath, String objectKey) throws IOException {
        Properties p = new Properties();
        Path lockPath = Paths.get(metaPath.toString(), objectKey + LOCK_FILE_EXTENSION);
        if (!Files.exists(lockPath)) throw new IOException("lock file not found");
        try (InputStream in = Files.newInputStream(lockPath)) {
            p.loadFromXML(in);
        }
        return new FSLock(p);

    }

    /**
     * Return the file of this lock;
     *
     * @param metaPath  meta data path of the bucket
     * @param objectKey key of the object to lock
     * @return the path of the lock file
     */
    public static Path getPath(Path metaPath, String objectKey) {
        return Paths.get(metaPath.toString(), objectKey + LOCK_FILE_EXTENSION);
    }

    /**
     * Deletes the lock file if allowed. That is if the user requesting the delete holds the lock
     * or if the lock is obsolete.
     *
     * @param requestingUser user requesting delete of the lock file
     * @param metaPath       meta data path of the bucket
     * @param objectKey      key of the object this lock is for
     * @throws IOException if the file can't be deleted
     */
    public void delete(S3User requestingUser, Path metaPath, String objectKey) throws IOException {
        if (isUnlockAllowed(requestingUser)) {
            Path lockPath = metaPath.resolve(objectKey + LOCK_FILE_EXTENSION);
            Files.delete(lockPath);
        }
    }

    /**
     * Save the FSLock as a properties file in the metaPath path.
     *
     * @param metaPath  meta data path of the bucket
     * @param objectKey the key of the object this lock is for
     * @throws IOException if the properties file can't be written
     */
    public void save(Path metaPath, String objectKey) throws IOException {
        Path lockPath = metaPath.resolve(objectKey + LOCK_FILE_EXTENSION);
        try (OutputStream out = Files.newOutputStream(lockPath)) {
            toProperties().storeToXML(out, null);
        }
    }

    /**
     * Create new FSLock with the given type and user
     *
     * @param type read or write type
     * @param user the current user using this lock
     */
    public FSLock(LockType type, S3User user) {
        this.type = type.name();
        this.user = user.getUserID();
        this.time = format.format(new Date());
    }

    /**
     * Creates a new lock reading the info from a properties object
     *
     * @param p properties object with FSLock data
     */
    private FSLock(Properties p) {
        if (!p.isEmpty()) {
            this.type = p.getProperty(LOCK_TYPE);
            this.user = p.getProperty(LOCK_USER);
            this.time = p.getProperty(LOCK_TIME);
        }
    }

    /**
     * Simple conversion to a properties object
     *
     * @return properties object with the data of this lock
     */
    public Properties toProperties() {
        Properties p = new Properties();
        p.setProperty(LOCK_TIME, time);
        p.setProperty(LOCK_TYPE, type);
        p.setProperty(LOCK_USER, user);
        return p;
    }

    /**
     * Set a new userid holding this lock
     *
     * @param user the user that updates this lock
     */
    public void setUser(S3User user) {
        this.user = user.getUserID();
    }

    /**
     * Refresh the timestamp of the lock
     */
    public void updateTime() {
        this.time = format.format(new Date());
    }

    /**
     * Tests whether the lock is still valid or just an obsolete file
     *
     * @return true if the lock is older than the timeout, false else
     */
    public boolean isObsolete() {
        try {
            return format.parse(time).getTime() < System.currentTimeMillis() - LOCK_TIMEOUT;
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Checks if it is save to delete the lock file
     *
     * @param requestingUser user requesting the unlock
     * @return true if the lock can be deleted from the given user
     */
    public boolean isUnlockAllowed(S3User requestingUser) {
        return user.equals(requestingUser.getUserID()) || isObsolete();
    }


}
