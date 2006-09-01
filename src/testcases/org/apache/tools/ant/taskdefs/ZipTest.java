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

import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.zip.UnixStat;

/**
 */
public class ZipTest extends BuildFileTest {
    //instance variable to allow cleanup
    ZipFile zfPrefixAddsDir = null;
    public ZipTest(String name) {
        super(name);
    }

    public void setUp() {
        configureProject("src/etc/testcases/taskdefs/zip.xml");
    }

    public void test1() {
        expectBuildException("test1", "required argument not specified");
    }

    public void test2() {
        expectBuildException("test2", "required argument not specified");
    }

    public void test3() {
        expectBuildException("test3", "zip cannot include itself");
    }

    //    public void test4() {
    //        expectBuildException("test4", "zip cannot include itself");
    //    }

    public void tearDown() {
        try {
            if ( zfPrefixAddsDir != null) {
                zfPrefixAddsDir.close();
            }

        } catch (IOException e) {
            //ignored
        }
        executeTarget("cleanup");
    }

    public void test5() {
        executeTarget("test5");
    }


    public void test6() {
        executeTarget("test6");
    }


    public void test7() {
        executeTarget("test7");
    }

    public void test8() {
        executeTarget("test8");
    }

    public void testZipgroupfileset() throws IOException {
        executeTarget("testZipgroupfileset");

        ZipFile zipFile = new ZipFile(new File(getProjectDir(), "zipgroupfileset.zip"));

        assertTrue(zipFile.getEntry("ant.xml") != null);
        assertTrue(zipFile.getEntry("optional/jspc.xml") != null);
        assertTrue(zipFile.getEntry("zip/zipgroupfileset3.zip") != null);

        assertTrue(zipFile.getEntry("test6.mf") == null);
        assertTrue(zipFile.getEntry("test7.mf") == null);

        zipFile.close();
    }

    public void testUpdateNotNecessary() {
        executeTarget("testUpdateNotNecessary");
        assertEquals(-1, getLog().indexOf("Updating"));
    }

    public void testUpdateIsNecessary() {
        expectLogContaining("testUpdateIsNecessary", "Updating");
    }

    // Bugzilla Report 18403
    public void testPrefixAddsDir() throws IOException {
        executeTarget("testPrefixAddsDir");
        File archive = getProject().resolveFile("test3.zip");
        zfPrefixAddsDir = new ZipFile(archive);
        ZipEntry ze = zfPrefixAddsDir.getEntry("test/");
        assertNotNull("test/ has been added", ze);

    }

    // Bugzilla Report 19449
    public void testFilesOnlyDoesntCauseRecreate()
        throws InterruptedException {
        executeTarget("testFilesOnlyDoesntCauseRecreateSetup");
        long l = getProject().resolveFile("test3.zip").lastModified();
        Thread.sleep(3000);
        executeTarget("testFilesOnlyDoesntCauseRecreate");
        assertEquals(l, getProject().resolveFile("test3.zip").lastModified());
    }

    // Bugzilla Report 22865
    public void testEmptySkip() {
        executeTarget("testEmptySkip");
    }
    // Bugzilla Report 30365
    public void testZipEmptyDir() {
        executeTarget("zipEmptyDir");
    }
    // Bugzilla Report 40258
    public void testZipEmptyDirFilesOnly() {
        executeTarget("zipEmptyDirFilesOnly");
    }
    public void testZipEmptyCreate() {
        expectLogContaining("zipEmptyCreate", "Note: creating empty");
    }
    // Bugzilla Report 25513
    public void testCompressionLevel() {
        executeTarget("testCompressionLevel");
    }

    // Bugzilla Report 33412
    public void testDefaultExcludesAndUpdate() 
        throws ZipException, IOException {
        executeTarget("testDefaultExcludesAndUpdate");
        ZipFile f = null;
        try {
            f = new ZipFile(getProject().resolveFile("test3.zip"));
            assertNotNull("ziptest~ should be included",
                          f.getEntry("ziptest~"));
        } finally {
            if (f != null) {
                f.close();
            }
        }
    }

    public void testFileResource() {
        executeTarget("testFileResource");
    }

    public void testNonFileResource() {
        executeTarget("testNonFileResource");
    }

    public void testTarFileSet() throws IOException {
        executeTarget("testTarFileSet");
        org.apache.tools.zip.ZipFile zf = null;
        try {
            zf = new org.apache.tools.zip.ZipFile(getProject()
                                                  .resolveFile("test3.zip"));
            org.apache.tools.zip.ZipEntry ze = zf.getEntry("asf-logo.gif");
            assertEquals(UnixStat.FILE_FLAG | 0446, ze.getUnixMode());
        } finally {
            if (zf != null) {
                zf.close();
            }
        }
    }
}
