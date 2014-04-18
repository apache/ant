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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 
 */
public class ParserSupportsTest {

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/conditions/parsersupports.xml");
    }

    @Test
    public void testEmpty() {
        try {
            buildRule.executeTarget("testEmpty");
            fail("Build exception expected: " + ParserSupports.ERROR_NO_ATTRIBUTES);
        } catch(BuildException ex) {
            assertEquals(ParserSupports.ERROR_NO_ATTRIBUTES, ex.getMessage());
        }
    }

    @Test
    public void testBoth() {
        try {
            buildRule.executeTarget("testBoth");
            fail("Build exception expected: " + ParserSupports.ERROR_BOTH_ATTRIBUTES);
        } catch(BuildException ex) {
            assertEquals(ParserSupports.ERROR_BOTH_ATTRIBUTES, ex.getMessage());
        }
    }

    @Test
    public void testNamespaces() {
        buildRule.executeTarget("testNamespaces");
    }

    @Test
    public void testPropertyNoValue() {
        try {
            buildRule.executeTarget("testPropertyNoValue");
            fail("Build exception expected: " + ParserSupports.ERROR_NO_VALUE);
        } catch(BuildException ex) {
            assertEquals(ParserSupports.ERROR_NO_VALUE, ex.getMessage());
        }
    }

    @Test
    public void testUnknownProperty() {
        buildRule.executeTarget("testUnknownProperty");
    }

    @Test
    @Ignore("Previously named in a manner to prevent execution")
    public void NotestPropertyInvalid() {
        buildRule.executeTarget("testPropertyInvalid");
    }

    @Test
    @Ignore("Previously named in a manner to prevent execution")
    public void NotestXercesProperty() {
        buildRule.executeTarget("testXercesProperty");
    }
}
