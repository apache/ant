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
import org.apache.tools.ant.BuildFileRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.apache.tools.ant.AntAssert.assertContains;
import static org.junit.Assert.fail;

/**
 * test nice
 */
public class NiceTest {

    @Rule
    public final BuildFileRule buildRule = new BuildFileRule();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/nice.xml");
    }

    @Test
    public void testNoop() {
        buildRule.executeTarget("noop");
    }

    @Test
    public void testCurrent() {
        buildRule.executeTarget("current");
    }

    @Test
    public void testFaster() {
        buildRule.executeTarget("faster");
    }

    @Test
    public void testSlower() {
        buildRule.executeTarget("slower");
    }

    @Test
    public void testTooSlow() {
        try {
			buildRule.executeTarget("too_slow");
			fail("BuildException expected: out of range");
		} catch (BuildException ex) {
			assertContains("out of the range 1-10", ex.getMessage());
		}
    }

    @Test
    public void testTooFast() {
        try {
			buildRule.executeTarget("too_fast");
			fail("BuildException expected: out of range");
		} catch (BuildException ex) {
			assertContains("out of the range 1-10", ex.getMessage());
		}
    }

}
