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


import org.apache.tools.ant.taskdefs.condition.Os;
import org.apache.tools.ant.util.JavaEnvUtils;
import org.apache.tools.ant.BuildFileTest;

/**
 * Tests &lt;bm:manifestclasspath&gt;.
 */
public class ManifestClassPathTest
             extends BuildFileTest {

    public void setUp() {
        configureProject("src/etc/testcases/taskdefs/manifestclasspath.xml");
    }

    public void testBadDirectory() {
        expectBuildExceptionContaining("test-bad-directory", "bad-jar-dir",
                                       "Jar's directory not found:");
        assertPropertyUnset("jar.classpath");
    }

    public void testBadNoProperty() {
        expectBuildExceptionContaining("test-bad-no-property", "no-property",
                                       "Missing 'property' attribute!");
        assertPropertyUnset("jar.classpath");
    }

    public void testBadPropertyExists() {
        expectBuildExceptionContaining("test-bad-property-exists",
            "property-exits", "Property 'jar.classpath' already set!");
        assertPropertyEquals("jar.classpath", "exists");
    }

    public void testBadNoJarfile() {
        expectBuildExceptionContaining("test-bad-no-jarfile", "no-jarfile",
                                       "Missing 'jarfile' attribute!");
        assertPropertyUnset("jar.classpath");
    }

    public void testBadNoClassPath() {
        expectBuildExceptionContaining("test-bad-no-classpath", "no-classpath",
                                       "Missing nested <classpath>!");
        assertPropertyUnset("jar.classpath");
    }

    public void testParentLevel1() {
        executeTarget("test-parent-level1");

        assertPropertyEquals("jar.classpath", "dsp-core/ " +
                                              "dsp-pres/ " +
                                              "dsp-void/ " +
                                              "../generated/dsp-core/ " +
                                              "../generated/dsp-pres/ " +
                                              "../generated/dsp-void/ " +
                                              "../resources/dsp-core/ " +
                                              "../resources/dsp-pres/ " +
                                              "../resources/dsp-void/");
    }

    public void testParentLevel2() {
        executeTarget("test-parent-level2");

        assertPropertyEquals("jar.classpath", "../dsp-core/ " +
                                              "../dsp-pres/ " +
                                              "../dsp-void/ " +
                                              "../../generated/dsp-core/ " +
                                              "../../generated/dsp-pres/ " +
                                              "../../generated/dsp-void/ " +
                                              "../../resources/dsp-core/ " +
                                              "../../resources/dsp-pres/ " +
                                              "../../resources/dsp-void/");
    }

    public void testParentLevel2TooDeep() {
        expectBuildExceptionContaining("test-parent-level2-too-deep", "nopath",
                                       "No suitable relative path from ");
        assertPropertyUnset("jar.classpath");
    }

    public void testPseudoTahoeRefid() {
        executeTarget("test-pseudo-tahoe-refid");

        assertPropertyEquals("jar.classpath", "classes/dsp-core/ " +
                                              "classes/dsp-pres/ " +
                                              "classes/dsp-void/ " +
                                              "generated/dsp-core/ " +
                                              "resources/dsp-core/ " +
                                              "resources/dsp-pres/");
    }

    public void testPseudoTahoeNested() {
        executeTarget("test-pseudo-tahoe-nested");

        assertPropertyEquals("jar.classpath", "classes/dsp-core/ " +
                                              "classes/dsp-pres/ " +
                                              "classes/dsp-void/ " +
                                              "generated/dsp-core/ " +
                                              "resources/dsp-core/ " +
                                              "resources/dsp-pres/");
    }

    public void testParentLevel2WithJars() {
        executeTarget("test-parent-level2-with-jars");

        assertPropertyEquals("jar.classpath", "../../lib/acme-core.jar " +
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
    public void testInternationalGerman() {
        if (!JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_1_4))
        {
            System.out.println("Test with international characters skipped under pre 1.4 jvm.");
            return;
        }
        executeTarget("international-german");
        expectLogContaining("run-two-jars", "beta alpha");
            
    }
    public void testInternationalHebrew() {
        if (!JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_1_4))        {
            System.out.println("Test with international characters skipped under pre 1.4 jvm.");
            return;
        }
        if (!Os.isFamily("windows")) {
            executeTarget("international-hebrew");
            expectLogContaining("run-two-jars", "beta alpha");
        } else {
            System.out.println("Test with hebrew path not attempted under Windows");
        }

    }

} // END class ManifestClassPathTest

