/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2002 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "Ant" and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
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
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
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
