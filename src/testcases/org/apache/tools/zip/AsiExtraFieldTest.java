/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

package org.apache.tools.zip;

import junit.framework.TestCase;

/**
 * JUnit 3 testcases for org.apache.tools.zip.AsiExtraField.
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a> 
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
        assert("plain file, no link", !a.isLink());
        assert("plain file, no dir", !a.isDirectory());
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
        assert("link, is link", a.isLink());
        assert("link, no dir", !a.isDirectory());
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
        assert("dir, no link", !a.isLink());
        assert("dir, is dir", a.isDirectory());
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
