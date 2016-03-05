/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.repository.impl;

import de.mc.s3server.entities.api.S3User;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Simple file system lock object. This is useful when using multiple server instances on the same file system.
 * One Server won't need it since it prevents concurrent access on resources.
 *
 * @author Ralf Ulrich on 28.02.16.
 */
public class FSLock {


    public static final String LOCK_FILE_EXTENSION = ".lock";
    public static final int LOCK_TIMEOUT = 3600 * 1000;
    public static final String SPLIT_MARKER = ">-//-<";
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

        Path lockPath = Paths.get(metaPath.toString(), objectKey + LOCK_FILE_EXTENSION);
        if (!Files.exists(lockPath)) throw new IOException("lock file not found");
        List<String> lines = Files.readAllLines(lockPath);
        return new FSLock(lines.get(0));

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
        Files.createDirectories(lockPath.getParent());
        Files.write(lockPath, Collections.singletonList(toString()));
    }

    /**
     * Create new FSLock with the given type and user
     *
     * @param type read or write type
     * @param user the current user using this lock
     */
    public FSLock(LockType type, S3User user) {
        this.type = type.name();
        this.user = user.getUserId();
        this.time = format.format(new Date());
    }

    /**
     * Creates a new lock reading the info from a string
     *
     * @param lockString a string representing the lock
     */
    private FSLock(String lockString) {
        if (lockString != null) {
            String[] parts = lockString.split(SPLIT_MARKER);
            this.type = parts[0];
            this.user = parts[1];
            this.time = parts[2];
        }
    }

    /**
     * Simple conversion to a string
     *
     * @return string with the data of this lock
     */
    public String toString() {
        return type + SPLIT_MARKER + user + SPLIT_MARKER + time;
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
        return user.equals(requestingUser.getUserId()) || isObsolete();
    }


}
