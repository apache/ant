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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeFalse;

/**
 * test assertion handling
 */
public class AssertionsTest {

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/types/assertions.xml");
    }

    /**
     * runs a test and expects an assertion thrown in forked code
     * @param target String
     */
    private void expectAssertion(String target) {
        thrown.expect(BuildException.class);
        thrown.expectMessage("Java returned: 1");
        buildRule.executeTarget(target);
    }

    @Test
    public void testClassname() {
        expectAssertion("test-classname");
    }

    @Test
    public void testPackage() {
        expectAssertion("test-package");
    }

    @Test
    public void testEmptyAssertions() {
        buildRule.executeTarget("test-empty-assertions");
    }

    @Test
    public void testDisable() {
        buildRule.executeTarget("test-disable");
    }

    @Test
    public void testOverride() {
        expectAssertion("test-override");
    }

    @Test
    public void testOverride2() {
        buildRule.executeTarget("test-override2");
    }

    @Test
    public void testReferences() {
        expectAssertion("test-references");
    }

    @Test
    public void testMultipleAssertions() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("Only one assertion declaration is allowed");
        buildRule.executeTarget("test-multiple-assertions");
    }

    @Test
    public void testReferenceAbuse() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("You must not specify");
        buildRule.executeTarget("test-reference-abuse");
    }

    @Test
    public void testNofork() {
        assumeFalse("ran Ant tests with -ea and this would fail spuriously",
                AssertionsTest.class.desiredAssertionStatus());
        buildRule.executeTarget("test-nofork");
        assertThat(buildRule.getLog(),
                containsString("Assertion statements are currently ignored in non-forked mode"));
    }

    @Test
    public void testJUnit() {
        buildRule.executeTarget("test-junit");
    }
}
