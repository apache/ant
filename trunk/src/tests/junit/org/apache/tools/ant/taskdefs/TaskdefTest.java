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

import org.apache.tools.ant.Project;
import org.apache.tools.ant.BuildFileTest;

/**
 */
public class TaskdefTest extends BuildFileTest {

    public TaskdefTest(String name) {
        super(name);
    }

    public void setUp() {
        configureProject("src/etc/testcases/taskdefs/taskdef.xml");
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
        expectBuildException("test4", "classname specified doesn't exist");
    }

    public void test5() {
        expectBuildException("test5", "No public execute() in " + Project.class);
    }

    public void test5a() {
        executeTarget("test5a");
    }

    public void test6() {
        expectLog("test6", "simpletask: worked");
    }

    public void test7() {
        expectLog("test7", "worked");
    }

    public void testGlobal() {
        expectLog("testGlobal", "worked");
    }

    public void testOverride() {
        executeTarget("testOverride");
        String log = getLog();
        assertTrue("override warning sent",
                   log.indexOf("Trying to override old definition of task copy") > -1);
        assertTrue("task inside target worked",
                   log.indexOf("In target") > -1);
        assertTrue("task inside target worked",
                   log.indexOf("In TaskContainer") > -1);
    }
}
