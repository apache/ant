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
 * JUnit 3 testcases for org.apache.tools.zip.ZipShort.
 *
 */
public class ZipShortTest extends TestCase {

    public ZipShortTest(String name) {
        super(name);
    }

    /**
     * Test conversion to bytes.
     */
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
    public void testFromBytes() {
        byte[] val = new byte[] {0x34, 0x12};
        ZipShort zs = new ZipShort(val);
        assertEquals("value from bytes", 0x1234, zs.getValue());
    }

    /**
     * Test the contract of the equals method.
     */
    public void testEquals() {
        ZipShort zs = new ZipShort(0x1234);
        ZipShort zs2 = new ZipShort(0x1234);
        ZipShort zs3 = new ZipShort(0x5678);

        assertTrue("reflexive", zs.equals(zs));

        assertTrue("works", zs.equals(zs2));
        assertTrue("works, part two", !zs.equals(zs3));

        assertTrue("symmetric", zs2.equals(zs));

        assertTrue("null handling", !zs.equals(null));
        assertTrue("non ZipShort handling", !zs.equals(new Integer(0x1234)));
    }

    /**
     * Test sign handling.
     */
    public void testSign() {
        ZipShort zs = new ZipShort(new byte[] {(byte)0xFF, (byte)0xFF});
        assertEquals(0x0000FFFF, zs.getValue());
    }

}
