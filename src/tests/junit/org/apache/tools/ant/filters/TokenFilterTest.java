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

package org.apache.tools.ant.filters;

import static org.apache.tools.ant.AntAssert.assertContains;
import static org.apache.tools.ant.AntAssert.assertNotContains;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.util.FileUtils;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 */
public class TokenFilterTest {
	
	@Rule
	public BuildFileRule buildRule = new BuildFileRule();

    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();
    
    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/filters/tokenfilter.xml");
        buildRule.executeTarget("setUp");
    }

    /** make sure tokenfilter exists */
    @Test
    public void testTokenfilter() throws IOException {
        buildRule.executeTarget("tokenfilter");
    }

    @Test
    public void testTrimignore() throws IOException {
    	buildRule.executeTarget("trimignore");
    	assertContains("Hello-World", buildRule.getLog());
    }

    @Test
    public void testStringTokenizer() throws IOException {
    	buildRule.executeTarget("stringtokenizer");
        assertContains("#This#is#a#number#of#words#", buildRule.getLog());
    }

    @Test
    public void testUnixLineOutput() throws IOException {
    	buildRule.executeTarget("unixlineoutput");
    	assertContains("\nThis\nis\na\nnumber\nof\nwords\n",
                getFileString(buildRule.getProject().getProperty("output") + "/unixlineoutput"));
    }

    @Test
    public void testDosLineOutput() throws IOException {

        buildRule.executeTarget("doslineoutput");
        assertContains("\r\nThis\r\nis\r\na\r\nnumber\r\nof\r\nwords\r\n",
                getFileString(buildRule.getProject().getProperty("output") + "/doslineoutput"));
    }

    @Test
    public void testFileTokenizer() throws IOException {
    	buildRule.executeTarget("filetokenizer");
        String contents = getFileString(buildRule.getProject().getProperty("output") + "/filetokenizer");
        assertContains("   of words", contents);
        assertNotContains(" This is", contents);
    }

    @Test
    public void testReplaceString() throws IOException {
    	buildRule.executeTarget("replacestring");
    	assertContains("this is the moon",
                getFileString(buildRule.getProject().getProperty("output") + "/replacestring"));
    }

    @Test
    public void testReplaceStrings() throws IOException {
    	buildRule.executeTarget("replacestrings");
    	assertContains("bar bar bar", buildRule.getLog());
    }

    @Test
    public void testContainsString() throws IOException {
    	buildRule.executeTarget("containsstring");
        String contents = getFileString(buildRule.getProject().getProperty("output") + "/containsstring");
        assertContains("this is a line contains foo", contents);
        assertNotContains("this line does not", contents);
    }

    @Test
    public void testReplaceRegex() throws IOException {

    	buildRule.executeTarget("hasregex");
        Assume.assumeTrue("Regex not present",
                getFileString(buildRule.getProject().getProperty("output") + "/replaceregexp").contains("bye world"));

        buildRule.executeTarget("replaceregex");
        String contents = getFileString(buildRule.getProject().getProperty("output") + "/replaceregex");
        assertContains("world world world world", contents);
        assertContains("dog Cat dog", contents);
        assertContains("moon Sun Sun", contents);
        assertContains("found WhiteSpace", contents);
        assertContains("Found digits [1234]", contents);
        assertNotContains("This is a line with digits", contents);
    }

    @Test
    public void testFilterReplaceRegex() throws IOException {
    	buildRule.executeTarget("hasregex");
        Assume.assumeTrue("Regex not present",
                getFileString(buildRule.getProject().getProperty("output") + "/replaceregexp").contains("bye world"));

        buildRule.executeTarget("filterreplaceregex");
        String contents = getFileString(buildRule.getProject().getProperty("output") + "/filterreplaceregex");
        assertContains("world world world world", contents);

    }

    @Test
    public void testHandleDollerMatch() throws IOException {
    	buildRule.executeTarget("hasregex");
        Assume.assumeTrue("Regex not present", getFileString(buildRule.getProject().getProperty("output") + "/replaceregexp").contains("bye world"));

        buildRule.executeTarget("dollermatch");
    }

    @Test
    public void testTrimFile() throws IOException {
    	buildRule.executeTarget("trimfile");
        String contents = getFileString(buildRule.getProject().getProperty("output") + "/trimfile");
        assertTrue("no ws at start", contents.startsWith("This is th"));
        assertTrue("no ws at end", contents.endsWith("second line."));
        assertContains("  This is the second", contents);
    }

    @Test
    public void testTrimFileByLine() throws IOException {
    	buildRule.executeTarget("trimfilebyline");
        String contents = getFileString(buildRule.getProject().getProperty("output") + "/trimfilebyline");
        assertFalse("no ws at start", contents.startsWith("This is th"));
        assertFalse("no ws at end", contents.endsWith("second line."));
        assertNotContains("  This is the second", contents);
        assertContains("file.\nThis is the second", contents);
    }

    @Test
    public void testFilterReplaceString() throws IOException {
    	buildRule.executeTarget("filterreplacestring");
        String contents = getFileString(buildRule.getProject().getProperty("output") + "/filterreplacestring");
        assertContains("This is the moon", contents);
    }

    @Test
    public void testFilterReplaceStrings() throws IOException {
    	buildRule.executeTarget("filterreplacestrings");
    	assertContains("bar bar bar", buildRule.getLog());
    }

    @Test
    public void testContainsRegex() throws IOException {
    	buildRule.executeTarget("hasregex");
        Assume.assumeTrue("Regex not present", getFileString(buildRule.getProject().getProperty("output") + "/replaceregexp").contains("bye world"));

        //expectFileContains(buildRule.getProject().getProperty("output") + "/replaceregexp", "bye world");

        buildRule.executeTarget("containsregex");
        String contents = getFileString(buildRule.getProject().getProperty("output") + "/containsregex");
        assertContains("hello world", contents);
        assertNotContains("this is the moon", contents);
        assertContains("World here", contents);
    }

    @Test
    public void testFilterContainsRegex() throws IOException {
    	buildRule.executeTarget("hasregex");
        Assume.assumeTrue("Regex not present", getFileString(buildRule.getProject().getProperty("output") + "/replaceregexp").contains("bye world"));

        buildRule.executeTarget("filtercontainsregex");
        String contents = getFileString(buildRule.getProject().getProperty("output") + "/filtercontainsregex");
        assertContains("hello world", contents);
        assertNotContains("this is the moon", contents);
        assertContains("World here", contents);
    }

    @Test
    public void testContainsRegex2() throws IOException {
    	buildRule.executeTarget("hasregex");
        Assume.assumeTrue("Regex not present", getFileString(buildRule.getProject().getProperty("output") + "/replaceregexp").contains("bye world"));
        
        buildRule.executeTarget("containsregex2");
        String contents = getFileString(buildRule.getProject().getProperty("output") + "/containsregex2");
        assertContains("void register_bits();", contents);
    }

    @Test
    public void testDeleteCharacters() throws IOException {
    	buildRule.executeTarget("deletecharacters");
        String contents = getFileString(buildRule.getProject().getProperty("output") + "/deletechars");
        assertNotContains("#", contents);
        assertNotContains("*", contents);
        assertContains("This is some ", contents);
    }

    @Test
    public void testScriptFilter() throws IOException {
    	Assume.assumeTrue("Project does not have 'testScriptFilter' target",
                buildRule.getProject().getTargets().contains("testScriptFilter"));
    	buildRule.executeTarget("scriptfilter");
    	assertContains("HELLO WORLD", getFileString(buildRule.getProject().getProperty("output") + "/scriptfilter"));

    }

    @Test
    public void testScriptFilter2() throws IOException {
    	Assume.assumeTrue("Project does not have 'testScriptFilter' target", buildRule.getProject().getTargets().contains("testScriptFilter"));
        buildRule.executeTarget("scriptfilter2");
    	assertContains("HELLO MOON", getFileString(buildRule.getProject().getProperty("output") + "/scriptfilter2"));
    }

    @Test
    public void testCustomTokenFilter() throws IOException {
        buildRule.executeTarget("customtokenfilter");
    	assertContains("Hello World", getFileString(buildRule.getProject().getProperty("output") + "/custom"));
    }

    // ------------------------------------------------------
    //   Helper methods
    // -----------------------------------------------------

    private String getFileString(String filename)
        throws IOException
    {
        Reader r = null;
        try {
            r = new FileReader(FILE_UTILS.resolveFile(buildRule.getProject().getBaseDir(),filename));
            return  FileUtils.readFully(r);
        }
        finally {
            FileUtils.close(r);
        }
    }


    public static class Capitalize
        implements TokenFilter.Filter
    {
        public String filter(String token) {
            if (token.length() == 0)
                return token;
            return token.substring(0, 1).toUpperCase() +
                token.substring(1);
        }
    }

}
