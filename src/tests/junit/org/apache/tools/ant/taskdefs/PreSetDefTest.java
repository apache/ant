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
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;

/**
 */
public class PreSetDefTest {

    @Rule
    public final BuildFileRule buildRule = new BuildFileRule();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/presetdef.xml");
    }

    @Test
    public void testSimple() {
        buildRule.executeTarget("simple");
        assertEquals("Hello world", buildRule.getLog());
    }

    @Test
    public void testText() {
        buildRule.executeTarget("text");
        assertEquals("Inner Text", buildRule.getLog());
    }

    @Test
    public void testUri() {
        buildRule.executeTarget("uri");
        assertEquals("Hello world", buildRule.getLog());
    }

    @Test
    public void testDefaultTest() {
        buildRule.executeTarget("defaulttest");
        assertEquals("attribute is false", buildRule.getLog());
    }

    @Test
    public void testDoubleDefault() {
        buildRule.executeTarget("doubledefault");
        assertEquals("attribute is falseattribute is true", buildRule.getLog());
    }

    @Test
    public void testTextOptional() {
        buildRule.executeTarget("text.optional");
        assertEquals("MyTextoverride text", buildRule.getLog());
    }

    @Test
    public void testElementOrder() {
        buildRule.executeTarget("element.order");
        assertEquals("Line 1Line 2", buildRule.getLog());
    }

    @Test
    public void testElementOrder2() {
        buildRule.executeTarget("element.order2");
        assertEquals("Line 1Line 2Line 3", buildRule.getLog());
    }

    @Test
    public void testAntTypeTest() {
        buildRule.executeTarget("antTypeTest");
        assertEquals("", buildRule.getLog());
    }

    @Test
    public void testCorrectTaskNameBadAttr() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("javac doesn't support the");
        buildRule.executeTarget("correct_taskname_badattr");
    }

    @Test
    public void testCorrectTaskNameBadEl() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("javac doesn't support the");
        buildRule.executeTarget("correct_taskname_badel");
    }

    @Test
    public void testPresetdefWithNestedElementTwice() { // #38056
        buildRule.executeTarget("presetdef-with-nested-element-twice");
        buildRule.executeTarget("presetdef-with-nested-element-twice");
    }

    /**
     * A test class to check default properties
     */
    public static class DefaultTest extends Task {
        boolean isSet = false;
        boolean attribute = false;
        public void setAttribute(boolean b) {
            if (isSet) {
                throw new BuildException("Attribute Already set");
            }
            attribute = b;
            isSet = true;
        }

        public void execute() {
            getProject().log("attribute is " + attribute);
        }
    }

    /**
     * A test class to check presetdef with add and addConfigured and ant-type
     */
    public static class AntTypeTest extends Task {
        public void addFileSet(FileSet fileset) {
        }
        public void addConfiguredConfigured(FileSet fileset) {
        }
    }
}
