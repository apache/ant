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

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
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

import static org.apache.tools.ant.AntAssert.assertContains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
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
			fail("BuildException expected: zip cannot include itself");
		} catch (BuildException ex) {
			//TODO assert value
		}
    }

    @Test
    @Ignore("Previously commented out")
    public void test4() {
        try {
			buildRule.executeTarget("test4");
			fail("BuildException expected: zip cannot include itself");
		} catch (BuildException ex) {
			//TODO assert value
		}
    }

    @After
    public void tearDown() {
        try {
            if ( zfPrefixAddsDir != null) {
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

        ZipFile zipFile = new ZipFile(new File(buildRule.getProject().getProperty("output"), "zipgroupfileset.zip"));

        assertTrue(zipFile.getEntry("ant.xml") != null);
        assertTrue(zipFile.getEntry("optional/jspc.xml") != null);
        assertTrue(zipFile.getEntry("zip/zipgroupfileset3.zip") != null);

        assertTrue(zipFile.getEntry("test6.mf") == null);
        assertTrue(zipFile.getEntry("test7.mf") == null);

        zipFile.close();
    }

    @Test
    public void testUpdateNotNecessary() {
       buildRule.executeTarget("testUpdateNotNecessary");
        assertEquals(-1, buildRule.getLog().indexOf("Updating"));
    }

    @Test
    public void testUpdateIsNecessary() {
        buildRule.executeTarget("testUpdateIsNecessary");
		assertContains("Updating", buildRule.getLog());
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
		assertContains("Note: creating empty", buildRule.getLog());
    }
    // Bugzilla Report 25513
    @Test
    public void testCompressionLevel() {
       buildRule.executeTarget("testCompressionLevel");
    }

    // Bugzilla Report 33412
    @Test
    public void testDefaultExcludesAndUpdate()
        throws ZipException, IOException {
       buildRule.executeTarget("testDefaultExcludesAndUpdate");
        ZipFile f = null;
        try {
            f = new ZipFile(new File(buildRule.getProject().getProperty("output"), "test3.zip"));
            assertNotNull("ziptest~ should be included",
                          f.getEntry("ziptest~"));
        } finally {
            if (f != null) {
                f.close();
            }
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
        org.apache.tools.zip.ZipFile zf = null;
        try {
            zf = new org.apache.tools.zip.ZipFile(new File(buildRule.getProject().getProperty("output"), "test3.zip"));
            org.apache.tools.zip.ZipEntry ze = zf.getEntry("asf-logo.gif");
            assertEquals(UnixStat.FILE_FLAG | 0446, ze.getUnixMode());
        } finally {
            if (zf != null) {
                zf.close();
            }
        }
    }

    @Test
    public void testRewriteZeroPermissions() throws IOException {
       buildRule.executeTarget("rewriteZeroPermissions");
        org.apache.tools.zip.ZipFile zf = null;
        try {
            zf = new org.apache.tools.zip.ZipFile(new File(buildRule.getProject().getProperty("output"), "test3.zip"));
            org.apache.tools.zip.ZipEntry ze = zf.getEntry("testdir/test.txt");
            assertEquals(UnixStat.FILE_FLAG | 0644, ze.getUnixMode());
        } finally {
            if (zf != null) {
                zf.close();
            }
        }
    }

    @Test
    public void testAcceptZeroPermissions() throws IOException {
       buildRule.executeTarget("acceptZeroPermissions");
        org.apache.tools.zip.ZipFile zf = null;
        try {
            zf = new org.apache.tools.zip.ZipFile(new File(buildRule.getProject().getProperty("output"), "test3.zip"));
            org.apache.tools.zip.ZipEntry ze = zf.getEntry("testdir/test.txt");
            assertEquals(0000, ze.getUnixMode());
        } finally {
            if (zf != null) {
                zf.close();
            }
        }
    }

    @Test
    public void testForBugzilla34764() throws IOException {
       buildRule.executeTarget("testForBugzilla34764");
        org.apache.tools.zip.ZipFile zf = null;
        try {
            zf = new org.apache.tools.zip.ZipFile(new File(buildRule.getProject().getProperty("output"), "test3.zip"));
            org.apache.tools.zip.ZipEntry ze = zf.getEntry("file1");
            assertEquals(UnixStat.FILE_FLAG | 0644, ze.getUnixMode());
        } finally {
            if (zf != null) {
                zf.close();
            }
        }
    }

}
