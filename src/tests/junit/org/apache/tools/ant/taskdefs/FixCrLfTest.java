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

package org.apache.tools.ant.taskdefs;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import junit.framework.AssertionFailedError;

import org.apache.tools.ant.AntAssert;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 */
public class FixCrLfTest {

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/fixcrlf/build.xml");
    }

    @Test
    public void test1() throws IOException {
        buildRule.executeTarget("test1");
    }

    @Test
    public void test2() throws IOException {
        buildRule.executeTarget("test2");
    }

    @Test
    public void test3() throws IOException {
        buildRule.executeTarget("test3");
    }

    @Test
    public void test4() throws IOException {
        buildRule.executeTarget("test4");
    }

    @Test
    public void test5() throws IOException {
        buildRule.executeTarget("test5");
    }

    @Test
    public void test6() throws IOException {
        buildRule.executeTarget("test6");
    }

    @Test
    public void test7() throws IOException {
        buildRule.executeTarget("test7");
    }

    @Test
    public void test8() throws IOException {
        buildRule.executeTarget("test8");
    }

    @Test
    public void test9() throws IOException {
        buildRule.executeTarget("test9");
    }

    @Test
    public void testMacLines() throws IOException {
        buildRule.executeTarget("testMacLines");
    }

    @Test
    public void testNoOverwrite() throws IOException {
        buildRule.executeTarget("testNoOverwrite");
    }

    @Test
    public void testEncoding() throws IOException {
        buildRule.executeTarget("testEncoding");
    }

    @Test
    public void testOutputEncoding() throws IOException {
        buildRule.executeTarget("testOutputEncoding");
    }

    @Test
    public void testLongLines() throws IOException {
        buildRule.executeTarget("testLongLines");
    }

    @Test
    public void testCrCrLfSequenceUnix() throws IOException {
        buildRule.executeTarget("testCrCrLfSequence-unix");
    }

    @Test
    public void testCrCrLfSequenceDos() throws IOException {
        buildRule.executeTarget("testCrCrLfSequence-dos");
    }

    @Test
    public void testCrCrLfSequenceMac() throws IOException {
        buildRule.executeTarget("testCrCrLfSequence-mac");
    }

    @Test
    public void testFixlastDos() throws IOException {
        buildRule.executeTarget("testFixlastDos");
    }

    @Test
    public void testFixlastFalseMac() throws IOException {
        buildRule.executeTarget("testFixlastFalseMac");
    }

    @Test
    public void testFixFile() throws Exception {
        buildRule.executeTarget("testFixFile");
    }

    @Test
    public void testFixFileExclusive() throws Exception {
        try {
            buildRule.executeTarget("testFixFileExclusive");
            fail(FixCRLF.ERROR_FILE_AND_SRCDIR);
        } catch (BuildException ex) {
            AntAssert.assertContains(FixCRLF.ERROR_FILE_AND_SRCDIR, ex.getMessage());
        }
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
    public void assertEqualContent(File expect, File result)
        throws AssertionFailedError, IOException {
        if (!result.exists()) {
            fail("Expected file "+result+" doesn\'t exist");
        }

        InputStream inExpect = null;
        InputStream inResult = null;
        try {
            inExpect = new BufferedInputStream(new FileInputStream(expect));
            inResult = new BufferedInputStream(new FileInputStream(result));

            int expectedByte = inExpect.read();
            while (expectedByte != -1) {
                assertEquals(expectedByte, inResult.read());
                expectedByte = inExpect.read();
            }
            assertEquals("End of file", -1, inResult.read());
        } finally {
            if (inResult != null) {
                inResult.close();
            }
            if (inExpect != null) {
                inExpect.close();
            }
        }
    }

}
