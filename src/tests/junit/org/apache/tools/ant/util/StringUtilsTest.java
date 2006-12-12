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

import java.util.Vector;

import junit.framework.TestCase;

/**
 * Test for StringUtils
 */
public class StringUtilsTest extends TestCase {
    public StringUtilsTest(String s) {
        super(s);
    }

    public void testSplit(){
        final String data = "a,b,,";
        Vector res = StringUtils.split(data, ',');
        assertEquals(4, res.size());
        assertEquals("a", res.elementAt(0));
        assertEquals("b", res.elementAt(1));
        assertEquals("", res.elementAt(2));
        assertEquals("", res.elementAt(3));
    }

    public void testSplitLines(){
        final String data = "a\r\nb\nc\nd\ne";
        Vector res = StringUtils.lineSplit(data);
        assertEquals(5, res.size());
        assertEquals("a\r", res.elementAt(0));
        assertEquals("b", res.elementAt(1));
        assertEquals("c", res.elementAt(2));
        assertEquals("d", res.elementAt(3));
        assertEquals("e", res.elementAt(4));
    }

    public void testReplace() {
        final String data = "abcabcabca";
        String res = StringUtils.replace(data, "a", "");
        assertEquals("bcbcbc", res);
    }

    public void testEndsWithBothEmpty() {
        assertTrue( StringUtils.endsWith( new StringBuffer(), "") );
    }

    public void testEndsWithEmptyString() {
        assertTrue( StringUtils.endsWith( new StringBuffer("12234545"), "") );
    }

    public void testEndsWithShorterString() {
        assertTrue( StringUtils.endsWith( new StringBuffer("12345678"), "78"));
    }

    public void testEndsWithSameString() {
        assertTrue( StringUtils.endsWith( new StringBuffer("123"), "123"));
    }

    public void testEndsWithLongerString() {
        assertFalse( StringUtils.endsWith( new StringBuffer("12"), "1245"));
    }

    public void testEndsWithNoMatch() {
        assertFalse( StringUtils.endsWith( new StringBuffer("12345678"), "789"));
    }

    public void testEndsWithEmptyBuffer() {
        assertFalse( StringUtils.endsWith( new StringBuffer(""), "12345667") );
    }

    public void testEndsWithJDKPerf() {
        StringBuffer buf = getFilledBuffer(1024*300, 'a');
        for (int i = 0; i < 1000; i++) {
            assertTrue(buf.toString().endsWith("aa"));
        }
    }

    public void testEndsWithPerf() {
        StringBuffer buf = getFilledBuffer(1024*300, 'a');
        for (int i = 0; i < 1000; i++) {
            assertTrue(StringUtils.endsWith(buf, "aa"));
        }
    }

    private StringBuffer getFilledBuffer(int size, char ch) {
        StringBuffer buf = new StringBuffer(size);
        for (int i = 0; i < size; i++) { buf.append(ch); };
        return buf;
    }
    
    public void testParseHumanSizes() throws Exception {
    	final long KILOBYTE = 1024;
    	final long MEGABYTE = KILOBYTE * 1024;
    	final long GIGABYTE = MEGABYTE * 1024;
    	final long TERABYTE = GIGABYTE * 1024;
    	final long PETABYTE = TERABYTE * 1024;
    	assertEquals(StringUtils.parseHumanSizes("1K"), KILOBYTE);
    	assertEquals(StringUtils.parseHumanSizes("1M"), MEGABYTE);
    	assertEquals(StringUtils.parseHumanSizes("1G"), GIGABYTE);
    	assertEquals(StringUtils.parseHumanSizes("1T"), TERABYTE);
    	assertEquals(StringUtils.parseHumanSizes("1P"), PETABYTE);
    	assertEquals(StringUtils.parseHumanSizes("1"), 1L);
    }
}
