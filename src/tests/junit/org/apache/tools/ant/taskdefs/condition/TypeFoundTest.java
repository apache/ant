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
package org.apache.tools.ant.taskdefs.condition;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * test the typeexists condition
 */
public class TypeFoundTest {

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/conditions/typefound.xml");
    }

    @Test
    public void testTask() {
        buildRule.executeTarget("testTask");
        assertEquals("true", buildRule.getProject().getProperty("testTask"));
    }

    /**
     * Expected failure due to missing name attribute
     */
    @Test
    public void testUndefined() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("No type specified");
        buildRule.executeTarget("testUndefined");
    }

    @Test
    public void testTaskThatIsntDefined() {
        buildRule.executeTarget("testTaskThatIsntDefined");
        assertNull(buildRule.getProject().getProperty("testTaskThatIsntDefined"));
    }

    @Test
    public void testTaskThatDoesntReallyExist() {
        buildRule.executeTarget("testTaskThatDoesntReallyExist");
        assertNull(buildRule.getProject().getProperty("testTaskThatDoesntReallyExist"));
    }

    @Test
    public void testType() {
        buildRule.executeTarget("testType");
        assertEquals("true", buildRule.getProject().getProperty("testType"));
    }

    @Test
    public void testPreset() {
        buildRule.executeTarget("testPreset");
        assertEquals("true", buildRule.getProject().getProperty("testPreset"));
    }

    @Test
    public void testMacro() {
        buildRule.executeTarget("testMacro");
        assertEquals("true", buildRule.getProject().getProperty("testMacro"));
    }

}
