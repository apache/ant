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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.Project;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 */
public class TaskdefTest {

    @Rule
    public final BuildFileRule buildRule = new BuildFileRule();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/taskdef.xml");
    }

    @Test
    public void test1() {
        try {
			buildRule.executeTarget("test1");
			fail("BuildException expected: required argument not specified");
		} catch (BuildException ex) {
			//TODO assert value
		}
    }

    @Test
    public void test2() {
        try {
			buildRule.executeTarget("test2");
			fail("BuildException expected: required argument not specified");
		} catch (BuildException ex) {
			//TODO assert value
		}
    }

    @Test
    public void test3() {
        try {
			buildRule.executeTarget("test3");
			fail("BuildException expected: required argument not specified");
		} catch (BuildException ex) {
			//TODO assert value
		}
    }

    @Test
    public void test4() {
        try {
			buildRule.executeTarget("test4");
			fail("BuildException expected: classname specified doesn't exist");
		} catch (BuildException ex) {
			//TODO assert value
		}
    }

    @Test
    public void test5() {
        try {
            buildRule.executeTarget("test5");
            fail("BuildException expected: No public execute() in " + Project.class);
        } catch (BuildException ex) {
            //TODO assert value
        }
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
        assertTrue("override warning sent",
                   log.indexOf("Trying to override old definition of task copy") > -1);
        assertTrue("task inside target worked",
                   log.indexOf("In target") > -1);
        assertTrue("task inside target worked",
                   log.indexOf("In TaskContainer") > -1);
    }
}
