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
import org.apache.tools.ant.FileUtilities;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class UnzipTest {

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/unzip.xml");
    }

    @Test
    public void test1() {
        try {
			buildRule.executeTarget("test1");
			fail("BuildException expected: required argument not specified");
		} catch (BuildException ex) {
			//TODO assert value
		}
    }

    @Test
    public void test2() {
        try {
			buildRule.executeTarget("test2");
			fail("BuildException expected: required argument not specified");
		} catch (BuildException ex) {
			//TODO assert value
		}
    }

    @Test
    public void test3() {
        try {
			buildRule.executeTarget("test3");
			fail("BuildException expected: required argument not specified");
		} catch (BuildException ex) {
			//TODO assert value
		}
    }


    @Test
    public void testRealTest() throws java.io.IOException {
        buildRule.executeTarget("realTest");
        assertLogoUncorrupted();
    }

    /**
     * test that the logo gif file has not been corrupted
     * @throws IOException
     */
    private void assertLogoUncorrupted() throws IOException {
        assertEquals(FileUtilities.getFileContents(buildRule.getProject().resolveFile("../asf-logo.gif")),
                FileUtilities.getFileContents(new File(buildRule.getProject().getProperty("output"), "asf-logo.gif")));

    }

    @Test
    public void testTestZipTask() throws java.io.IOException {
        buildRule.executeTarget("testZipTask");
        assertLogoUncorrupted();
    }

    @Test
    public void testTestUncompressedZipTask() throws java.io.IOException {
        buildRule.executeTarget("testUncompressedZipTask");
        assertLogoUncorrupted();
    }

    /*
     * PR 11100
     */
    @Test
    public void testPatternSetExcludeOnly() {
        buildRule.executeTarget("testPatternSetExcludeOnly");
        assertFileMissing("1/foo is excluded", buildRule.getProject().getProperty("output") + "/unziptestout/1/foo");
        assertFileExists("2/bar is not excluded", buildRule.getProject().getProperty("output") + "/unziptestout/2/bar");
    }

    /*
     * PR 11100
     */
    @Test
    public void testPatternSetIncludeOnly() {
        buildRule.executeTarget("testPatternSetIncludeOnly");
        assertFileMissing("1/foo is not included", buildRule.getProject().getProperty("output") + "/unziptestout/1/foo");
        assertFileExists("2/bar is included", buildRule.getProject().getProperty("output") + "/unziptestout/2/bar");
    }

    /*
     * PR 11100
     */
    @Test
    public void testPatternSetIncludeAndExclude() {
        buildRule.executeTarget("testPatternSetIncludeAndExclude");
        assertFileMissing("1/foo is not included", buildRule.getProject().getProperty("output") + "/unziptestout/1/foo");
        assertFileMissing("2/bar is excluded", buildRule.getProject().getProperty("output") + "/unziptestout/2/bar");
    }

    /*
     * PR 38973
     */
    @Test
    public void testTwoPatternSets() {
        buildRule.executeTarget("testTwoPatternSets");
        assertFileMissing("1/foo is not included", buildRule.getProject().getProperty("output") + "/unziptestout/1/foo");
        assertFileExists("2/bar is included", buildRule.getProject().getProperty("output") + "/unziptestout/2/bar");
    }

    /*
     * PR 38973
     */
    @Test
    public void testTwoPatternSetsWithExcludes() {
        buildRule.executeTarget("testTwoPatternSetsWithExcludes");
        assertFileMissing("1/foo is not included", buildRule.getProject().getProperty("output") + "/unziptestout/1/foo");
        assertFileMissing("2/bar is excluded", buildRule.getProject().getProperty("output") + "/unziptestout/2/bar");
    }

    /*
     * PR 16213
     */
    @Test
    @Ignore("we lack a self extracting archive that we are allowed to distribute - see PR 49080")
    public void testSelfExtractingArchive() {
        // disabled because we lack a self extracting archive that we
        // are allowed to distribute - see PR 49080
        buildRule.executeTarget("selfExtractingArchive");
    }


    /*
     * PR 20969
     */
    @Test
    public void testPatternSetSlashOnly() {
        buildRule.executeTarget("testPatternSetSlashOnly");
        assertFileMissing("1/foo is not included", buildRule.getProject().getProperty("output") + "/unziptestout/1/foo");
        assertFileExists("\"2/bar is included", buildRule.getProject().getProperty("output") + "/unziptestout/2/bar");
    }


    /*
     * PR 10504
     */
    @Test
    public void testEncoding() {
        buildRule.executeTarget("encodingTest");
        assertFileExists("foo has been properly named", buildRule.getProject().getProperty("output") + "/unziptestout/foo");
    }

    /*
     * PR 21996
     */
    @Test
    public void testFlattenMapper() {
        buildRule.executeTarget("testFlattenMapper");
        assertFileMissing("1/foo is not flattened", buildRule.getProject().getProperty("output") + "/unziptestout/1/foo");
        assertFileExists("foo is flattened", buildRule.getProject().getProperty("output") + "/unziptestout/foo");
    }

    /**
     * assert that a file exists, relative to the project
     * @param message message if there is no mpatch
     * @param filename filename to resolve against the project
     */
    private void assertFileExists(String message, String filename) {
        assertTrue(message,
                buildRule.getProject().resolveFile(filename).exists());
    }

    /**
     * assert that a file doesnt exist, relative to the project
     *
     * @param message  message if there is no mpatch
     * @param filename filename to resolve against the project
     */
    private void assertFileMissing(String message, String filename) {
        assertTrue(message,
                !buildRule.getProject().resolveFile(filename).exists());
    }

    /**
     * PR 21996
     */
    @Test
    public void testGlobMapper() {
        buildRule.executeTarget("testGlobMapper");
        assertFileMissing("1/foo is not mapped", buildRule.getProject().getProperty("output") + "/unziptestout/1/foo");
        assertFileExists("1/foo is mapped", buildRule.getProject().getProperty("output") + "/unziptestout/1/foo.txt");
    }

    @Test
    public void testTwoMappers() {
        try {
			buildRule.executeTarget("testTwoMappers");
			fail("BuildException expected: " + Expand.ERROR_MULTIPLE_MAPPERS);
		} catch (BuildException ex) {
			//TODO assert value
		}
    }

    @Test
    public void testResourceCollections() {
        buildRule.executeTarget("testResourceCollection");
        assertFileExists("junit.jar has been extracted",
                buildRule.getProject().getProperty("output") + "/unziptestout/junit/framework/Assert.class");
    }

    @Test
    public void testDocumentationClaimsOnCopy() {
        buildRule.executeTarget("testDocumentationClaimsOnCopy");
        assertFileMissing("1/foo is excluded", buildRule.getProject().getProperty("output") + "/unziptestout/1/foo");
        assertFileExists("2/bar is not excluded", buildRule.getProject().getProperty("output") + "/unziptestout/2/bar");
    }
}
