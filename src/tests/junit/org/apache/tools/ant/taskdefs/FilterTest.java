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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class FilterTest {

    @Rule
    public final BuildFileRule buildRule = new BuildFileRule();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/filter.xml");
    }

    @After
    public void tearDown() {
        buildRule.executeTarget("cleanup");
    }

    @Test(expected = BuildException.class)
    public void test1() {
        buildRule.executeTarget("test1");
        // TODO assert value
    }

    @Test(expected = BuildException.class)
    public void test2() {
        buildRule.executeTarget("test2");
        // TODO assert value
    }

    @Test(expected = BuildException.class)
    public void test3() {
        buildRule.executeTarget("test3");
        // TODO assert value
    }

    @Test
    public void test4() {
        buildRule.executeTarget("test4");
    }

    @Test
    public void test5() {
        buildRule.executeTarget("test5");
        assertEquals("2000",
                     getFilteredFile("5", "filtered.tmp"));
    }


    @Test
    public void test6() {
        buildRule.executeTarget("test6");
        assertEquals("2000",
                     getFilteredFile("6", "taskdefs.tmp/filter1.txt"));
    }

    @Test
    public void test7() {
        buildRule.executeTarget("test7");
        assertEquals("<%@ include file=\"root/some/include.jsp\"%>",
                     getFilteredFile("7", "filtered.tmp"));
    }

    @Test
    public void test8() {
        buildRule.executeTarget("test8");
        assertEquals("<%@ include file=\"root/some/include.jsp\"%>",
                     getFilteredFile("8", "taskdefs.tmp/filter2.txt"));
    }

    @Test
    public void test9() {
        buildRule.executeTarget("test9");
        assertEquals("included",
                    getFilteredFile("9", "taskdefs.tmp/filter3.txt"));
    }

    private String getFilteredFile(String testNumber, String filteredFile) {
        String line = null;
        File f = new File(buildRule.getProject().getBaseDir(), filteredFile);
        assertTrue("filter test" + testNumber + " failed", f.exists());

        try (BufferedReader in = new BufferedReader(new FileReader(f))) {
            line = in.readLine();
        } catch (IOException ioe) {
            fail("filter test" + testNumber
                 + " failed.  IOException while reading filtered file: " + ioe);
        }
        f.delete();
        return line;
    }
}
