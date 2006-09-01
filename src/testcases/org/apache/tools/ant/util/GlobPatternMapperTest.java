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

import junit.framework.TestCase;

/**
 * Tests for org.apache.tools.ant.util;GlobPatternMapper.
 *
 */
public class GlobPatternMapperTest extends TestCase {

    public GlobPatternMapperTest(String name) {
        super(name);
    }

    public void testNoPatternAtAll() {
        GlobPatternMapper m = new GlobPatternMapper();
        m.setFrom("foobar");
        m.setTo("baz");
        assertNull("Shouldn\'t match foobar", m.mapFileName("plonk"));
        String[] result = m.mapFileName("foobar");
        assertNotNull("Should match foobar", result);
        assertEquals("only one result for foobar", 1, result.length);
        assertEquals("baz", result[0]);
    }

    public void testPostfixOnly() {
        GlobPatternMapper m = new GlobPatternMapper();
        m.setFrom("*foo");
        m.setTo("*plonk");
        assertNull("Shouldn\'t match *foo", m.mapFileName("bar.baz"));
        String[] result = m.mapFileName("bar.foo");
        assertNotNull("Should match *.foo", result);
        assertEquals("only one result for bar.foo", 1, result.length);
        assertEquals("bar.plonk", result[0]);

        // Try a silly case
        m.setTo("foo*");
        result = m.mapFileName("bar.foo");
        assertEquals("foobar.", result[0]);
    }

    public void testPrefixOnly() {
        GlobPatternMapper m = new GlobPatternMapper();
        m.setFrom("foo*");
        m.setTo("plonk*");
        assertNull("Shouldn\'t match foo*", m.mapFileName("bar.baz"));
        String[] result = m.mapFileName("foo.bar");
        assertNotNull("Should match foo*", result);
        assertEquals("only one result for foo.bar", 1, result.length);
        assertEquals("plonk.bar", result[0]);

        // Try a silly case
        m.setTo("*foo");
        result = m.mapFileName("foo.bar");
        assertEquals(".barfoo", result[0]);
    }

    public void testPreAndPostfix() {
        GlobPatternMapper m = new GlobPatternMapper();
        m.setFrom("foo*bar");
        m.setTo("plonk*pling");
        assertNull("Shouldn\'t match foo*bar", m.mapFileName("bar.baz"));
        String[] result = m.mapFileName("foo.bar");
        assertNotNull("Should match foo*bar", result);
        assertEquals("only one result for foo.bar", 1, result.length);
        assertEquals("plonk.pling", result[0]);

        // and a little longer
        result = m.mapFileName("foo.baz.bar");
        assertNotNull("Should match foo*bar", result);
        assertEquals("only one result for foo.baz.bar", 1, result.length);
        assertEquals("plonk.baz.pling", result[0]);

        // and a little shorter
        result = m.mapFileName("foobar");
        assertNotNull("Should match foo*bar", result);
        assertEquals("only one result for foobar", 1, result.length);
        assertEquals("plonkpling", result[0]);
    }
}
