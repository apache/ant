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
        executeTarget("cleanup");
    }

    public void testFilterReaderNoArgs() throws IOException {
        executeTarget("testFilterReaderNoArgs");
        File expected = getProject().resolveFile("input/concatfilter.test");
        File result = getProject().resolveFile("result/concat.FilterReaderNoArgs.test");
        assertTrue("testFilterReaderNoArgs: Result not like expected", fu.contentEquals(expected, result));
    }

    public void testFilterReaderBefore() {
        doTest("testFilterReaderPrepend", FILE_PREPEND_WITH, FILE_APPEND);
    }

    public void testFilterReaderAfter() {
        doTest("testFilterReaderAppend", FILE_PREPEND, FILE_APPEND_WITH);
    }

    public void testFilterReaderBeforeAfter() {
        doTest("testFilterReaderPrependAppend", FILE_PREPEND_WITH, FILE_APPEND_WITH);
    }

    public void testConcatFilter() {
        doTest("testConcatFilter", FILE_PREPEND, FILE_APPEND);
    }

    public void testConcatFilterBefore() {
        doTest("testConcatFilterPrepend", FILE_PREPEND_WITH, FILE_APPEND);
    }

    public void testConcatFilterAfter() {
        doTest("testConcatFilterAppend", FILE_PREPEND, FILE_APPEND_WITH);
    }

    public void testConcatFilterBeforeAfter() {
        doTest("testConcatFilterPrependAppend", FILE_PREPEND_WITH, FILE_APPEND_WITH);
    }


    /**
     * Executes a target and checks the beginning and the ending of a file.
     * The filename depends on the target name: target name <i>testHelloWorld</i>
     * will search for a file <i>result/concat.HelloWorld.test</i>.
     * @param target The target to invoke
     * @param expectedStart The string which should be at the beginning of the file
     * @param expectedEnd The string which should be at the end of the file
     */
    protected void doTest(String target, String expectedStart, String expectedEnd) {
        executeTarget(target);
        String resultContent = read("result/concat." + target.substring(4) + ".test");
        assertTrue("First 5 lines differs.", resultContent.startsWith(expectedStart));
        assertTrue("Last 5 lines differs.", resultContent.endsWith(expectedEnd));
    }


    /**
     * Wrapper for FileUtils.readFully().
     * Additionally it resolves the filename according the the projects basedir
     * and closes the used reader.
     * @param filename The name of the file to read
     * @return the content of the file or <i>null</i> if something goes wrong
     */
    protected String read(String filename) {
        String content = null;
        try {
            File file = getProject().resolveFile(filename);
            java.io.FileReader rdr = new java.io.FileReader(file);
            content = fu.readFully(rdr);
            rdr.close();
            rdr = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return content;
    }

}
