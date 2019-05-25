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
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.zip.UnixStat;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeTrue;

public class ZipTest {

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    //instance variable to allow cleanup
    ZipFile zfPrefixAddsDir = null;

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/zip.xml");
        buildRule.executeTarget("setUp");
    }

    /**
     * Fail due to required argument not specified
     */
    @Test(expected = BuildException.class)
    public void test1() {
        buildRule.executeTarget("test1");
        //TODO assert value
    }

    /**
     * Fail due to required argument not specified
     */
    @Test(expected = BuildException.class)
    public void test2() {
        buildRule.executeTarget("test2");
        //TODO assert value
    }

    /**
     * Fail because zip cannot include itself
     */
    @Test(expected = BuildException.class)
    public void test3() {
        buildRule.executeTarget("test3");
        //TODO assert value
    }

    /**
     * Fail because zip cannot include itself
     */
    @Test(expected = BuildException.class)
    @Ignore("Previously commented out")
    public void test4() {
        buildRule.executeTarget("test4");
        //TODO assert value
    }

    @After
    public void tearDown() {
        try {
            if (zfPrefixAddsDir != null) {
                zfPrefixAddsDir.close();
            }
        } catch (IOException e) {
            //ignored
        }
    }

    @Test
    public void test5() {
       buildRule.executeTarget("test5");
    }


    @Test
    public void test6() {
       buildRule.executeTarget("test6");
    }


    @Test
    public void test7() {
       buildRule.executeTarget("test7");
    }

    @Test
    public void test8() {
       buildRule.executeTarget("test8");
    }

    @Test
    public void testZipgroupfileset() throws IOException {
       buildRule.executeTarget("testZipgroupfileset");

        ZipFile zipFile = new ZipFile(new File(buildRule.getProject().getProperty("output"),
                "zipgroupfileset.zip"));

        assertNotNull(zipFile.getEntry("ant.xml"));
        assertNotNull(zipFile.getEntry("optional/jspc.xml"));
        assertNotNull(zipFile.getEntry("zip/zipgroupfileset3.zip"));

        assertNull(zipFile.getEntry("test6.mf"));
        assertNull(zipFile.getEntry("test7.mf"));

        zipFile.close();
    }

    @Test
    public void testUpdateNotNecessary() {
       buildRule.executeTarget("testUpdateNotNecessary");
       assertThat(buildRule.getLog(), not(containsString("Updating")));
    }

    @Test
    public void testUpdateIsNecessary() {
        buildRule.executeTarget("testUpdateIsNecessary");
        assertThat(buildRule.getLog(), containsString("Updating"));
    }

    // Bugzilla Report 18403
    @Test
    public void testPrefixAddsDir() throws IOException {
       buildRule.executeTarget("testPrefixAddsDir");
        File archive = new File(buildRule.getProject().getProperty("output"), "test3.zip");
        zfPrefixAddsDir = new ZipFile(archive);
        ZipEntry ze = zfPrefixAddsDir.getEntry("test/");
        assertNotNull("test/ has been added", ze);

    }

    // Bugzilla Report 19449
    @Test
    public void testFilesOnlyDoesntCauseRecreate() {
        buildRule.executeTarget("testFilesOnlyDoesntCauseRecreateSetup");
        File testFile = new File(buildRule.getOutputDir(), "test3.zip");
        assumeTrue("Could not change file modification time",
                testFile.setLastModified(testFile.lastModified() - (FileUtils.getFileUtils().getFileTimestampGranularity() * 5)));
        long l = testFile.lastModified();

        buildRule.executeTarget("testFilesOnlyDoesntCauseRecreate");
        assertEquals(l, testFile.lastModified());
    }

    // Bugzilla Report 22865
    @Test
    public void testEmptySkip() {
       buildRule.executeTarget("testEmptySkip");
    }

    // Bugzilla Report 30365
    @Test
    public void testZipEmptyDir() {
       buildRule.executeTarget("zipEmptyDir");
    }

    // Bugzilla Report 40258
    @Test
    public void testZipEmptyDirFilesOnly() {
       buildRule.executeTarget("zipEmptyDirFilesOnly");
    }

    @Test
    public void testZipEmptyCreate() {
        buildRule.executeTarget("zipEmptyCreate");
        assertThat(buildRule.getLog(), containsString("Note: creating empty"));
    }

    // Bugzilla Report 25513
    @Test
    public void testCompressionLevel() {
       buildRule.executeTarget("testCompressionLevel");
    }

    // Bugzilla Report 33412
    @Test
    public void testDefaultExcludesAndUpdate() throws IOException {
       buildRule.executeTarget("testDefaultExcludesAndUpdate");
        try (ZipFile f = new ZipFile(new File(buildRule.getProject().getProperty("output"), "test3.zip"))) {
            assertNotNull("ziptest~ should be included", f.getEntry("ziptest~"));
        }
    }

    @Test
    public void testFileResource() {
       buildRule.executeTarget("testFileResource");
    }

    @Test
    public void testNonFileResource() {
       buildRule.executeTarget("testNonFileResource");
    }

    @Test
    public void testTarFileSet() throws IOException {
        buildRule.executeTarget("testTarFileSet");
        try (org.apache.tools.zip.ZipFile zf = new org.apache.tools.zip.ZipFile(new File(buildRule.getProject().getProperty("output"), "test3.zip"))) {
            org.apache.tools.zip.ZipEntry ze = zf.getEntry("asf-logo.gif");
            assertEquals(UnixStat.FILE_FLAG | 0446, ze.getUnixMode());
        }
    }

    @Test
    public void testRewriteZeroPermissions() throws IOException {
       buildRule.executeTarget("rewriteZeroPermissions");
        try (org.apache.tools.zip.ZipFile zf = new org.apache.tools.zip.ZipFile(new File(buildRule.getProject().getProperty("output"), "test3.zip"))) {
            org.apache.tools.zip.ZipEntry ze = zf.getEntry("testdir/test.txt");
            assertEquals(UnixStat.FILE_FLAG | 0644, ze.getUnixMode());
        }
    }

    @Test
    public void testAcceptZeroPermissions() throws IOException {
       buildRule.executeTarget("acceptZeroPermissions");
        try (org.apache.tools.zip.ZipFile zf = new org.apache.tools.zip.ZipFile(new File(buildRule.getProject().getProperty("output"), "test3.zip"))) {
            org.apache.tools.zip.ZipEntry ze = zf.getEntry("testdir/test.txt");
            assertEquals(0000, ze.getUnixMode());
        }
    }

    @Test
    public void testForBugzilla34764() throws IOException {
       buildRule.executeTarget("testForBugzilla34764");
        try (org.apache.tools.zip.ZipFile zf = new org.apache.tools.zip.ZipFile(new File(buildRule.getProject().getProperty("output"), "test3.zip"))) {
            org.apache.tools.zip.ZipEntry ze = zf.getEntry("file1");
            assertEquals(UnixStat.FILE_FLAG | 0644, ze.getUnixMode());
        }
    }

    @Test
    public void testRegexpMapper() {
        buildRule.executeTarget("testRegexpMapper1");
        File testFile = new File(buildRule.getOutputDir(), "regexp.zip");
        long l = testFile.lastModified();
        buildRule.executeTarget("testRegexpMapper2");
        assertEquals(l, testFile.lastModified());
    }
}
