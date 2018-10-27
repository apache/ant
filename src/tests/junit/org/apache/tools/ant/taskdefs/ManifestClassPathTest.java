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


import static org.apache.tools.ant.AntAssert.assertContains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.apache.tools.ant.util.regexp.RegexpMatcherFactory;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Tests &lt;bm:manifestclasspath&gt;.
 */
public class ManifestClassPathTest {
	
	@Rule
	public BuildFileRule buildRule = new BuildFileRule();

	@Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/manifestclasspath.xml");
    }

	@Test
    public void testBadDirectory() {
		try {
			buildRule.executeTarget("test-bad-directory");
			fail("Build exception should have been thrown on bad directory");
		} catch (BuildException ex) {
			assertContains("Jar's directory not found:", ex.getMessage());
		}
        assertNull(buildRule.getProject().getProperty("jar.classpath"));
    }

	@Test
    public void testBadNoProperty() {
        try {
			buildRule.executeTarget("test-bad-no-property");
			fail("Build exception should have been thrown on no property");
		} catch (BuildException ex) {
			assertContains("Missing 'property' attribute!", ex.getMessage());
		}
        assertNull(buildRule.getProject().getProperty("jar.classpath"));
    }

	@Test
    public void testBadPropertyExists() {
        try {
			buildRule.executeTarget("test-bad-property-exists");
			fail("Build exception should have been thrown on bad property");
		} catch (BuildException ex) {
			assertContains("Property 'jar.classpath' already set!", ex.getMessage());
		}
        assertEquals(buildRule.getProject().getProperty("jar.classpath"), "exists");
    }

	@Test
    public void testBadNoJarfile() {
		try {
			buildRule.executeTarget("test-bad-no-jarfile");
			fail("Build exception should have been thrown on bad jar file");
		} catch (BuildException ex) {
			assertContains("Missing 'jarfile' attribute!", ex.getMessage());
		}
        assertNull(buildRule.getProject().getProperty("jar.classpath"));
    }

	@Test
    public void testBadNoClassPath() {
		try {
			buildRule.executeTarget("test-bad-no-classpath");
			fail("Build exception should have been thrown on no classpath");
		} catch (BuildException ex) {
			assertContains("Missing nested <classpath>!", ex.getMessage());
		}
        assertNull(buildRule.getProject().getProperty("jar.classpath"));
    }

	@Test
    public void testParentLevel1() {
        buildRule.executeTarget("test-parent-level1");

        assertEquals(buildRule.getProject().getProperty("jar.classpath"), "dsp-core/ " +
                                              "dsp-pres/ " +
                                              "dsp-void/ " +
                                              "../generated/dsp-core/ " +
                                              "../generated/dsp-pres/ " +
                                              "../generated/dsp-void/ " +
                                              "../resources/dsp-core/ " +
                                              "../resources/dsp-pres/ " +
                                              "../resources/dsp-void/");
    }

	@Test
    public void testParentLevel2() {
        buildRule.executeTarget("test-parent-level2");

        assertEquals(buildRule.getProject().getProperty("jar.classpath"), "../dsp-core/ " +
                                              "../dsp-pres/ " +
                                              "../dsp-void/ " +
                                              "../../generated/dsp-core/ " +
                                              "../../generated/dsp-pres/ " +
                                              "../../generated/dsp-void/ " +
                                              "../../resources/dsp-core/ " +
                                              "../../resources/dsp-pres/ " +
                                              "../../resources/dsp-void/");
    }

	@Test
    public void testParentLevel2TooDeep() {
		try {
			buildRule.executeTarget("test-parent-level2-too-deep");
			fail("Build exception should have been thrown on no suitable path");
		} catch (BuildException ex) {
			assertContains("No suitable relative path from ", ex.getMessage());
		}
        assertNull(buildRule.getProject().getProperty("jar.classpath"));
    }

    @Test
    public void testPseudoTahoeRefid() {
        Assume.assumeTrue("No regexp matcher is present", RegexpMatcherFactory.regexpMatcherPresent(buildRule.getProject()));
        
        buildRule.executeTarget("test-pseudo-tahoe-refid");
        assertEquals(buildRule.getProject().getProperty("jar.classpath"), "classes/dsp-core/ " +
                                              "classes/dsp-pres/ " +
                                              "classes/dsp-void/ " +
                                              "generated/dsp-core/ " +
                                              "resources/dsp-core/ " +
                                              "resources/dsp-pres/");
    }

    @Test
    public void testPseudoTahoeNested() {
    	Assume.assumeTrue("No regexp matcher is present", RegexpMatcherFactory.regexpMatcherPresent(buildRule.getProject()));
        
    	buildRule.executeTarget("test-pseudo-tahoe-nested");
        assertEquals(buildRule.getProject().getProperty("jar.classpath"), "classes/dsp-core/ " +
                                              "classes/dsp-pres/ " +
                                              "classes/dsp-void/ " +
                                              "generated/dsp-core/ " +
                                              "resources/dsp-core/ " +
                                              "resources/dsp-pres/");
    }

    @Test
    public void testParentLevel2WithJars() {
        buildRule.executeTarget("test-parent-level2-with-jars");

        assertEquals(buildRule.getProject().getProperty("jar.classpath"), "../../lib/acme-core.jar " +
                                              "../../lib/acme-pres.jar " +
                                              "../dsp-core/ " +
                                              "../dsp-pres/ " +
                                              "../dsp-void/ " +
                                              "../../generated/dsp-core/ " +
                                              "../../generated/dsp-pres/ " +
                                              "../../generated/dsp-void/ " +
                                              "../../resources/dsp-core/ " +
                                              "../../resources/dsp-pres/ " +
                                              "../../resources/dsp-void/");
    }
    
    @Test
    public void testInternationalGerman() {
        buildRule.executeTarget("international-german");
        buildRule.executeTarget("run-two-jars");
        assertContains("beta alpha", buildRule.getLog());

    }
    
    @Test
    public void testInternationalHebrew() {
        Assume.assumeFalse("Test with hebrew path not attempted under Windows", Os.isFamily("windows")); 
        buildRule.executeTarget("international-hebrew");
        buildRule.executeTarget("run-two-jars");
        assertContains("beta alpha", buildRule.getLog());
    }

    @Test
    public void testSameWindowsDrive() {
        Assume.assumeTrue("Test with drive letters only run on windows", Os.isFamily("windows"));
        buildRule.executeTarget("testSameDrive");
        assertEquals(buildRule.getProject().getProperty("cp"), "../a/b/x.jar");
    }

    @Test
    public void testDifferentWindowsDrive() {
    	Assume.assumeTrue("Test with drive letters only run on windows", Os.isFamily("windows"));
        // the lines below try to find a drive name different than the one containing the temp dir
        // if the temp dir is C will try to use D
        // if the temp dir is on D or other will try to use C
        File tmpdir = new File(System.getProperty("java.io.tmpdir"));
        String driveLetter = "C";
        try {
            String tmpCanonicalPath = tmpdir.getCanonicalPath();
            driveLetter = tmpCanonicalPath.substring(0, 1).toUpperCase();
        } catch (IOException ioe) {
            System.out.println("exception happened getting canonical path of java.io.tmpdir : " + ioe.getMessage());
        }
        String altDriveLetter = null;
        try {
            if ("C".equals(driveLetter)) {
                altDriveLetter = "D";
            } else {
                altDriveLetter = "C";
            }
            new java.io.File(altDriveLetter + ":/foo.txt").getCanonicalPath();
        } catch (java.io.IOException e) {
        	Assume.assumeNoException("Drive " + altDriveLetter + ": doesn't exist or is not ready", e);
        }
        buildRule.getProject().setProperty("altDriveLetter", altDriveLetter);
        
        try {
			buildRule.executeTarget("testDifferentDrive");
			fail("Build exception should have been thrown on no alternative drive");
		} catch (BuildException ex) {
			assertContains("No suitable relative path from ", ex.getMessage());
		}
        
        assertNull(buildRule.getProject().getProperty("cp"));
    }
} // END class ManifestClassPathTest
