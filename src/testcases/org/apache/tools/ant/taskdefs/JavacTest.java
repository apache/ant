/*
 * Copyright  2001-2002,2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
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

import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.compilers.CompilerAdapter;
import org.apache.tools.ant.taskdefs.compilers.CompilerAdapterFactory;
import org.apache.tools.ant.taskdefs.compilers.DefaultCompilerAdapter;
import org.apache.tools.ant.taskdefs.compilers.Javac12;
import org.apache.tools.ant.taskdefs.compilers.Javac13;
import org.apache.tools.ant.taskdefs.compilers.JavacExternal;
import org.apache.tools.ant.util.JavaEnvUtils;

import junit.framework.TestCase;

/**
 * Testcase for <javac>.
 *
 * @version $Revision$ $Date$
 */
public class JavacTest extends TestCase {

    private Project project;
    private Javac javac;

    public JavacTest(String name) {
        super(name);
    }

    public void setUp() {
        project = new Project();
        project.init();
        javac = new Javac();
        javac.setProject(project);
    }

    /**
     * Test setting the name of the javac executable.
     */
    public void testForkedExecutableName() {
        assertNull("no fork means no executable", javac.getJavacExecutable());

        project.setProperty("build.compiler", "modern");
        assertNull("no fork means no executable", javac.getJavacExecutable());

        javac.setFork(true);
        assertNotNull("normal fork", javac.getJavacExecutable());
        assertTrue("name should contain \"javac\"",
                   javac.getJavacExecutable().indexOf("javac") > -1);

        project.setProperty("build.compiler", "extJavac");
        javac.setFork(false);
        assertNotNull("fork via property", javac.getJavacExecutable());
        assertTrue("name should contain \"javac\"",
                   javac.getJavacExecutable().indexOf("javac") > -1);

        project.setProperty("build.compiler", "whatever");
        assertNull("no fork and not extJavac means no executable",
                   javac.getJavacExecutable());

        String myJavac = "Slartibartfast";
        javac.setFork(true);
        javac.setExecutable(myJavac);
        assertEquals(myJavac, javac.getJavacExecutable());
    }

    /**
     * Test nested compiler args.
     */
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
    public void testCompilerAttribute() {
        // check defaults
        String compiler = javac.getCompiler();
        assertNotNull(compiler);
        assertTrue("default value",
                   "javac1.1".equals(compiler)
                   || "javac1.2".equals(compiler)
                   || "javac1.3".equals(compiler)
                   || "javac1.4".equals(compiler)
                   || "javac1.5".equals(compiler)
                   || "classic".equals(compiler));

        javac.setFork(true);
        assertNotNull(javac.getCompiler());
        assertEquals("extJavac", javac.getCompiler());
        assertEquals(compiler, javac.getCompilerVersion());

        // check build.compiler provides defaults
        javac = new Javac();
        javac.setProject(project);
        project.setNewProperty("build.compiler", "jikes");
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

    public void testCompilerAdapter() {
        if (JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_1)
            || JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_2)
            || JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_3)) {
            javac.setCompiler("javac1.1");
        } else {
            javac.setCompiler("javac1.4");
        }

        javac.setDepend(true);
        CompilerAdapter adapter =
            CompilerAdapterFactory.getCompiler(javac.getCompiler(), javac);

        if (JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_1)
            || JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_2)
            || JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_3)) {
            assertTrue(adapter instanceof Javac12);
        } else {
            assertTrue(adapter instanceof Javac13);
        }

        javac.setFork(true);
        adapter =
            CompilerAdapterFactory.getCompiler(javac.getCompiler(), javac);
        assertTrue(adapter instanceof JavacExternal);
    }

}
