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

package org.apache.tools.ant.types;

import junit.framework.TestCase;

import org.apache.tools.ant.MagicNames;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.util.JavaEnvUtils;

/**
 * JUnit 3 testcases for org.apache.tools.ant.CommandlineJava
 *
 */
public class CommandlineJavaTest extends TestCase {

    private String cloneVm;

    public CommandlineJavaTest(String name) {
        super(name);
    }

    private Project project;

    public void setUp() {
        project = new Project();
        project.setBasedir(System.getProperty("root"));
        project.setProperty("build.sysclasspath", "ignore");
        cloneVm = System.getProperty("ant.build.clonevm");
        if (cloneVm != null) {
            System.setProperty("ant.build.clonevm", "false");
        }
    }

    public void tearDown() {
        if (cloneVm != null) {
            System.setProperty("ant.build.clonevm", cloneVm);
        }
    }

    public void testGetCommandline() throws Exception {
        CommandlineJava c = new CommandlineJava();
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
        try {
            CommandlineJava c2 = (CommandlineJava) c.clone();
        } catch (NullPointerException ex) {
            fail("cloning should work without classpath specified");
        }

        c.createClasspath(project).setLocation(project.resolveFile("build.xml"));
        c.createClasspath(project).setLocation(project.resolveFile(
            System.getProperty(MagicNames.ANT_HOME)+"/lib/ant.jar"));
        s = c.getCommandline();
        assertEquals("with classpath", 6, s.length);
        //        assertEquals("with classpath", "java", s[0]);
        assertEquals("with classpath", "-Djava.compiler=NONE", s[1]);
        assertEquals("with classpath", "-classpath", s[2]);
        assertTrue("build.xml contained",
               s[3].indexOf("build.xml"+java.io.File.pathSeparator) >= 0);
        assertTrue("ant.jar contained", s[3].endsWith("ant.jar"));
        assertEquals("with classpath", "junit.textui.TestRunner", s[4]);
        assertEquals("with classpath",
                     "org.apache.tools.ant.CommandlineJavaTest", s[5]);
    }

    public void testJarOption() throws Exception {
        CommandlineJava c = new CommandlineJava();
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

    public void testSysproperties() {
        String currentClasspath = System.getProperty("java.class.path");
        assertNotNull(currentClasspath);
        assertNull(System.getProperty("key"));
        CommandlineJava c = new CommandlineJava();
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
            assertTrue(System.getProperties().containsKey("java.class.path"));
            assertNotNull(System.getProperty("key2"));
            assertEquals("value2", System.getProperty("key2"));
        } finally {
            c.restoreSystemProperties();
        }
        assertNull(System.getProperty("key"));
        assertNull(System.getProperty("key2"));
    }

    public void testAssertions() throws Exception {
        if (JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_2)
            || JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_3)) {
            return;
        }

        CommandlineJava c = new CommandlineJava();
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
