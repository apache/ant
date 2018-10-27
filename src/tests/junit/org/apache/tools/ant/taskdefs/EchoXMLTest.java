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
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.apache.tools.ant.AntAssert.assertContains;
import static org.junit.Assert.fail;

public class EchoXMLTest {

    @Rule
    public final BuildFileRule buildRule = new BuildFileRule();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/echoxml.xml");
    }

    @After
    public void tearDown() {
        buildRule.executeTarget("tearDown");
    }

    @Test
    public void testPass() {
        buildRule.executeTarget("testPass");
    }

    @Test
    public void testFail() {
        try {
            buildRule.executeTarget("testFail");
            fail("BuildException expected: must fail");
        } catch (BuildException ex) {
            assertContains("${foo}=bar", ex.getMessage());
        }
    }

    @Test
    public void testEmpty() {
        try {
            buildRule.executeTarget("testEmpty");
            fail("BuildException expected: must fail");
        } catch (BuildException ex) {
            assertContains("No nested XML specified", ex.getMessage());
        }
    }

}
