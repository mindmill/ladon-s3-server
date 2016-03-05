/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.s3server.common;

/**
 * @author Ralf Ulrich on 05.03.16.
 */
public class Validator {

    private Validator() {
    }

    public static boolean isValidBucketName(String name) {
        return !name.contains("..") && !name.contains("/") && !name.contains("\\");
    }

}
