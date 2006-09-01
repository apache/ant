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

import org.apache.tools.ant.BuildFileTest;

import java.io.*;

import junit.framework.AssertionFailedError;

/**
 */
public class ReplaceTest extends BuildFileTest {

    public ReplaceTest(String name) {
        super(name);
    }

    public void setUp() {
        configureProject("src/etc/testcases/taskdefs/replace.xml");
    }

    public void test1() {
        expectBuildException("test1", "required argument not specified");
    }

    public void test2() {
        expectBuildException("test2", "required argument not specified");
    }

    public void test3() {
        expectBuildException("test3", "required argument not specified");
    }

    public void test4() {
        expectBuildException("test4", "empty token not allowed");
    }

    public void test5() {
        executeTarget("test5");
    }

    public void test6() {
        expectBuildException("test6", "required argument not specified");
    }

    public void test7() {
        expectBuildException("test7", "empty token not allowed");
    }

    public void test8() {
        executeTarget("test8");
    }

    public void test9() throws IOException{
        executeTarget("test9");
        String tmpdir = project.getProperty("tmp.dir");
        assertEqualContent(new File(tmpdir, "result.txt"),
                    new File(tmpdir, "output.txt"));
    }
    public void tearDown() {
        executeTarget("cleanup");
    }
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
