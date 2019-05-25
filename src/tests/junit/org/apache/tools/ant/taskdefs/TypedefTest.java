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
import org.junit.rules.ExpectedException;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 */
public class TypedefTest {

    @Rule
    public final BuildFileRule buildRule = new BuildFileRule();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/typedef.xml");
    }

    @Test(expected = BuildException.class)
    public void testEmpty() {
        buildRule.executeTarget("empty");
        // TODO assert value
    }

    @Test
    public void testNoName() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("Only antlib URIs can be located from the URI alone, not the URI ''");
        buildRule.executeTarget("noName");
    }

    @Test(expected = BuildException.class)
    public void testNoClassname() {
        buildRule.executeTarget("noClassname");
        // TODO assert value
    }

    @Test(expected = BuildException.class)
    public void testClassNotFound() {
        buildRule.executeTarget("classNotFound");
        // TODO assert value
    }

    @Test
    public void testGlobal() {
        buildRule.executeTarget("testGlobal");
        assertEquals("", buildRule.getLog());
        Object ref = buildRule.getProject().getReferences().get("global");
        assertNotNull("ref is not null", ref);
        assertEquals("org.example.types.TypedefTestType", ref.getClass().getName());
    }

    @Test
    public void testLocal() {
        buildRule.executeTarget("testLocal");
        assertEquals("", buildRule.getLog());
        Object ref = buildRule.getProject().getReferences().get("local");
        assertNotNull("ref is not null", ref);
        assertEquals("org.example.types.TypedefTestType", ref.getClass().getName());
    }

    /**
     * test to make sure that one can define a not present
     * optional type twice and then have a valid definition.
     */
    @Test
    public void testDoubleNotPresent() {
        buildRule.executeTarget("double-notpresent");
        assertThat(buildRule.getLog(), containsString("hi"));
    }

    @Test
    public void testNoResourceOnErrorFailAll() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("Could not load definitions from resource ");
        buildRule.executeTarget("noresourcefailall");
    }

    @Test
    public void testNoResourceOnErrorFail() {
        buildRule.executeTarget("noresourcefail");
        assertThat(buildRule.getLog(), containsString("Could not load definitions from resource "));
    }

    @Test
    public void testNoResourceOnErrorNotFail() {
        buildRule.executeTarget("noresourcenotfail");
        assertThat(buildRule.getLog(), containsString("Could not load definitions from resource "));
    }
}
