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

import junit.framework.AssertionFailedError;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 */
public class FixCrLfTest {

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/fixcrlf/build.xml");
    }

    @Test
    public void test1() {
        buildRule.executeTarget("test1");
    }

    @Test
    public void test2() {
        buildRule.executeTarget("test2");
    }

    @Test
    public void test3() {
        buildRule.executeTarget("test3");
    }

    @Test
    public void test4() {
        buildRule.executeTarget("test4");
    }

    @Test
    public void test5() {
        buildRule.executeTarget("test5");
    }

    @Test
    public void test6() {
        buildRule.executeTarget("test6");
    }

    @Test
    public void test7() {
        buildRule.executeTarget("test7");
    }

    @Test
    public void test8() {
        buildRule.executeTarget("test8");
    }

    @Test
    public void test9() {
        buildRule.executeTarget("test9");
    }

    @Test
    public void testMacLines() {
        buildRule.executeTarget("testMacLines");
    }

    @Test
    public void testNoOverwrite() {
        buildRule.executeTarget("testNoOverwrite");
    }

    @Test
    public void testEncoding() {
        buildRule.executeTarget("testEncoding");
    }

    @Test
    public void testOutputEncoding() {
        buildRule.executeTarget("testOutputEncoding");
    }

    @Test
    public void testLongLines() {
        buildRule.executeTarget("testLongLines");
    }

    @Test
    public void testCrCrLfSequenceUnix() {
        buildRule.executeTarget("testCrCrLfSequence-unix");
    }

    @Test
    public void testCrCrLfSequenceDos() {
        buildRule.executeTarget("testCrCrLfSequence-dos");
    }

    @Test
    public void testCrCrLfSequenceMac() {
        buildRule.executeTarget("testCrCrLfSequence-mac");
    }

    @Test
    public void testFixlastDos() {
        buildRule.executeTarget("testFixlastDos");
    }

    @Test
    public void testFixlastFalseMac() {
        buildRule.executeTarget("testFixlastFalseMac");
    }

    @Test
    public void testFixFile() {
        buildRule.executeTarget("testFixFile");
    }

    @Test
    public void testFixFileExclusive() {
        thrown.expect(BuildException.class);
        thrown.expectMessage(FixCRLF.ERROR_FILE_AND_SRCDIR);
        buildRule.executeTarget("testFixFileExclusive");
    }

    /**
     * Bugzilla Report 20840
     *
     * Will fail with an exception if the parent directories do not
     * get created.
     */
    @Test
    public void testCreateParentDirs() {
        buildRule.executeTarget("createParentDirs");
    }

    @Test
    public void testPreserveLastModified() {
        buildRule.executeTarget("testPreserveLastModified");
    }

    @Test
    public void testFilter1() {
        buildRule.executeTarget("testFilter1");
    }

    @Test
    public void testFilter2() {
        buildRule.executeTarget("testFilter2");
    }

    @Test
    public void testFilter3() {
        buildRule.executeTarget("testFilter3");
    }

    @Test
    public void testFilter4() {
        buildRule.executeTarget("testFilter4");
    }

    @Test
    public void testFilter5() {
        buildRule.executeTarget("testFilter5");
    }

    @Test
    public void testFilter6() {
        buildRule.executeTarget("testFilter6");
    }

    @Test
    public void testFilter7() {
        buildRule.executeTarget("testFilter7");
    }

    @Test
    public void testFilter8() {
        buildRule.executeTarget("testFilter8");
    }

    @Test
    public void testFilter9() {
        buildRule.executeTarget("testFilter9");
    }

    @Test
    public void testCannotDoubleEof() {
        buildRule.executeTarget("testCannotDoubleEof");
    }

    @Test
    public void testTabInLiteralInComment() {
        buildRule.executeTarget("testTabInLiteralInComment");
    }

    // not used, but public so theoretically must remain for BC?
    @Deprecated
    public void assertEqualContent(File expect, File result) throws AssertionFailedError, IOException {
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
