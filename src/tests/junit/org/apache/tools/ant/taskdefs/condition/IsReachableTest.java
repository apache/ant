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
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * test for reachable things
 */
public class IsReachableTest {

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/conditions/isreachable.xml");
    }

    @Test
    public void testLocalhost() {
        buildRule.executeTarget("testLocalhost");
    }

    @Test
    public void testLocalhostURL() {
        buildRule.executeTarget("testLocalhostURL");
    }

    @Test
    public void testIpv4localhost() {
        buildRule.executeTarget("testIpv4localhost");
    }

    @Test
    public void testFTPURL() {
        buildRule.executeTarget("testFTPURL");
    }

    @Test
    public void testBoth() {
        thrown.expect(BuildException.class);
        thrown.expectMessage(IsReachable.ERROR_BOTH_TARGETS);
        buildRule.executeTarget("testBoth");
    }

    @Test
    public void testNoTargets() {
        thrown.expect(BuildException.class);
        thrown.expectMessage(IsReachable.ERROR_NO_HOSTNAME);
        buildRule.executeTarget("testNoTargets");
    }

    @Test
    public void testBadTimeout() {
        thrown.expect(BuildException.class);
        thrown.expectMessage(IsReachable.ERROR_BAD_TIMEOUT);
        buildRule.executeTarget("testBadTimeout");
    }

    @Test
    @Ignore("Previously named in a way to prevent execution")
    public void NotestFile() {
        thrown.expect(BuildException.class);
        thrown.expectMessage(IsReachable.ERROR_NO_HOST_IN_URL);
        buildRule.executeTarget("testFile");
    }

    @Test
    public void testBadURL() {
        thrown.expect(BuildException.class);
        thrown.expectMessage(IsReachable.ERROR_BAD_URL);
        buildRule.executeTarget("testBadURL");
    }
}
