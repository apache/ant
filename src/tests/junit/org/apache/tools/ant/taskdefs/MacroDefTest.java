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
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 */
public class MacroDefTest {

    @Rule
    public final BuildFileRule buildRule = new BuildFileRule();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/macrodef.xml");
    }

    @Test
    public void testSimple() {
        buildRule.executeTarget("simple");
        assertEquals("Hello World", buildRule.getLog());
    }

    @Test
    public void testText() {
        buildRule.executeTarget("text");
        assertEquals("Inner Text", buildRule.getLog());
    }

    @Test(expected = BuildException.class)
    public void testDuplicateAttribute() {
        buildRule.executeTarget("duplicate.attribute");
        //TODO assert value
    }

    @Test(expected = BuildException.class)
    public void testDuplicateElement() {
        buildRule.executeTarget("duplicate.element");
        //TODO assert value
    }

    @Test
    public void testUri() {
        buildRule.executeTarget("uri");
        assertEquals("Hello World", buildRule.getLog());
    }

    @Test
    public void testNested() {
        buildRule.executeTarget("nested");
        assertEquals("A nested element", buildRule.getLog());
    }

    @Test
    public void testDouble() {
        buildRule.executeTarget("double");
        assertEquals("@{prop} is 'property', value of ${property} is 'A property value'", buildRule.getLog());
    }

    @Test
    public void testIgnoreCase() {
        buildRule.executeTarget("ignorecase");
        assertEquals("a is ab is b", buildRule.getLog());
    }

    @Test
    public void testIgnoreElementCase() {
        buildRule.executeTarget("ignore-element-case");
        assertEquals("nested elementnested element", buildRule.getLog());
    }

    @Test
    public void testTextElement() {
        buildRule.executeTarget("textelement");
        assertThat(buildRule.getLog(), containsString("Hello world"));
    }

    @Test
    public void testTextTrim() {
        buildRule.executeTarget("text.trim");
        assertThat(buildRule.getLog(), containsString("[Hello world]"));
    }

    /**
     * Fail due to the name "text" already used as an attribute
     */
    @Test(expected = BuildException.class)
    public void testDuplicateTextName() {
        buildRule.executeTarget("duplicatetextname");
        // TODO assert value
    }

    /**
     * Fail due to the attribute name "text" already used by a text element
     */
    @Test(expected = BuildException.class)
    public void testDuplicateTextName2() {
        buildRule.executeTarget("duplicatetextname2");
        // TODO assert value
    }

    @Test
    public void testEscape() {
        buildRule.executeTarget("escape");
        assertEquals("a@b or a@b is avalue@bvalue", buildRule.getLog());
    }

    @Test
    public void testAttributeDescription() {
        buildRule.executeTarget("attribute.description");
        assertEquals("description is hello world", buildRule.getLog());
    }

    @Test
    public void testOverrideDefault() {
        buildRule.executeTarget("override.default");
        assertEquals("value is new", buildRule.getLog());
    }

    @Test
    public void testImplicit() {
        buildRule.executeTarget("implicit");
        assertEquals("Before implicitIn implicitAfter implicit", buildRule.getLog());
    }

    @Test
    public void testImplicitNotOptional() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("Missing nested elements for implicit element implicit");
        buildRule.executeTarget("implicit.notoptional");
    }

    @Test
    public void testImplicitOptional() {
        buildRule.executeTarget("implicit.optional");
        assertEquals("Before implicitAfter implicit", buildRule.getLog());
    }

    @Test
    public void testImplicitExplicit() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("Only one element allowed when using implicit elements");
        buildRule.executeTarget("implicit.explicit");
    }

    @Test
    public void testBackTraceOff() {
        thrown.expect(BuildException.class);
        thrown.expectMessage(not(containsString("following error occurred")));
        buildRule.executeTarget("backtraceoff");
    }

    @Test
    public void testBackTrace() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("following error occurred");
        buildRule.executeTarget("backtraceon");
    }

    @Test
    public void testTopLevelText() {
        buildRule.executeTarget("top-level-text");
        assertThat(buildRule.getLog(), containsString("Hello World"));
    }
}
