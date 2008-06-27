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

import org.apache.tools.ant.Project;
import junit.framework.TestCase;


/**
 * Testcase for <rmic>.
 *
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
        assertEquals("expected sun or kaffe, but found "+compiler,compiler,"default");

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
