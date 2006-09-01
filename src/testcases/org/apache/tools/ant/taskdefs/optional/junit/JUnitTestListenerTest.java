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

package org.apache.tools.ant.taskdefs.optional.junit;

import org.apache.tools.ant.BuildFileTest;

public class JUnitTestListenerTest extends BuildFileTest {

    // The captureToSummary test writes to stdout and stderr, good for
    // verifying that the TestListener support doesn't break anything.
    private static final String PASS_TEST_TARGET = "captureToSummary";

    // testNoCrash is the test invoked by the captureToSummary's junit task
    private static final String PASS_TEST = "testNoCrash";

    public JUnitTestListenerTest(String name) {
        super(name);
    }

    public void setUp() {
        configureProject("src/etc/testcases/taskdefs/optional/junit.xml");
    }

    public void testFullLogOutput() {
        executeTarget(PASS_TEST_TARGET);
        assertTrue("expecting full log to have BuildListener events", 
                   hasBuildListenerEvents(getFullLog()));
    }
    
    public void testNoLogOutput() {
        executeTarget(PASS_TEST_TARGET);
        assertFalse("expecting log to not have BuildListener events", 
                    hasBuildListenerEvents(getLog()));
    }

    public void testTestCountFired() {
        executeTarget(PASS_TEST_TARGET);
	assertTrue("expecting test count message",
		   hasEventMessage(JUnitTask.TESTLISTENER_PREFIX + 
				   "tests to run: "));
    }
    
    public void testStartTestFired() {
        executeTarget(PASS_TEST_TARGET);
	assertTrue("expecting test started message",
		   hasEventMessage(JUnitTask.TESTLISTENER_PREFIX + 
				   "startTest(" + PASS_TEST + ")"));
    }
    
    public void testEndTestFired() {
        executeTarget(PASS_TEST_TARGET);
	assertTrue("expecting test ended message",
		   hasEventMessage(JUnitTask.TESTLISTENER_PREFIX + 
				   "endTest(" + PASS_TEST + ")"));
    }
    
    private boolean hasBuildListenerEvents(String log) {
        return log.indexOf(JUnitTask.TESTLISTENER_PREFIX) >= 0;
    }

    private boolean hasEventMessage(String eventPrefix) {
	return getFullLog().indexOf(eventPrefix) >= 0;
    }
}
