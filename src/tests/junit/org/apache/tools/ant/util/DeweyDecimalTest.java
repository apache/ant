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

package org.apache.tools.ant.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public class DeweyDecimalTest {

    @Test
    public void parse() {
        assertEquals("1.2.3", new DeweyDecimal("1.2.3").toString());
    }

    @Test(expected = NumberFormatException.class)
    public void misparseEmpty() {
        new DeweyDecimal("1..2");
    }

    @Test(expected = NumberFormatException.class)
    public void misparseNonNumeric() {
        new DeweyDecimal("1.2.3-beta-5");
    }

    @Test(expected = NumberFormatException.class)
    public void misparseFinalDot() {
        new DeweyDecimal("1.2.");
    }

    // TODO initial dots, empty string, null, negative numbers, ...

    @Test
    public void testHashCode() {
        assertEquals(new DeweyDecimal("1.2.3").hashCode(), new DeweyDecimal("1.2.3").hashCode());
    }

    @Test
    public void testEquals() {
        assertEquals(new DeweyDecimal("1.2.3"), new DeweyDecimal("1.2.3"));
        assertNotEquals(new DeweyDecimal("1.2.3"), new DeweyDecimal("1.2.4"));
        assertEquals(new DeweyDecimal("1.2.0"), new DeweyDecimal("1.2"));
        assertEquals(new DeweyDecimal("1.2"), new DeweyDecimal("1.2.0"));
    }

    @Test
    public void compareTo() {
        assertTrue(new DeweyDecimal("1.2.3").compareTo(new DeweyDecimal("1.2")) > 0);
        assertTrue(new DeweyDecimal("1.2").compareTo(new DeweyDecimal("1.2.3")) < 0);
        assertEquals(0, new DeweyDecimal("1.2.3").compareTo(new DeweyDecimal("1.2.3")));
        assertTrue(new DeweyDecimal("1.2.3").compareTo(new DeweyDecimal("1.1.4")) > 0);
        assertTrue(new DeweyDecimal("1.2.3").compareTo(new DeweyDecimal("1.2.2.9")) > 0);
        assertEquals(0, new DeweyDecimal("1.2.0").compareTo(new DeweyDecimal("1.2")));
        assertEquals(0, new DeweyDecimal("1.2").compareTo(new DeweyDecimal("1.2.0")));
    }

    @Test
    public void intConstructor() {
        int[] args = {1,2,3};
        assertEquals("1.2.3", new DeweyDecimal(args).toString());
    }

    @Test
    public void intConstructorNegativeValues() {
        int[] args = {-1,-2,-3};
        assertEquals("-1.-2.-3", new DeweyDecimal(args).toString());
    }

    @Test
    public void details() {
         DeweyDecimal dd = new DeweyDecimal("1.2.3");
         assertEquals(3, dd.getSize());
         assertEquals(2, dd.get(1));
    }

    @Test
    public void isGreaterThanOrEqual() {
         DeweyDecimal first = new DeweyDecimal("1.2.3");
         assertTrue(first.isGreaterThanOrEqual(new DeweyDecimal("1")));
         assertTrue(first.isGreaterThanOrEqual(new DeweyDecimal("1.2")));
         assertTrue(first.isGreaterThanOrEqual(new DeweyDecimal("1.2.3")));
         assertTrue(first.isGreaterThanOrEqual(new DeweyDecimal("1.2.3.0")));
         assertFalse(first.isGreaterThanOrEqual(new DeweyDecimal("1.2.4")));
         assertFalse(first.isGreaterThanOrEqual(new DeweyDecimal("1.3")));
         assertFalse(first.isGreaterThanOrEqual(new DeweyDecimal("2")));
    }

    @Test
    public void equals() {
         DeweyDecimal dd = new DeweyDecimal("1.2.3");
         assertFalse(dd.equals("other"));
         assertFalse(dd.equals(null));
         assertTrue(dd.equals(new DeweyDecimal("1.2.3")));
         assertTrue(dd.equals(new DeweyDecimal("1.2.3.0")));
    }

    @Test
    public void isLessThan() {
         DeweyDecimal dd = new DeweyDecimal("1.2.3");
         assertTrue(dd.isLessThan(new DeweyDecimal("2")));
         assertFalse(dd.isLessThan(new DeweyDecimal("1")));
         assertFalse(dd.isLessThan(new DeweyDecimal("1.2.3")));
    }

    @Test
    public void isLessThanOrEqual() {
         DeweyDecimal dd = new DeweyDecimal("1.2.3");
         assertTrue(dd.isLessThanOrEqual(new DeweyDecimal("2")));
         assertFalse(dd.isLessThanOrEqual(new DeweyDecimal("1")));
         assertTrue(dd.isLessThanOrEqual(new DeweyDecimal("1.2.3")));
    }
}
