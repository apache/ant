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

import org.apache.tools.ant.BuildFileTest;

/**
 */
public class FixCrLfTest extends BuildFileTest {

    public FixCrLfTest(String name) {
        super(name);
    }

    public void setUp() {
        configureProject("src/etc/testcases/taskdefs/fixcrlf/build.xml");
    }

    public void tearDown() {
        executeTarget("cleanup");
    }

    public void test1() throws IOException {
        executeTarget("test1");
    }

    public void test2() throws IOException {
        executeTarget("test2");
    }

    public void test3() throws IOException {
        executeTarget("test3");
    }

    public void test4() throws IOException {
        executeTarget("test4");
    }

    public void test5() throws IOException {
        executeTarget("test5");
    }

    public void test6() throws IOException {
        executeTarget("test6");
    }

    public void test7() throws IOException {
        executeTarget("test7");
    }

    public void test8() throws IOException {
        executeTarget("test8");
    }

    public void test9() throws IOException {
        executeTarget("test9");
    }

    public void testMacLines() throws IOException {
        executeTarget("testMacLines");
    }

    public void testNoOverwrite() throws IOException {
        executeTarget("testNoOverwrite");
    }

    public void testEncoding() throws IOException {
        executeTarget("testEncoding");
    }

    public void testOutputEncoding() throws IOException {
        executeTarget("testOutputEncoding");
    }

    public void testLongLines() throws IOException {
        executeTarget("testLongLines");
    }

    public void testCrCrLfSequenceUnix() throws IOException {
        executeTarget("testCrCrLfSequence-unix");
    }

    public void testCrCrLfSequenceDos() throws IOException {
        executeTarget("testCrCrLfSequence-dos");
    }

    public void testCrCrLfSequenceMac() throws IOException {
        executeTarget("testCrCrLfSequence-mac");
    }

    public void testFixlastDos() throws IOException {
        executeTarget("testFixlastDos");
    }

    public void testFixlastFalseMac() throws IOException {
        executeTarget("testFixlastFalseMac");
    }

    public void testFixFile() throws Exception {
        executeTarget("testFixFile");
    }

    public void testFixFileExclusive() throws Exception {
        expectBuildExceptionContaining("testFixFileExclusive",
                FixCRLF.ERROR_FILE_AND_SRCDIR, FixCRLF.ERROR_FILE_AND_SRCDIR);
    }

    /**
     * Bugzilla Report 20840
     *
     * Will fail with an exception if the parent directories do not
     * get created.
     */
    public void testCreateParentDirs() {
        executeTarget("createParentDirs");
    }

    public void testPreserveLastModified() {
        executeTarget("testPreserveLastModified");
    }

    public void testFilter1() {
        executeTarget("testFilter1");
    }

    public void testFilter2() {
        executeTarget("testFilter2");
    }

    public void testFilter3() {
        executeTarget("testFilter3");
    }

    public void testFilter4() {
        executeTarget("testFilter4");
    }

    public void testFilter5() {
        executeTarget("testFilter5");
    }

    public void testFilter6() {
        executeTarget("testFilter6");
    }

    public void testFilter7() {
        executeTarget("testFilter7");
    }

    public void testFilter8() {
        executeTarget("testFilter8");
    }

    public void testFilter9() {
        executeTarget("testFilter9");
    }

    public void testCannotDoubleEof() {
        executeTarget("testCannotDoubleEof");
    }

    public void testTabInLiteralInComment() {
        executeTarget("testTabInLiteralInComment");
    }

    // not used, but public so theoretically must remain for BC?
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
