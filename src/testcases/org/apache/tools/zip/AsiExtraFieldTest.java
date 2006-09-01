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

package org.apache.tools.zip;

import junit.framework.TestCase;

/**
 * JUnit 3 testcases for org.apache.tools.zip.AsiExtraField.
 *
 */
public class AsiExtraFieldTest extends TestCase implements UnixStat {
    public AsiExtraFieldTest(String name) {
        super(name);
    }

    /**
     * Test file mode magic.
     */
    public void testModes() {
        AsiExtraField a = new AsiExtraField();
        a.setMode(0123);
        assertEquals("plain file", 0100123, a.getMode());
        a.setDirectory(true);
        assertEquals("directory", 040123, a.getMode());
        a.setLinkedFile("test");
        assertEquals("symbolic link", 0120123, a.getMode());
    }

    /**
     * Test content.
     */
    public void testContent() {
        AsiExtraField a = new AsiExtraField();
        a.setMode(0123);
        a.setUserId(5);
        a.setGroupId(6);
        byte[] b = a.getLocalFileDataData();

        // CRC manually calculated, sorry
        byte[] expect = {(byte)0xC6, 0x02, 0x78, (byte)0xB6, // CRC
                         0123, (byte)0x80,                   // mode
                         0, 0, 0, 0,                         // link length
                         5, 0, 6, 0};                        // uid, gid
        assertEquals("no link", expect.length, b.length);
        for (int i=0; i<expect.length; i++) {
            assertEquals("no link, byte "+i, expect[i], b[i]);
        }

        a.setLinkedFile("test");
        expect = new byte[] {0x75, (byte)0x8E, 0x41, (byte)0xFD, // CRC
                             0123, (byte)0xA0,                   // mode
                             4, 0, 0, 0,                         // link length
                             5, 0, 6, 0,                         // uid, gid
                             (byte)'t', (byte)'e', (byte)'s', (byte)'t'};
        b = a.getLocalFileDataData();
        assertEquals("no link", expect.length, b.length);
        for (int i=0; i<expect.length; i++) {
            assertEquals("no link, byte "+i, expect[i], b[i]);
        }

    }

    /**
     * Test reparse
     */
    public void testReparse() throws Exception {
        // CRC manually calculated, sorry
        byte[] data = {(byte)0xC6, 0x02, 0x78, (byte)0xB6, // CRC
                       0123, (byte)0x80,                   // mode
                       0, 0, 0, 0,                         // link length
                       5, 0, 6, 0};                        // uid, gid
        AsiExtraField a = new AsiExtraField();
        a.parseFromLocalFileData(data, 0, data.length);
        assertEquals("length plain file", data.length,
                     a.getLocalFileDataLength().getValue());
        assertTrue("plain file, no link", !a.isLink());
        assertTrue("plain file, no dir", !a.isDirectory());
        assertEquals("mode plain file", FILE_FLAG | 0123, a.getMode());
        assertEquals("uid plain file", 5, a.getUserId());
        assertEquals("gid plain file", 6, a.getGroupId());

        data = new byte[] {0x75, (byte)0x8E, 0x41, (byte)0xFD, // CRC
                           0123, (byte)0xA0,                   // mode
                           4, 0, 0, 0,                         // link length
                           5, 0, 6, 0,                         // uid, gid
                           (byte)'t', (byte)'e', (byte)'s', (byte)'t'};
        a = new AsiExtraField();
        a.parseFromLocalFileData(data, 0, data.length);
        assertEquals("length link", data.length,
                     a.getLocalFileDataLength().getValue());
        assertTrue("link, is link", a.isLink());
        assertTrue("link, no dir", !a.isDirectory());
        assertEquals("mode link", LINK_FLAG | 0123, a.getMode());
        assertEquals("uid link", 5, a.getUserId());
        assertEquals("gid link", 6, a.getGroupId());
        assertEquals("test", a.getLinkedFile());

        data = new byte[] {(byte)0x8E, 0x01, (byte)0xBF, (byte)0x0E, // CRC
                           0123, (byte)0x40,                         // mode
                           0, 0, 0, 0,                               // link
                           5, 0, 6, 0};                          // uid, gid
        a = new AsiExtraField();
        a.parseFromLocalFileData(data, 0, data.length);
        assertEquals("length dir", data.length,
                     a.getLocalFileDataLength().getValue());
        assertTrue("dir, no link", !a.isLink());
        assertTrue("dir, is dir", a.isDirectory());
        assertEquals("mode dir", DIR_FLAG | 0123, a.getMode());
        assertEquals("uid dir", 5, a.getUserId());
        assertEquals("gid dir", 6, a.getGroupId());

        data = new byte[] {0, 0, 0, 0,                           // bad CRC
                           0123, (byte)0x40,                     // mode
                           0, 0, 0, 0,                           // link
                           5, 0, 6, 0};                          // uid, gid
        a = new AsiExtraField();
        try {
            a.parseFromLocalFileData(data, 0, data.length);
            fail("should raise bad CRC exception");
        } catch (Exception e) {
            assertEquals("bad CRC checksum 0 instead of ebf018e",
                         e.getMessage());
        }
    }
}
