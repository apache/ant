/*
 * Copyright  2003-2004 The Apache Software Foundation
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
}

