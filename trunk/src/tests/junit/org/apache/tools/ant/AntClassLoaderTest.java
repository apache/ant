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

package org.apache.tools.ant;

import java.io.File;
import org.apache.tools.ant.types.Path;

/**
 * Test case for ant class loader
 *
 */
public class AntClassLoaderTest extends BuildFileTest {

    private Project p;

    public AntClassLoaderTest(String name) {
        super(name);
    }

    public void setUp() {
        p = new Project();
        p.init();
        configureProject("src/etc/testcases/core/antclassloader.xml");
        getProject().executeTarget("setup");
    }

    public void tearDown() {
        getProject().executeTarget("cleanup");
    }
    //test inspired by bug report 37085
    public void testJarWithManifestInDirWithSpace() {
        String mainjarstring = getProject().getProperty("main.jar");
        String extjarstring = getProject().getProperty("ext.jar");
        Path myPath = new Path(getProject());
        myPath.setLocation(new File(mainjarstring));
        getProject().setUserProperty("build.sysclasspath","ignore");
        AntClassLoader myLoader = getProject().createClassLoader(myPath);
        String path = myLoader.getClasspath();
        assertEquals(mainjarstring + File.pathSeparator + extjarstring, path);
    }
    public void testJarWithManifestInNonAsciiDir() {
        String mainjarstring = getProject().getProperty("main.jar.nonascii");
        String extjarstring = getProject().getProperty("ext.jar.nonascii");
        Path myPath = new Path(getProject());
        myPath.setLocation(new File(mainjarstring));
        getProject().setUserProperty("build.sysclasspath","ignore");
        AntClassLoader myLoader = getProject().createClassLoader(myPath);
        String path = myLoader.getClasspath();
        assertEquals(mainjarstring + File.pathSeparator + extjarstring, path);
    }
    public void testCleanup() throws BuildException {
        Path path = new Path(p, ".");
        AntClassLoader loader = p.createClassLoader(path);
        try {
            // we don't expect to find this
            loader.findClass("fubar");
            fail("Did not expect to find fubar class");
        } catch (ClassNotFoundException e) {
            // ignore expected
        }

        loader.cleanup();
        try {
            // we don't expect to find this
            loader.findClass("fubar");
            fail("Did not expect to find fubar class");
        } catch (ClassNotFoundException e) {
            // ignore expected
        } catch (NullPointerException e) {
            fail("loader should not fail even if cleaned up");
        }

        // tell the build it is finished
        p.fireBuildFinished(null);
        try {
            // we don't expect to find this
            loader.findClass("fubar");
            fail("Did not expect to find fubar class");
        } catch (ClassNotFoundException e) {
            // ignore expected
        } catch (NullPointerException e) {
            fail("loader should not fail even if project finished");
        }
    }
}
