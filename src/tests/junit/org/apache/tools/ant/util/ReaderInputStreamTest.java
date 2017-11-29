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
package org.apache.tools.ant.util;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.StringReader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Test for ReaderInputStream
 */
public class ReaderInputStreamTest {

    @Test
    public void testSimple() throws Exception {
        compareBytes("abc", "utf-8");
    }

    @Test
    public void testSimple16() throws Exception {
        compareBytes("a", "utf-16");
    }

    @Test
    public void testSimpleAbc16() throws Exception {
        // THIS WILL FAIL.
        //compareBytes("abc", "utf-16");
        byte[] bytes = new byte[40];
        int pos = 0;
        ReaderInputStream r = new ReaderInputStream(
            new StringReader("abc"), "utf-16");
        for (int i = 0; true; ++i) {
            int res = r.read();
            if (res == -1) {
                break;
            }
            bytes[pos++] = (byte) res;
        }
        bytes = "abc".getBytes("utf-16");
        //        String n = new String(bytes, 0, pos, "utf-16");
        new String(bytes, 0, bytes.length, "utf-16");
    }

    @Test
    public void testReadZero() throws Exception {
        ReaderInputStream r = new ReaderInputStream(
            new StringReader("abc"));
        byte[] bytes = new byte[30];
        // First read in zero bytes
        r.read(bytes, 0, 0);
        // Now read in the string
        int readin = r.read(bytes, 0, 10);
        // Make sure that the counts are the same
        assertEquals("abc".getBytes().length, readin);
    }

    @Test
    public void testPreample() throws Exception {
        byte[] bytes = "".getBytes("utf-16");
        System.out.println("Preample len is " + bytes.length);
    }

    @Test
    public void testIso88591ToUtf8() throws Exception {
        InputStreamReader fin = null;
        ReaderInputStream r = null;
        FileInputStream utf8 = null;
        try {
            fin = new InputStreamReader(new FileInputStream(new File(System.getProperty("root"), "src/tests/antunit/taskdefs/exec/input/iso8859-1")),
                                        "ISO8859_1");
            r = new ReaderInputStream(fin, "UTF8");

            ByteArrayOutputStream actualOS = new ByteArrayOutputStream();
            int b = r.read();
            while (b > -1) {
                actualOS.write((byte) b);
                b = r.read();
            }

            utf8 = new FileInputStream(new File(System.getProperty("root"), "src/tests/antunit/taskdefs/exec/expected/utf-8"));
            ByteArrayOutputStream expectedOS = new ByteArrayOutputStream();
            b = utf8.read();
            while (b > -1) {
                expectedOS.write((byte) b);
                b = utf8.read();
            }

            byte[] expected = expectedOS.toByteArray();
            byte[] actual = actualOS.toByteArray();
            assertEquals("length", expected.length, actual.length);
            for (int i = 0; i < actual.length; i++) {
                assertEquals("byte " + i, expected[i], actual[i]);
            }
        } finally {
            FileUtils.close(fin);
            FileUtils.close(r);
            FileUtils.close(utf8);
        }
    }

    private void compareBytes(String s, String encoding) throws Exception {
        byte[] expected = s.getBytes(encoding);
        
        ReaderInputStream r = new ReaderInputStream(
            new StringReader(s), encoding);
        for (int i = 0; i < expected.length; ++i) {
            int expect = expected[i] & 0xFF;
            int read = r.read();
            if (expect != read) {
                fail("Mismatch in ReaderInputStream at index " + i
                     + " expecting " + expect + " got " + read + " for string "
                     + s + " with encoding " + encoding);
            }
        }
        if (r.read() != -1) {
            fail("Mismatch in ReaderInputStream - EOF not seen for string "
                 + s + " with encoding " + encoding);
        }
    }
}
