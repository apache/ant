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

import org.apache.tools.ant.AntAssert;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests the examples of the &lt;scriptdef&gt; task.
 *
 * @since Ant 1.6
 */
public class ScriptDefTest {

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/optional/script/scriptdef.xml");
    }

    @Test
    public void testSimple() {
        buildRule.executeTarget("simple");
        // get the fileset and its basedir
        Project p = buildRule.getProject();
        FileSet fileset = (FileSet) p.getReference("testfileset");
        File baseDir = fileset.getDir(p);
        String log = buildRule.getLog();
        assertTrue("Expecting attribute value printed",
            log.indexOf("Attribute attr1 = test") != -1);

        assertTrue("Expecting nested element value printed",
            log.indexOf("Fileset basedir = " + baseDir.getAbsolutePath()) != -1);
    }

    @Test
    public void testNoLang() {
        try {
            buildRule.executeTarget("nolang");
            fail("Absence of language attribute not detected");
        } catch(BuildException ex) {
            AntAssert.assertContains("requires a language attribute", ex.getMessage());
        }
    }

    @Test
    public void testNoName() {
        try {
            buildRule.executeTarget("noname");
            fail("Absence of name attribute not detected");
        } catch(BuildException ex) {
            AntAssert.assertContains("scriptdef requires a name attribute", ex.getMessage());
        }
    }

    @Test
    public void testNestedByClassName() {
        buildRule.executeTarget("nestedbyclassname");
        // get the fileset and its basedir
        Project p = buildRule.getProject();
        FileSet fileset = (FileSet) p.getReference("testfileset");
        File baseDir = fileset.getDir(p);
        String log = buildRule.getLog();
        assertTrue("Expecting attribute value to be printed",
            log.indexOf("Attribute attr1 = test") != -1);

        assertTrue("Expecting nested element value to be printed",
            log.indexOf("Fileset basedir = " + baseDir.getAbsolutePath()) != -1);
    }

    @Test
    public void testNoElement() {
        buildRule.executeTarget("noelement");
        assertEquals("Attribute attr1 = test", buildRule.getOutput().trim());
    }

    @Test
    public void testException() {
        try {
            buildRule.executeTarget("exception");
            fail("Should have thrown an exception in the script");
        } catch(BuildException ex) {
            AntAssert.assertContains("TypeError", ex.getMessage());
        }
    }

    @Test
    public void testDoubleDef() {
        buildRule.executeTarget("doubledef");
        String log = buildRule.getLog();
        assertTrue("Task1 did not execute",
            log.indexOf("Task1") != -1);
        assertTrue("Task2 did not execute",
            log.indexOf("Task2") != -1);
    }

    @Test
    public void testDoubleAttribute() {
        try {
            buildRule.executeTarget("doubleAttributeDef");
            fail("Should have detected duplicate attirbute definition");
        } catch(BuildException ex) {
            AntAssert.assertContains("attr1 attribute more than once", ex.getMessage());
        }
    }

    @Test
    public void testProperty() {
        buildRule.executeTarget("property");
        // get the fileset and its basedir
        String log = buildRule.getLog();
        assertTrue("Expecting property in attribute value replaced",
            log.indexOf("Attribute value = test") != -1);
    }


}
