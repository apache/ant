/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002-2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "Ant" and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.util.FileUtils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

/**
 * A test class for the 'concat' task, used to concatenate a series of
 * files into a single stream.
 *
 * @author <a href="mailto:derek@activate.net">Derek Slager</a>
 */
public class ConcatTest 
    extends BuildFileTest {

    /**
     * The name of the temporary file.
     */
    private static final String tempFile = "concat.tmp";

    /**
     * The name of the temporary file.
     */
    private static final String tempFile2 = "concat.tmp.2";

    /**
     * Required constructor.
     */
    public ConcatTest(String name) {
        super(name);
    }

    /**
     * Test set up, called by the unit test framework prior to each
     * test.
     */
    public void setUp() {
        configureProject("src/etc/testcases/taskdefs/concat.xml");
    }

    /**
     * Test tear down, called by the unit test framework prior to each
     * test.
     */
    public void tearDown() {
        executeTarget("cleanup");
    }

    /**
     * Expect an exception when insufficient information is provided.
     */
    public void test1() {
        expectBuildException("test1", "Insufficient information.");
    }

    /**
     * Expect an exception when the destination file is invalid.
     */
    public void test2() {
        expectBuildException("test2", "Invalid destination file.");
    }

    /**
     * Cats the string 'Hello, World!' to a temporary file.
     */
    public void test3() {

        File file = new File(getProjectDir(), tempFile);
        if (file.exists()) {
            file.delete();
        }

        executeTarget("test3");

        assertTrue(file.exists());
    }

    /**
     * Cats the file created in test3 three times.
     */
    public void test4() {
        test3();

        File file = new File(getProjectDir(), tempFile);
        final long origSize = file.length();

        executeTarget("test4");

        File file2 = new File(getProjectDir(), tempFile2);
        final long newSize = file2.length();

        assertEquals(origSize * 3, newSize);
    }

    /**
     * Cats the string 'Hello, World!' to the console.
     */
    public void test5() {
        expectLog("test5", "Hello, World!");
    }

    public void test6() {
        String filename = "src/etc/testcases/taskdefs/thisfiledoesnotexist"
            .replace('/', File.separatorChar);
        expectLogContaining("test6", filename +" does not exist.");
    }

    public void testConcatNoNewline() {
        expectLog("testConcatNoNewline", "ab");
    }

    public void testConcatNoNewlineEncoding() {
        expectLog("testConcatNoNewlineEncoding", "ab");
    }

    public void testPath() {
        test3();

        File file = new File(getProjectDir(), tempFile);
        final long origSize = file.length();

        executeTarget("testPath");

        File file2 = new File(getProjectDir(), tempFile2);
        final long newSize = file2.length();

        assertEquals(origSize, newSize);
        
    }
    public void testAppend() {
        test3();

        File file = new File(getProjectDir(), tempFile);
        final long origSize = file.length();

        executeTarget("testAppend");

        File file2 = new File(getProjectDir(), tempFile2);
        final long newSize = file2.length();

        assertEquals(origSize*2, newSize);
        
    }

    public void testFilter() {
        executeTarget("testfilter");
        assertTrue(getLog().indexOf("REPLACED") > -1);
    }

    public void testNoOverwrite() {
        executeTarget("testnooverwrite");
        File file2 = new File(getProjectDir(), tempFile2);
        long size = file2.length();
        assertEquals(size, 0);
    }

    public void testheaderfooter() {
        test3();
        expectLog("testheaderfooter", "headerHello, World!footer");
    }

    public void testfileheader() {
        test3();
        expectLog("testfileheader", "Hello, World!Hello, World!");
    }

    /**
     * Expect an exception when attempting to cat an file to itself
     */
    public void testsame() {
        expectBuildException("samefile", "output file same as input");
    }

    /**
     * Check if filter inline works
     */
    public void testfilterinline() {
        executeTarget("testfilterinline");
        assertTrue(getLog().indexOf("REPLACED") > -1);
    }

    /**
     * Check if multireader works
     */
    public void testmultireader() {
        executeTarget("testmultireader");
        assertTrue(getLog().indexOf("Bye") > -1);
        assertTrue(getLog().indexOf("Hello") == -1);
    }        
    /**
     * Check if fixlastline works
     */
    public void testfixlastline()
        throws IOException
    {
        expectFileContains(
            "testfixlastline", "concat.line4",
            "end of line" + System.getProperty("line.separator")
            + "This has");
    }        

    /**
     * Check if fixlastline works with eol
     */
    public void testfixlastlineeol()
        throws IOException
    {
        expectFileContains(
            "testfixlastlineeol", "concat.linecr",
            "end of line\rThis has");
    }        

    // ------------------------------------------------------
    //   Helper methods - should be in BuildFileTest
    // -----------------------------------------------------

    private String getFileString(String filename)
        throws IOException
    {
        Reader r = null;
        try {
            r = new FileReader(getProject().resolveFile(filename));
            return  FileUtils.newFileUtils().readFully(r);
        }
        finally {
            try {r.close();} catch (Throwable ignore) {}
        }
        
    }
        
    private String getFileString(String target, String filename)
        throws IOException
    {
        executeTarget(target);
        return getFileString(filename);
    }
    
    private void expectFileContains(
        String target, String filename, String contains)
        throws IOException
    {
        String content = getFileString(target, filename);
        assertTrue(
            "expecting file " + filename + " to contain " +
            contains +
            " but got " + content, content.indexOf(contains) > -1);
    }

    public void testTranscoding() throws IOException {
        executeTarget("testTranscoding");
        FileUtils fileUtils = FileUtils.newFileUtils();
        File f1 = getProject().resolveFile("copy/expected/utf-8");
        File f2 = getProject().resolveFile("concat.utf8");
        assertTrue(fileUtils.contentEquals(f1, f2));
    }
}
