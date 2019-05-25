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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Vector;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 */
public class CallTargetTest {

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/calltarget.xml");
        buildRule.executeTarget("setUp");
    }

    // see bugrep 21724 (references not passing through with antcall)
    @Test
    public void testInheritRefFileSet() {
        buildRule.executeTarget("testinheritreffileset");
        assertThat(buildRule.getLog(), containsString("calltarget.xml"));
    }

    // see bugrep 21724 (references not passing through with antcall)
    @Test
    public void testInheritFilterset() {
        buildRule.getProject().executeTarget("testinheritreffilterset");
    }

    // see bugrep 11418 (In repeated calls to the same target,
    // params will not be passed in)
    @Test
    public void testMultiCall() {
        Vector<String> v = new Vector<>();
        v.add("call-multi");
        v.add("call-multi");
        buildRule.getProject().executeTargets(v);
        assertThat(buildRule.getLog(), containsString("multi is SETmulti is SET"));
    }

    /**
     * Expected failure due to empty target name
     */
    @Test(expected = BuildException.class)
    public void testBlankTarget() {
        buildRule.executeTarget("blank-target");
    }

    @Test
    public void testMultipleTargets() {
        buildRule.executeTarget("multiple-targets");
        assertEquals("tadadctbdbtc", buildRule.getLog());
    }

    @Test
    public void testMultipleTargets2() {
        buildRule.executeTarget("multiple-targets-2");
        assertEquals("dadctb", buildRule.getLog());
    }

}
