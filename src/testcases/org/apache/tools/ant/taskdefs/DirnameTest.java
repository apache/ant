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
import org.apache.tools.ant.taskdefs.condition.Os;

/**
 */
public class DirnameTest extends BuildFileTest {

    public DirnameTest(String name) {
        super(name);
    }

    public void setUp() {
        configureProject("src/etc/testcases/taskdefs/dirname.xml");
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
        if (Os.isFamily("netware") || Os.isFamily("dos")) {
            return;
        }
        executeTarget("test4");
        String filesep = System.getProperty("file.separator");
        String expected = filesep + "usr" + filesep + "local";
        String checkprop = project.getProperty("local.dir");
        if (!checkprop.equals(expected)) {
            fail("dirname failed");
        }
    }

    public void test5() {
        executeTarget("test5");
        String expected = project.getProperty("basedir");
        String checkprop = project.getProperty("base.dir");
        if (!checkprop.equals(expected)) {
            fail("dirname failed");
        }
    }

}
