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

import org.apache.tools.ant.AntAssert;
import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.condition.Condition;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.fail;

public class AddTypeTest {

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();
    
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
        AntAssert.assertContains("add A called", buildRule.getLog());
    }

    @Test
    public void testNestedB() {
        buildRule.executeTarget("nested.b");
         AntAssert.assertContains( "add B called", buildRule.getLog());
    }

    @Test
    public void testNestedC() {
        buildRule.executeTarget("nested.c");
        AntAssert.assertContains( "add C called", buildRule.getLog());
    }

    @Test
    public void testNestedAB() {
        try {
            buildRule.executeTarget("nested.ab");
            fail("Build exception expected: Should have got ambiguous");
        } catch (BuildException ex) {
            AntAssert.assertContains("ambiguous", ex.getMessage());
        }
    }

    @Test
    public void testConditionType() {
        buildRule.executeTarget("condition.type");
        AntAssert.assertContains( "beforeafter", buildRule.getLog());
    }

    @Test
    public void testConditionTask() {
        buildRule.executeTarget("condition.task");
        AntAssert.assertContains( "My Condition execution", buildRule.getLog());
    }
    
    @Test
    public void testConditionConditionType() {
        buildRule.executeTarget("condition.condition.type");
        AntAssert.assertContains( "My Condition eval", buildRule.getLog());
    }
    
    @Test
    public void testConditionConditionTask() {
        try {
            buildRule.executeTarget("condition.condition.task");
            fail("Build exception expected: Task masking condition");
        } catch (BuildException ex) {
             AntAssert.assertContains("doesn't support the nested", ex.getMessage());
        }
    }

    @Test
    public void testAddConfigured() {
        buildRule.executeTarget("myaddconfigured");
        AntAssert.assertContains("value is Value Setexecute: value is Value Set",
                buildRule.getLog());
    }

    @Test
    public void testAddConfiguredValue() {
        buildRule.executeTarget("myaddconfiguredvalue");
        AntAssert.assertContains("value is Value Setexecute: value is Value Set",
                buildRule.getLog());
    }

    @Test
    public void testNamespace() {
        buildRule.executeTarget("namespacetest");
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
