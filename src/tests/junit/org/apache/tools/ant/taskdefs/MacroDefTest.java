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
import static org.junit.Assert.fail;

/**
 */
public class MacroDefTest {

    @Rule
    public final BuildFileRule buildRule = new BuildFileRule();

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

    @Test
    public void testDuplicateAttribute() {
        try {
			buildRule.executeTarget("duplicate.attribute");
			fail("BuildException expected: the attribute text has already been specified");
		} catch (BuildException ex) {
			//TODO assert value
		}
    }

    @Test
    public void testDuplicateElement() {
        try {
			buildRule.executeTarget("duplicate.element");
			fail("BuildException expected: the element text has already been specified");
		} catch (BuildException ex) {
			//TODO assert value
		}
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
		assertContains("Hello world", buildRule.getLog());
    }

    @Test
    public void testTextTrim() {
        buildRule.executeTarget("text.trim");
		assertContains("[Hello world]", buildRule.getLog());
    }

    @Test
    public void testDuplicateTextName() {
        try {
			buildRule.executeTarget("duplicatetextname");
			fail("BuildException expected: the name \"text\" is already used as an attribute");
		} catch (BuildException ex) {
			//TODO assert value
		}
    }
    @Test
    public void testDuplicateTextName2() {
        try {
			buildRule.executeTarget("duplicatetextname2");
			fail("BuildException expected: the attribute name \"text\" has already been used by the text element");
		} catch (BuildException ex) {
			//TODO assert value
		}
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
        try {
            buildRule.executeTarget("implicit.notoptional");
            fail("BuildException expected: Missing nested elements for implicit element implicit");
        } catch (BuildException ex) {
            assertEquals("Missing nested elements for implicit element implicit", ex.getMessage());
        }
    }
    @Test
    public void testImplicitOptional() {
        buildRule.executeTarget("implicit.optional");
		assertEquals("Before implicitAfter implicit", buildRule.getLog());
    }
    @Test
    public void testImplicitExplicit() {
        try {
            buildRule.executeTarget("implicit.explicit");
            fail("BuildException expected: Only one element allowed when using implicit elements");
        } catch (BuildException ex) {
            assertEquals("Only one element allowed when using implicit elements", ex.getMessage());
        }
    }

    @Test
    public void testBackTraceOff() {
        try {
            buildRule.executeTarget("backtraceoff");
        } catch (BuildException ex) {
            if (ex.getMessage().indexOf("following error occurred") != -1) {
                fail("error message contained backtrace - " + ex.getMessage());
            }
        }
    }

    @Test
    public void testBackTrace() {
        try {
            buildRule.executeTarget("backtraceon");
            fail("BuildException expected: Checking if a back trace is created");
        } catch (BuildException ex) {
            assertContains("following error occurred", ex.getMessage());
        }
    }

    @Test
    public void testTopLevelText() {
        buildRule.executeTarget("top-level-text");
		assertContains("Hello World", buildRule.getLog());
    }
}

