/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
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

import junit.framework.TestCase;

/**
 * Testcase for <rmic>.
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @version $Revision$
 * @since Ant 1.5
 */
public class RmicTest extends TestCase {

    private Project project;
    private Rmic rmic;

    public RmicTest(String name) {
        super(name);
    }

    public void setUp() {
        project = new Project();
        project.init();
        rmic = new Rmic();
        rmic.setProject(project);
    }

    /**
     * Test nested compiler args.
     */
    public void testCompilerArg() {
        String[] args = rmic.getCurrentCompilerArgs();
        assertNotNull(args);
        assertEquals("no args", 0, args.length);

        Rmic.ImplementationSpecificArgument arg = rmic.createCompilerArg();
        String ford = "Ford";
        String prefect = "Prefect";
        String testArg = ford + " " + prefect;
        arg.setValue(testArg);
        args = rmic.getCurrentCompilerArgs();
        assertEquals("unconditional single arg", 1, args.length);
        assertEquals(testArg, args[0]);

        arg.setCompiler("weblogic");
        args = rmic.getCurrentCompilerArgs();
        assertNotNull(args);
        assertEquals("implementation is weblogic but build.rmic is null", 
                     0, args.length);

        project.setProperty("build.rmic", "sun");
        args = rmic.getCurrentCompilerArgs();
        assertNotNull(args);
        assertEquals("implementation is weblogic but build.rmic is sun", 
                     0, args.length);

        project.setProperty("build.rmic", "weblogic");
        args = rmic.getCurrentCompilerArgs();
        assertEquals("both are weblogic", 1, args.length);
        assertEquals(testArg, args[0]);
    }

    /**
     * Test compiler attribute.
     */
    public void testCompilerAttribute() {
        // check defaults
        String compiler = rmic.getCompiler();
        assertNotNull(compiler);
        assertTrue("default value", 
                   "sun".equals(compiler) || "kaffe".equals(compiler));

        project.setNewProperty("build.rmic", "weblogic");
        compiler = rmic.getCompiler();
        assertNotNull(compiler);
        assertEquals("weblogic", compiler);

        // check attribute overrides build.compiler
        rmic.setCompiler("kaffe");
        compiler = rmic.getCompiler();
        assertNotNull(compiler);
        assertEquals("kaffe", compiler);
    }

}
