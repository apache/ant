/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2003 The Apache Software Foundation.  All rights
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

package org.apache.tools.ant.filters;

import java.io.File;
import java.io.IOException;

import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.util.FileUtils;

/**
 * JUnit Testcases for ConcatReader
 * @author Jan Matèrne
 */
public class ConcatFilterTest extends BuildFileTest {

    private static FileUtils fu = FileUtils.newFileUtils();
    private static final String lSep = System.getProperty("line.separator");

    private static final String FILE_PREPEND_WITH =
          "this-should-be-the-first-line" + lSep
        + "Line  1" + lSep
        + "Line  2" + lSep
        + "Line  3" + lSep
        + "Line  4" + lSep
    ;

    private static final String FILE_PREPEND =
          "Line  1" + lSep
        + "Line  2" + lSep
        + "Line  3" + lSep
        + "Line  4" + lSep
        + "Line  5" + lSep
    ;

    private static final String FILE_APPEND_WITH =
          "Line 57" + lSep
        + "Line 58" + lSep
        + "Line 59" + lSep
        + "Line 60" + lSep
        + "this-should-be-the-last-line" + lSep
    ;

    private static final String FILE_APPEND =
          "Line 56" + lSep
        + "Line 57" + lSep
        + "Line 58" + lSep
        + "Line 59" + lSep
        + "Line 60" + lSep
    ;


    public ConcatFilterTest(String name) {
        super(name);
    }

    public void setUp() {
        configureProject("src/etc/testcases/filters/concat.xml");
    }

    public void tearDown() {
        // I dont know why - but on my machine I always get a
        // "Unable to delete file ...result\append.txt" (or prepend.txt)
        // from Delete.removeDir(Delete.java:612).
        // Win2000, JDK 1.4.1_02
        // A <sleep> before <delete> doesn´t work. From 10ms to 3000ms.
        // I modified the taskdefs.Delete.DELETE_RETRY_SLEEP_MILLIS
        // from 10 up to 2000 ms, but no success.
        // So I give up - and hope for a suggestion from another one.
        // But this shouldn´t let the testcases fail, so I do the cleanup
        // inside a try-block
        //    Jan
        try {
            executeTarget("cleanup");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void testFilterReaderNoArgs() throws IOException {
        executeTarget("testFilterReaderNoArgs");
        File expected = getProject().resolveFile("input/concatfilter.test");
        File result = getProject().resolveFile("result/concat.filterReaderNoArgs.test");
        assertTrue("testFilterReaderNoArgs: Result not like expected", fu.contentEquals(expected, result));
    }

    public void testFilterReaderBefore() throws IOException {
        executeTarget("testFilterReaderPrepend");
        File resultFile = getProject().resolveFile("result/concat.filterReaderPrepend.test");
        String resultContent = fu.readFully(new java.io.FileReader(resultFile));
        assertTrue("First 5 lines differs.", resultContent.startsWith(FILE_PREPEND_WITH));
        assertTrue("Last 5 lines differs.", resultContent.endsWith(FILE_APPEND));
    }

    public void testFilterReaderAfter() throws IOException {
        executeTarget("testFilterReaderAppend");
        File resultFile = getProject().resolveFile("result/concat.filterReaderAppend.test");
        String resultContent = fu.readFully(new java.io.FileReader(resultFile));
        assertTrue("First 5 lines differs.", resultContent.startsWith(FILE_PREPEND));
        assertTrue("Last 5 lines differs.", resultContent.endsWith(FILE_APPEND_WITH));
    }

    public void testFilterReaderBeforeAfter() throws IOException {
        executeTarget("testFilterReaderPrependAppend");
        File resultFile = getProject().resolveFile("result/concat.filterReaderPrependAppend.test");
        String resultContent = fu.readFully(new java.io.FileReader(resultFile));
        assertTrue("First 5 lines differs.", resultContent.startsWith(FILE_PREPEND_WITH));
        assertTrue("Last 5 lines differs.", resultContent.endsWith(FILE_APPEND_WITH));
    }

    public void testConcatFilter() throws IOException {
        executeTarget("testConcatFilter");
        File resultFile = getProject().resolveFile("result/concat.concatfilter.test");
        String resultContent = fu.readFully(new java.io.FileReader(resultFile));
        assertTrue("First 5 lines differs.", resultContent.startsWith(FILE_PREPEND));
        assertTrue("Last 5 lines differs.", resultContent.endsWith(FILE_APPEND));
    }

    public void testConcatFilterBefore() throws IOException {
        executeTarget("testConcatFilterPrepend");
        File resultFile = getProject().resolveFile("result/concat.concatfilterPrepend.test");
        String resultContent = fu.readFully(new java.io.FileReader(resultFile));
        assertTrue("First 5 lines differs.", resultContent.startsWith(FILE_PREPEND_WITH));
        assertTrue("Last 5 lines differs.", resultContent.endsWith(FILE_APPEND));
    }

    public void testConcatFilterAfter() throws IOException {
        executeTarget("testConcatFilterAppend");
        File resultFile = getProject().resolveFile("result/concat.concatfilterAppend.test");
        String resultContent = fu.readFully(new java.io.FileReader(resultFile));
        assertTrue("First 5 lines differs.", resultContent.startsWith(FILE_PREPEND));
        assertTrue("Last 5 lines differs.", resultContent.endsWith(FILE_APPEND_WITH));
    }

    public void testConcatFilterBeforeAfter() throws IOException {
        executeTarget("testConcatFilterPrependAppend");
        File resultFile = getProject().resolveFile("result/concat.concatfilterPrependAppend.test");
        String resultContent = fu.readFully(new java.io.FileReader(resultFile));
        assertTrue("First 5 lines differs.", resultContent.startsWith(FILE_PREPEND_WITH));
        assertTrue("Last 5 lines differs.", resultContent.endsWith(FILE_APPEND_WITH));
    }

}