package de.mc.ladon.s3server.common;

import javax.xml.bind.DatatypeConverter;

/**
 * @author Ralf Ulrich
 * on 28.10.16.
 */
public class Encoding {

    private Encoding() {
    }

    public static String toHex(byte[] bytes) {
        return DatatypeConverter.printHexBinary(bytes).toLowerCase();
    }
}
