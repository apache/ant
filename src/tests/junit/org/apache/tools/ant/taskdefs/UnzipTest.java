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
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UnzipTest {

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/unzip.xml");
    }

    /**
     * Expected failure due to required argument not specified
     */
    @Test(expected = BuildException.class)
    public void test1() {
        buildRule.executeTarget("test1");
        // TODO Assert exception message
    }

    /**
     * Expected failure due to required argument not specified
     */
    @Test(expected = BuildException.class)
    public void test2() {
        buildRule.executeTarget("test2");
        // TODO Assert exception message
    }

    /**
     * Expected failure due to required argument not specified
     */
    @Test(expected = BuildException.class)
    public void test3() {
        buildRule.executeTarget("test3");
        // TODO Assert exception message
    }

    @Test
    public void testRealTest() throws IOException {
        buildRule.executeTarget("realTest");
        assertLogoUncorrupted();
    }

    /**
     * test that the logo gif file has not been corrupted
     * @throws IOException if something goes wrong
     */
    private void assertLogoUncorrupted() throws IOException {
        assertEquals(FileUtilities.getFileContents(buildRule.getProject().resolveFile("../asf-logo.gif")),
                FileUtilities.getFileContents(new File(buildRule.getProject().getProperty("output"), "asf-logo.gif")));

    }

    @Test
    public void testTestZipTask() throws IOException {
        buildRule.executeTarget("testZipTask");
        assertLogoUncorrupted();
    }

    @Test
    public void testTestUncompressedZipTask() throws IOException {
        buildRule.executeTarget("testUncompressedZipTask");
        assertLogoUncorrupted();
    }

    /*
     * PR 11100
     */
    @Test
    public void testPatternSetExcludeOnly() {
        buildRule.executeTarget("testPatternSetExcludeOnly");
        final String output = buildRule.getProject().getProperty("output");
        assertFileMissing("1/foo is excluded", output + "/unziptestout/1/foo");
        assertFileExists("2/bar is not excluded", output + "/unziptestout/2/bar");
    }

    /*
     * PR 11100
     */
    @Test
    public void testPatternSetIncludeOnly() {
        buildRule.executeTarget("testPatternSetIncludeOnly");
        final String output = buildRule.getProject().getProperty("output");
        assertFileMissing("1/foo is not included", output + "/unziptestout/1/foo");
        assertFileExists("2/bar is included", output + "/unziptestout/2/bar");
    }

    /*
     * PR 11100
     */
    @Test
    public void testPatternSetIncludeAndExclude() {
        buildRule.executeTarget("testPatternSetIncludeAndExclude");
        final String output = buildRule.getProject().getProperty("output");
        assertFileMissing("1/foo is not included", output + "/unziptestout/1/foo");
        assertFileMissing("2/bar is excluded", output + "/unziptestout/2/bar");
    }

    /*
     * PR 38973
     */
    @Test
    public void testTwoPatternSets() {
        buildRule.executeTarget("testTwoPatternSets");
        final String output = buildRule.getProject().getProperty("output");
        assertFileMissing("1/foo is not included", output + "/unziptestout/1/foo");
        assertFileExists("2/bar is included", output + "/unziptestout/2/bar");
    }

    /*
     * PR 38973
     */
    @Test
    public void testTwoPatternSetsWithExcludes() {
        buildRule.executeTarget("testTwoPatternSetsWithExcludes");
        final String output = buildRule.getProject().getProperty("output");
        assertFileMissing("1/foo is not included", output + "/unziptestout/1/foo");
        assertFileMissing("2/bar is excluded", output + "/unziptestout/2/bar");
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
        final String output = buildRule.getProject().getProperty("output");
        assertFileMissing("1/foo is not included", output + "/unziptestout/1/foo");
        assertFileExists("\"2/bar is included", output + "/unziptestout/2/bar");
    }

    /*
     * PR 10504
     */
    @Test
    public void testEncoding() {
        buildRule.executeTarget("encodingTest");
        assertFileExists("foo has been properly named",
                buildRule.getProject().getProperty("output") + "/unziptestout/foo");
    }

    /*
     * PR 21996
     */
    @Test
    public void testFlattenMapper() {
        buildRule.executeTarget("testFlattenMapper");
        final String output = buildRule.getProject().getProperty("output");
        assertFileMissing("1/foo is not flattened", output + "/unziptestout/1/foo");
        assertFileExists("foo is flattened", output + "/unziptestout/foo");
    }

    /**
     * assert that a file exists, relative to the project
     * @param message message if there is no mpatch
     * @param filename filename to resolve against the project
     */
    private void assertFileExists(String message, String filename) {
        assertTrue(message, buildRule.getProject().resolveFile(filename).exists());
    }

    /**
     * assert that a file doesn't exist, relative to the project
     *
     * @param message  message if there is no mpatch
     * @param filename filename to resolve against the project
     */
    private void assertFileMissing(String message, String filename) {
        assertFalse(message, buildRule.getProject().resolveFile(filename).exists());
    }

    /**
     * PR 21996
     */
    @Test
    public void testGlobMapper() {
        buildRule.executeTarget("testGlobMapper");
        final String output = buildRule.getProject().getProperty("output");
        assertFileMissing("1/foo is not mapped", output + "/unziptestout/1/foo");
        assertFileExists("1/foo is mapped", output + "/unziptestout/1/foo.txt");
    }

    /**
     * Expected failure due to multiple mappers
     */
    @Test(expected = BuildException.class)
    public void testTwoMappers() {
        buildRule.executeTarget("testTwoMappers");
        // TODO Assert exception message
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
        final String output = buildRule.getProject().getProperty("output");
        assertFileMissing("1/foo is excluded", output + "/unziptestout/1/foo");
        assertFileExists("2/bar is not excluded", output + "/unziptestout/2/bar");
    }
}
