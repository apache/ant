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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** JUnit Testcases for TailFilter and HeadFilter
 */
/* I wrote the testcases in one java file because I want also to test the
 * combined behaviour (see end of the class).
*/
public class HeadTailTest {

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();
    
    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/filters/head-tail.xml");
    }

    @Test
    public void testHead() throws IOException {
        buildRule.executeTarget("testHead");
        File expected = buildRule.getProject().resolveFile("expected/head-tail.head.test");
        File result = new File(buildRule.getProject().getProperty("output") + "/head-tail.head.test");
        assertEquals("testHead: Result not like expected", FileUtilities.getFileContents(expected), FileUtilities.getFileContents(result));
    }

    @Test
    public void testHeadLines() throws IOException {
        buildRule.executeTarget("testHeadLines");
        File expected = buildRule.getProject().resolveFile("expected/head-tail.headLines.test");
        File result = new File(buildRule.getProject().getProperty("output") + "/head-tail.headLines.test");
        assertEquals("testHeadLines: Result not like expected", FileUtilities.getFileContents(expected), FileUtilities.getFileContents(result));
    }

    @Test
    public void testHeadSkip() throws IOException {
        buildRule.executeTarget("testHeadSkip");
        File expected = buildRule.getProject().resolveFile("expected/head-tail.headSkip.test");
        File result = new File(buildRule.getProject().getProperty("output") + "/head-tail.headSkip.test");
        assertEquals("testHeadSkip: Result not like expected", FileUtilities.getFileContents(expected), FileUtilities.getFileContents(result));
    }

    @Test
    public void testHeadLinesSkip() throws IOException {
        buildRule.executeTarget("testHeadLinesSkip");
        File expected = buildRule.getProject().resolveFile("expected/head-tail.headLinesSkip.test");
        File result = new File(buildRule.getProject().getProperty("output") + "/head-tail.headLinesSkip.test");
        assertEquals("testHeadLinesSkip: Result not like expected", FileUtilities.getFileContents(expected), FileUtilities.getFileContents(result));
    }

    @Test
    public void testFilterReaderHeadLinesSkip() throws IOException {
        buildRule.executeTarget("testFilterReaderHeadLinesSkip");
        File expected = buildRule.getProject().resolveFile("expected/head-tail.headLinesSkip.test");
        File result = new File(buildRule.getProject().getProperty("output") + "/head-tail.filterReaderHeadLinesSkip.test");
        assertEquals("testFilterReaderHeadLinesSkip: Result not like expected",
                FileUtilities.getFileContents(expected), FileUtilities.getFileContents(result));
    }

    @Test
    public void testTail() throws IOException {
        buildRule.executeTarget("testTail");
        File expected =buildRule.getProject().resolveFile("expected/head-tail.tail.test");
        File result = new File(buildRule.getProject().getProperty("output") + "/head-tail.tail.test");
        assertEquals("testTail: Result not like expected", FileUtilities.getFileContents(expected), FileUtilities.getFileContents(result));
    }

    @Test
    public void testTailLines() throws IOException {
        buildRule.executeTarget("testTailLines");
        File expected = buildRule.getProject().resolveFile("expected/head-tail.tailLines.test");
        File result = new File(buildRule.getProject().getProperty("output") + "/head-tail.tailLines.test");
        assertEquals("testTailLines: Result not like expected", FileUtilities.getFileContents(expected), FileUtilities.getFileContents(result));
    }

    @Test
    public void testTailSkip() throws IOException {
        buildRule.executeTarget("testTailSkip");
        File expected = buildRule.getProject().resolveFile("expected/head-tail.tailSkip.test");
        File result = new File(buildRule.getProject().getProperty("output") + "/head-tail.tailSkip.test");
        assertEquals("testTailSkip: Result not like expected", FileUtilities.getFileContents(expected), FileUtilities.getFileContents(result));
    }

    @Test
    public void testTailLinesSkip() throws IOException {
        buildRule.executeTarget("testTailLinesSkip");
        File expected = buildRule.getProject().resolveFile("expected/head-tail.tailLinesSkip.test");
        File result = new File(buildRule.getProject().getProperty("output") + "/head-tail.tailLinesSkip.test");
        assertEquals("testTailLinesSkip: Result not like expected", FileUtilities.getFileContents(expected), FileUtilities.getFileContents(result));
    }

    @Test
    public void testFilterReaderTailLinesSkip() throws IOException {
        buildRule.executeTarget("testFilterReaderTailLinesSkip");
        File expected = buildRule.getProject().resolveFile("expected/head-tail.tailLinesSkip.test");
        File result = new File(buildRule.getProject().getProperty("output") + "/head-tail.filterReaderTailLinesSkip.test");
        assertEquals("testFilterReaderTailLinesSkip: Result not like expected",
                FileUtilities.getFileContents(expected), FileUtilities.getFileContents(result));
    }

    @Test
    public void testHeadTail() throws IOException {
        buildRule.executeTarget("testHeadTail");
        File expected = buildRule.getProject().resolveFile("expected/head-tail.headtail.test");
        File result = new File(buildRule.getProject().getProperty("output") + "/head-tail.headtail.test");
        assertEquals("testHeadTail: Result not like expected", FileUtilities.getFileContents(expected), FileUtilities.getFileContents(result));
    }

}
