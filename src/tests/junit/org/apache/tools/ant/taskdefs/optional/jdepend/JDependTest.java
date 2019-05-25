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

package org.apache.tools.ant.taskdefs.optional.jdepend;

import org.apache.tools.ant.BuildFileRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 * Testcase for the JDepend optional task.
 *
 */
public class JDependTest {

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Before
    public void setUp() {
        assertNotNull("build.tests.value not set", System.getProperty("build.tests.value"));
        buildRule.configureProject("src/etc/testcases/taskdefs/optional/jdepend/jdepend.xml");
    }

    /**
     * Test simple
     */
    @Test
    public void testSimple() {
        buildRule.executeTarget("simple");
        assertThat(buildRule.getOutput(),
                containsString("Package: org.apache.tools.ant.util.facade"));
    }

    /**
     * Test xml
     */
    @Test
    public void testXml() {
        buildRule.executeTarget("xml");
        assertThat(buildRule.getOutput(), containsString("<DependsUpon>"));
    }

    /**
     * Test fork
     * - forked output goes to log
     */
    @Test
    public void testFork() {
        buildRule.executeTarget("fork");
        assertThat(buildRule.getLog(),
                containsString("Package: org.apache.tools.ant.util.facade"));
    }

    /**
     * Test fork xml
     */
    @Test
    public void testForkXml() {
        buildRule.executeTarget("fork-xml");
        assertThat(buildRule.getLog(), containsString("<DependsUpon>"));
    }

    /**
     * Test timeout
     */
    @Test
    public void testTimeout() {
        buildRule.executeTarget("fork-timeout");
        assertThat(buildRule.getLog(), containsString("JDepend FAILED - Timed out"));
    }


    /**
     * Test timeout without timing out
     */
    @Test
    public void testTimeoutNot() {
        buildRule.executeTarget("fork-timeout-not");
        assertThat(buildRule.getLog(),
                containsString("Package: org.apache.tools.ant.util.facade"));
    }

}
