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

package org.apache.tools.zip;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotSame;

/**
 * JUnit 4 testcases for org.apache.tools.zip.ZipShort.
 */
public class ZipShortTest {

    /**
     * Test conversion to bytes.
     */
    @Test
    public void testToBytes() {
        ZipShort zs = new ZipShort(0x1234);
        byte[] result = zs.getBytes();
        assertEquals("length getBytes", 2, result.length);
        assertEquals("first byte getBytes", 0x34, result[0]);
        assertEquals("second byte getBytes", 0x12, result[1]);
    }

    /**
     * Test conversion from bytes.
     */
    @Test
    public void testFromBytes() {
        byte[] val = new byte[] {0x34, 0x12};
        ZipShort zs = new ZipShort(val);
        assertEquals("value from bytes", 0x1234, zs.getValue());
    }

    /**
     * Test the contract of the equals method.
     */
    @Test
    public void testEquals() {
        ZipShort zs = new ZipShort(0x1234);
        ZipShort zs2 = new ZipShort(0x1234);
        ZipShort zs3 = new ZipShort(0x5678);

        assertEquals("reflexive", zs, zs);

        assertEquals("works", zs, zs2);
        assertNotEquals("works, part two", zs, zs3);

        assertEquals("symmetric", zs2, zs);

        assertNotEquals("null handling", null, zs);
        assertNotEquals("non ZipShort handling", 0x1234, zs);
    }

    /**
     * Test sign handling.
     */
    @Test
    public void testSign() {
        ZipShort zs = new ZipShort(new byte[] {(byte) 0xFF, (byte) 0xFF});
        assertEquals(0x0000FFFF, zs.getValue());
    }

    @Test
    public void testClone() {
        ZipShort s1 = new ZipShort(42);
        ZipShort s2 = (ZipShort) s1.clone();
        assertNotSame(s1, s2);
        assertEquals(s1, s2);
        assertEquals(s1.getValue(), s2.getValue());
    }
}
