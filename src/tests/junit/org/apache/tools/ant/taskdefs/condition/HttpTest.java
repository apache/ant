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
package org.apache.tools.ant.taskdefs.condition;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Testcases for the &lt;http&gt; condition. All these tests require
 * us to be online as they attempt to get the status of various pages
 * on the Ant Apache web site.
 */
public class HttpTest {

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/conditions/http.xml");
    }

    @Test
    public void testNoMethod() {
        buildRule.executeTarget("basic-no-method");
        assertEquals("true", buildRule.getProject().getProperty("basic-no-method"));
        assertNull(buildRule.getProject().getProperty("basic-no-method-bad-url"));
    }

    @Test
    public void testHeadRequest() {
        buildRule.executeTarget("test-head-request");
        assertEquals("true", buildRule.getProject().getProperty("test-head-request"));
        assertNull(buildRule.getProject().getProperty("test-head-request-bad-url"));
    }

    @Test
    public void testGetRequest() {
        buildRule.executeTarget("test-get-request");
        assertEquals("true", buildRule.getProject().getProperty("test-get-request"));
        assertNull(buildRule.getProject().getProperty("test-get-request-bad-url"));
    }

    /**
     * Expected failure due to invalid HTTP request method specified
     */
    @Test(expected = BuildException.class)
    public void testBadRequestMethod() {
        buildRule.executeTarget("bad-request-method");
    }

}
