package org.apache.tools.tar;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import junit.framework.TestCase;

public class TarRoundTripTest extends TestCase {

    private static final String LONG_NAME
        = "this/path/name/contains/more/than/one/hundred/characters/in/order/"
            + "to/test/the/GNU/long/file/name/capability/round/tripped";

    public TarRoundTripTest(String name) {
        super(name);
    }

    /**
     * test round-tripping long (GNU) entries
     */
    public void testLongRoundTripping() throws IOException {
        TarEntry original = new TarEntry(LONG_NAME);
        assertEquals("over 100 chars", true, LONG_NAME.length() > 100);
        assertEquals("original name", LONG_NAME, original.getName());


        ByteArrayOutputStream buff = new ByteArrayOutputStream();
        TarOutputStream tos = new TarOutputStream(buff);
        tos.setLongFileMode(TarOutputStream.LONGFILE_GNU);
        tos.putNextEntry(original);
        tos.closeEntry();
        tos.close();

        TarInputStream tis
            = new TarInputStream(new ByteArrayInputStream(buff.toByteArray()));
        TarEntry tripped = tis.getNextEntry();
        assertEquals("round-tripped name", LONG_NAME, tripped.getName());
        assertNull("no more entries", tis.getNextEntry());
        tis.close();
    }
}


