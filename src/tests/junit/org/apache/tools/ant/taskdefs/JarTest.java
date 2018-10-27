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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.FileUtilities;
import org.apache.tools.ant.util.FileUtils;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.apache.tools.ant.AntAssert.assertContains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 */
public class JarTest {
    
    @Rule
    public final BuildFileRule buildRule = new BuildFileRule();

    private static String tempJar = "tmp.jar";
    private static String tempDir = "jartmp/";
    private Reader r1, r2;

    
    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/jar.xml");
        buildRule.executeTarget("setUp");
    }

    @After
    public void tearDown() {
        if (r1 != null) {
            try {
                r1.close();
            } catch (IOException e) {
            }
        }
        if (r2 != null) {
            try {
                r2.close();
            } catch (IOException e) {
            }
        }
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
			fail("BuildException expected: manifest file does not exist");
		} catch (BuildException ex) {
			//TODO assert value
		}
    }

    @Test
    public void test3() {
        try {
			buildRule.executeTarget("test3");
			fail("BuildException expected: Unrecognized whenempty attribute: format C: /y");
		} catch (BuildException ex) {
			//TODO assert value
		}
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
        Assume.assumeTrue(jarFile.setLastModified(jarFile.lastModified()
                - (FileUtils.getFileUtils().getFileTimestampGranularity() * 3)));
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
        testRecreate("testRecreateNewerFileSetup",
                     "testRecreateWithoutUpdateNewerFile");
    }

    @Test
    public void testRecreateWithUpdateNewerFile() {
        testRecreate("testRecreateNewerFileSetup",
                     "testRecreateWithUpdateNewerFile");
    }

    private void testRecreate(String firstTarget, String secondTarget) {
        //Move the modified date on all input back a couple of seconds rather then delay the test to achieve a similar effect
        FileUtilities.rollbackTimetamps(buildRule.getProject().getBaseDir(), 5);

        buildRule.executeTarget(firstTarget);
        File jarFile = new File(getOutputDir(), tempJar);

        //Move the modified date back a couple of seconds rather then delay the test to achieve a similar effect
        FileUtilities.rollbackTimetamps(buildRule.getOutputDir(), 5);

        long jarModifiedDate = jarFile.lastModified();
        buildRule.executeTarget(secondTarget);
        jarFile = new File(getOutputDir(), tempJar);
        assertTrue("jar has been recreated in " + secondTarget,
                   jarModifiedDate < jarFile.lastModified());
    }

    @Test
    public void testManifestStaysIntact()
        throws IOException, ManifestException {
        buildRule.executeTarget("testManifestStaysIntact");

        r1 = new FileReader(new File(getOutputDir(),
                            tempDir + "manifest"));
        r2 = new FileReader(new File(getOutputDir(),
                tempDir + "META-INF/MANIFEST.MF"));

        Manifest mf1 = new Manifest(r1);
        Manifest mf2 = new Manifest(r2);
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
        testRecreate("test4",
                     "testRecreateZipfilesetWithoutUpdateAdditionalFiles");
    }

    @Test
    public void testRecreateZipfilesetWithUpdateAdditionalFiles() {
        testRecreate("test4",
                     "testRecreateZipfilesetWithUpdateAdditionalFiles");
    }

    @Test
    public void testRecreateZipfilesetWithoutUpdateNewerFile() {
        testRecreate("testRecreateNewerFileSetup",
                     "testRecreateZipfilesetWithoutUpdateNewerFile");
    }

    @Test
    public void testRecreateZipfilesetWithUpdateNewerFile() {
        testRecreate("testRecreateNewerFileSetup",
                     "testRecreateZipfilesetWithUpdateNewerFile");
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
        ZipFile archive = null;
        try {
            buildRule.executeTarget("testIndexTests");
            archive = new ZipFile(new File(getOutputDir(), tempJar));
            Enumeration e = archive.entries();
            int numberOfIndexLists = 0;
            while (e.hasMoreElements()) {
                ZipEntry ze = (ZipEntry) e.nextElement();
                if (ze.getName().equals("META-INF/INDEX.LIST")) {
                    numberOfIndexLists++;
                }
            }
            assertEquals(1, numberOfIndexLists);
        } finally {
            if (archive != null) {
                archive.close();
            }
        }
    }

    // bugzilla report 16972
    @Test
    public void testRootFilesInIndex() throws IOException {
        ZipFile archive = null;
        try {
            buildRule.executeTarget("testIndexTests");
            archive = new ZipFile(new File(getOutputDir(), tempJar));
            ZipEntry ze = archive.getEntry("META-INF/INDEX.LIST");
            InputStream is = archive.getInputStream(ze);
            BufferedReader r = new BufferedReader(new InputStreamReader(is,
                                                                        "UTF8"));
            boolean foundSub = false;
            boolean foundSubFoo = false;
            boolean foundFoo = false;

            String line = r.readLine();
            while (line != null) {
                if (line.equals("foo")) {
                    foundFoo = true;
                } else if (line.equals("sub")) {
                    foundSub = true;
                } else if (line.equals("sub/foo")) {
                    foundSubFoo = true;
                }
                line = r.readLine();
            }

            assertTrue(foundSub);
            assertTrue(!foundSubFoo);
            assertTrue(foundFoo);
        } finally {
            if (archive != null) {
                archive.close();
            }
        }
    }
    @Test
    public void testManifestOnlyJar() {

        buildRule.executeTarget("testManifestOnlyJar");
        assertContains("Building MANIFEST-only jar: ", buildRule.getLog());
        File manifestFile = new File(getOutputDir(), tempDir + "META-INF" + File.separator + "MANIFEST.MF");
        assertTrue(manifestFile.exists());
    }

    @Test
    public void testIndexJarsPlusJarMarker() {
        buildRule.executeTarget("testIndexJarsPlusJarMarker");
    }
    
    @Test
    public void testNoVersionInfoFail() {
        try {
			buildRule.executeTarget("testNoVersionInfoFail");
			fail("BuildException expected: Manifest Implemention information missing.");
		} catch (BuildException ex) {
			assertContains("No Implementation-Title set.", ex.getMessage());
		}
    }
    
    @Test
    public void testNoVersionInfoIgnore() {
        buildRule.executeTarget("testNoVersionInfoIgnore");
        assertTrue(buildRule.getFullLog().indexOf("No Implementation-Title set.") > -1 );
        assertTrue(buildRule.getFullLog().indexOf("No Implementation-Version set.") > -1 );
        assertTrue(buildRule.getFullLog().indexOf("No Implementation-Vendor set.") > -1 );
    }

    @Test
    public void testNoVersionInfoWarn() {
        buildRule.executeTarget("testNoVersionInfoWarn");
        assertTrue(buildRule.getLog().indexOf("No Implementation-Title set.") > -1 );
        assertTrue(buildRule.getLog().indexOf("No Implementation-Version set.") > -1 );
        assertTrue(buildRule.getLog().indexOf("No Implementation-Vendor set.") > -1 );
    }

    @Test
    public void testNoVersionInfoNoStrict() {
        buildRule.executeTarget("testNoVersionInfoNoStrict");
        assertFalse(buildRule.getLog().indexOf("No Implementation-Title set.") > -1 );
        assertFalse(buildRule.getLog().indexOf("No Implementation-Version set.") > -1 );
        assertFalse(buildRule.getLog().indexOf("No Implementation-Vendor set.") > -1 );
    }
    
    @Test
    public void testHasVersionInfo() {
        buildRule.executeTarget("testHasVersionInfo");
        assertFalse(buildRule.getLog().indexOf("No Implementation-Title set.") > -1 );
        assertFalse(buildRule.getLog().indexOf("No Implementation-Version set.") > -1 );
        assertFalse(buildRule.getLog().indexOf("No Implementation-Vendor set.") > -1 );
    }
    
}
