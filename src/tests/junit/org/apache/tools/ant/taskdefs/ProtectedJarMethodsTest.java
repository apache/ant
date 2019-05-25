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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.tools.ant.BuildFileRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

/**
 */
public class ProtectedJarMethodsTest {

    @Rule
    public final BuildFileRule buildRule = new BuildFileRule();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/jar.xml");
        buildRule.executeTarget("setUp");
    }

    @Test
    public void testGrabFilesAndDirs() throws IOException {
        buildRule.executeTarget("testIndexTests");
        String tempJar = "tmp.jar";
        String archive = buildRule.getProject().getProperty(tempJar);
        List<String> dirs = new ArrayList<>();
        List<String> files = new ArrayList<>();
        String[] expectedDirs = new String[] {
            "META-INF/",
            "sub/",
        };
        String[] expectedFiles = new String[] {
            "foo",
        };
        Jar.grabFilesAndDirs(archive, dirs, files);

        assertEquals(expectedDirs.length, dirs.size());
        Arrays.stream(expectedDirs).forEach(expectedDir -> assertThat("Found " + expectedDir,
                dirs, hasItem(expectedDir)));

        assertEquals(expectedFiles.length, files.size());
        Arrays.stream(expectedFiles).forEach(expectedFile -> assertThat("Found " + expectedFile,
                files, hasItem(expectedFile)));
    }

    @Test
    public void testFindJarNameNoClasspath() {
        assertEquals("foo", Jar.findJarName("foo", null));
        assertEquals("foo", Jar.findJarName("lib" + File.separatorChar + "foo",
                                            null));
    }

    @Test
    public void testFindJarNameNoMatch() {
        assertNull(Jar.findJarName("foo", new String[] { "bar" }));
    }

    @Test
    public void testFindJarNameSimpleMatches() {
        assertEquals("foo", Jar.findJarName("foo", new String[] { "foo" }));
        assertEquals("lib/foo",
            Jar.findJarName("foo", new String[] { "lib/foo" }));
        assertEquals("foo", Jar.findJarName("bar" + File.separatorChar + "foo",
            new String[] { "foo" }));
        assertEquals("lib/foo", Jar.findJarName(
            "bar" + File.separatorChar + "foo", new String[] { "lib/foo" }));
    }

    @Test
    public void testFindJarNameLongestMatchWins() {
        assertEquals("lib/foo", Jar.findJarName("lib/foo",
            new String[] { "foo", "lib/foo", "lib/bar/foo" }));
    }
}
