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

import java.io.File;
import java.io.IOException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.Location;
import org.apache.tools.ant.Project;

/**
 */
public class ImportTest extends BuildFileTest {

    public ImportTest(String name) {
        super(name);
    }

    public void setUp() {
    }

    public void tearDown() {
    }

    public void testSimpleImport() {
        configureProject("src/etc/testcases/taskdefs/import/import.xml");
        assertLogContaining("Before importIn imported topAfter import");
    }

    public void testUnnamedNesting() {
        configureProject("src/etc/testcases/taskdefs/import/unnamedImport.xml",
                         Project.MSG_WARN);
        String log = getLog();
        assertTrue("Warnings logged when not expected: " + log,
                    log.length() == 0);
    }

    public void testSerial() {
        configureProject("src/etc/testcases/taskdefs/import/subdir/serial.xml");
        assertLogContaining("Unnamed2.xmlUnnamed1.xml");
        String fullLog = getFullLog();
        String substring = "Skipped already imported file";
        assertTrue("expecting full log to contain \"" + substring
            + "\" full log was \"" + fullLog + "\"",
            fullLog.indexOf(substring) >= 0);
    }

    // allow this as imported in targets are only tested when a target is run
    public void testImportInTargetNoEffect() {
        configureProject("src/etc/testcases/taskdefs/import/subdir/importintarget.xml");
        expectPropertyUnset("no-import", "foo");
        assertTrue(null == getProject().getReference("baz"));
    }

    // deactivate this test as imports within targets are not allowed
    public void notTestImportInTargetWithEffect() {
        configureProject("src/etc/testcases/taskdefs/import/subdir/importintarget.xml");
        expectPropertySet("do-import", "foo", "bar");
        assertNotNull(getProject().getReference("baz"));
    }

    public void testImportInTargetNotAllowed() {
        configureProject(
            "src/etc/testcases/taskdefs/import/subdir/importintarget.xml");
        expectBuildExceptionContaining(
            "do-import", "not a top level task",
            "import only allowed as a top-level task");
    }

    public void testImportInSequential() {
        configureProject(
            "src/etc/testcases/taskdefs/import/subdir/importinsequential.xml");
        expectPropertySet("within-imported", "foo", "bar");
        assertNotNull(getProject().getReference("baz"));
    }

    public void testImportSameTargets() {
        try {
            configureProject(
                "src/etc/testcases/taskdefs/import/same_target.xml");
        } catch (BuildException ex) {
            String message = ex.getMessage();
            if (message.indexOf("Duplicate target") == -1) {
                assertTrue("Did not see 'Duplicate target' in '" + message +"'", false);
            }
            return;
        }
        assertTrue(
            "Did not see build exception",
            false);
    }

    public void testImportError() {
        try {
            configureProject(
                "src/etc/testcases/taskdefs/import/import_bad_import.xml");
        } catch (BuildException ex) {
            Location lo = ex.getLocation();
            assertTrue(
                "expected location of build exception to be set",
                (lo != null));
            assertTrue(
                "expected location to contain calling file",
                lo.getFileName().indexOf("import_bad_import.xml") != -1);
            assertTrue(
                "expected message of ex to contain called file",
                ex.getMessage().indexOf("bad.xml") != -1);
            return;
        }
        assertTrue(
            "Did not see build exception",
            false);
    }

    public void testSymlinkedImports() throws Exception {
        String ln = "/usr/bin/ln";
        if (!new File(ln).exists()) {
            ln = "/bin/ln";
        }
        if (!new File(ln).exists()) {
            // Running on Windows or something, so skip it.
            return;
        }
        String symlink = "src/etc/testcases/taskdefs/import/symlinks/d3b";
        File symlinkFile = new File(System.getProperty("root"), symlink);
        if (Runtime.getRuntime().exec(new String[] {ln, "-s", "d3a", symlinkFile.getAbsolutePath()}).waitFor() != 0) {
            throw new IOException("'" + ln + " -s d3a " + symlink + "' failed");
        }
        try {
            configureProject(
                "src/etc/testcases/taskdefs/import/symlinks/d1/p1.xml");
            assertPropertyEquals(
                "ant.file.p2",
                new File(System.getProperty("root"), "src/etc/testcases/taskdefs/import/symlinks/d2/p2.xml")
                .getAbsolutePath());
            assertPropertyEquals(
                "ant.file.p3",
                new File(System.getProperty("root"), "src/etc/testcases/taskdefs/import/symlinks/d3b/p3.xml")
                .getAbsolutePath());
        } finally {
            symlinkFile.delete();
        }
    }

    public void testTargetFirst() {
        configureProject("src/etc/testcases/taskdefs/import/importtargetfirst.xml");
        assertLogContaining("Importing targetfirstAfter target firstAfter importing");
    }

    public void testTargetName() {
        configureProject("src/etc/testcases/taskdefs/import/c.xml");
    }

}
