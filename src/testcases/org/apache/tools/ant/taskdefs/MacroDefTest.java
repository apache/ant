/*
 * Copyright  2003-2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
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

import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

/**
 * @author Peter Reilly
 */
public class MacroDefTest extends BuildFileTest {
    public MacroDefTest(String name) {
        super(name);
    }

    public void setUp() {
        configureProject("src/etc/testcases/taskdefs/macrodef.xml");
    }

    public void testSimple() {
        expectLog("simple", "Hello World");
    }

    public void testText() {
        expectLog("text", "Inner Text");
    }

    public void testDuplicateAttribute() {
        expectBuildException(
            "duplicate.attribute",
            "the attribute text has already been specified");
    }

    public void testDuplicateElement() {
        expectBuildException(
            "duplicate.element",
            "the element text has already been specified");
    }

    public void testUri() {
        expectLog("uri", "Hello World");
    }

    public void testNested() {
        expectLog("nested", "A nested element");
    }

    public void testDouble() {
        expectLog(
            "double",
            "@{prop} is 'property', value of ${property} is 'A property value'");
    }

    public void testIgnoreCase() {
        expectLog(
            "ignorecase",
            "a is ab is b");
    }

    public void testIgnoreElementCase() {
        expectLog(
            "ignore-element-case",
            "nested elementnested element");
    }

    public void testTextElement() {
        expectLogContaining(
            "textelement", "Hello world");
    }

    public void testTextTrim() {
        expectLogContaining(
            "text.trim", "[Hello world]");
    }

    public void testDuplicateTextName() {
        expectBuildException(
            "duplicatetextname",
            "the name \"text\" is already used as an attribute");
    }
    public void testDuplicateTextName2() {
        expectBuildException(
            "duplicatetextname2",
            "the attribute name \"text\" has already been used by the text element");
    }
}

