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

package org.apache.tools.ant.taskdefs;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.util.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

/**
 */
public class ReplaceTest {

    @Rule
    public final BuildFileRule buildRule = new BuildFileRule();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/replace.xml");
        buildRule.executeTarget("setUp");
    }

    /**
     * Fail: required argument not specified
     */
    @Test(expected = BuildException.class)
    public void test1() {
        buildRule.executeTarget("test1");
        // TODO assert value
    }

    /**
     * Fail: required argument not specified
     */
    @Test(expected = BuildException.class)
    public void test2() {
        buildRule.executeTarget("test2");
        // TODO assert value
    }

    /**
     * Fail: required argument not specified
     */
    @Test(expected = BuildException.class)
    public void test3() {
        buildRule.executeTarget("test3");
        // TODO assert value
    }

    /**
     * Fail: empty token not allowed
     */
    @Test(expected = BuildException.class)
    public void test4() {
        buildRule.executeTarget("test4");
        // TODO assert value
    }

    @Test
    public void test5() {
        buildRule.executeTarget("test5");
    }

    /**
     * Fail: required argument not specified
     */
    @Test(expected = BuildException.class)
    public void test6() {
        buildRule.executeTarget("test6");
        // TODO assert value
    }

    /**
     * Fail: empty token not allowed
     */
    @Test(expected = BuildException.class)
    public void test7() {
        buildRule.executeTarget("test7");
        // TODO assert value
    }

    @Test
    public void test8() {
        buildRule.executeTarget("test8");
    }

    @Test
    public void test9() throws IOException {
        buildRule.executeTarget("test9");
        assertEqualContent(new File(buildRule.getOutputDir(), "result.txt"),
                    new File(buildRule.getOutputDir(), "output.txt"));
    }

    @Test
    public void testNoPreserveLastModified() {
        buildRule.executeTarget("lastModifiedSetup");
        File testFile = new File(buildRule.getOutputDir(), "test.txt");
        assumeTrue("Could not change file modification time",
                testFile.setLastModified(testFile.lastModified() - FileUtils.getFileUtils().getFileTimestampGranularity() * 5));
        long ts1 = testFile.lastModified();
        buildRule.executeTarget("testNoPreserve");
        assertTrue(ts1 < new File(buildRule.getOutputDir(), "test.txt").lastModified());
    }

    @Test
    public void testPreserveLastModified() {
        buildRule.executeTarget("lastModifiedSetup");
        File testFile = new File(buildRule.getOutputDir(), "test.txt");
        assumeTrue("Could not change file modification time",
                testFile.setLastModified(testFile.lastModified() - FileUtils.getFileUtils().getFileTimestampGranularity() * 5));
        long ts1 = testFile.lastModified();
        buildRule.executeTarget("testPreserve");
        assertEquals(ts1, new File(buildRule.getOutputDir(), "test.txt").lastModified());
    }

    public void assertEqualContent(File expect, File result) throws IOException {
        assertTrue("Expected file " + result + " doesn't exist", result.exists());

        try (InputStream inExpect = new BufferedInputStream(Files.newInputStream(expect.toPath()));
             InputStream inResult = new BufferedInputStream(Files.newInputStream(result.toPath()))) {
            int expectedByte = inExpect.read();
            while (expectedByte != -1) {
                assertEquals(expectedByte, inResult.read());
                expectedByte = inExpect.read();
            }
            assertEquals("End of file", -1, inResult.read());
        }
    }
}
