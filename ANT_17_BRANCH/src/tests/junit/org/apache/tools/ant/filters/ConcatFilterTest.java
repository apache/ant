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

import java.io.File;
import java.io.IOException;

import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.StringUtils;

/**
 * JUnit Testcases for ConcatReader
 */
public class ConcatFilterTest extends BuildFileTest {

    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();
    private static final String lSep = StringUtils.LINE_SEP;

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
        File expected = FILE_UTILS.resolveFile(getProject().getBaseDir(),"input/concatfilter.test");
        File result = FILE_UTILS.resolveFile(getProject().getBaseDir(), "result/concat.FilterReaderNoArgs.test");
        assertTrue("testFilterReaderNoArgs: Result not like expected", FILE_UTILS.contentEquals(expected, result));
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
            File file = FILE_UTILS.resolveFile(getProject().getBaseDir(), filename);
            java.io.FileReader rdr = new java.io.FileReader(file);
            content = FileUtils.readFully(rdr);
            rdr.close();
            rdr = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return content;
    }

}
