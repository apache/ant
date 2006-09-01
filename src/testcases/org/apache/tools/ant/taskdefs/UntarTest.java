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

import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.util.FileUtils;

/**
 */
public class UntarTest extends BuildFileTest {

    /** Utilities used for file operations */
    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    public UntarTest(String name) {
        super(name);
    }

    public void setUp() {
        configureProject("src/etc/testcases/taskdefs/untar.xml");
    }

    public void tearDown() {
        executeTarget("cleanup");
    }

    public void testRealTest() throws java.io.IOException {
        testLogoExtraction("realTest");
    }

    public void testRealGzipTest() throws java.io.IOException {
        testLogoExtraction("realGzipTest");
    }

    public void testRealBzip2Test() throws java.io.IOException {
        testLogoExtraction("realBzip2Test");
    }

    public void testTestTarTask() throws java.io.IOException {
        testLogoExtraction("testTarTask");
    }

    public void testTestGzipTarTask() throws java.io.IOException {
        testLogoExtraction("testGzipTarTask");
    }

    public void testTestBzip2TarTask() throws java.io.IOException {
        testLogoExtraction("testBzip2TarTask");
    }

    public void testSrcDirTest() {
        expectBuildException("srcDirTest", "Src cannot be a directory.");
    }

    public void testEncoding() {
        expectSpecificBuildException("encoding",
                                     "<untar> overrides setEncoding.",
                                     "The untar task doesn't support the "
                                     + "encoding attribute");
    }

    public void testResourceCollection() throws java.io.IOException {
        testLogoExtraction("resourceCollection");
    }

    private void testLogoExtraction(String target) throws java.io.IOException {
        executeTarget(target);
        assertTrue(FILE_UTILS.contentEquals(project.resolveFile("../asf-logo.gif"),
                                           project.resolveFile("asf-logo.gif")));
    }

    public void testDocumentationClaimsOnCopy() {
        executeTarget("testDocumentationClaimsOnCopy");
        assertFalse(getProject().resolveFile("untartestout/1/foo").exists());
        assertTrue(getProject().resolveFile("untartestout/2/bar").exists());
    }
}
