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

package org.apache.tools.ant.taskdefs.optional.junit;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import org.apache.tools.ant.BuildFileRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class JUnitTestListenerTest {

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    // The captureToSummary test writes to stdout and stderr, good for
    // verifying that the TestListener support doesn't break anything.
    private static final String PASS_TEST_TARGET = "captureToSummary";

    // testNoCrash is the test invoked by the captureToSummary's junit task
    private static final String PASS_TEST = "testNoCrash";

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/optional/junit.xml");
    }


    @Test
    public void testFullLogOutput() {
        buildRule.getProject().setProperty("enableEvents", "true");
        buildRule.executeTarget(PASS_TEST_TARGET);
        assertThat("expecting full log to have BuildListener events", buildRule.getFullLog(),
                containsString(JUnitTask.TESTLISTENER_PREFIX));
    }

    @Test
    public void testNoLogOutput() {
        buildRule.getProject().setProperty("enableEvents", "true");
        buildRule.executeTarget(PASS_TEST_TARGET);
        assertThat("expecting log to not have BuildListener events", buildRule.getLog(),
                not(containsString(JUnitTask.TESTLISTENER_PREFIX)));
    }

    @Test
    public void testTestCountFired() {
        buildRule.getProject().setProperty("enableEvents", "true");
        buildRule.executeTarget(PASS_TEST_TARGET);
        assertThat("expecting test count message", buildRule.getFullLog(),
                containsString(JUnitTask.TESTLISTENER_PREFIX + "tests to run: "));
    }

    @Test
    public void testStartTestFired() {
        buildRule.getProject().setProperty("enableEvents", "true");
        buildRule.executeTarget(PASS_TEST_TARGET);
        assertThat("expecting test started message", buildRule.getFullLog(),
                containsString(JUnitTask.TESTLISTENER_PREFIX + "startTest(" + PASS_TEST + ")"));
    }

    @Test
    public void testEndTestFired() {
        buildRule.getProject().setProperty("enableEvents", "true");
        buildRule.executeTarget(PASS_TEST_TARGET);
        assertThat("expecting test ended message", buildRule.getFullLog(),
                containsString(JUnitTask.TESTLISTENER_PREFIX + "endTest(" + PASS_TEST + ")"));
    }

    @Test
    public void testNoFullLogOutputByDefault() {
        buildRule.executeTarget(PASS_TEST_TARGET);
        assertThat("expecting full log to not have BuildListener events", buildRule.getFullLog(),
                not(containsString(JUnitTask.TESTLISTENER_PREFIX)));
    }

    @Test
    public void testFullLogOutputMagicProperty() {
        buildRule.getProject().setProperty(JUnitTask.ENABLE_TESTLISTENER_EVENTS, "true");
        buildRule.executeTarget(PASS_TEST_TARGET);
        assertThat("expecting full log to have BuildListener events", buildRule.getFullLog(),
                containsString(JUnitTask.TESTLISTENER_PREFIX));
    }

    @Test
    public void testNoFullLogOutputMagicPropertyWins() {
        buildRule.getProject().setProperty(JUnitTask.ENABLE_TESTLISTENER_EVENTS, "false");
        buildRule.getProject().setProperty("enableEvents", "true");
        buildRule.executeTarget(PASS_TEST_TARGET);
        assertThat("expecting full log to not have BuildListener events", buildRule.getFullLog(),
                not(containsString(JUnitTask.TESTLISTENER_PREFIX)));
    }

}
