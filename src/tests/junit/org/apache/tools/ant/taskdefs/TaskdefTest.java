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

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 */
public class TaskdefTest {

    @Rule
    public final BuildFileRule buildRule = new BuildFileRule();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/taskdef.xml");
    }

    /**
     * Expected failure due lacking a required argument
     */
    @Test(expected = BuildException.class)
    public void test1() {
        buildRule.executeTarget("test1");
        // TODO Assert exception message
    }

    /**
     * Expected failure due lacking a required argument
     */
    @Test(expected = BuildException.class)
    public void test2() {
        buildRule.executeTarget("test2");
        // TODO Assert exception message
    }

    /**
     * Expected failure due lacking a required argument
     */
    @Test(expected = BuildException.class)
    public void test3() {
        buildRule.executeTarget("test3");
        // TODO Assert exception message
    }

    /**
     * Expected failure due to nonexistent class specified
     */
    @Test(expected = BuildException.class)
    public void test4() {
        buildRule.executeTarget("test4");
        // TODO Assert exception message
    }

    /**
     * Expected failure due to non-public execute() method
     */
    @Test(expected = BuildException.class)
    public void test5() {
        buildRule.executeTarget("test5");
        // TODO Assert exception message
    }

    @Test
    public void test5a() {
        buildRule.executeTarget("test5a");
    }

    @Test
    public void test6() {
        buildRule.executeTarget("test6");
        assertEquals("simpletask: worked", buildRule.getLog());
    }

    @Test
    public void test7() {
        buildRule.executeTarget("test7");
        assertEquals("worked", buildRule.getLog());
    }

    @Test
    public void testGlobal() {
        buildRule.executeTarget("testGlobal");
        assertEquals("worked", buildRule.getLog());
    }

    @Test
    public void testOverride() {
        buildRule.executeTarget("testOverride");
        String log = buildRule.getLog();
        assertThat("override warning sent", log,
                containsString("Trying to override old definition of task copy"));
        assertThat("task inside target worked", log,
                containsString("In target"));
        assertThat("task inside target worked", log,
                containsString("In TaskContainer"));
    }
}
