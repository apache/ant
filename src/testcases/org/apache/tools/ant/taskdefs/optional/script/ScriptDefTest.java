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
package org.apache.tools.ant.taskdefs.optional.script;

import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;
import java.io.File;

/**
 * Tests the examples of the &lt;scriptdef&gt; task.
 *
 * @since Ant 1.6
 */
public class ScriptDefTest extends BuildFileTest {

    public ScriptDefTest(String name) {
        super(name);
    }

    /**
     * The JUnit setup method
     */
    public void setUp() {
        configureProject("src/etc/testcases/taskdefs/optional/script/scriptdef.xml");
    }

    public void testSimple() {
        executeTarget("simple");
        // get the fileset and its basedir
        Project p = getProject();
        FileSet fileset = (FileSet) p.getReference("testfileset");
        File baseDir = fileset.getDir(p);
        String log = getLog();
        assertTrue("Expecting attribute value printed",
            log.indexOf("Attribute attr1 = test") != -1);

        assertTrue("Expecting nested element value printed",
            log.indexOf("Fileset basedir = " + baseDir.getAbsolutePath()) != -1);
    }

    public void testNoLang() {
        expectBuildExceptionContaining("nolang",
            "Absence of language attribute not detected",
            "requires a language attribute");
    }

    public void testNoName() {
        expectBuildExceptionContaining("noname",
            "Absence of name attribute not detected",
            "scriptdef requires a name attribute");
    }

    public void testNestedByClassName() {
        executeTarget("nestedbyclassname");
        // get the fileset and its basedir
        Project p = getProject();
        FileSet fileset = (FileSet) p.getReference("testfileset");
        File baseDir = fileset.getDir(p);
        String log = getLog();
        assertTrue("Expecting attribute value to be printed",
            log.indexOf("Attribute attr1 = test") != -1);

        assertTrue("Expecting nested element value to be printed",
            log.indexOf("Fileset basedir = " + baseDir.getAbsolutePath()) != -1);
    }

    public void testNoElement() {
        expectOutput("noelement", "Attribute attr1 = test");
    }

    public void testException() {
        expectBuildExceptionContaining("exception",
            "Should have thrown an exception in the script",
            "TypeError");
    }

    public void testDoubleDef() {
        executeTarget("doubledef");
        String log = getLog();
        assertTrue("Task1 did not execute",
            log.indexOf("Task1") != -1);
        assertTrue("Task2 did not execute",
            log.indexOf("Task2") != -1);
    }

    public void testDoubleAttribute() {
        expectBuildExceptionContaining("doubleAttributeDef",
            "Should have detected duplicate attribute definition",
            "attr1 attribute more than once");
    }

    public void testProperty() {
        executeTarget("property");
        // get the fileset and its basedir
        String log = getLog();
        assertTrue("Expecting property in attribute value replaced",
            log.indexOf("Attribute value = test") != -1);
    }


}
