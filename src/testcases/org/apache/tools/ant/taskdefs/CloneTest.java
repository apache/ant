/*
 * Copyright 2005 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
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

public class CloneTest extends BuildFileTest {

    public CloneTest(String name) {
        super(name);
    }

    public void setUp() {
        configureProject("src/etc/testcases/taskdefs/clone.xml");
    }

    public void testClone1() {
        executeTarget("testClone1");
    }

    public void testClone2() {
        executeTarget("testClone2");
    }

    public void testClone3() {
        executeTarget("testClone3");
    }

    public void testNoClone() {
        expectBuildExceptionContaining("testNoClone",
            "should fail because Object cannot be cloned", "public clone method");
    }

    public void testNoAttr() {
        expectSpecificBuildException("testNoAttr",
            "cloneref attribute not set", "cloneref attribute not set");
    }

    public void testNoRef() {
        expectSpecificBuildException("testNoRef", "reference does not exist",
        "reference \"thisreferencehasnotbeensetinthecurrentproject\" not found");
    }

}
