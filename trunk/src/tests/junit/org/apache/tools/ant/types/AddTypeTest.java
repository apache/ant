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

package org.apache.tools.ant.types;

import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.condition.Condition;

public class AddTypeTest extends BuildFileTest {

    public AddTypeTest(String name) {
        super(name);
    }

    public void setUp() {
        configureProject("src/etc/testcases/types/addtype.xml");
    }

    public void testAddPath() {
        executeTarget("addpath");
    }

    public void testAddCondition() {
        executeTarget("addcondition");
    }

    public void testAddFilter() {
        executeTarget("addfilter");
    }

    public void testAddSelector() {
        executeTarget("addselector");
    }

    public void testNestedA() {
        expectLogContaining("nested.a", "add A called");
    }

    public void testNestedB() {
        expectLogContaining("nested.b", "add B called");
    }

    public void testNestedC() {
        expectLogContaining("nested.c", "add C called");
    }

    public void testNestedAB() {
        expectBuildExceptionContaining(
            "nested.ab", "Should have got ambiguous", "ambiguous");
    }

    public void testConditionType() {
        expectLogContaining("condition.type", "beforeafter");
    }

    public void testConditionTask() {
        expectLogContaining("condition.task", "My Condition execution");
    }
    public void testConditionConditionType() {
        expectLogContaining("condition.condition.type", "My Condition eval");
    }
    public void testConditionConditionTask() {
        expectBuildExceptionContaining(
            "condition.condition.task", "task masking condition",
            "doesn't support the nested");
    }

    public void testAddConfigured() {
        expectLogContaining(
            "myaddconfigured", "value is Value Setexecute: value is Value Set");
    }

    public void testAddConfiguredValue() {
        expectLogContaining(
            "myaddconfiguredvalue",
            "value is Value Setexecute: value is Value Set");
    }

    public void testNamespace() {
        executeTarget("namespacetest");
    }

    // The following will be used as types and tasks

    public static interface A {}
    public static interface B {}
    public static interface C extends A {}
    public static interface AB extends A, B {}

    public static class AImpl implements A{}
    public static class BImpl implements B{}
    public static class CImpl implements C{}
    public static class ABImpl implements AB{}

    public static class NestedContainer
        extends Task
    {
        public void add(A el) {
            log("add A called");
        }
        public void add(B el) {
            log("add B called");
        }
        public void add(C el) {
            log("add C called");
        }
    }

    public static class MyCondition
        implements Condition
    {
        Project project;
        public void setProject(Project project) {
            this.project = project;
        }
        public boolean eval() {
            project.log("My Condition eval");
            return true;
        }
        public void execute() {
            project.log("My Condition execution");
        }
    }

    public static class MyValue
    {
        private String text = "NOT SET YET";
        public void addText(String text) {
            this.text = text;
        }
        public String toString() {
            return text;
        }
    }

    public static class MyAddConfigured
        extends Task
    {
        MyValue value;
        public void addConfigured(MyValue value) {
            log("value is " + value);
            this.value = value;
        }
        public void add(MyValue value) {
            throw new BuildException("Should not be called");
        }
        public void execute() {
            log("execute: value is " + value);
        }
    }

    public static class MyAddConfiguredValue
        extends Task
    {
        MyValue value;
        public void addConfiguredValue(MyValue value) {
            log("value is " + value);
            this.value = value;
        }
        public void addValue(MyValue value) {
            throw new BuildException("Should not be called");
        }
        public void execute() {
            log("execute: value is " + value);
        }
    }

}
