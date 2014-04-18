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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 */
public class DirnameTest {
	
	@Rule
	public BuildFileRule buildRule = new BuildFileRule();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/dirname.xml");
    }

    @Test
    public void test1() {
    	try {
        	buildRule.executeTarget("test1");
        	fail("Build exception should have been thrown as property attribute is required");
    	} catch(BuildException ex) {
    		assertEquals("property attribute required", ex.getMessage());
    	}
    }

    @Test
    public void test2() {
    	try {
        	buildRule.executeTarget("test2");
        	fail("Build exception should have been thrown as file attribute is required");
    	} catch(BuildException ex) {
    		assertEquals("file attribute required", ex.getMessage());
    	}
    }

    @Test
    public void test3() {
    	try {
        	buildRule.executeTarget("test3");
        	fail("Build exception should have been thrown as property attribute is required");
    	} catch(BuildException ex) {
    		assertEquals("property attribute required", ex.getMessage());
    	}
    }

    @Test
    public void test4() {
        Assume.assumeFalse("Test not possible on DOS or Netware family OS", Os.isFamily("netware") || Os.isFamily("dos"));
        buildRule.executeTarget("test4");
        String filesep = System.getProperty("file.separator");
        String expected = filesep + "usr" + filesep + "local";
        String checkprop = buildRule.getProject().getProperty("local.dir");
        assertEquals("dirname failed", expected, checkprop);
    }

    @Test
    public void test5() {
        buildRule.executeTarget("test5");
        String expected = buildRule.getProject().getProperty("basedir");
        String checkprop = buildRule.getProject().getProperty("base.dir");
        assertEquals("dirname failed", expected, checkprop);
    }

}
