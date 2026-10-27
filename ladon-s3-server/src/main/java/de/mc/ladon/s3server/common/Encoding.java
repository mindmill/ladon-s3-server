package de.mc.ladon.s3server.common;

import java.util.HexFormat;

/**
 * @author Ralf Ulrich on 28.10.16.
 */
public class Encoding {

    private Encoding() {
    }

    public static String toHex(byte[] bytes) {
        if (bytes == null) {
            return "";
        }
        return HexFormat.of().formatHex(bytes);
    }
}
