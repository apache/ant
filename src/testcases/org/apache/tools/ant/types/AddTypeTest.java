/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "Ant" and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

package org.apache.tools.ant.types;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileTest;
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
        public void execute() {
            log("execute: value is " + value);
        }
    }

}
