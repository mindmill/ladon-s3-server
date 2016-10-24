/*
 * Copyright (c) 2016 Mind Consulting UG(haftungsbeschränkt)
 */

package de.mc.ladon.s3server.common;

/**
 * @author Ralf Ulrich on 05.03.16.
 */
public class Validator {

    private Validator() {
    }

    public static boolean isValidBucketName(String name) {
         return !(null == name || name.length() < 3 || name.length() > 63 ) && name.matches("^[a-z0-9[.][_][:][-]]+");
    }

}
