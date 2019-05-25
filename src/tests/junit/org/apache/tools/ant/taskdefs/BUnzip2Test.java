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

import java.io.File;
import java.io.IOException;

import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.FileUtilities;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class BUnzip2Test {

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    private File outputDir;

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/bunzip2.xml");
        outputDir = new File(buildRule.getProject().getProperty("output"));
        buildRule.executeTarget("prepare");
    }

    @Test
    public void testRealTest() throws IOException {
        testRealTest("realTest");
    }

    @Test
    public void testRealTestWithResource() throws IOException {
        testRealTest("realTestWithResource");
    }

    private void testRealTest(String target) throws IOException {
        buildRule.executeTarget(target);
        assertEquals("File content mismatch after bunzip2",
                FileUtilities.getFileContents(new File(outputDir, "asf-logo-huge-from-gzip.tar")),
                FileUtilities.getFileContents(new File(outputDir, "asf-logo-huge.tar")));
    }

    @Test
    public void testDocumentationClaimsOnCopy() throws IOException {
        testRealTest("testDocumentationClaimsOnCopy");
    }
}
