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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import org.apache.tools.ant.BuildFileTest;

/**
 */
public class FilterTest extends BuildFileTest {

    public FilterTest(String name) {
        super(name);
    }

    public void setUp() {
        configureProject("src/etc/testcases/taskdefs/filter.xml");
    }

    public void tearDown() {
        executeTarget("cleanup");
    }

    public void test1() {
        expectBuildException("test1", "required argument missing");
    }

    public void test2() {
        expectBuildException("test2", "required argument missing");
    }

    public void test3() {
        expectBuildException("test3", "required argument missing");
    }

    public void test4() {
        executeTarget("test4");
    }

    public void test5() {
        executeTarget("test5");
        assertEquals("2000",
                     getFilteredFile("5", "filtered.tmp"));
    }


    public void test6() {
        executeTarget("test6");
        assertEquals("2000",
                     getFilteredFile("6", "taskdefs.tmp/filter1.txt"));
    }

    public void test7() {
        executeTarget("test7");
        assertEquals("<%@ include file=\"root/some/include.jsp\"%>",
                     getFilteredFile("7", "filtered.tmp"));
    }

    public void test8() {
        executeTarget("test8");
        assertEquals("<%@ include file=\"root/some/include.jsp\"%>",
                     getFilteredFile("8", "taskdefs.tmp/filter2.txt"));
    }

    public void test9() {
        executeTarget("test9");
        assertEquals("included",
                    getFilteredFile("9", "taskdefs.tmp/filter3.txt"));
    }

    private String getFilteredFile(String testNumber, String filteredFile) {

        String line = null;
        File f = new File(getProjectDir(), filteredFile);
        if (!f.exists()) {
            fail("filter test"+testNumber+" failed");
        } else {
            BufferedReader in = null;
            try {
                in = new BufferedReader(new FileReader(f));
            } catch (FileNotFoundException fnfe) {
                fail("filter test"+testNumber+" failed, filtered file: " + f.toString() + " not found");
            }
            try {
                line = in.readLine();
                in.close();
            } catch (IOException ioe) {
                fail("filter test"+testNumber+" failed.  IOException while reading filtered file: " + ioe);
            }
        }
        f.delete();
        return line;
    }
}
