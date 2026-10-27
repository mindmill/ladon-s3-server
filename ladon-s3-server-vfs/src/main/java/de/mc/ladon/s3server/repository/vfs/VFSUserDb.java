/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.ladon.s3server.repository.vfs;

import de.mc.ladon.s3server.entities.api.S3User;
import de.mc.ladon.s3server.entities.impl.S3UserImpl;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;

/**
 * The user database of the {@link VFSRepository}, stored as a plain text file
 * ({@code userdb.txt}) in the repository root. One line per user:
 *
 * <pre>
 * accessKey TAB secretKey TAB userId TAB userName
 * </pre>
 *
 * On the first access a default (demo) user with the same credentials as the
 * {@code FSRepository} is created, so that existing boot tests and example
 * clients work without any additional setup.
 */
public final class VFSUserDb {

    public static final String DEFAULT_ACCESS_KEY = "rHUYeAk58Ilhg6iUEFtr";
    public static final String DEFAULT_SECRET_KEY = "IVimdW7BIQLq9PLyVpXzZUq8zS4nLfrsoiZSJanu";
    public static final String SYSTEM_DEFAULT_USER = "SYSTEM";

    private static final char TAB = '\t';

    private final Path userFile;

    public VFSUserDb(Path userFile) {
        this.userFile = userFile;
    }

    /**
     * Creates the default user file if it does not exist yet.
     */
    public void initIfMissing() {
        try {
            if (!Files.exists(userFile)) {
                Files.createDirectories(userFile.getParent());
                addUser(SYSTEM_DEFAULT_USER, SYSTEM_DEFAULT_USER, DEFAULT_ACCESS_KEY, DEFAULT_SECRET_KEY);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("could not initialize user db at " + userFile, e);
        }
    }

    /**
     * Finds the user with the given access key.
     *
     * @param accessKey the access key of the request
     * @return the user, or {@code null} if none is known
     */
    public S3User getByAccessKey(String accessKey) {
        if (accessKey == null) {
            return null;
        }
        try {
            initIfMissing();
            for (String line : Files.readAllLines(userFile, StandardCharsets.UTF_8)) {
                if (line.isEmpty()) {
                    continue;
                }
                String[] parts = line.split("\t");
                if (parts.length >= 4 && accessKey.equals(parts[0])) {
                    return new S3UserImpl(parts[2], parts[3], parts[0], parts[1], Collections.emptySet());
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("could not read user db at " + userFile, e);
        }
        return null;
    }

    /**
     * Adds (or replaces) a user in the database.
     *
     * @param userId    the user id
     * @param userName  the user name
     * @param accessKey the access key of the request
     * @param secretKey the secret signing key
     */
    public void addUser(String userId, String userName, String accessKey, String secretKey) {
        try {
            if (Files.exists(userFile)) {
                StringBuilder remaining = new StringBuilder();
                for (String line : Files.readAllLines(userFile, StandardCharsets.UTF_8)) {
                    if (line.isEmpty()) {
                        continue;
                    }
                    String[] parts = line.split("\t");
                    if (parts.length >= 1 && accessKey.equals(parts[0])) {
                        continue;
                    }
                    remaining.append(line).append('\n');
                }
                Files.write(userFile, remaining.toString().getBytes(StandardCharsets.UTF_8));
            } else {
                Files.createDirectories(userFile.getParent());
            }
            Files.write(userFile, (accessKey + TAB + secretKey + TAB + userId + TAB + userName + "\n").getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new UncheckedIOException("could not write user db at " + userFile, e);
        }
    }
}
