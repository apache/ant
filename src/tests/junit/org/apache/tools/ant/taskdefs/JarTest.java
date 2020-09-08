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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.FileUtilities;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.StreamUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

/**
 */
public class JarTest {

    @Rule
    public final BuildFileRule buildRule = new BuildFileRule();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private static String tempJar = "tmp.jar";
    private static String tempDir = "jartmp/";

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/jar.xml");
        buildRule.executeTarget("setUp");
    }

    /**
     * Expected failure due to required argument not specified
     */
    @Test(expected = BuildException.class)
    public void test1() {
        buildRule.executeTarget("test1");
        // TODO assert exception message
    }

    /**
     * Expected failure due to nonexistent manifest file
     */
    @Test(expected = BuildException.class)
    public void test2() {
        buildRule.executeTarget("test2");
        // TODO assert exception message
    }

    /**
     * Expected failure due to unrecognized whenempty attribute: format C: /y
     */
    @Test(expected = BuildException.class)
    public void test3() {
        buildRule.executeTarget("test3");
        // TODO assert exception message
    }

    private File getOutputDir() {
        return new File(buildRule.getProject().getProperty("output"));
    }

    @Test
    public void test4() {
        buildRule.executeTarget("test4");
        File jarFile = new File(getOutputDir(), tempJar);
        assertTrue(jarFile.exists());
    }

    @Test
    public void testNoRecreateWithoutUpdate() {
        testNoRecreate("test4");
    }

    @Test
    public void testNoRecreateWithUpdate() {
        testNoRecreate("testNoRecreateWithUpdate");
    }

    private void testNoRecreate(String secondTarget) {
        buildRule.executeTarget("test4");
        File jarFile = new File(getOutputDir(), tempJar);

        // move the modified date back a couple of seconds rather than delay the test on each run
        assumeTrue(jarFile.setLastModified(jarFile.lastModified()
                - FileUtils.getFileUtils().getFileTimestampGranularity() * 3));
        long jarModifiedDate = jarFile.lastModified();

        buildRule.executeTarget(secondTarget);
        assertEquals("jar has not been recreated in " + secondTarget,
                     jarModifiedDate, jarFile.lastModified());
    }

    @Test
    public void testRecreateWithoutUpdateAdditionalFiles() {
        testRecreate("test4", "testRecreateWithoutUpdateAdditionalFiles");
    }

    @Test
    public void testRecreateWithUpdateAdditionalFiles() {
        testRecreate("test4", "testRecreateWithUpdateAdditionalFiles");
    }

    @Test
    public void testRecreateWithoutUpdateNewerFile() {
        testRecreate("testRecreateNewerFileSetup", "testRecreateWithoutUpdateNewerFile");
    }

    @Test
    public void testRecreateWithUpdateNewerFile() {
        testRecreate("testRecreateNewerFileSetup", "testRecreateWithUpdateNewerFile");
    }

    private void testRecreate(String firstTarget, String secondTarget) {
        //Move the modified date on all input back a couple of seconds rather then delay the test to achieve a similar effect
        FileUtilities.rollbackTimestamps(buildRule.getProject().getBaseDir(), 5);

        buildRule.executeTarget(firstTarget);
        File jarFile = new File(getOutputDir(), tempJar);

        //Move the modified date back a couple of seconds rather then delay the test to achieve a similar effect
        FileUtilities.rollbackTimestamps(buildRule.getOutputDir(), 5);

        long jarModifiedDate = jarFile.lastModified();
        buildRule.executeTarget(secondTarget);
        jarFile = new File(getOutputDir(), tempJar);
        assertTrue("jar has been recreated in " + secondTarget, jarModifiedDate < jarFile.lastModified());
    }

    @Test
    public void testManifestStaysIntact() throws IOException, ManifestException {
        buildRule.executeTarget("testManifestStaysIntact");

        Manifest mf1;
        try (FileReader r1 = new FileReader(new File(getOutputDir(), tempDir + "manifest"))) {
            mf1 = new Manifest(r1);
        }

        Manifest mf2;
        try (FileReader r2 = new FileReader(new File(getOutputDir(), tempDir
                + "META-INF/MANIFEST.MF"))) {
            mf2 = new Manifest(r2);
        }

        assertEquals(mf1, mf2);
    }

    @Test
    public void testNoRecreateBasedirExcludesWithUpdate() {
        testNoRecreate("testNoRecreateBasedirExcludesWithUpdate");
    }

    @Test
    public void testNoRecreateBasedirExcludesWithoutUpdate() {
        testNoRecreate("testNoRecreateBasedirExcludesWithoutUpdate");
    }

    @Test
    public void testNoRecreateZipfilesetExcludesWithUpdate() {
        testNoRecreate("testNoRecreateZipfilesetExcludesWithUpdate");
    }

    @Test
    public void testNoRecreateZipfilesetExcludesWithoutUpdate() {
        testNoRecreate("testNoRecreateZipfilesetExcludesWithoutUpdate");
    }

    @Test
    public void testRecreateZipfilesetWithoutUpdateAdditionalFiles() {
        testRecreate("test4", "testRecreateZipfilesetWithoutUpdateAdditionalFiles");
    }

    @Test
    public void testRecreateZipfilesetWithUpdateAdditionalFiles() {
        testRecreate("test4", "testRecreateZipfilesetWithUpdateAdditionalFiles");
    }

    @Test
    public void testRecreateZipfilesetWithoutUpdateNewerFile() {
        testRecreate("testRecreateNewerFileSetup", "testRecreateZipfilesetWithoutUpdateNewerFile");
    }

    @Test
    public void testRecreateZipfilesetWithUpdateNewerFile() {
        testRecreate("testRecreateNewerFileSetup", "testRecreateZipfilesetWithUpdateNewerFile");
    }

    @Test
    public void testCreateWithEmptyFileset() {
        buildRule.executeTarget("testCreateWithEmptyFilesetSetUp");
        buildRule.executeTarget("testCreateWithEmptyFileset");
        buildRule.executeTarget("testCreateWithEmptyFileset");
    }

    @Test
    public void testUpdateIfOnlyManifestHasChanged() {
        buildRule.executeTarget("testUpdateIfOnlyManifestHasChanged");
        File jarXml = new File(getOutputDir(), tempDir + "jar.xml");
        assertTrue(jarXml.exists());
    }

    // bugzilla report 10262
    @Test
    public void testNoDuplicateIndex() throws IOException {
        buildRule.executeTarget("testIndexTests");
        try (ZipFile archive = new ZipFile(new File(getOutputDir(), tempJar))) {
            assertEquals(1, StreamUtils.enumerationAsStream(archive.entries())
                    .filter(ze -> ze.getName().equals("META-INF/INDEX.LIST")).count());
        }
    }

    // bugzilla report 16972
    @Test
    public void testRootFilesInIndex() throws IOException {
        buildRule.executeTarget("testIndexTests");
        try (ZipFile archive = new ZipFile(new File(getOutputDir(), tempJar))) {
            ZipEntry ze = archive.getEntry("META-INF/INDEX.LIST");
            InputStream is = archive.getInputStream(ze);
            BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            boolean foundArchive = false;
            boolean foundSub = false;
            boolean foundSubFoo = false;
            boolean foundFoo = false;

            String line = r.readLine();
            while (line != null) {
                switch (line) {
                    case "tmp.jar":
                        foundArchive = true;
                        break;
                    case "foo":
                        foundFoo = true;
                        break;
                    case "sub":
                        foundSub = true;
                        break;
                    case "sub/foo":
                        foundSubFoo = true;
                        break;
                }
                line = r.readLine();
            }

            assertTrue(foundArchive);
            assertTrue(foundSub);
            assertFalse(foundSubFoo);
            assertTrue(foundFoo);
        }
    }
    @Test
    public void testManifestOnlyJar() {
        buildRule.executeTarget("testManifestOnlyJar");
        assertThat(buildRule.getLog(), containsString("Building MANIFEST-only jar: "));
        File manifestFile = new File(getOutputDir(), tempDir + "META-INF" + File.separator + "MANIFEST.MF");
        assertTrue(manifestFile.exists());
    }

    @Test
    public void testIndexJarsPlusJarMarker() throws IOException {
        buildRule.executeTarget("testIndexJarsPlusJarMarker");
        try (ZipFile archive = new ZipFile(new File(getOutputDir(), tempJar + "2"))) {
            ZipEntry ze = archive.getEntry("META-INF/INDEX.LIST");
            InputStream is = archive.getInputStream(ze);
            BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            // tmp.jar
            boolean foundTmp = false;
            boolean foundA = false;
            boolean foundAB = false;
            boolean foundABC = false;

            // tmp2.jar
            boolean foundTmp2 = false;
            boolean foundD = false;
            boolean foundDE = false;
            boolean foundDEF = false;

            String line = r.readLine();
            while (line != null) {
                switch (line) {
                    case "tmp.jar":
                        foundTmp = true;
                        break;
                    case "a":
                        foundA = true;
                        break;
                    case "a/b":
                        foundAB = true;
                        break;
                    case "a/b/c":
                        foundABC = true;
                        break;
                    case "tmp.jar2":
                        foundTmp2 = true;
                        break;
                    case "d":
                        foundD = true;
                        break;
                    case "d/e":
                        foundDE = true;
                        break;
                    case "d/e/f":
                        foundDEF = true;
                        break;
                }
                line = r.readLine();
            }

            assertTrue(foundTmp);
            assertTrue(foundA);
            assertTrue(foundAB);
            assertTrue(foundABC);
            assertTrue(foundTmp2);
            assertTrue(foundD);
            assertTrue(foundDE);
            assertTrue(foundDEF);
        }
    }

    @Test
    public void testIndexJarsPlusJarMarkerWithMapping() throws IOException {
        buildRule.executeTarget("testIndexJarsPlusJarMarkerWithMapping");
        try (ZipFile archive = new ZipFile(new File(getOutputDir(), tempJar + "2"))) {
            ZipEntry ze = archive.getEntry("META-INF/INDEX.LIST");
            InputStream is = archive.getInputStream(ze);
            BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            // tmp.jar
            boolean foundTmp = false;
            boolean foundA = false;
            boolean foundAB = false;
            boolean foundABC = false;

            // tmp2.jar
            boolean foundTmp2 = false;
            boolean foundD = false;
            boolean foundDE = false;
            boolean foundDEF = false;

            String line = r.readLine();
            while (line != null) {
                System.out.println("line = " + line);
                switch (line) {
                    case "foo/tmp.jar":
                        foundTmp = true;
                        break;
                    case "a":
                        foundA = true;
                        break;
                    case "a/b":
                        foundAB = true;
                        break;
                    case "a/b/c":
                        foundABC = true;
                        break;
                    case "tmp.jar2":
                        foundTmp2 = true;
                        break;
                    case "d":
                        foundD = true;
                        break;
                    case "d/e":
                        foundDE = true;
                        break;
                    case "d/e/f":
                        foundDEF = true;
                        break;
                }
                line = r.readLine();
            }

            assertTrue(foundTmp);
            assertTrue(foundA);
            assertTrue(foundAB);
            assertTrue(foundABC);
            assertTrue(foundTmp2);
            assertTrue(foundD);
            assertTrue(foundDE);
            assertTrue(foundDEF);
        }
    }

    @Test
    public void testNoVersionInfoFail() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("No Implementation-Title set.");
        buildRule.executeTarget("testNoVersionInfoFail");
    }

    @Test
    public void testNoVersionInfoIgnore() {
        buildRule.executeTarget("testNoVersionInfoIgnore");
        assertThat(buildRule.getFullLog(), containsString("No Implementation-Title set."));
        assertThat(buildRule.getFullLog(), containsString("No Implementation-Version set."));
        assertThat(buildRule.getFullLog(), containsString("No Implementation-Vendor set."));
    }

    @Test
    public void testNoVersionInfoWarn() {
        buildRule.executeTarget("testNoVersionInfoWarn");
        assertThat(buildRule.getLog(), containsString("No Implementation-Title set."));
        assertThat(buildRule.getLog(), containsString("No Implementation-Version set."));
        assertThat(buildRule.getLog(), containsString("No Implementation-Vendor set."));
    }

    @Test
    public void testNoVersionInfoNoStrict() {
        buildRule.executeTarget("testNoVersionInfoNoStrict");
        assertThat(buildRule.getLog(), not(containsString("No Implementation-Title set.")));
        assertThat(buildRule.getLog(), not(containsString("No Implementation-Version set.")));
        assertThat(buildRule.getLog(), not(containsString("No Implementation-Vendor set.")));
    }

    @Test
    public void testHasVersionInfo() {
        buildRule.executeTarget("testHasVersionInfo");
        assertThat(buildRule.getLog(), not(containsString("No Implementation-Title set.")));
        assertThat(buildRule.getLog(), not(containsString("No Implementation-Version set.")));
        assertThat(buildRule.getLog(), not(containsString("No Implementation-Vendor set.")));
    }

}
