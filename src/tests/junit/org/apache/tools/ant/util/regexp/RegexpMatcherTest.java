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

package org.apache.tools.ant.util.regexp;

import org.junit.Before;
import org.junit.Test;

import java.util.Vector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests for all implementations of the RegexpMatcher interface.
 *
 */
public abstract class RegexpMatcherTest {

    public static final String UNIX_LINE = "\n";

    private RegexpMatcher reg;

    public abstract RegexpMatcher getImplementation();

    protected final RegexpMatcher getReg() {
        return reg;
    }

    @Before
    public void setUp() {
        reg = getImplementation();
    }

    @Test
    public void testMatches() {
        reg.setPattern("aaaa");
        assertTrue("aaaa should match itself", reg.matches("aaaa"));
        assertTrue("aaaa should match xaaaa", reg.matches("xaaaa"));
        assertFalse("aaaa shouldn't match xaaa", reg.matches("xaaa"));
        reg.setPattern("^aaaa");
        assertFalse("^aaaa shouldn't match xaaaa", reg.matches("xaaaa"));
        assertTrue("^aaaa should match aaaax", reg.matches("aaaax"));
        reg.setPattern("aaaa$");
        assertFalse("aaaa$ shouldn't match aaaax", reg.matches("aaaax"));
        assertTrue("aaaa$ should match xaaaa", reg.matches("xaaaa"));
        reg.setPattern("[0-9]+");
        assertTrue("[0-9]+ should match 123", reg.matches("123"));
        assertTrue("[0-9]+ should match 1", reg.matches("1"));
        assertFalse("[0-9]+ shouldn't match ''", reg.matches(""));
        assertFalse("[0-9]+ shouldn't match a", reg.matches("a"));
        reg.setPattern("[0-9]*");
        assertTrue("[0-9]* should match 123", reg.matches("123"));
        assertTrue("[0-9]* should match 1", reg.matches("1"));
        assertTrue("[0-9]* should match ''", reg.matches(""));
        assertTrue("[0-9]* should match a", reg.matches("a"));
        reg.setPattern("([0-9]+)=\\1");
        assertTrue("([0-9]+)=\\1 should match 1=1", reg.matches("1=1"));
        assertFalse("([0-9]+)=\\1 shouldn't match 1=2", reg.matches("1=2"));
    }

    @Test
    public void testGroups() {
        reg.setPattern("aaaa");
        Vector<String> v = reg.getGroups("xaaaa");
        assertEquals("No parens -> no extra groups", 1, v.size());
        assertEquals("Trivial match with no parens", "aaaa",
                v.elementAt(0));

        reg.setPattern("(aaaa)");
        v = reg.getGroups("xaaaa");
        assertEquals("Trivial match with single paren", 2, v.size());
        assertEquals("Trivial match with single paren, full match", "aaaa",
                v.elementAt(0));
        assertEquals("Trivial match with single paren, matched paren", "aaaa",
                v.elementAt(0));

        reg.setPattern("(a+)b(b+)");
        v = reg.getGroups("xaabb");
        assertEquals(3, v.size());
        assertEquals("aabb", v.elementAt(0));
        assertEquals("aa", v.elementAt(1));
        assertEquals("b", v.elementAt(2));
    }

    @Test
    public void testBugzillaReport14619() {
        reg.setPattern("^(.*)/src/((.*/)*)([a-zA-Z0-9_\\.]+)\\.java$");
        Vector<String> v = reg.getGroups("de/tom/src/Google.java");
        assertEquals(5, v.size());
        assertEquals("de/tom", v.elementAt(1));
        assertEquals("", v.elementAt(2));
        assertEquals("", v.elementAt(3));
        assertEquals("Google", v.elementAt(4));
    }

    @Test
    public void testCaseInsensitiveMatch() {
        reg.setPattern("aaaa");
        assertFalse("aaaa doesn't match AAaa", reg.matches("AAaa"));
        assertTrue("aaaa matches AAaa ignoring case",
                   reg.matches("AAaa", RegexpMatcher.MATCH_CASE_INSENSITIVE));
    }

    // make sure there are no issues concerning line separator interpretation
    // a line separator for regex (perl) is always a unix line (ie \n)

    @Test
    public void testParagraphCharacter() {
        reg.setPattern("end of text$");
        assertFalse("paragraph character", reg.matches("end of text\u2029"));
    }

    @Test
    public void testLineSeparatorCharacter() {
        reg.setPattern("end of text$");
        assertFalse("line-separator character", reg.matches("end of text\u2028"));
    }

    @Test
    public void testNextLineCharacter() {
        reg.setPattern("end of text$");
        assertFalse("next-line character", reg.matches("end of text\u0085"));
    }

    @Test
    public void testStandaloneCR() {
        reg.setPattern("end of text$");
        assertFalse("standalone CR", reg.matches("end of text\r"));
    }

    @Test
    public void testWindowsLineSeparator() {
        reg.setPattern("end of text$");
        assertFalse("Windows line separator", reg.matches("end of text\r\n"));
    }

    @Test
    public void testWindowsLineSeparator2() {
        reg.setPattern("end of text\r$");
        assertTrue("Windows line separator", reg.matches("end of text\r\n"));
    }

    @Test
    public void testUnixLineSeparator() {
        reg.setPattern("end of text$");
        assertTrue("Unix line separator", reg.matches("end of text\n"));
    }

    @Test
    public void testMultiVersusSingleLine() {
        String text = "Line1" + UNIX_LINE +
                "starttest Line2" + UNIX_LINE +
                "Line3 endtest" + UNIX_LINE +
                "Line4" + UNIX_LINE;

        doStartTest1(text);
        doStartTest2(text);
        doEndTest1(text);
        doEndTest2(text);
    }

    protected void doStartTest1(String text) {
        reg.setPattern("^starttest");
        assertFalse("^starttest in default mode", reg.matches(text));
        assertFalse("^starttest in single line mode",
                reg.matches(text, RegexpMatcher.MATCH_SINGLELINE));
        assertTrue("^starttest in multi line mode",
               reg.matches(text, RegexpMatcher.MATCH_MULTILINE));
    }

    protected void doStartTest2(String text) {
        reg.setPattern("^Line1");
        assertTrue("^Line1 in default mode", reg.matches(text));
        assertTrue("^Line1 in single line mode",
               reg.matches(text, RegexpMatcher.MATCH_SINGLELINE));
        assertTrue("^Line1 in multi line mode",
               reg.matches(text, RegexpMatcher.MATCH_MULTILINE));
    }

    protected void doEndTest1(String text) {
        reg.setPattern("endtest$");
        assertFalse("endtest$ in default mode", reg.matches(text));
        assertFalse("endtest$ in single line mode",
                reg.matches(text, RegexpMatcher.MATCH_SINGLELINE));
        assertTrue("endtest$ in multi line mode",
                reg.matches(text, RegexpMatcher.MATCH_MULTILINE));
    }

    protected void doEndTest2(String text) {
        reg.setPattern("Line4$");
        assertTrue("Line4$ in default mode", reg.matches(text));
        assertTrue("Line4$ in single line mode",
               reg.matches(text, RegexpMatcher.MATCH_SINGLELINE));
        assertTrue("Line4$ in multi line mode",
               reg.matches(text, RegexpMatcher.MATCH_MULTILINE));
    }

}
