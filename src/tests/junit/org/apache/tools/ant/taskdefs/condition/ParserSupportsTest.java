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

 */
public class ParserSupportsTest {

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/conditions/parsersupports.xml");
    }

    @Test
    public void testEmpty() {
        thrown.expect(BuildException .class);
        thrown.expectMessage(ParserSupports.ERROR_NO_ATTRIBUTES);
        buildRule.executeTarget("testEmpty");
    }

    @Test
    public void testBoth() {
        thrown.expect(BuildException .class);
        thrown.expectMessage(ParserSupports.ERROR_BOTH_ATTRIBUTES);
        buildRule.executeTarget("testBoth");
    }

    @Test
    public void testNamespaces() {
        buildRule.executeTarget("testNamespaces");
    }

    @Test
    public void testPropertyNoValue() {
        thrown.expect(BuildException .class);
        thrown.expectMessage(ParserSupports.ERROR_NO_VALUE);
        buildRule.executeTarget("testPropertyNoValue");
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
