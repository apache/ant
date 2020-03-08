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

package org.apache.tools.ant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;

import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.JavaEnvUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Test case for ant class loader
 *
 */
public class AntClassLoaderTest {

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private AntClassLoader loader;

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/core/antclassloader.xml");
        buildRule.executeTarget("setUp");
    }

    @After
    public void tearDown() {
        if (loader != null) {
            loader.cleanup();
        }
    }

    //test inspired by bug report 37085
    @Test
    public void testJarWithManifestInDirWithSpace() {
        String mainjarstring = buildRule.getProject().getProperty("main.jar");
        String extjarstring = buildRule.getProject().getProperty("ext.jar");
        Path myPath = new Path(buildRule.getProject());
        myPath.setLocation(new File(mainjarstring));
        buildRule.getProject().setUserProperty(MagicNames.BUILD_SYSCLASSPATH, "ignore");
        loader = buildRule.getProject().createClassLoader(myPath);
        String path = loader.getClasspath();
        assertEquals(mainjarstring + File.pathSeparator + extjarstring, path);
    }

    @Test
    public void testJarWithManifestInNonAsciiDir() {
        String mainjarstring = buildRule.getProject().getProperty("main.jar.nonascii");
        String extjarstring = buildRule.getProject().getProperty("ext.jar.nonascii");
        Path myPath = new Path(buildRule.getProject());
        myPath.setLocation(new File(mainjarstring));
        buildRule.getProject().setUserProperty(MagicNames.BUILD_SYSCLASSPATH, "ignore");
        loader = buildRule.getProject().createClassLoader(myPath);
        String path = loader.getClasspath();
        assertEquals(mainjarstring + File.pathSeparator + extjarstring, path);
    }

    /**
     * The test should fail if NullPointException is thrown
     *
     * @throws ClassNotFoundException if a class is not found, ignored
     */
    @Test
    public void testCleanup() throws ClassNotFoundException {
        thrown.expect(ClassNotFoundException.class);
        Path path = new Path(buildRule.getProject(), ".");
        loader = buildRule.getProject().createClassLoader(path);
        boolean canary = false;
        try {
            // we don't expect to find this
            loader.findClass("fubar");
            canary = true;
        } finally {
            assertFalse("Nonexistent class found", canary);
            loader.cleanup();
            try {
                // we don't expect to find this
                loader.findClass("fubar");
                canary = true;
            } finally {
                assertFalse("Nonexistent class found", canary);
                // tell the build it is finished
                buildRule.getProject().fireBuildFinished(null);
                try {
                    // we don't expect to find this
                    loader.findClass("fubar");
                    canary = true;
                } finally {
                    assertFalse("Nonexistent class found", canary);
                }
            }
        }
    }

    @Test
    public void testGetPackage() throws Exception {
        buildRule.executeTarget("prepareGetPackageTest");
        Path myPath = new Path(buildRule.getProject());
        myPath.setLocation(new File(buildRule.getProject().getProperty("test.jar")));
        buildRule.getProject().setUserProperty(MagicNames.BUILD_SYSCLASSPATH, "ignore");
        loader = buildRule.getProject().createClassLoader(myPath);
        assertNotNull("should find class", loader.findClass("org.example.Foo"));
        assertNotNull("should find package",
                      new GetPackageWrapper(loader).getPackage("org.example"));
    }

    @Test
    public void testCodeSource() throws Exception {
        buildRule.executeTarget("prepareGetPackageTest");
        Path myPath = new Path(buildRule.getProject());
        File testJar = new File(buildRule.getProject().getProperty("test.jar"));
        myPath.setLocation(testJar);
        buildRule.getProject().setUserProperty(MagicNames.BUILD_SYSCLASSPATH, "ignore");
        loader = buildRule.getProject().createClassLoader(myPath);
        Class<?> foo = loader.findClass("org.example.Foo");
        URL codeSourceLocation =
            foo.getProtectionDomain().getCodeSource().getLocation();
        assertEquals(codeSourceLocation + " should point to test.jar",
                   FileUtils.getFileUtils().getFileURL(testJar), codeSourceLocation);
    }

    @Test
    public void testSignedJar() throws Exception {
        buildRule.executeTarget("signTestJar");
        File jar = new File(buildRule.getProject().getProperty("test.jar"));

        Path myPath = new Path(buildRule.getProject());
        myPath.setLocation(jar);
        buildRule.getProject().setUserProperty(MagicNames.BUILD_SYSCLASSPATH, "ignore");
        loader = buildRule.getProject().createClassLoader(myPath);
        Class<?> foo = loader.findClass("org.example.Foo");

        assertNotNull("should find class", foo);
        assertNotNull("should have certificates",
                      foo.getProtectionDomain().getCodeSource()
                      .getCertificates());
        assertNotNull("should be signed", foo.getSigners());
    }

    /**
     * @see <a href="https://issues.apache.org/bugzilla/show_bug.cgi?id=47593">bug 47593</a>
     *     request to log the name of corrupt zip files from which
     *     classes cannot be loaded
     */
    @Test
    public void testInvalidZipException() {
        buildRule.executeTarget("createNonJar");
        File jar = new File(buildRule.getProject().getProperty("tmp.dir")
                            + "/foo.jar");

        Path myPath = new Path(buildRule.getProject());
        myPath.setLocation(jar);
        buildRule.getProject().setUserProperty(MagicNames.BUILD_SYSCLASSPATH, "ignore");
        loader = buildRule.getProject().createClassLoader(myPath);
        PrintStream sysErr = System.err;
        try {
            StringBuffer errBuffer = new StringBuffer();
            PrintStream err =
                new PrintStream(new BuildFileRule.AntOutputStream(errBuffer));
            System.setErr(err);
            loader.getResource("foo.txt");
            String log = buildRule.getLog();
            int startMessage = log.indexOf("CLASSPATH element ");
            assertTrue(startMessage >= 0);
            assertTrue(log.indexOf("foo.jar is not a JAR", startMessage) > 0);
        } finally {
            System.setErr(sysErr);
        }
    }

    /**
     * Asserts that getResources won't return resources that cannot be
     * seen by AntClassLoader but by ClassLoader.this.parent.
     *
     * @see <a href="https://issues.apache.org/bugzilla/show_bug.cgi?id=46752">bug 46752</a>
     */
    @SuppressWarnings("resource")
    @Test
    public void testGetResources() throws IOException {
        AntClassLoader acl = new AntClassLoader(new EmptyLoader(), null,
                                                new Path(null), true);
        assertNull(acl.getResource("META-INF/MANIFEST.MF"));
        assertFalse(acl.getResources("META-INF/MANIFEST.MF").hasMoreElements());

        // double check using system classloader as parent
        acl = new AntClassLoader(null, null, new Path(null), true);
        assertNotNull(acl.getResource("META-INF/MANIFEST.MF"));
        assertTrue(acl.getResources("META-INF/MANIFEST.MF").hasMoreElements());
    }

    @Test
    public void testGetResourcesUsingFactory() throws IOException {
        AntClassLoader acl =
            AntClassLoader.newAntClassLoader(new EmptyLoader(), null,
                                             new Path(null), true);
        assertNull(acl.getResource("META-INF/MANIFEST.MF"));
        assertFalse(acl.getResources("META-INF/MANIFEST.MF").hasMoreElements());
    }

    /**
     * Tests that {@link AntClassLoader} supports multi-release jar files while dealing with
     * runtime resources in Java 9+ runtime environments.
     *
     * @see <a href="bz-62952">https://bz.apache.org/bugzilla/show_bug.cgi?id=62952</a>
     */
    @Test
    public void testMultiReleaseJar() {
        buildRule.executeTarget("testMRJar");
        final boolean atleastJava9 = JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_9);
        final String targetOutput = buildRule.getOutput();
        Assert.assertNotNull("Multi-release jar test did not generate any output", targetOutput);
        if (atleastJava9) {
            Assert.assertTrue("Unexpected output from multi-release jar test for Java runtime >= 9",
                    targetOutput.contains("mrjar test result = 9"));
        } else {
            Assert.assertTrue("Unexpected output from multi-release jar test", targetOutput.contains("mrjar test result = default"));
        }
    }

    private static class EmptyLoader extends ClassLoader {
        public URL getResource(String n) {
            return null;
        }

        public Enumeration<URL> getResources(String n) {
            return Collections.emptyEnumeration();
        }
    }

    private static class GetPackageWrapper extends ClassLoader {
        GetPackageWrapper(ClassLoader parent) {
            super(parent);
        }

        public Package getPackage(String s) {
            return super.getPackage(s);
        }
    }
}
