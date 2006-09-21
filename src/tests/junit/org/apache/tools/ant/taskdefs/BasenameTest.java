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

/**
 */
public class BasenameTest extends BuildFileTest {

    public BasenameTest(String name) {
        super(name);
    }

    public void setUp() {
        configureProject("src/etc/testcases/taskdefs/basename.xml");
    }

    public void test1() {
        expectBuildException("test1", "required attribute missing");
    }

    public void test2() {
        expectBuildException("test2", "required attribute missing");
    }

    public void test3() {
        expectBuildException("test3", "required attribute missing");
    }

    public void test4() {
        executeTarget("test4");
        String checkprop = project.getProperty("file.w.suf");
        assertEquals("foo.txt", checkprop);
    }

    public void test5() {
        executeTarget("test5");
        String checkprop = project.getProperty("file.wo.suf");
        assertEquals("foo", checkprop);
    }

    public void testMultipleDots() {
        executeTarget("testMultipleDots");
        String checkprop = project.getProperty("file.wo.suf");
        assertEquals("foo.bar", checkprop);
    }

    public void testNoDots() {
        executeTarget("testNoDots");
        String checkprop = project.getProperty("file.wo.suf");
        assertEquals("foo.bar", checkprop);
    }

    public void testValueEqualsSuffixWithDot() {
        executeTarget("testValueEqualsSuffixWithDot");
        String checkprop = project.getProperty("file.wo.suf");
        assertEquals("", checkprop);
    }

    public void testValueEqualsSuffixWithoutDot() {
        executeTarget("testValueEqualsSuffixWithoutDot");
        String checkprop = project.getProperty("file.wo.suf");
        assertEquals("", checkprop);
    }

}
