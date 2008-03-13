/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.tools.tar;

import java.io.IOException;
import java.io.ByteArrayInputStream;
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
        assertTrue("over 100 chars", LONG_NAME.length() > 100);
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


