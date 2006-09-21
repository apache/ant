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

/** JUnit Testcases for TailFilter and HeadFilter
 */
/* I wrote the testcases in one java file because I want also to test the
 * combined behaviour (see end of the class).
*/
public class HeadTailTest extends BuildFileTest {

    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();
    
    public HeadTailTest(String name) {
        super(name);
    }

    public void setUp() {
        configureProject("src/etc/testcases/filters/head-tail.xml");
    }

    public void tearDown() {
        executeTarget("cleanup");
    }

    public void testHead() throws IOException {
        executeTarget("testHead");
        File expected = FILE_UTILS.resolveFile(getProject().getBaseDir(), "expected/head-tail.head.test");
        File result = FILE_UTILS.resolveFile(getProject().getBaseDir(), "result/head-tail.head.test");
        assertTrue("testHead: Result not like expected", FILE_UTILS.contentEquals(expected, result));
    }

    public void testHeadLines() throws IOException {
        executeTarget("testHeadLines");
        File expected = FILE_UTILS.resolveFile(getProject().getBaseDir(), "expected/head-tail.headLines.test");
        File result = FILE_UTILS.resolveFile(getProject().getBaseDir(), "result/head-tail.headLines.test");
        assertTrue("testHeadLines: Result not like expected", FILE_UTILS.contentEquals(expected, result));
    }

    public void testHeadSkip() throws IOException {
        executeTarget("testHeadSkip");
        File expected = FILE_UTILS.resolveFile(getProject().getBaseDir(),"expected/head-tail.headSkip.test");
        File result = FILE_UTILS.resolveFile(getProject().getBaseDir(),"result/head-tail.headSkip.test");
        assertTrue("testHeadSkip: Result not like expected", FILE_UTILS.contentEquals(expected, result));
    }

    public void testHeadLinesSkip() throws IOException {
        executeTarget("testHeadLinesSkip");
        File expected = FILE_UTILS.resolveFile(getProject().getBaseDir(),"expected/head-tail.headLinesSkip.test");
        File result = FILE_UTILS.resolveFile(getProject().getBaseDir(),"result/head-tail.headLinesSkip.test");
        assertTrue("testHeadLinesSkip: Result not like expected", FILE_UTILS.contentEquals(expected, result));
    }

    public void testFilterReaderHeadLinesSkip() throws IOException {
        executeTarget("testFilterReaderHeadLinesSkip");
        File expected = FILE_UTILS.resolveFile(getProject().getBaseDir(),
            "expected/head-tail.headLinesSkip.test");
        File result = FILE_UTILS.resolveFile(getProject().getBaseDir(),
            "result/head-tail.filterReaderHeadLinesSkip.test");
        assertTrue("testFilterReaderHeadLinesSkip: Result not like expected",
                   FILE_UTILS.contentEquals(expected, result));
    }

    public void testTail() throws IOException {
        executeTarget("testTail");
        File expected = FILE_UTILS.resolveFile(getProject().getBaseDir(),"expected/head-tail.tail.test");
        File result = FILE_UTILS.resolveFile(getProject().getBaseDir(),"result/head-tail.tail.test");
        assertTrue("testTail: Result not like expected", FILE_UTILS.contentEquals(expected, result));
    }

    public void testTailLines() throws IOException {
        executeTarget("testTailLines");
        File expected = FILE_UTILS.resolveFile(getProject().getBaseDir(),"expected/head-tail.tailLines.test");
        File result = FILE_UTILS.resolveFile(getProject().getBaseDir(),"result/head-tail.tailLines.test");
        assertTrue("testTailLines: Result not like expected", FILE_UTILS.contentEquals(expected, result));
    }

    public void testTailSkip() throws IOException {
        executeTarget("testTailSkip");
        File expected = FILE_UTILS.resolveFile(getProject().getBaseDir(),"expected/head-tail.tailSkip.test");
        File result = FILE_UTILS.resolveFile(getProject().getBaseDir(),"result/head-tail.tailSkip.test");
        assertTrue("testTailSkip: Result not like expected", FILE_UTILS.contentEquals(expected, result));
    }

    public void testTailLinesSkip() throws IOException {
        executeTarget("testTailLinesSkip");
        File expected = FILE_UTILS.resolveFile(getProject().getBaseDir(),"expected/head-tail.tailLinesSkip.test");
        File result = FILE_UTILS.resolveFile(getProject().getBaseDir(),"result/head-tail.tailLinesSkip.test");
        assertTrue("testTailLinesSkip: Result not like expected", FILE_UTILS.contentEquals(expected, result));
    }

    public void testFilterReaderTailLinesSkip() throws IOException {
        executeTarget("testFilterReaderTailLinesSkip");
        File expected = FILE_UTILS.resolveFile(getProject().getBaseDir(),
            "expected/head-tail.tailLinesSkip.test");
        File result = FILE_UTILS.resolveFile(getProject().getBaseDir(),
            "result/head-tail.filterReaderTailLinesSkip.test");
        assertTrue("testFilterReaderTailLinesSkip: Result not like expected",
                   FILE_UTILS.contentEquals(expected, result));
    }

    public void testHeadTail() throws IOException {
        executeTarget("testHeadTail");
        File expected = FILE_UTILS.resolveFile(getProject().getBaseDir(),"expected/head-tail.headtail.test");
        File result = FILE_UTILS.resolveFile(getProject().getBaseDir(),"result/head-tail.headtail.test");
        assertTrue("testHeadTail: Result not like expected", FILE_UTILS.contentEquals(expected, result));
    }

}
