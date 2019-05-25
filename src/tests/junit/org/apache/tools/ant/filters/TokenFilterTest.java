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

package org.apache.tools.ant.filters;

import static org.apache.tools.ant.util.FileUtils.readFully;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import org.apache.tools.ant.BuildFileRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 */
public class TokenFilterTest {

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/filters/tokenfilter.xml");
        buildRule.executeTarget("setUp");
    }

    /** make sure tokenfilter exists */
    @Test
    public void testTokenfilter() {
        buildRule.executeTarget("tokenfilter");
    }

    @Test
    public void testTrimignore() {
        buildRule.executeTarget("trimignore");
        assertThat(buildRule.getLog(), containsString("Hello-World"));
    }

    @Test
    public void testStringTokenizer() {
        buildRule.executeTarget("stringtokenizer");
        assertThat(buildRule.getLog(), containsString("#This#is#a#number#of#words#"));
    }

    @Test
    public void testUnixLineOutput() throws IOException {
        buildRule.executeTarget("unixlineoutput");
        assertThat(getFileString(buildRule.getProject().getProperty("output") + "/unixlineoutput"),
                containsString("\nThis\nis\na\nnumber\nof\nwords\n"));
    }

    @Test
    public void testDosLineOutput() throws IOException {
        buildRule.executeTarget("doslineoutput");
        assertThat(getFileString(buildRule.getProject().getProperty("output") + "/doslineoutput"),
                containsString("\r\nThis\r\nis\r\na\r\nnumber\r\nof\r\nwords\r\n"));
    }

    @Test
    public void testFileTokenizer() throws IOException {
        buildRule.executeTarget("filetokenizer");
        assertThat(getFileString(buildRule.getProject().getProperty("output") + "/filetokenizer"),
                both(containsString("   of words")).and(not(containsString(" This is"))));
    }

    @Test
    public void testReplaceString() throws IOException {
        buildRule.executeTarget("replacestring");
        assertThat(getFileString(buildRule.getProject().getProperty("output") + "/replacestring"),
                containsString("this is the moon"));
    }

    @Test
    public void testReplaceStrings() {
        buildRule.executeTarget("replacestrings");
        assertThat(buildRule.getLog(), containsString("bar bar bar"));
    }

    @Test
    public void testContainsString() throws IOException {
        buildRule.executeTarget("containsstring");
        assertThat(getFileString(buildRule.getProject().getProperty("output") + "/containsstring"),
                both(containsString("this is a line contains foo"))
                        .and(not(containsString("this line does not"))));
    }

    @Test
    public void testReplaceRegex() throws IOException {

        buildRule.executeTarget("hasregex");
        assumeThat("Regex not present",
                getFileString(buildRule.getProject().getProperty("output") + "/replaceregexp"),
                containsString("bye world"));

        buildRule.executeTarget("replaceregex");
        String contents = getFileString(buildRule.getProject().getProperty("output") + "/replaceregex");
        assertThat(contents, containsString("world world world world"));
        assertThat(contents, containsString("dog Cat dog"));
        assertThat(contents, containsString("moon Sun Sun"));
        assertThat(contents, containsString("found WhiteSpace"));
        assertThat(contents, containsString("Found digits [1234]"));
        assertThat(contents, not(containsString("This is a line with digits")));
    }

    @Test
    public void testFilterReplaceRegex() throws IOException {
        buildRule.executeTarget("hasregex");
        assumeThat("Regex not present",
                getFileString(buildRule.getProject().getProperty("output") + "/replaceregexp"),
                containsString("bye world"));

        buildRule.executeTarget("filterreplaceregex");
        assertThat(getFileString(buildRule.getProject().getProperty("output") + "/filterreplaceregex"),
                containsString("world world world world"));

    }

    @Test
    public void testHandleDollerMatch() throws IOException {
        buildRule.executeTarget("hasregex");
        assumeThat("Regex not present",
                getFileString(buildRule.getProject().getProperty("output") + "/replaceregexp"),
                containsString("bye world"));

        buildRule.executeTarget("dollermatch");
    }

    @Test
    public void testTrimFile() throws IOException {
        buildRule.executeTarget("trimfile");
        String contents = getFileString(buildRule.getProject().getProperty("output") + "/trimfile");
        assertThat("no ws at start", contents, startsWith("This is th"));
        assertThat("no ws at end", contents, endsWith("second line."));
        assertThat(contents, containsString("  This is the second"));
    }

    @Test
    public void testTrimFileByLine() throws IOException {
        buildRule.executeTarget("trimfilebyline");
        String contents = getFileString(buildRule.getProject().getProperty("output") + "/trimfilebyline");
        assertThat("no ws at start", contents, not(startsWith("This is th")));
        assertThat("no ws at end", contents, not(endsWith("second line.")));
        assertThat(contents, not(containsString("  This is the second")));
        assertThat(contents, containsString("file.\nThis is the second"));
    }

    @Test
    public void testFilterReplaceString() throws IOException {
        buildRule.executeTarget("filterreplacestring");
        assertThat(getFileString(buildRule.getProject().getProperty("output") + "/filterreplacestring"),
                containsString("This is the moon"));
    }

    @Test
    public void testFilterReplaceStrings() {
        buildRule.executeTarget("filterreplacestrings");
        assertThat(buildRule.getLog(), containsString("bar bar bar"));
    }

    @Test
    public void testContainsRegex() throws IOException {
        buildRule.executeTarget("hasregex");
        assumeThat("Regex not present",
                getFileString(buildRule.getProject().getProperty("output") + "/replaceregexp"),
                containsString("bye world"));

        // assertThat(buildRule.getProject().getProperty("output") + "/replaceregexp",
        // containsString("bye world"));

        buildRule.executeTarget("containsregex");
        String contents = getFileString(buildRule.getProject().getProperty("output") + "/containsregex");
        assertThat(contents, containsString("hello world"));
        assertThat(contents, not(containsString("this is the moon")));
        assertThat(contents, containsString("World here"));
    }

    @Test
    public void testFilterContainsRegex() throws IOException {
        buildRule.executeTarget("hasregex");
        assumeThat("Regex not present",
                getFileString(buildRule.getProject().getProperty("output") + "/replaceregexp"),
                containsString("bye world"));

        buildRule.executeTarget("filtercontainsregex");
        String contents = getFileString(buildRule.getProject().getProperty("output") + "/filtercontainsregex");
        assertThat(contents, containsString("hello world"));
        assertThat(contents, not(containsString("this is the moon")));
        assertThat(contents, containsString("World here"));
    }

    @Test
    public void testContainsRegex2() throws IOException {
        buildRule.executeTarget("hasregex");
        assumeThat("Regex not present",
                getFileString(buildRule.getProject().getProperty("output") + "/replaceregexp"),
                containsString("bye world"));

        buildRule.executeTarget("containsregex2");
        assertThat(getFileString(buildRule.getProject().getProperty("output") + "/containsregex2"),
                containsString("void register_bits();"));
    }

    @Test
    public void testDeleteCharacters() throws IOException {
        buildRule.executeTarget("deletecharacters");
        String contents = getFileString(buildRule.getProject().getProperty("output") + "/deletechars");
        assertThat(contents, not(containsString("#")));
        assertThat(contents, not(containsString("*")));
        assertThat(contents, containsString("This is some "));
    }

    @Test
    public void testScriptFilter() throws IOException {
        assumeThat("Project does not have 'testScriptFilter' target",
                buildRule.getProject().getTargets(), hasKey("testScriptFilter"));
        buildRule.executeTarget("scriptfilter");
        assertThat(getFileString(buildRule.getProject().getProperty("output") + "/scriptfilter"),
                containsString("HELLO WORLD"));

    }

    @Test
    public void testScriptFilter2() throws IOException {
        assumeThat("Project does not have 'testScriptFilter' target",
                buildRule.getProject().getTargets(), hasKey("testScriptFilter"));
        buildRule.executeTarget("scriptfilter2");
        assertThat(getFileString(buildRule.getProject().getProperty("output") + "/scriptfilter2"),
                containsString("HELLO MOON"));
    }

    @Test
    public void testCustomTokenFilter() throws IOException {
        buildRule.executeTarget("customtokenfilter");
        assertThat(getFileString(buildRule.getProject().getProperty("output") + "/custom"),
                containsString("Hello World"));
    }

    // ------------------------------------------------------
    //   Helper methods
    // -----------------------------------------------------

    private String getFileString(String filename) throws IOException {
        try (Reader r = new FileReader(buildRule.getProject().resolveFile(filename))) {
            return readFully(r);
        }
    }


    public static class Capitalize implements TokenFilter.Filter {
        public String filter(String token) {
            return token.isEmpty() ? token : token.substring(0, 1).toUpperCase() + token.substring(1);
        }
    }

}
