/*
 * Copyright  2001,2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
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

}
