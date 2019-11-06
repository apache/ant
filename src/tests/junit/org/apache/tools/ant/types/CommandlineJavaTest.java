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

package org.apache.tools.ant.types;

import org.apache.tools.ant.MagicNames;
import org.apache.tools.ant.MagicTestNames;
import org.apache.tools.ant.Project;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasKey;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

/**
 * JUnit testcases for org.apache.tools.ant.CommandlineJava
 *
 */
public class CommandlineJavaTest {

    private String cloneVm;

    private Project project;

    private CommandlineJava c;

    @Before
    public void setUp() {
        project = new Project();
        if (System.getProperty(MagicTestNames.TEST_ROOT_DIRECTORY) != null) {
            project.setBasedir(System.getProperty(MagicTestNames.TEST_ROOT_DIRECTORY));
        }
        project.setProperty(MagicNames.BUILD_SYSCLASSPATH, "ignore");
        cloneVm = System.getProperty("ant.build.clonevm");
        if (cloneVm != null) {
            System.setProperty("ant.build.clonevm", "false");
        }
        c = new CommandlineJava();
    }

    @After
    public void tearDown() {
        if (cloneVm != null) {
            System.setProperty("ant.build.clonevm", cloneVm);
        }
    }

    /**
     * NullPointerException may break the build
     *
     * @throws CloneNotSupportedException if clone() fails
     */
    @Test
    public void testGetCommandline() throws CloneNotSupportedException {
        assertNotNull("Ant home not set", System.getProperty(MagicNames.ANT_HOME));
        c.createArgument().setValue("org.apache.tools.ant.CommandlineJavaTest");
        c.setClassname("junit.textui.TestRunner");
        c.createVmArgument().setValue("-Djava.compiler=NONE");
        String[] s = c.getCommandline();
        assertEquals("no classpath", 4, s.length);
        /*
         * After changing CommandlineJava to search for the java
         * executable, I don't know, how to tests the value returned
         * here without using the same logic as applied in the class
         * itself.
         *
         * assertTrue("no classpath", "java", s[0]);
         */
        assertEquals("no classpath", "-Djava.compiler=NONE", s[1]);
        assertEquals("no classpath", "junit.textui.TestRunner", s[2]);
        assertEquals("no classpath",
                     "org.apache.tools.ant.CommandlineJavaTest", s[3]);
        c.clone();
        c.createClasspath(project).setLocation(project.resolveFile("build.xml"));
        c.createClasspath(project).setLocation(project.resolveFile(
            System.getProperty(MagicNames.ANT_HOME) + "/lib/ant.jar"));
        s = c.getCommandline();
        assertEquals("with classpath", 6, s.length);
        // assertEquals("with classpath", "java", s[0]);
        assertEquals("with classpath", "-Djava.compiler=NONE", s[1]);
        assertEquals("with classpath", "-classpath", s[2]);
        assertThat("build.xml contained", s[3], containsString("build.xml" + File.pathSeparator));
        assertThat("ant.jar contained", s[3], endsWith("ant.jar"));
        assertEquals("with classpath", "junit.textui.TestRunner", s[4]);
        assertEquals("with classpath",
                     "org.apache.tools.ant.CommandlineJavaTest", s[5]);
    }

    @Test
    public void testJarOption() {
        c.createArgument().setValue("arg1");
        c.setJar("myfile.jar");
        c.createVmArgument().setValue("-classic");
        c.createVmArgument().setValue("-Dx=y");
        String[] s = c.getCommandline();
        assertEquals("-classic", s[1]);
        assertEquals("-Dx=y", s[2]);
        assertEquals("-jar", s[3]);
        assertEquals("myfile.jar", s[4]);
        assertEquals("arg1", s[5]);
    }

    @Test
    public void testSysproperties() {
        String currentClasspath = System.getProperty("java.class.path");
        assertNotNull(currentClasspath);
        assertNull(System.getProperty("key"));
        Environment.Variable v = new Environment.Variable();
        v.setKey("key");
        v.setValue("value");
        c.addSysproperty(v);

        project.setProperty("key2", "value2");
        PropertySet ps = new PropertySet();
        ps.setProject(project);
        ps.appendName("key2");
        c.addSyspropertyset(ps);

        try {
            c.setSystemProperties();
            String newClasspath = System.getProperty("java.class.path");
            assertNotNull(newClasspath);
            assertEquals(currentClasspath, newClasspath);
            assertNotNull(System.getProperty("key"));
            assertEquals("value", System.getProperty("key"));
            assertThat(System.getProperties(), hasKey("java.class.path"));
            assertNotNull(System.getProperty("key2"));
            assertEquals("value2", System.getProperty("key2"));
        } finally {
            c.restoreSystemProperties();
        }
        assertNull(System.getProperty("key"));
        assertNull(System.getProperty("key2"));
    }

    @Test
    public void testAssertions() throws Exception {
        c.createArgument().setValue("org.apache.tools.ant.CommandlineJavaTest");
        c.setClassname("junit.textui.TestRunner");
        c.createVmArgument().setValue("-Djava.compiler=NONE");
        Assertions a = new Assertions();
        a.setProject(project);
        Assertions.EnabledAssertion ea = new Assertions.EnabledAssertion();
        ea.setClass("junit.textui.TestRunner");
        a.addEnable(ea);
        c.setAssertions(a);

        String[] expected = new String[] {
            null,
            "-Djava.compiler=NONE",
            "-ea:junit.textui.TestRunner",
            "junit.textui.TestRunner",
            "org.apache.tools.ant.CommandlineJavaTest",
        };

        // only the second iteration would pass because of PR 27218
        for (int i = 0; i < 3; i++) {
            String[] s = c.getCommandline();
            assertEquals(expected.length, s.length);
            for (int j = 1; j < expected.length; j++) {
                assertEquals(expected[j], s[j]);
            }
        }
        CommandlineJava c2 = (CommandlineJava) c.clone();
        String[] s = c2.getCommandline();
        assertEquals(expected.length, s.length);
        for (int j = 1; j < expected.length; j++) {
            assertEquals(expected[j], s[j]);
        }
    }

}
