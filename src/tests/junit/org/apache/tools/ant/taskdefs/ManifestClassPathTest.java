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

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeNoException;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.io.IOException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.apache.tools.ant.util.regexp.RegexpMatcherFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Tests &lt;bm:manifestclasspath&gt;.
 */
public class ManifestClassPathTest {

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/manifestclasspath.xml");
    }

    @Test
    public void testBadDirectory() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("Jar's directory not found:");
        try {
            buildRule.executeTarget("test-bad-directory");
        } finally {
            // post-mortem
            assertNull(buildRule.getProject().getProperty("jar.classpath"));
        }
    }

    @Test
    public void testBadNoProperty() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("Missing 'property' attribute!");
        try {
            buildRule.executeTarget("test-bad-no-property");
        } finally {
            // post-mortem
            assertNull(buildRule.getProject().getProperty("jar.classpath"));
        }
    }

    @Test
    public void testBadPropertyExists() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("Property 'jar.classpath' already set!");
        try {
            buildRule.executeTarget("test-bad-property-exists");
        } finally {
            // post-mortem
            assertEquals("exists", buildRule.getProject().getProperty("jar.classpath"));
        }
    }

    @Test
    public void testBadNoJarfile() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("Missing 'jarfile' attribute!");
        try {
            buildRule.executeTarget("test-bad-no-jarfile");
        } finally {
            // post-mortem
            assertNull(buildRule.getProject().getProperty("jar.classpath"));
        }
    }

    @Test
    public void testBadNoClassPath() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("Missing nested <classpath>!");
        try {
            buildRule.executeTarget("test-bad-no-classpath");
        } finally {
            // post-mortem
            assertNull(buildRule.getProject().getProperty("jar.classpath"));
        }
    }

    @Test
    public void testParentLevel1() {
        buildRule.executeTarget("test-parent-level1");

        assertEquals(buildRule.getProject().getProperty("jar.classpath"), "dsp-core/ "
                + "dsp-pres/ "
                + "dsp-void/ "
                + "../generated/dsp-core/ "
                + "../generated/dsp-pres/ "
                + "../generated/dsp-void/ "
                + "../resources/dsp-core/ "
                + "../resources/dsp-pres/ "
                + "../resources/dsp-void/");
    }

    @Test
    public void testParentLevel2() {
        buildRule.executeTarget("test-parent-level2");

        assertEquals(buildRule.getProject().getProperty("jar.classpath"), "../dsp-core/ "
                + "../dsp-pres/ "
                + "../dsp-void/ "
                + "../../generated/dsp-core/ "
                + "../../generated/dsp-pres/ "
                + "../../generated/dsp-void/ "
                + "../../resources/dsp-core/ "
                + "../../resources/dsp-pres/ "
                + "../../resources/dsp-void/");
    }

    @Test
    public void testParentLevel2TooDeep() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("No suitable relative path from ");
        try {
            buildRule.executeTarget("test-parent-level2-too-deep");
        } finally {
            // post-mortem
            assertNull(buildRule.getProject().getProperty("jar.classpath"));
        }
    }

    @Test
    public void testPseudoTahoeRefid() {
        assumeTrue("No regexp matcher is present",
                RegexpMatcherFactory.regexpMatcherPresent(buildRule.getProject()));

        buildRule.executeTarget("test-pseudo-tahoe-refid");
        assertEquals(buildRule.getProject().getProperty("jar.classpath"), "classes/dsp-core/ "
                + "classes/dsp-pres/ "
                + "classes/dsp-void/ "
                + "generated/dsp-core/ "
                + "resources/dsp-core/ "
                + "resources/dsp-pres/");
    }

    @Test
    public void testPseudoTahoeNested() {
        assumeTrue("No regexp matcher is present",
                RegexpMatcherFactory.regexpMatcherPresent(buildRule.getProject()));

        buildRule.executeTarget("test-pseudo-tahoe-nested");
        assertEquals(buildRule.getProject().getProperty("jar.classpath"), "classes/dsp-core/ "
                + "classes/dsp-pres/ "
                + "classes/dsp-void/ "
                + "generated/dsp-core/ "
                + "resources/dsp-core/ "
                + "resources/dsp-pres/");
    }

    @Test
    public void testParentLevel2WithJars() {
        buildRule.executeTarget("test-parent-level2-with-jars");

        assertEquals(buildRule.getProject().getProperty("jar.classpath"), "../../lib/acme-core.jar "
                + "../../lib/acme-pres.jar "
                + "../dsp-core/ "
                + "../dsp-pres/ "
                + "../dsp-void/ "
                + "../../generated/dsp-core/ "
                + "../../generated/dsp-pres/ "
                + "../../generated/dsp-void/ "
                + "../../resources/dsp-core/ "
                + "../../resources/dsp-pres/ "
                + "../../resources/dsp-void/");
    }

    @Test
    public void testInternationalGerman() {
        buildRule.executeTarget("international-german");
        buildRule.executeTarget("run-two-jars");
        assertThat(buildRule.getLog(), containsString("beta alpha"));
    }

    @Test
    public void testInternationalHebrew() {
        assumeFalse("Test with hebrew path not attempted under Windows", Os.isFamily("windows"));
        buildRule.executeTarget("international-hebrew");
        buildRule.executeTarget("run-two-jars");
        assertThat(buildRule.getLog(), containsString("beta alpha"));
    }

    @Test
    public void testSameWindowsDrive() {
        assumeTrue("Test with drive letters only run on windows", Os.isFamily("windows"));
        buildRule.executeTarget("testSameDrive");
        assertEquals(buildRule.getProject().getProperty("cp"), "../a/b/x.jar");
    }

    @Test
    public void testDifferentWindowsDrive() {
        assumeTrue("Test with drive letters only run on windows", Os.isFamily("windows"));
        // the lines below try to find a drive name different than the one containing the temp dir
        // if the temp dir is C will try to use D
        // if the temp dir is on D or other will try to use C
        File tmpdir = new File(System.getProperty("java.io.tmpdir"));
        String driveLetter = "C";
        try {
            String tmpCanonicalPath = tmpdir.getCanonicalPath();
            driveLetter = tmpCanonicalPath.substring(0, 1).toUpperCase();
        } catch (IOException ioe) {
            System.out.println("exception happened getting canonical path of java.io.tmpdir : "
                    + ioe.getMessage());
        }
        String altDriveLetter = null;
        try {
            if ("C".equals(driveLetter)) {
                altDriveLetter = "D";
            } else {
                altDriveLetter = "C";
            }
            new File(altDriveLetter + ":/foo.txt").getCanonicalPath();
        } catch (IOException e) {
            assumeNoException("Drive " + altDriveLetter + ": doesn't exist or is not ready", e);
        }
        buildRule.getProject().setProperty("altDriveLetter", altDriveLetter);

        thrown.expect(BuildException.class);
        thrown.expectMessage("No suitable relative path from ");
        try {
            buildRule.executeTarget("testDifferentDrive");
        } finally {
            // post-mortem
            assertNull(buildRule.getProject().getProperty("cp"));
        }
    }
}
