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

import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.FileUtilities;
import org.apache.tools.ant.util.StringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * JUnit Testcases for ConcatReader
 */
public class ConcatFilterTest {

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

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/filters/concat.xml");
    }

    @Test
    public void testFilterReaderNoArgs() throws IOException {
        buildRule.executeTarget("testFilterReaderNoArgs");
        File expected = new File(buildRule.getProject().getProperty("output"), "concatfilter.test");
        File result = new File(buildRule.getProject().getProperty("output"),  "concat.FilterReaderNoArgs.test");
        assertEquals("testFilterReaderNoArgs: Result not like expected", FileUtilities.getFileContents(expected),
                FileUtilities.getFileContents(result));
    }

    @Test
    public void testFilterReaderBefore() throws IOException {
        doTest("testFilterReaderPrepend", FILE_PREPEND_WITH, FILE_APPEND);
    }

    @Test
    public void testFilterReaderAfter() throws IOException {
        doTest("testFilterReaderAppend", FILE_PREPEND, FILE_APPEND_WITH);
    }

    @Test
    public void testFilterReaderBeforeAfter() throws IOException {
        doTest("testFilterReaderPrependAppend", FILE_PREPEND_WITH, FILE_APPEND_WITH);
    }

    @Test
    public void testConcatFilter() throws IOException {
        doTest("testConcatFilter", FILE_PREPEND, FILE_APPEND);
    }

    @Test
    public void testConcatFilterBefore() throws IOException {
        doTest("testConcatFilterPrepend", FILE_PREPEND_WITH, FILE_APPEND);
    }

    @Test
    public void testConcatFilterAfter() throws IOException {
        doTest("testConcatFilterAppend", FILE_PREPEND, FILE_APPEND_WITH);
    }

    @Test
    public void testConcatFilterBeforeAfter() throws IOException {
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
    protected void doTest(String target, String expectedStart, String expectedEnd) throws IOException {
        buildRule.executeTarget(target);
        String resultContent = FileUtilities.getFileContents(
                new File(buildRule.getProject().getProperty("output") + "/concat." + target.substring(4) + ".test"));
        assertTrue("First 5 lines differs.", resultContent.startsWith(expectedStart));
        assertTrue("Last 5 lines differs.", resultContent.endsWith(expectedEnd));
    }


}
