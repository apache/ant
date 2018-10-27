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

package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.FileUtilities;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.apache.tools.ant.AntAssert.assertContains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * A test class for the 'concat' task, used to concatenate a series of
 * files into a single stream.
 *
 */
public class ConcatTest {

    /**
     * The name of the temporary file.
     */
    private static final String tempFile = "concat.tmp";

    /**
     * The name of the temporary file.
     */
    private static final String tempFile2 = "concat.tmp.2";


    @Rule
    public BuildFileRule buildRule = new BuildFileRule();


    /**
     * Test set up, called by the unit test framework prior to each
     * test.
     */
    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/concat.xml");
    }

    /**
     * Test tear down, called by the unit test framework prior to each
     * test.
     */
    @After
    public void tearDown() {
        buildRule.executeTarget("cleanup");
    }

    /**
     * Expect an exception when insufficient information is provided.
     */
    @Test
    public void test1() {
        try {
            buildRule.executeTarget("test1");
            fail("BuildException should have been thrown - Insufficient information");
        } catch (BuildException ex) {
            //TODO assert value
        }

    }

    /**
     * Expect an exception when the destination file is invalid.
     */
    @Test
    public void test2() {
        try {
            buildRule.executeTarget("test2");
            fail("BuildException should have been thrown - Invalid destination file");
        } catch(BuildException ex) {
            //TODO assert value
        }
    }

    /**
     * Cats the string 'Hello, World!' to a temporary file.
     */
    @Test
    public void test3() {

        File file = new File(buildRule.getProject().getBaseDir(), tempFile);
        if (file.exists()) {
            file.delete();
        }

        buildRule.executeTarget("test3");

        assertTrue(file.exists());
    }

    /**
     * Cats the file created in test3 three times.
     */
    @Test
    public void test4() {
        test3();

        File file = new File(buildRule.getProject().getBaseDir(), tempFile);
        final long origSize = file.length();

        buildRule.executeTarget("test4");

        File file2 = new File(buildRule.getProject().getBaseDir(), tempFile2);
        final long newSize = file2.length();

        assertEquals(origSize * 3, newSize);
    }

    /**
     * Cats the string 'Hello, World!' to the console.
     */
    @Test
    public void test5() {
        buildRule.executeTarget("test5");
        assertEquals("Hello, World!", buildRule.getLog());
    }

    @Test
    public void test6() {
        String filename = "src/etc/testcases/taskdefs/thisfiledoesnotexist"
            .replace('/', File.separatorChar);
        buildRule.executeTarget("test6");
        assertContains(filename + " does not exist", buildRule.getLog());
    }

    @Test
    public void testConcatNoNewline() {
        buildRule.executeTarget("testConcatNoNewline");
        assertEquals("ab", buildRule.getLog());
    }

    @Test
    public void testConcatNoNewlineEncoding() {
        buildRule.executeTarget("testConcatNoNewlineEncoding");
        assertEquals("ab", buildRule.getLog());
    }

    @Test
    public void testPath() {
        test3();

        File file = new File(buildRule.getProject().getBaseDir(), tempFile);
        final long origSize = file.length();

        buildRule.executeTarget("testPath");

        File file2 = new File(buildRule.getProject().getBaseDir(), tempFile2);
        final long newSize = file2.length();

        assertEquals(origSize, newSize);

    }

    @Test
    public void testAppend() {
        test3();

        File file = new File(buildRule.getProject().getBaseDir(), tempFile);
        final long origSize = file.length();

        buildRule.executeTarget("testAppend");

        File file2 = new File(buildRule.getProject().getBaseDir(), tempFile2);
        final long newSize = file2.length();

        assertEquals(origSize*2, newSize);

    }

    @Test
    public void testFilter() {
        buildRule.executeTarget("testfilter");
        assertTrue(buildRule.getLog().indexOf("REPLACED") > -1);
    }

    @Test
    public void testNoOverwrite() {
        buildRule.executeTarget("testnooverwrite");
        File file2 = new File(buildRule.getProject().getBaseDir(), tempFile2);
        long size = file2.length();
        assertEquals(size, 0);
    }

    @Test
    public void testOverwrite() {
        buildRule.executeTarget("testoverwrite");
        File file2 = new File(buildRule.getProject().getBaseDir(), tempFile2);
        long size = file2.length();
        assertTrue(size > 0);
    }

    @Test
    public void testheaderfooter() {
        test3();
        buildRule.executeTarget("testheaderfooter");
        assertEquals("headerHello, World!footer", buildRule.getLog());
    }

    @Test
    public void testfileheader() {
        test3();
        buildRule.executeTarget("testfileheader");
        assertEquals("Hello, World!Hello, World!", buildRule.getLog());
    }

    /**
     * Expect an exception when attempting to cat an file to itself
     */
    @Test
    public void testsame() {
        try {
            buildRule.executeTarget("samefile");
            fail("Build exception should have been thrown - output file same as input");
        } catch(BuildException ex) {
            //TODO assert value
        }
    }

    /**
     * Check if filter inline works
     */
    @Test
    public void testfilterinline() {
        buildRule.executeTarget("testfilterinline");
        assertTrue(buildRule.getLog().indexOf("REPLACED") > -1);
    }

    /**
     * Check if multireader works
     */
    @Test
    public void testmultireader() {
        buildRule.executeTarget("testmultireader");
        assertTrue(buildRule.getLog().indexOf("Bye") > -1);
        assertTrue(buildRule.getLog().indexOf("Hello") == -1);
    }
    /**
     * Check if fixlastline works
     */
    @Test
    public void testfixlastline()
        throws IOException
    {
        buildRule.executeTarget("testfixlastline");
        assertContains("end of line" + System.getProperty("line.separator") + "This has",
                FileUtilities.getFileContents(buildRule.getProject(), "concat.line4"));
    }

    /**
     * Check if fixlastline works with eol
     */
    @Test
    public void testfixlastlineeol()
        throws IOException
    {
        buildRule.executeTarget("testfixlastlineeol");
        assertContains("end of line\rThis has", FileUtilities.getFileContents(buildRule.getProject(), "concat.linecr"));
    }


    @Test
    public void testTranscoding() throws IOException {
        buildRule.executeTarget("testTranscoding");
        File f1 = buildRule.getProject().resolveFile("copy/expected/utf-8");
        File f2 = buildRule.getProject().resolveFile("concat.utf8");
        assertEquals(f1.toString() + " differs from " + f2.toString(),
                FileUtilities.getFileContents(f1), FileUtilities.getFileContents(f2));
    }

    // ------------------------------------------------------
    //   Helper methods - should be in a utility class
    // -----------------------------------------------------
    private void expectFileContainsx(
        String target, String filename, String contains)
        throws IOException
    {
        buildRule.executeTarget(target);
        String content = FileUtilities.getFileContents(buildRule.getProject(), filename);
        assertTrue(
            "expecting file " + filename + " to contain " +
            contains +
            " but got " + content, content.indexOf(contains) > -1);
    }


}
