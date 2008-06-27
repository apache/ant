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

package org.apache.tools.ant.taskdefs.condition;

import org.apache.tools.ant.BuildFileTest;

/**
 * Testcase for the &lt;isfileselected&gt; condition.
 *
 */
public class IsFileSelectedTest extends BuildFileTest {

    public IsFileSelectedTest(String name) {
        super(name);
    }
    public void setUp() {
        configureProject("src/etc/testcases/taskdefs/conditions/isfileselected.xml");
    }

    public void testSimple() {
        executeTarget("simple");
    }
    public void testName() {
        executeTarget("name");
    }
    public void testBaseDir() {
        executeTarget("basedir");
    }
    public void testType() {
        executeTarget("type");
    }
    public void testNotSelector() {
        expectBuildExceptionContaining(
            "not.selector", "checking for use as a selector (not allowed)",
            "fileset doesn't support the nested \"isfile");
    }
}
