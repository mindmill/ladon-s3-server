package de.mc.ladon.s3server.common;

public class DelimiterUtil {
    private DelimiterUtil() {
    }


    public static String getCommonPrefix(String key, String prefix, String delimiter) {
        int pos = prefix.length();
        if (!key.startsWith(prefix)) return null;
        String rest = key.substring(pos);
        int right = rest.indexOf(delimiter);
        if (right == -1) return null;
        int left = prefix.lastIndexOf(delimiter);
        if (left == -1) {
            return key.substring(0, right + pos);
        } else {
            return key.substring(left + 1, right + pos);
        }

    }
}
