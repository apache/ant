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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import org.apache.tools.ant.BuildFileRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TearDownOnVmCrashTest {

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/optional/junit/teardownlistener.xml");
    }

    @Test
    public void testNoTeardown() {
        buildRule.executeTarget("testNoTeardown");
        assertEquals("true", buildRule.getProject().getProperty("error"));
        assertThat(buildRule.getOutput(), not(containsString("tearDown called on Timeout")));
    }

    @Test
    public void testTeardown() {
        buildRule.executeTarget("testTeardown");
        assertEquals("true", buildRule.getProject().getProperty("error"));
        assertThat(buildRule.getOutput(), containsString("tearDown called on Timeout"));
    }
}
