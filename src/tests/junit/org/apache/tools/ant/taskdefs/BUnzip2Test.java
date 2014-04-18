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

import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.FileUtilities;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class BUnzip2Test {

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/bunzip2.xml");
        buildRule.executeTarget("prepare");
    }

    @Test
    public void tearDown() {
        buildRule.executeTarget("cleanup");
    }

    @Test
    public void testRealTest() throws java.io.IOException {
        testRealTest("realTest");
    }

    @Test
    public void testRealTestWithResource() throws java.io.IOException {
        testRealTest("realTestWithResource");
    }

    private void testRealTest(String target) throws java.io.IOException {
        buildRule.executeTarget(target);
        assertEquals("File content mismatch after bunzip2",
                FileUtilities.getFileContents(buildRule.getProject().resolveFile("expected/asf-logo-huge.tar")),
                FileUtilities.getFileContents(buildRule.getProject().resolveFile("asf-logo-huge.tar")));
    }

    @Test
    public void testDocumentationClaimsOnCopy() throws java.io.IOException {
        testRealTest("testDocumentationClaimsOnCopy");
    }
}
