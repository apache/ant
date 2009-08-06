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
package org.apache.tools.ant.types;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.BuildFileTest;

public class RedirectorElementTest extends BuildFileTest {

    public RedirectorElementTest(String name) {
        super(name);
    }

    public void setUp() {
        configureProject("src/etc/testcases/types/redirector.xml", Project.MSG_VERBOSE);
    }

    public void test1() {
        executeTarget("test1");
        assertTrue((getProject().getReference("test1")
            instanceof RedirectorElement));
    }

    public void test2() {
        expectBuildException("test2", "You must not specify more than one "
            + "attribute when using refid");
    }

    public void test3() {
        expectBuildException("test3", "You must not specify nested elements "
            + "when using refid");
    }

    public void test4() {
        executeTarget("test4");
    }

    public void testLogInputString() {
        executeTarget("testLogInputString");
        if (super.getLog().indexOf("testLogInputString can-cat") >=0 ) {
            assertDebuglogContaining("Using input string");
        }
    }

    public void testRefid() {
        executeTarget("testRefid");
    }

}
