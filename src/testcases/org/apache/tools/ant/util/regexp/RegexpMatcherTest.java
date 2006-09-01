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

package org.apache.tools.ant.util.regexp;

import java.io.IOException;
import java.util.Vector;

import junit.framework.TestCase;

/**
 * Tests for all implementations of the RegexpMatcher interface.
 *
 */
public abstract class RegexpMatcherTest extends TestCase {

    public final static String UNIX_LINE = "\n";

    private RegexpMatcher reg;

    public abstract RegexpMatcher getImplementation();

    protected final RegexpMatcher getReg() {return reg;}

    public RegexpMatcherTest(String name) {
        super(name);
    }

    public void setUp() {
        reg = getImplementation();
    }

    public void testMatches() {
        reg.setPattern("aaaa");
        assertTrue("aaaa should match itself", reg.matches("aaaa"));
        assertTrue("aaaa should match xaaaa", reg.matches("xaaaa"));
        assertTrue("aaaa shouldn\'t match xaaa", !reg.matches("xaaa"));
        reg.setPattern("^aaaa");
        assertTrue("^aaaa shouldn\'t match xaaaa", !reg.matches("xaaaa"));
        assertTrue("^aaaa should match aaaax", reg.matches("aaaax"));
        reg.setPattern("aaaa$");
        assertTrue("aaaa$ shouldn\'t match aaaax", !reg.matches("aaaax"));
        assertTrue("aaaa$ should match xaaaa", reg.matches("xaaaa"));
        reg.setPattern("[0-9]+");
        assertTrue("[0-9]+ should match 123", reg.matches("123"));
        assertTrue("[0-9]+ should match 1", reg.matches("1"));
        assertTrue("[0-9]+ shouldn\'t match \'\'", !reg.matches(""));
        assertTrue("[0-9]+ shouldn\'t match a", !reg.matches("a"));
        reg.setPattern("[0-9]*");
        assertTrue("[0-9]* should match 123", reg.matches("123"));
        assertTrue("[0-9]* should match 1", reg.matches("1"));
        assertTrue("[0-9]* should match \'\'", reg.matches(""));
        assertTrue("[0-9]* should match a", reg.matches("a"));
        reg.setPattern("([0-9]+)=\\1");
        assertTrue("([0-9]+)=\\1 should match 1=1", reg.matches("1=1"));
        assertTrue("([0-9]+)=\\1 shouldn\'t match 1=2", !reg.matches("1=2"));
    }

    public void testGroups() {
        reg.setPattern("aaaa");
        Vector v = reg.getGroups("xaaaa");
        assertEquals("No parens -> no extra groups", 1, v.size());
        assertEquals("Trivial match with no parens", "aaaa",
                     (String) v.elementAt(0));

        reg.setPattern("(aaaa)");
        v = reg.getGroups("xaaaa");
        assertEquals("Trivial match with single paren", 2, v.size());
        assertEquals("Trivial match with single paren, full match", "aaaa",
                     (String) v.elementAt(0));
        assertEquals("Trivial match with single paren, matched paren", "aaaa",
                     (String) v.elementAt(0));

        reg.setPattern("(a+)b(b+)");
        v = reg.getGroups("xaabb");
        assertEquals(3, v.size());
        assertEquals("aabb", (String) v.elementAt(0));
        assertEquals("aa", (String) v.elementAt(1));
        assertEquals("b", (String) v.elementAt(2));
    }

    public void testBugzillaReport14619() {
        reg.setPattern("^(.*)/src/((.*/)*)([a-zA-Z0-9_\\.]+)\\.java$");
        Vector v = reg.getGroups("de/tom/src/Google.java");
        assertEquals(5, v.size());
        assertEquals("de/tom", v.elementAt(1));
        assertEquals("", v.elementAt(2));
        assertEquals("", v.elementAt(3));
        assertEquals("Google", v.elementAt(4));
    }

    public void testCaseInsensitiveMatch() {
        reg.setPattern("aaaa");
        assertTrue("aaaa doesn't match AAaa", !reg.matches("AAaa"));
        assertTrue("aaaa matches AAaa ignoring case",
                   reg.matches("AAaa", RegexpMatcher.MATCH_CASE_INSENSITIVE));
    }


// make sure there are no issues concerning line separator interpretation
// a line separator for regex (perl) is always a unix line (ie \n)

    public void testParagraphCharacter() throws IOException {
        reg.setPattern("end of text$");
        assertTrue("paragraph character", !reg.matches("end of text\u2029"));
    }

    public void testLineSeparatorCharacter() throws IOException {
        reg.setPattern("end of text$");
        assertTrue("line-separator character", !reg.matches("end of text\u2028"));
    }

    public void testNextLineCharacter() throws IOException {
        reg.setPattern("end of text$");
        assertTrue("next-line character", !reg.matches("end of text\u0085"));
    }

    public void testStandaloneCR() throws IOException {
        reg.setPattern("end of text$");
        assertTrue("standalone CR", !reg.matches("end of text\r"));
    }

    public void testWindowsLineSeparator() throws IOException {
        reg.setPattern("end of text$");
        assertTrue("Windows line separator", !reg.matches("end of text\r\n"));
    }

    public void testWindowsLineSeparator2() throws IOException {
        reg.setPattern("end of text\r$");
        assertTrue("Windows line separator", reg.matches("end of text\r\n"));
    }

    public void testUnixLineSeparator() throws IOException {
        reg.setPattern("end of text$");
        assertTrue("Unix line separator", reg.matches("end of text\n"));
    }


    public void testMultiVersusSingleLine() throws IOException {
        StringBuffer buf = new StringBuffer();
        buf.append("Line1").append(UNIX_LINE);
        buf.append("starttest Line2").append(UNIX_LINE);
        buf.append("Line3 endtest").append(UNIX_LINE);
        buf.append("Line4").append(UNIX_LINE);
        String text = buf.toString();

        doStartTest1(text);
        doStartTest2(text);
        doEndTest1(text);
        doEndTest2(text);
    }

    protected void doStartTest1(String text) {
        reg.setPattern("^starttest");
        assertTrue("^starttest in default mode", !reg.matches(text));
        assertTrue("^starttest in single line mode",
               !reg.matches(text, RegexpMatcher.MATCH_SINGLELINE));
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
        assertTrue("endtest$ in default mode", !reg.matches(text));
        assertTrue("endtest$ in single line mode",
               !reg.matches(text, RegexpMatcher.MATCH_SINGLELINE));
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
