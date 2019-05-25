/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.tools.tar;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TarRoundTripTest {

    private static final String LONG_NAME
        = "this/path/name/contains/more/than/one/hundred/characters/in/order/"
            + "to/test/the/GNU/long/file/name/capability/round/tripped";

    /**
     * test round-tripping long (GNU) entries
     */
    @Test
    public void testLongRoundTrippingGNU() throws IOException {
        testLongRoundTripping(TarOutputStream.LONGFILE_GNU);
    }

    /**
     * test round-tripping long (POSIX) entries
     */
    @Test
    public void testLongRoundTrippingPOSIX() throws IOException {
        testLongRoundTripping(TarOutputStream.LONGFILE_POSIX);
    }

    private void testLongRoundTripping(int mode) throws IOException {
        TarEntry original = new TarEntry(LONG_NAME);
        assertTrue("over 100 chars", LONG_NAME.length() > 100);
        assertEquals("original name", LONG_NAME, original.getName());


        ByteArrayOutputStream buff = new ByteArrayOutputStream();
        TarOutputStream tos = new TarOutputStream(buff);
        tos.setLongFileMode(mode);
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
