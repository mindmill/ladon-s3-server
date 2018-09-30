package de.mc.ladon.s3server.auth;

import de.mc.ladon.s3server.common.DelimiterUtil;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DelimiterUtilTest {


    @Test
    public void testDelimiterUtil() {
        assertEquals("hier", DelimiterUtil.getCommonPrefix("test/hier/", "test/", "/"));
        assertEquals("test", DelimiterUtil.getCommonPrefix("test/hier/", "test", "/"));

        assertEquals("zwei", DelimiterUtil.getCommonPrefix("eins/zwei/drei", "eins/", "/"));
        assertEquals("zwei", DelimiterUtil.getCommonPrefix("eins/zwei/drei", "eins/zwei", "/"));




        assertNull( DelimiterUtil.getCommonPrefix("test/hier", "test/", "/"));
        assertNull( DelimiterUtil.getCommonPrefix("/test/hier", "test/", "/"));


    }
}
