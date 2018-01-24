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

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

/**
 * JUnit testcases for org.apache.tools.zip.ZipLong.
 *
 */
public class ZipLongTest {

    /**
     * Test conversion to bytes.
     */
    @Test
    public void testToBytes() {
        ZipLong zl = new ZipLong(0x12345678);
        byte[] result = zl.getBytes();
        assertEquals("length getBytes", 4, result.length);
        assertEquals("first byte getBytes", 0x78, result[0]);
        assertEquals("second byte getBytes", 0x56, result[1]);
        assertEquals("third byte getBytes", 0x34, result[2]);
        assertEquals("fourth byte getBytes", 0x12, result[3]);
    }

    /**
     * Test conversion from bytes.
     */
    @Test
    public void testFromBytes() {
        byte[] val = new byte[] {0x78, 0x56, 0x34, 0x12};
        ZipLong zl = new ZipLong(val);
        assertEquals("value from bytes", 0x12345678, zl.getValue());
    }

    /**
     * Test the contract of the equals method.
     */
    @Test
    public void testEquals() {
        ZipLong zl = new ZipLong(0x12345678);
        ZipLong zl2 = new ZipLong(0x12345678);
        ZipLong zl3 = new ZipLong(0x87654321);

        assertTrue("reflexive", zl.equals(zl));

        assertTrue("works", zl.equals(zl2));
        assertTrue("works, part two", !zl.equals(zl3));

        assertTrue("symmetric", zl2.equals(zl));

        assertTrue("null handling", !zl.equals(null));
        assertTrue("non ZipLong handling", !zl.equals(Integer.valueOf(0x1234)));
    }

    /**
     * Test sign handling.
     */
    @Test
    public void testSign() {
        ZipLong zl = new ZipLong(new byte[] {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF});
        assertEquals(0x00000000FFFFFFFFL, zl.getValue());
    }

    @Test
    public void testClone() {
        ZipLong s1 = new ZipLong(42);
        ZipLong s2 = (ZipLong) s1.clone();
        assertNotSame(s1, s2);
        assertEquals(s1, s2);
        assertEquals(s1.getValue(), s2.getValue());
    }
}
