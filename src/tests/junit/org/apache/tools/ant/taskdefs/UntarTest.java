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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.FileUtilities;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UntarTest {

    @Rule
    public final BuildFileRule buildRule = new BuildFileRule();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/untar.xml");
    }

    @Test
    public void testRealTest() throws IOException {
        testLogoExtraction("realTest");
    }

    @Test
    public void testRealGzipTest() throws IOException {
        testLogoExtraction("realGzipTest");
    }

    @Test
    public void testRealBzip2Test() throws IOException {
        testLogoExtraction("realBzip2Test");
    }

    @Test
    public void testTestTarTask() throws IOException {
        testLogoExtraction("testTarTask");
    }

    @Test
    public void testTestGzipTarTask() throws IOException {
        testLogoExtraction("testGzipTarTask");
    }

    @Test
    public void testTestBzip2TarTask() throws IOException {
        testLogoExtraction("testBzip2TarTask");
    }

    @Test(expected = BuildException.class)
    public void testSrcDirTest() {
        buildRule.executeTarget("srcDirTest");
        // TODO assert value
    }

    @Test
    public void testEncoding() {
        buildRule.executeTarget("encodingTest");
        String filename = buildRule.getProject().getProperty("output") + "/untartestout/foo";
        assertTrue("foo has been properly named",
                   buildRule.getProject().resolveFile(filename).exists());
    }

    @Test
    public void testResourceCollection() throws IOException {
        testLogoExtraction("resourceCollection");
    }

    private void testLogoExtraction(String target) throws IOException {
        buildRule.executeTarget(target);
        assertEquals(FileUtilities.getFileContents(buildRule.getProject().resolveFile("../asf-logo.gif")),
                FileUtilities.getFileContents(new File(buildRule.getProject().getProperty("output"), "untar/asf-logo.gif")));

    }

    @Test
    public void testDocumentationClaimsOnCopy() {
        buildRule.executeTarget("testDocumentationClaimsOnCopy");
        assertFalse(new File(buildRule.getProject().getProperty("output"), "untar/1/foo").exists());
        assertTrue(new File(buildRule.getProject().getProperty("output"), "untar/2/bar").exists());
    }
}
