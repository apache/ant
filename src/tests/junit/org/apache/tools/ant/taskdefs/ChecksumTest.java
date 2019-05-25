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

package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.BuildFileRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class ChecksumTest {

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/checksum.xml");
    }

    @After
    public void tearDown() {
        buildRule.executeTarget("cleanup");
    }

    @Test
    public void testCreateMd5() {
        buildRule.executeTarget("createMd5");
    }

    @Test
    public void testCreateMD5SUMformat() {
        buildRule.executeTarget("createMD5SUMformat");
    }

    @Test
    public void testCreateSVFformat() {
        buildRule.executeTarget("createSVFformat");
    }

    @Test
    public void testCreatePattern() {
        buildRule.executeTarget("createPattern");
    }

    @Test
    public void testSetProperty() {
        buildRule.executeTarget("setProperty");
    }

    @Test
    public void testVerifyTotal() {
        buildRule.executeTarget("verifyTotal");
    }

    @Test
    public void testVerifyTotalRC() {
        buildRule.executeTarget("verifyTotalRC");
    }

    @Test
    public void testVerifyChecksumdir() {
        buildRule.executeTarget("verifyChecksumdir");
    }

    @Test
    public void testVerifyAsTask() {
        buildRule.executeTarget("verifyAsTask");
    }

    @Test
    public void testVerifyMD5SUMAsTask() {
        buildRule.executeTarget("verifyMD5SUMAsTask");
    }

    @Test
    public void testVerifyAsCondition() {
        buildRule.executeTarget("verifyAsCondition");
    }

    @Test
    public void testVerifyFromProperty() {
        buildRule.executeTarget("verifyFromProperty");
    }

    @Test
    public void testVerifyChecksumdirNoTotal() {
        buildRule.executeTarget("verifyChecksumdirNoTotal");
    }

}
