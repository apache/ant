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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.FileUtilities;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertEquals;

/** JUnit Testcases for TailFilter and HeadFilter
 */
/* I wrote the testcases in one java file because I want also to test the
 * combined behaviour (see end of the class).
*/
@RunWith(Parameterized.class)
public class HeadTailTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> targets() {
        return Arrays.asList(new Object [][] {
                {"head", "head"},
                {"headLines", "headLines"},
                {"headSkip", "headSkip"},
                {"headLinesSkip", "headLinesSkip"},
                {"filterReaderHeadLinesSkip", "headLinesSkip"},
                {"tail", "tail"},
                {"tailSkip", "tailSkip"},
                {"tailLines", "tailLines"},
                {"tailLinesSkip", "tailLinesSkip"},
                {"filterReaderTailLinesSkip", "tailLinesSkip"},
                {"headTail", "headtail"}});
    }

    @Parameterized.Parameter
    public String result;

    @Parameterized.Parameter(1)
    public String input;

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/filters/head-tail.xml");
    }

    @Test
    public void test() throws  IOException {
        buildRule.executeTarget("test" + result.substring(0, 1).toUpperCase()
                + result.substring(1));
        File expected = buildRule.getProject().resolveFile("expected/head-tail."
                + input + ".test");
        File actual = new File(buildRule.getProject().getProperty("output")
                + "/head-tail." +  result + ".test");
        assertEquals(result + ": Result not like expected",
                FileUtilities.getFileContents(expected), FileUtilities.getFileContents(actual));
    }
}
