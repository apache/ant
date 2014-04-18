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
package org.apache.tools.ant.taskdefs.condition;

import org.apache.tools.ant.AntAssert;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * test for reachable things
 */
public class IsReachableTest {

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Before
    public void setUp() {
        buildRule.configureProject(
                "src/etc/testcases/taskdefs/conditions/isreachable.xml");
    }


    @Test
    public void testLocalhost() throws Exception {
        buildRule.executeTarget("testLocalhost");
    }

    @Test
    public void testLocalhostURL() throws Exception {
        buildRule.executeTarget("testLocalhostURL");
    }

    @Test
    public void testIpv4localhost() throws Exception {
        buildRule.executeTarget("testIpv4localhost");
    }

    @Test
    public void testFTPURL() throws Exception {
        buildRule.executeTarget("testFTPURL");
    }

    @Test
    public void testBoth() throws Exception {
        try {
           buildRule.executeTarget("testBoth");
            fail("Build exception expected: error on two targets");
        } catch(BuildException ex) {
            assertEquals(IsReachable.ERROR_BOTH_TARGETS, ex.getMessage());
        }
    }

    @Test
    public void testNoTargets() throws Exception {
        try {
            buildRule.executeTarget("testNoTargets");
            fail("Build exception expected: no params");
        } catch(BuildException ex) {
            assertEquals(IsReachable.ERROR_NO_HOSTNAME, ex.getMessage());
        }
    }

    @Test
    public void testBadTimeout() throws Exception {
        try {
            buildRule.executeTarget("testBadTimeout");
            fail("Build exception expected: error on -ve timeout");
        } catch(BuildException ex) {
            assertEquals(IsReachable.ERROR_BAD_TIMEOUT, ex.getMessage());
        }
    }

    @Test
    @Ignore("Previously named in a way to prevent execution")
    public void NotestFile() throws Exception {
        try {
            buildRule.executeTarget("testFile");
            fail("Build exception expected: error on file URL");
        } catch(BuildException ex) {
            assertEquals(IsReachable.ERROR_NO_HOST_IN_URL, ex.getMessage());
        }
    }

    @Test
    public void testBadURL() throws Exception {
        try {
            buildRule.executeTarget("testBadURL");
            fail("Build exception expected: error in URL");
        } catch(BuildException ex) {
            AntAssert.assertContains(IsReachable.ERROR_BAD_URL, ex.getMessage());
        }
    }
}
