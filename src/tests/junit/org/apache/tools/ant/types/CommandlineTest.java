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

package org.apache.tools.ant.types;

import org.apache.tools.ant.BuildException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * JUnit 4 testcases for org.apache.tools.ant.CommandLine
 */
public class CommandlineTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testTokenizer() {
        String[] s = Commandline.translateCommandline("1 2 3");
        assertEquals("Simple case", 3, s.length);
        for (int i = 0; i < 3; i++) {
            assertEquals("" + (i + 1), s[i]);
        }

        s = Commandline.translateCommandline("");
        assertEquals("empty string", 0, s.length);

        s = Commandline.translateCommandline(null);
        assertEquals("null", 0, s.length);

        s = Commandline.translateCommandline("1 '2' 3");
        assertEquals("Simple case with single quotes", 3, s.length);
        assertEquals("Single quotes have been stripped", "2", s[1]);

        s = Commandline.translateCommandline("1 \"2\" 3");
        assertEquals("Simple case with double quotes", 3, s.length);
        assertEquals("Double quotes have been stripped", "2", s[1]);

        s = Commandline.translateCommandline("1 \"2 3\" 4");
        assertEquals("Case with double quotes and whitespace", 3, s.length);
        assertEquals("Double quotes stripped, space included", "2 3", s[1]);

        s = Commandline.translateCommandline("1 \"2'3\" 4");
        assertEquals("Case with double quotes around single quote", 3, s.length);
        assertEquals("Double quotes stripped, single quote included", "2'3", s[1]);

        s = Commandline.translateCommandline("1 '2 3' 4");
        assertEquals("Case with single quotes and whitespace", 3, s.length);
        assertEquals("Single quotes stripped, space included", "2 3", s[1]);

        s = Commandline.translateCommandline("1 '2\"3' 4");
        assertEquals("Case with single quotes around double quote", 3, s.length);
        assertEquals("Single quotes stripped, double quote included", "2\"3", s[1]);

        // \ doesn't have a special meaning anymore - this is different from
        // what the Unix sh does but causes a lot of problems on DOS
        // based platforms otherwise
        s = Commandline.translateCommandline("1 2\\ 3 4");
        assertEquals("case with quoted whitespace", 4, s.length);
        assertEquals("backslash included", "2\\", s[1]);

        // "" should become a single empty argument, same for ''
        // PR 5906
        s = Commandline.translateCommandline("\"\" a");
        assertEquals("Doublequoted null arg prepend", 2, s.length);
        assertEquals("Doublequoted null arg prepend", "", s[0]);
        assertEquals("Doublequoted null arg prepend", "a", s[1]);
        s = Commandline.translateCommandline("a \"\"");
        assertEquals("Doublequoted null arg append", 2, s.length);
        assertEquals("Doublequoted null arg append", "a", s[0]);
        assertEquals("Doublequoted null arg append", "", s[1]);
        s = Commandline.translateCommandline("\"\"");
        assertEquals("Doublequoted null arg", 1, s.length);
        assertEquals("Doublequoted null arg", "", s[0]);

        s = Commandline.translateCommandline("'' a");
        assertEquals("Singlequoted null arg prepend", 2, s.length);
        assertEquals("Singlequoted null arg prepend", "", s[0]);
        assertEquals("Singlequoted null arg prepend", "a", s[1]);
        s = Commandline.translateCommandline("a ''");
        assertEquals("Singlequoted null arg append", 2, s.length);
        assertEquals("Singlequoted null arg append", "a", s[0]);
        assertEquals("Singlequoted null arg append", "", s[1]);
        s = Commandline.translateCommandline("''");
        assertEquals("Singlequoted null arg", 1, s.length);
        assertEquals("Singlequoted null arg", "", s[0]);
    }

    /**
     * Expected failure due to unbalanced single quote
     */
    @Test
    public void testTokenizerUnbalancedSingleQuote() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("unbalanced quotes in a 'b c");
        Commandline.translateCommandline("a 'b c");
    }

    /**
     * Expected failure due to unbalanced double quote
     */
    @Test
    public void testTokenizerUnbalancedDoubleQuote() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("unbalanced quotes in a \"b c");
        Commandline.translateCommandline("a \"b c");
    }

    @Test
    public void testToString() {
        assertEquals("", Commandline.toString(new String[0]));
        assertEquals("", Commandline.toString(null));
        assertEquals("1 2 3", Commandline.toString(new String[] {"1", "2", "3"}));
        assertEquals("1 \"2 3\"", Commandline.toString(new String[] {"1", "2 3"}));
        assertEquals("1 \"2'3\"", Commandline.toString(new String[] {"1", "2'3"}));
        assertEquals("1 '2\"3'", Commandline.toString(new String[] {"1", "2\"3"}));
    }

    @Test
    public void testAwkCommand() {
        Commandline c = new Commandline();
        c.setExecutable("awk");
        c.createArgument().setValue("'NR == 2 { print $NF }'");
        String[] s = c.getCommandline();
        assertNotNull(s);
        assertEquals(2, s.length);
        assertEquals("awk", s[0]);
        assertEquals("'NR == 2 { print $NF }'", s[1]);
    }

    @Test
    public void testPrefix() {
        Commandline c = new Commandline();
        Commandline.Argument a = c.createArgument();
        a.setValue("foo");
        a.setPrefix("-f=");
        String[] s = c.getCommandline();
        assertEquals(1, s.length);
        assertEquals("-f=foo", s[0]);
    }

    @Test
    public void testSuffix() {
        Commandline c = new Commandline();
        Commandline.Argument a = c.createArgument();
        a.setValue("foo");
        a.setSuffix(",1");
        String[] s = c.getCommandline();
        assertEquals(1, s.length);
        assertEquals("foo,1", s[0]);
    }

    @Test
    public void testPrefixSuffixLine() {
        Commandline c = new Commandline();
        Commandline.Argument a = c.createArgument();
        a.setLine("one two");
        a.setPrefix("number ");
        a.setSuffix(".");
        String[] s = c.getCommandline();
        assertEquals(2, s.length);
        assertEquals("number one.", s[0]);
        assertEquals("number two.", s[1]);
    }
}
