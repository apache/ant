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

/**
 * test assertion handling
 */
public class AssertionsTest extends BuildFileTest {

    public AssertionsTest(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        configureProject("src/etc/testcases/types/assertions.xml");
    }

    protected void tearDown() throws Exception {
        executeTarget("teardown");
    }

    /**
     * runs a test and expects an assertion thrown in forked code
     * @param target
     */
    protected void expectAssertion(String target) {
        expectBuildExceptionContaining(target,
                "assertion not thrown in "+target,
                "Java returned: 1");
    }

    public void testClassname() {
        expectAssertion("test-classname");
    }

    public void testPackage() {
        expectAssertion("test-package");
    }

    public void testEmptyAssertions() {
        executeTarget("test-empty-assertions");
    }

    public void testDisable() {
        executeTarget("test-disable");
    }

    public void testOverride() {
        expectAssertion("test-override");
    }

    public void testOverride2() {
        executeTarget("test-override2");
    }
    public void testReferences() {
        expectAssertion("test-references");
    }

    public void testMultipleAssertions() {
        expectBuildExceptionContaining("test-multiple-assertions",
                "multiple assertions rejected",
                "Only one assertion declaration is allowed");
    }

    public void testReferenceAbuse() {
        expectBuildExceptionContaining("test-reference-abuse",
                "reference abuse rejected",
                "You must not specify");
    }

    public void testNofork() {
        if (AssertionsTest.class.desiredAssertionStatus()) {
            return; // ran Ant tests with -ea and this would fail spuriously
        }
        expectLogContaining("test-nofork",
                "Assertion statements are currently ignored in non-forked mode");
    }


    public void testJunit() {
        executeTarget("test-junit");
    }
}


