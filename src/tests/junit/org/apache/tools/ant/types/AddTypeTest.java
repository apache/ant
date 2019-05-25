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

package org.apache.tools.ant.types;

import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.condition.Condition;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

public class AddTypeTest {

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/types/addtype.xml");
    }

    @Test
    public void testAddPath() {
        buildRule.executeTarget("addpath");
    }

    @Test
    public void testAddCondition() {
        buildRule.executeTarget("addcondition");
    }

    @Test
    public void testAddFilter() {
        buildRule.executeTarget("addfilter");
    }

    @Test
    public void testAddSelector() {
        buildRule.executeTarget("addselector");
    }

    @Test
    public void testNestedA() {
        buildRule.executeTarget("nested.a");
        assertThat(buildRule.getLog(), containsString("add A called"));
    }

    @Test
    public void testNestedB() {
        buildRule.executeTarget("nested.b");
        assertThat(buildRule.getLog(), containsString("add B called"));
    }

    @Test
    public void testNestedC() {
        buildRule.executeTarget("nested.c");
        assertThat(buildRule.getLog(), containsString("add C called"));
    }

    @Test
    public void testNestedAB() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("ambiguous");
        buildRule.executeTarget("nested.ab");
    }

    @Test
    public void testConditionType() {
        buildRule.executeTarget("condition.type");
        assertThat(buildRule.getLog(), containsString("beforeafter"));
    }

    @Test
    public void testConditionTask() {
        buildRule.executeTarget("condition.task");
        assertThat(buildRule.getLog(), containsString("My Condition execution"));
    }

    @Test
    public void testConditionConditionType() {
        buildRule.executeTarget("condition.condition.type");
        assertThat(buildRule.getLog(), containsString("My Condition eval"));
    }

    @Test
    public void testConditionConditionTask() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("doesn't support the nested");
        buildRule.executeTarget("condition.condition.task");
    }

    @Test
    public void testAddConfigured() {
        buildRule.executeTarget("myaddconfigured");
        assertThat(buildRule.getLog(),
                containsString("value is Value Setexecute: value is Value Set"));
    }

    @Test
    public void testAddConfiguredValue() {
        buildRule.executeTarget("myaddconfiguredvalue");
        assertThat(buildRule.getLog(),
                containsString("value is Value Setexecute: value is Value Set"));
    }

    @Test
    public void testNamespace() {
        buildRule.executeTarget("namespacetest");
    }

    // The following will be used as types and tasks

    public interface A {
    }
    public interface B {
    }
    public interface C extends A {
    }
    public interface AB extends A, B {
    }

    public static class AImpl implements A {
    }
    public static class BImpl implements B {
    }
    public static class CImpl implements C {
    }
    public static class ABImpl implements AB {
    }

    public static class NestedContainer extends Task {
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

    public static class MyCondition implements Condition {
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

    public static class MyValue {
        private String text = "NOT SET YET";
        public void addText(String text) {
            this.text = text;
        }
        public String toString() {
            return text;
        }
    }

    public static class MyAddConfigured extends Task {
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

    public static class MyAddConfiguredValue extends Task {
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
