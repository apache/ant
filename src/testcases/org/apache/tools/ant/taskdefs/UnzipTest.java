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

import java.io.IOException;

/**
 */
public class UnzipTest extends BuildFileTest {

    /** Utilities used for file operations */
    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    public UnzipTest(String name) {
        super(name);
    }

    public void setUp() {
        configureProject("src/etc/testcases/taskdefs/unzip.xml");
    }

    public void tearDown() {
        executeTarget("cleanup");
    }

    public void test1() {
        expectBuildException("test1", "required argument not specified");
    }

    public void test2() {
        expectBuildException("test2", "required argument not specified");
    }

    public void test3() {
        expectBuildException("test3", "required argument not specified");
    }


    public void testRealTest() throws java.io.IOException {
        executeTarget("realTest");
        assertLogoUncorrupted();
    }

    /**
     * test that the logo gif file has not been corrupted
     * @throws IOException
     */
    private void assertLogoUncorrupted() throws IOException {
        assertTrue(FILE_UTILS.contentEquals(project.resolveFile("../asf-logo.gif"),
                                           project.resolveFile("asf-logo.gif")));
    }

    public void testTestZipTask() throws java.io.IOException {
        executeTarget("testZipTask");
        assertLogoUncorrupted();
    }

    public void testTestUncompressedZipTask() throws java.io.IOException {
        executeTarget("testUncompressedZipTask");
        assertLogoUncorrupted();
    }

    /*
     * PR 11100
     */
    public void testPatternSetExcludeOnly() {
        executeTarget("testPatternSetExcludeOnly");
        assertFileMissing("1/foo is excluded", "unziptestout/1/foo");
        assertFileExists("2/bar is not excluded", "unziptestout/2/bar");
    }

    /*
     * PR 11100
     */
    public void testPatternSetIncludeOnly() {
        executeTarget("testPatternSetIncludeOnly");
        assertFileMissing("1/foo is not included", "unziptestout/1/foo");
        assertFileExists("2/bar is included", "unziptestout/2/bar");
    }

    /*
     * PR 11100
     */
    public void testPatternSetIncludeAndExclude() {
        executeTarget("testPatternSetIncludeAndExclude");
        assertFileMissing("1/foo is not included", "unziptestout/1/foo");
        assertFileMissing("2/bar is excluded", "unziptestout/2/bar");
    }

    /*
     * PR 38973
     */
    public void testTwoPatternSets() {
        executeTarget("testTwoPatternSets");
        assertFileMissing("1/foo is not included", "unziptestout/1/foo");
        assertFileExists("2/bar is included", "unziptestout/2/bar");
    }

    /*
     * PR 38973
     */
    public void testTwoPatternSetsWithExcludes() {
        executeTarget("testTwoPatternSetsWithExcludes");
        assertFileMissing("1/foo is not included", "unziptestout/1/foo");
        assertFileMissing("2/bar is excluded", "unziptestout/2/bar");
    }

    /*
     * PR 16213
     */
    public void testSelfExtractingArchive() {
        executeTarget("selfExtractingArchive");
    }


    /*
     * PR 20969
     */
    public void testPatternSetSlashOnly() {
        executeTarget("testPatternSetSlashOnly");
        assertFileMissing("1/foo is not included", "unziptestout/1/foo");
        assertFileExists("\"2/bar is included", "unziptestout/2/bar");
    }


    /*
     * PR 10504
     */
    public void testEncoding() {
        executeTarget("encodingTest");
        assertFileExists("foo has been properly named", "unziptestout/foo");
    }

    /*
     * PR 21996
     */
    public void testFlattenMapper() {
        executeTarget("testFlattenMapper");
        assertFileMissing("1/foo is not flattened", "unziptestout/1/foo");
        assertFileExists("foo is flattened", "unziptestout/foo");
    }

    /**
     * assert that a file exists, relative to the project
     * @param message message if there is no mpatch
     * @param filename filename to resolve against the project
     */
    private void assertFileExists(String message, String filename) {
        assertTrue(message,
                   getProject().resolveFile(filename).exists());
    }

    /**
     * assert that a file doesnt exist, relative to the project
     *
     * @param message  message if there is no mpatch
     * @param filename filename to resolve against the project
     */
    private void assertFileMissing(String message, String filename) {
        assertTrue(message,
                !getProject().resolveFile(filename).exists());
    }

    /**
     * PR 21996
     */
    public void testGlobMapper() {
        executeTarget("testGlobMapper");
        assertFileMissing("1/foo is not mapped", "unziptestout/1/foo");
        assertFileExists("1/foo is mapped", "unziptestout/1/foo.txt");
    }

    public void testTwoMappers() {
        expectBuildException("testTwoMappers",Expand.ERROR_MULTIPLE_MAPPERS);
    }

    public void testResourceCollections() {
        executeTarget("testResourceCollection");
        assertFileExists("junit.jar has been extracted",
                         "unziptestout/junit/framework/Assert.class");
    }

    public void testDocumentationClaimsOnCopy() {
        executeTarget("testDocumentationClaimsOnCopy");
        assertFileMissing("1/foo is excluded", "unziptestout/1/foo");
        assertFileExists("2/bar is not excluded", "unziptestout/2/bar");
    }
}
