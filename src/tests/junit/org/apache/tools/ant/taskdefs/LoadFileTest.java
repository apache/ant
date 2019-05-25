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
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

/**
 * Test the load file task
 */
public class LoadFileTest {

    @Rule
    public final BuildFileRule buildRule = new BuildFileRule();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/loadfile.xml");
    }

    @After
    public void tearDown() {
        buildRule.executeTarget("cleanup");
    }

    /**
     * Fail due to source file not defined
     */
    @Test(expected = BuildException.class)
    public void testNoSourcefileDefined() {
        buildRule.executeTarget("testNoSourcefileDefined");
        // TODO assert value
    }

    /**
     * Fail due to output property not defined
     */
    @Test(expected = BuildException.class)
    public void testNoPropertyDefined() {
        buildRule.executeTarget("testNoPropertyDefined");
        // TODO assert value
    }

    /**
     * A unit test for JUnit
     */
    @Test
    public void testNoSourcefilefound() {
        thrown.expect(BuildException.class);
        thrown.expectMessage(" doesn't exist");
        buildRule.executeTarget("testNoSourcefilefound");
    }

    /**
     * A unit test for JUnit
     */
    @Test
    public void testFailOnError() throws BuildException {
        buildRule.executeTarget("testFailOnError");
        assertNull(buildRule.getProject().getProperty("testFailOnError"));
    }

    /**
     * A unit test for JUnit
     */
    @Test
    public void testLoadAFile() throws BuildException {
        buildRule.executeTarget("testLoadAFile");
        assertThat("property is not all in the file",
                buildRule.getProject().getProperty("testLoadAFile"), containsString("eh?"));
    }

    /**
     * A unit test for JUnit
     */
    @Test
    public void testLoadAFileEnc() throws BuildException {
        buildRule.executeTarget("testLoadAFileEnc");
        assertNotNull("file load files", buildRule.getProject().getProperty("testLoadAFileEnc"));
    }

    /**
     * A unit test for JUnit
     */
    @Test
    public void testEvalProps() throws BuildException {
        buildRule.executeTarget("testEvalProps");
        assertThat("property eval broken",
                buildRule.getProject().getProperty("testEvalProps"), containsString("rain"));
    }

    /**
     * Test FilterChain and FilterReaders
     */
    @Test
    public void testFilterChain() throws BuildException {
        buildRule.executeTarget("testFilterChain");
        assertThat("Filter Chain broken",
                buildRule.getProject().getProperty("testFilterChain"), containsString("World!"));
    }

    /**
     * Test StripJavaComments filterreader functionality.
     */
    @Test
    public final void testStripJavaComments() throws BuildException {
        buildRule.executeTarget("testStripJavaComments");
        assertEquals(buildRule.getProject().getProperty("expected"),
                buildRule.getProject().getProperty("testStripJavaComments"));
    }

    @Test
    public void testOneLine() {
        buildRule.executeTarget("testOneLine");
        assertEquals("1,2,3,4", buildRule.getProject().getProperty("testOneLine"));
    }
}
