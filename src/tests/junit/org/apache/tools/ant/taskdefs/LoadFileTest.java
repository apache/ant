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
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.apache.tools.ant.AntAssert.assertContains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * Test the load file task
 */
public class LoadFileTest {

    @Rule
    public final BuildFileRule buildRule = new BuildFileRule();
    

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/loadfile.xml");
    }


    @After
    public void tearDown() {
        buildRule.executeTarget("cleanup");
    }


    /**
     * A unit test for JUnit
     */
    @Test
    public void testNoSourcefileDefined() {
        try {
			buildRule.executeTarget("testNoSourcefileDefined");
			fail("BuildException expected: source file not defined");
		} catch (BuildException ex) {
			//TODO assert value
		}
    }


    /**
     * A unit test for JUnit
     */
    @Test
    public void testNoPropertyDefined() {
        try {
			buildRule.executeTarget("testNoPropertyDefined");
			fail("BuildException expected: output property not defined");
		} catch (BuildException ex) {
			//TODO assert value
		}
    }


    /**
     * A unit test for JUnit
     */
    @Test
    public void testNoSourcefilefound() {
        try {
			buildRule.executeTarget("testNoSourcefilefound");
			fail("BuildException expected: File not found");
		} catch (BuildException ex) {
			assertContains(" doesn't exist", ex.getMessage());
		}
    }

    /**
     * A unit test for JUnit
     */
    @Test
    public void testFailOnError()
            throws BuildException {
        buildRule.executeTarget("testFailOnError");
		assertNull(buildRule.getProject().getProperty("testFailOnError"));
    }


    /**
     * A unit test for JUnit
     */
    @Test
    public void testLoadAFile()
            throws BuildException {
        buildRule.executeTarget("testLoadAFile");
        if(buildRule.getProject().getProperty("testLoadAFile").indexOf("eh?")<0) {
            fail("property is not all in the file");
        }
    }


    /**
     * A unit test for JUnit
     */
    @Test
    public void testLoadAFileEnc()
            throws BuildException {
        buildRule.executeTarget("testLoadAFileEnc");
        assertNotNull("file load files", buildRule.getProject().getProperty("testLoadAFileEnc"));
    }

    /**
     * A unit test for JUnit
     */
    @Test
    public void testEvalProps()
            throws BuildException {
        buildRule.executeTarget("testEvalProps");
        if(buildRule.getProject().getProperty("testEvalProps").indexOf("rain")<0) {
            fail("property eval broken");
        }
    }

    /**
     * Test FilterChain and FilterReaders
     */
    @Test
    public void testFilterChain()
            throws BuildException {
        buildRule.executeTarget("testFilterChain");
        if(buildRule.getProject().getProperty("testFilterChain").indexOf("World!")<0) {
            fail("Filter Chain broken");
        }
    }

    /**
     * Test StripJavaComments filterreader functionality.
     */
    @Test
    public final void testStripJavaComments()
            throws BuildException {
        buildRule.executeTarget("testStripJavaComments");
        final String expected = buildRule.getProject().getProperty("expected");
        final String generated = buildRule.getProject().getProperty("testStripJavaComments");
        assertEquals(expected, generated);
    }

    @Test
    public void testOneLine() {
        buildRule.executeTarget("testOneLine");
        assertEquals("1,2,3,4", buildRule.getProject().getProperty("testOneLine"));
    }
}
