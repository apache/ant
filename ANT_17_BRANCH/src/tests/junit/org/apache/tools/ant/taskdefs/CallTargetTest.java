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

import java.util.Vector;

import org.apache.tools.ant.BuildFileTest;

/**
 */
public class CallTargetTest extends BuildFileTest {

    public CallTargetTest(String name) {
        super(name);
    }

    public void setUp() {
        configureProject("src/etc/testcases/taskdefs/calltarget.xml");
    }

    // see bugrep 21724 (references not passing through with antcall)
    public void testInheritRefFileSet() {
        expectLogContaining("testinheritreffileset", "calltarget.xml");
    }

    // see bugrep 21724 (references not passing through with antcall)
    public void testInheritFilterset() {
        project.executeTarget("testinheritreffilterset");
    }

    // see bugrep 11418 (In repeated calls to the same target,
    // params will not be passed in)
    public void testMultiCall() {
        Vector v = new Vector();
        v.add("call-multi");
        v.add("call-multi");
        project.executeTargets(v);
        assertLogContaining("multi is SETmulti is SET");
    }

    public void testBlankTarget() {
        expectBuildException("blank-target", "target name must not be empty");
    }

    public void testMultipleTargets() {
        expectLog("multiple-targets", "tadadctbdbtc");
    }

    public void testMultipleTargets2() {
        expectLog("multiple-targets-2", "dadctb");
    }

    public void tearDown() {
        project.executeTarget("cleanup");
    }
}
