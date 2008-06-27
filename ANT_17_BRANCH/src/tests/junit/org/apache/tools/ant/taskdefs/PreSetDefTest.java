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
import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;

/**
 */
public class PreSetDefTest extends BuildFileTest {
    public PreSetDefTest(String name) {
        super(name);
    }

    public void setUp() {
        configureProject("src/etc/testcases/taskdefs/presetdef.xml");
    }

    public void testSimple() {
        expectLog("simple", "Hello world");
    }

    public void testText() {
        expectLog("text", "Inner Text");
    }

    public void testUri() {
        expectLog("uri", "Hello world");
    }

    public void testDefaultTest() {
        expectLog("defaulttest", "attribute is false");
    }

    public void testDoubleDefault() {
        expectLog("doubledefault", "attribute is falseattribute is true");
    }

    public void testTextOptional() {
        expectLog("text.optional", "MyTextoverride text");
    }

    public void testElementOrder() {
        expectLog("element.order", "Line 1Line 2");
    }

    public void testElementOrder2() {
        expectLog("element.order2", "Line 1Line 2Line 3");
    }

    public void testAntTypeTest() {
        expectLog("antTypeTest", "");
    }

    public void testCorrectTaskNameBadAttr() {
        expectBuildExceptionContaining(
            "correct_taskname_badattr", "attribute message", "javac doesn't support the");
    }

    public void testCorrectTaskNameBadEl() {
        expectBuildExceptionContaining(
            "correct_taskname_badel", "element message", "javac doesn't support the");
    }
    
    public void testPresetdefWithNestedElementTwice() { // #38056
        executeTarget("presetdef-with-nested-element-twice");
        executeTarget("presetdef-with-nested-element-twice");
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

