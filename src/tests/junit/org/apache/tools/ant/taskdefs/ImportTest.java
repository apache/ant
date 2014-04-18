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

import static org.apache.tools.ant.AntAssert.assertContains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.Location;
import org.apache.tools.ant.Project;
import org.junit.Assume;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

public class ImportTest {

	@Rule
	public BuildFileRule buildRule = new BuildFileRule();
	
	@Test
    public void testSimpleImport() {
        buildRule.configureProject("src/etc/testcases/taskdefs/import/import.xml");
        assertContains("Before importIn imported topAfter import", buildRule.getLog());
    }

	@Test
    public void testUnnamedNesting() {
        buildRule.configureProject("src/etc/testcases/taskdefs/import/unnamedImport.xml",
                         Project.MSG_WARN);
        String log = buildRule.getLog();
        assertTrue("Warnings logged when not expected: " + log,
                    log.length() == 0);
    }

	@Test
    public void testSerial() {
        buildRule.configureProject("src/etc/testcases/taskdefs/import/subdir/serial.xml");
        assertContains("Unnamed2.xmlUnnamed1.xml", buildRule.getLog());
        assertContains("Expected string was not found in log",
        		"Skipped already imported file", buildRule.getFullLog());
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
        buildRule.configureProject(
            "src/etc/testcases/taskdefs/import/subdir/importintarget.xml");
        try {
        	buildRule.executeTarget("do-import");
        	fail("Build exception should have been thrown as import only allowed in top level task");
        } catch(BuildException ex) {
        	assertContains( "not a top level task", "import only allowed as a top-level task", ex.getMessage());
        }
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
        try {
            buildRule.configureProject(
                "src/etc/testcases/taskdefs/import/same_target.xml");
            fail("Expected build exception");
        } catch (BuildException ex) {
        	assertContains("Message did not contain expected contents", "Duplicate target", ex.getMessage());
        }
    }

    @Test
    public void testImportError() {
        try {
            buildRule.configureProject(
                "src/etc/testcases/taskdefs/import/import_bad_import.xml");
            fail("Build exception should have been thrown");
        } catch (BuildException ex) {
            Location lo = ex.getLocation();
            assertNotNull(
                "expected location of build exception to be set", lo);
            assertContains(
                "expected location to contain calling file", "import_bad_import.xml", lo.getFileName());
            assertContains(
                "expected message of ex to contain called file", "bad.xml", ex.getMessage());
        }
    }

    @Test
    public void testSymlinkedImports() throws Exception {
        String ln = "/usr/bin/ln";
        if (!new File(ln).exists()) {
            ln = "/bin/ln";
        }
        Assume.assumeTrue("Current system does not support Symlinks", new File(ln).exists());
        String symlink = "src/etc/testcases/taskdefs/import/symlinks/d3b";
        File symlinkFile = new File(System.getProperty("root"), symlink);
        if (Runtime.getRuntime().exec(new String[] {ln, "-s", "d3a", symlinkFile.getAbsolutePath()}).waitFor() != 0) {
            throw new IOException("'" + ln + " -s d3a " + symlink + "' failed");
        }
        try {
            buildRule.configureProject(
                "src/etc/testcases/taskdefs/import/symlinks/d1/p1.xml");
            assertEquals(
                buildRule.getProject().getProperty("ant.file.p2"),
                new File(System.getProperty("root"), "src/etc/testcases/taskdefs/import/symlinks/d2/p2.xml")
                .getAbsolutePath());
            assertEquals(
                buildRule.getProject().getProperty("ant.file.p3"),
                new File(System.getProperty("root"), "src/etc/testcases/taskdefs/import/symlinks/d3b/p3.xml")
                .getAbsolutePath());
        } finally {
            symlinkFile.delete();
        }
    }

    @Test
    public void testTargetFirst() {
        buildRule.configureProject("src/etc/testcases/taskdefs/import/importtargetfirst.xml");
        assertContains("Importing targetfirstAfter target firstAfter importing", buildRule.getLog());
    }

    @Test
    public void testTargetName() {
        buildRule.configureProject("src/etc/testcases/taskdefs/import/c.xml");
    }

}
