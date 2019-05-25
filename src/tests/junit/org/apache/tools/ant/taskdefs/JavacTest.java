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

import org.apache.tools.ant.MagicNames;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.compilers.CompilerAdapter;
import org.apache.tools.ant.taskdefs.compilers.CompilerAdapterFactory;
import org.apache.tools.ant.taskdefs.compilers.Javac13;
import org.apache.tools.ant.taskdefs.compilers.JavacExternal;

import org.junit.Before;
import org.junit.Test;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Path;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Testcase for <javac>.
 *
 */
public class JavacTest {

    private Project project;
    private Javac javac;

    @Before
    public void setUp() {
        project = new Project();
        project.init();
        javac = new Javac();
        javac.setProject(project);
    }

    /**
     * Test setting the name of the javac executable.
     */
    @Test
    public void testForkedExecutableName() {
        assertNull("no fork means no executable", javac.getJavacExecutable());

        project.setProperty("build.compiler", "modern");
        assertNull("no fork means no executable", javac.getJavacExecutable());

        javac.setFork(true);
        assertNotNull("normal fork", javac.getJavacExecutable());
        assertThat("name should contain \"javac\"", javac.getJavacExecutable(), containsString("javac"));

        project.setProperty("build.compiler", "extJavac");
        javac.setFork(false);
        assertNotNull("fork via property", javac.getJavacExecutable());
        assertThat("name should contain \"javac\"", javac.getJavacExecutable(), containsString("javac"));

        project.setProperty("build.compiler", "whatever");
        assertNull("no fork and not extJavac means no executable", javac.getJavacExecutable());

        String myJavac = "Slartibartfast";
        javac.setFork(true);
        javac.setExecutable(myJavac);
        assertEquals(myJavac, javac.getJavacExecutable());
    }

    /**
     * Test nested compiler args.
     */
    @Test
    public void testCompilerArg() {
        String[] args = javac.getCurrentCompilerArgs();
        assertNotNull(args);
        assertEquals("no args", 0, args.length);

        Javac.ImplementationSpecificArgument arg = javac.createCompilerArg();
        String ford = "Ford";
        String prefect = "Prefect";
        String testArg = ford + " " + prefect;
        arg.setValue(testArg);
        args = javac.getCurrentCompilerArgs();
        assertEquals("unconditional single arg", 1, args.length);
        assertEquals(testArg, args[0]);

        arg.setCompiler("jikes");
        args = javac.getCurrentCompilerArgs();
        assertNotNull(args);
        assertEquals("implementation is jikes but build.compiler is null",
                     0, args.length);

        project.setProperty("build.compiler", "jvc");
        args = javac.getCurrentCompilerArgs();
        assertNotNull(args);
        assertEquals("implementation is jikes but build.compiler is jvc",
                     0, args.length);

        project.setProperty("build.compiler", "jikes");
        args = javac.getCurrentCompilerArgs();
        assertEquals("both are jikes", 1, args.length);
        assertEquals(testArg, args[0]);

        arg.setLine(testArg);
        args = javac.getCurrentCompilerArgs();
        assertEquals("split at space", 2, args.length);
        assertEquals(ford, args[0]);
        assertEquals(prefect, args[1]);
    }

    /**
     * Test nested compiler args in the fork="true" and
     * implementation="extJavac" case.
     */
    @Test
    public void testCompilerArgForForkAndExtJavac() {
        Javac.ImplementationSpecificArgument arg = javac.createCompilerArg();
        String ford = "Ford";
        String prefect = "Prefect";
        String testArg = ford + " " + prefect;
        arg.setValue(testArg);
        arg.setCompiler("extJavac");
        javac.setFork(true);
        String[] args = javac.getCurrentCompilerArgs();
        assertEquals("both are forked javac", 1, args.length);
        assertEquals(testArg, args[0]);
    }

    /**
     * Test compiler attribute.
     */
    @Test
    public void testCompilerAttribute() {
        // check defaults
        String compiler = javac.getCompiler();
        assertNotNull(compiler);
        if (System.getProperty("build.compiler") != null) {
            assertEquals(System.getProperty("build.compiler"), compiler);
        } else {
            assertTrue("default value",
                       "javac1.1".equals(compiler)
                       || "javac1.2".equals(compiler)
                       || "javac1.3".equals(compiler)
                       || "javac1.4".equals(compiler)
                       || "javac1.5".equals(compiler)
                       || "classic".equals(compiler));
        }

        javac.setFork(true);
        assertNotNull(javac.getCompiler());
        assertEquals("extJavac", javac.getCompiler());
        assertEquals(compiler, javac.getCompilerVersion());

        // check build.compiler provides defaults
        javac = new Javac();
        javac.setProject(project);
        // setUserProperty to override system properties
        project.setUserProperty("build.compiler", "jikes");
        compiler = javac.getCompiler();
        assertNotNull(compiler);
        assertEquals("jikes", compiler);

        javac.setFork(true);
        compiler = javac.getCompiler();
        assertNotNull(compiler);
        assertEquals("jikes", compiler);

        // check attribute overrides build.compiler
        javac.setFork(false);
        javac.setCompiler("jvc");
        compiler = javac.getCompiler();
        assertNotNull(compiler);
        assertEquals("jvc", compiler);

        javac.setFork(true);
        compiler = javac.getCompiler();
        assertNotNull(compiler);
        assertEquals("jvc", compiler);
    }

    @Test
    public void testCompilerAdapter() {
        javac.setCompiler("javac1.4");
        javac.setDepend(true);
        CompilerAdapter adapter = CompilerAdapterFactory.getCompiler(javac.getCompiler(), javac);
        assertThat(adapter, instanceOf(Javac13.class));

        javac.setFork(true);
        adapter = CompilerAdapterFactory.getCompiler(javac.getCompiler(), javac);
        assertThat(adapter, instanceOf(JavacExternal.class));
    }

    @Test
    public void testSourceNoDefault() {
        assertNull(javac.getSource());
    }

    @Test
    public void testSourceWithDefault() {
        project.setNewProperty(MagicNames.BUILD_JAVAC_SOURCE, "1.4");
        assertEquals("1.4", javac.getSource());
    }

    @Test
    public void testSourceOverridesDefault() {
        project.setNewProperty(MagicNames.BUILD_JAVAC_SOURCE, "1.4");
        javac.setSource("1.5");
        assertEquals("1.5", javac.getSource());
    }

    @Test
    public void testTargetNoDefault() {
        assertNull(javac.getTarget());
    }

    @Test
    public void testTargetWithDefault() {
        project.setNewProperty(MagicNames.BUILD_JAVAC_TARGET, "1.4");
        assertEquals("1.4", javac.getTarget());
    }

    @Test
    public void testTargetOverridesDefault() {
        project.setNewProperty(MagicNames.BUILD_JAVAC_TARGET, "1.4");
        javac.setTarget("1.5");
        assertEquals("1.5", javac.getTarget());
    }

    @Test(expected = BuildException.class)
    public void testModulesourcepathOrSrcDirRequired() {
        javac.checkParameters();
    }

    @Test(expected = BuildException.class)
    public void testModulesourcepathAndSrcDirForbidden() {
        javac.checkParameters();
        final Path p = new Path(project);
        p.setPath("src");
        javac.setSrcdir(p);
        final Path mp = new Path(project);
        p.setPath("modsrc");
        javac.setModulesourcepath(mp);
    }

    @Test(expected = BuildException.class)
    public void testModulesourcepathAndSourcepathForbidden() {
        javac.checkParameters();
        final Path p = new Path(project);
        p.setPath("src");
        javac.setSourcepath(p);
        final Path mp = new Path(project);
        p.setPath("modsrc");
        javac.setModulesourcepath(mp);
    }

    @Test
    public void testSrcDir() {
        final Path p = new Path(project);
        p.setPath("src");
        javac.setSrcdir(p);
        javac.checkParameters();
    }

    @Test
    public void testModulesourcepath() {
        final File tmp = new File(System.getProperty("java.io.tmpdir"));   //NOI18N
        final File destDir = new File(tmp, String.format("%stestMP%d",
                getClass().getName(),
                System.currentTimeMillis() / 1000));
        destDir.mkdirs();
        try {
            final Path p = new Path(project);
            p.setPath("src");
            javac.setModulesourcepath(p);
            javac.setDestdir(destDir);
            javac.checkParameters();
        } finally {
            destDir.delete();
        }
    }

    @Test(expected = BuildException.class)
    public void testModulesourcepathRequiresDestdir() {
        final Path p = new Path(project);
        p.setPath("src");
        javac.setModulesourcepath(p);
        javac.checkParameters();
    }
}
