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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeTrue;

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.Project;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ImportTest {

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testSimpleImport() {
        buildRule.configureProject("src/etc/testcases/taskdefs/import/import.xml");
        assertThat(buildRule.getLog(), containsString("Before importIn imported topAfter import"));
    }

    @Test
    public void testUnnamedNesting() {
        buildRule.configureProject("src/etc/testcases/taskdefs/import/unnamedImport.xml",
                         Project.MSG_WARN);
        String log = buildRule.getLog();
        assertEquals("Warnings logged when not expected: " + log, 0, log.length());
    }

    @Test
    public void testSerial() {
        buildRule.configureProject("src/etc/testcases/taskdefs/import/subdir/serial.xml");
        assertThat(buildRule.getLog(), containsString("Unnamed2.xmlUnnamed1.xml"));
        assertThat("Expected string was not found in log",
                buildRule.getFullLog(), containsString("Skipped already imported file"));
    }

    // allow this as imported in targets are only tested when a target is run
    @Test
    public void testImportInTargetNoEffect() {
        buildRule.configureProject("src/etc/testcases/taskdefs/import/subdir/importintarget.xml");
        buildRule.executeTarget("no-import");
        assertNull(buildRule.getProject().getProperty("foo"));
        assertNull(buildRule.getProject().getReference("baz"));
    }

    @Ignore("deactivate this test as imports within targets are not allowed")
    @Test
    public void notTestImportInTargetWithEffect() {
        buildRule.configureProject("src/etc/testcases/taskdefs/import/subdir/importintarget.xml");
        buildRule.executeTarget("do-import");
        assertEquals(buildRule.getProject().getProperty("foo"), "bar");
        assertNotNull(buildRule.getProject().getReference("baz"));
    }

    @Test
    public void testImportInTargetNotAllowed() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("import only allowed as a top-level task");
        buildRule.configureProject("src/etc/testcases/taskdefs/import/subdir/importintarget.xml");
        buildRule.executeTarget("do-import");
    }

    @Test
    public void testImportInSequential() {
        buildRule.configureProject(
            "src/etc/testcases/taskdefs/import/subdir/importinsequential.xml");
        buildRule.executeTarget("within-imported");
        assertEquals(buildRule.getProject().getProperty("foo"), "bar");
        assertNotNull(buildRule.getProject().getReference("baz"));
    }

    @Test
    public void testImportSameTargets() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("Duplicate target");
        buildRule.configureProject("src/etc/testcases/taskdefs/import/same_target.xml");
    }

    @Test
    public void testImportError() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("bad.xml");
        thrown.expect(hasProperty("location",
                hasProperty("fileName", containsString("import_bad_import.xml"))));
        buildRule.configureProject("src/etc/testcases/taskdefs/import/import_bad_import.xml");
    }

    @Test
    public void testSymlinkedImports() throws Exception {
        String ln = "/usr/bin/ln";
        if (!new File(ln).exists()) {
            ln = "/bin/ln";
        }
        assumeTrue("Current system does not support Symlinks", new File(ln).exists());
        buildRule.configureProject("src/etc/testcases/taskdefs/import/import.xml");
        File symlinkFile = buildRule.getProject().resolveFile("symlinks/d3b");
        assertEquals("'" + ln + " -s d3a " + symlinkFile.getAbsolutePath() + "' failed",
                Runtime.getRuntime().exec(new String[] {ln, "-s", "d3a", symlinkFile.getAbsolutePath()}).waitFor(), 0);
        try {
            buildRule.configureProject("src/etc/testcases/taskdefs/import/symlinks/d1/p1.xml");
            assertEquals(buildRule.getProject().getProperty("ant.file.p2"),
                buildRule.getProject().resolveFile("../d2/p2.xml").getAbsolutePath());
            assertEquals(buildRule.getProject().getProperty("ant.file.p3"),
                    buildRule.getProject().resolveFile("../d3b/p3.xml").getAbsolutePath());
        } finally {
            symlinkFile.delete();
        }
    }

    @Test
    public void testTargetFirst() {
        buildRule.configureProject("src/etc/testcases/taskdefs/import/importtargetfirst.xml");
        assertThat(buildRule.getLog(),
                containsString("Importing targetfirstAfter target firstAfter importing"));
    }

    @Test
    public void testTargetName() {
        buildRule.configureProject("src/etc/testcases/taskdefs/import/c.xml");
    }

}
