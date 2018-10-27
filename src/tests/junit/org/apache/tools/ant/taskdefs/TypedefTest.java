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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.apache.tools.ant.AntAssert.assertContains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 */
public class TypedefTest {
    
    @Rule
    public final BuildFileRule buildRule = new BuildFileRule();


    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/typedef.xml");
    }

    @Test
    public void testEmpty() {
        try {
			buildRule.executeTarget("empty");
			fail("BuildException expected: required argument not specified");
		} catch (BuildException ex) {
			//TODO assert value
		}
    }

    @Test
    public void testNoName() {
        try {
			buildRule.executeTarget("noName");
			fail("BuildException expected: required argument not specified");
		} catch (BuildException ex) {
			//TODO assert value
		}
    }

    @Test
    public void testNoClassname() {
        try {
			buildRule.executeTarget("noClassname");
			fail("BuildException expected: required argument not specified");
		} catch (BuildException ex) {
			//TODO assert value
		}
    }

    @Test
    public void testClassNotFound() {
        try {
			buildRule.executeTarget("classNotFound");
			fail("BuildException expected: classname specified doesn't exist");
		} catch (BuildException ex) {
			//TODO assert value
		}
    }

    @Test
    public void testGlobal() {
        buildRule.executeTarget("testGlobal");
        assertEquals("", buildRule.getLog());
        Object ref = buildRule.getProject().getReferences().get("global");
        assertNotNull("ref is not null", ref);
        assertEquals("org.example.types.TypedefTestType",
                     ref.getClass().getName());
    }

    @Test
    public void testLocal() {
        buildRule.executeTarget("testLocal");
        assertEquals("", buildRule.getLog());
        Object ref = buildRule.getProject().getReferences().get("local");
        assertNotNull("ref is not null", ref);
        assertEquals("org.example.types.TypedefTestType",
                     ref.getClass().getName());
    }

    /**
     * test to make sure that one can define a not present
     * optional type twice and then have a valid definition.
     */
    @Test
    public void testDoubleNotPresent() {
        buildRule.executeTarget("double-notpresent");
		assertContains("hi", buildRule.getLog());
    }
    
    @Test
    public void testNoResourceOnErrorFailAll(){
    		try {
			buildRule.executeTarget("noresourcefailall");
			fail("BuildException expected: the requested resource does not exist");
		} catch (BuildException ex) {
			assertContains("Could not load definitions from resource ", ex.getMessage());
		}
    }
    
    @Test
    public void testNoResourceOnErrorFail(){
		buildRule.executeTarget("noresourcefail");
		assertContains("Could not load definitions from resource ", buildRule.getLog());
    }
    
    @Test
    public void testNoResourceOnErrorNotFail(){
    		buildRule.executeTarget("noresourcenotfail");
		assertContains("Could not load definitions from resource ", buildRule.getLog());
    }
}
