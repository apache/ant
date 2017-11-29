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

/*
 * Since the initial version of this file was deveolped on the clock on
 * an NSF grant I should say the following boilerplate:
 *
 * This material is based upon work supported by the National Science
 * Foundaton under Grant No. EIA-0196404. Any opinions, findings, and
 * conclusions or recommendations expressed in this material are those
 * of the author and do not necessarily reflect the views of the
 * National Science Foundation.
 */

package org.apache.tools.ant.taskdefs.optional.unix;

import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.taskdefs.condition.Os;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.util.SymbolicLinkUtils;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;

/**
 * Test cases for the Symlink task. Link creation, link deletion, recording
 * of links in multiple directories, and restoration of links recorded are
 * all tested. A separate test for the utility method Symlink.deleteSymlink
 * is not included because action="delete" only prints a message and calls
 * Symlink.deleteSymlink, making a separate test redundant.
 *
 */

public class SymlinkTest {

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    private boolean supportsSymlinks = Os.isFamily("unix");

    @Before
    public void setUp() {
        Assume.assumeTrue("Symlinks not supported on current operating system", supportsSymlinks);
        buildRule.configureProject("src/etc/testcases/taskdefs/optional/unix/symlink.xml");
        buildRule.executeTarget("setUp");
    }

    @Test
    public void testSingle() {
        buildRule.executeTarget("test-single");
        Project p = buildRule.getProject();
        assertNotNull("Failed to create file",
                          p.getProperty("test.single.file.created"));
        assertNotNull("Failed to create link",
                          p.getProperty("test.single.link.created"));
    }

    @Test
    public void testDelete() {
        buildRule.executeTarget("test-delete");
        Project p = buildRule.getProject();
        String linkDeleted = p.getProperty("test.delete.link.still.there");
        assertNotNull("Actual file deleted by symlink",
                      p.getProperty("test.delete.file.still.there"));
        if (linkDeleted != null) {
            fail(linkDeleted);
        }
    }

    @Test
    public void testRecord() {
        buildRule.executeTarget("test-record");
        Project p = buildRule.getProject();

        assertNotNull("Failed to create dir1",
                      p.getProperty("test.record.dir1.created"));

        assertNotNull("Failed to create dir2",
                      p.getProperty("test.record.dir2.created"));

        assertNotNull("Failed to create file1",
                      p.getProperty("test.record.file1.created"));

        assertNotNull("Failed to create file2",
                      p.getProperty("test.record.file2.created"));

        assertNotNull("Failed to create fileA",
                      p.getProperty("test.record.fileA.created"));

        assertNotNull("Failed to create fileB",
                      p.getProperty("test.record.fileB.created"));

        assertNotNull("Failed to create fileC",
                      p.getProperty("test.record.fileC.created"));

        assertNotNull("Failed to create link1",
                      p.getProperty("test.record.link1.created"));

        assertNotNull("Failed to create link2",
                      p.getProperty("test.record.link2.created"));

        assertNotNull("Failed to create link3",
                      p.getProperty("test.record.link3.created"));

        assertNotNull("Failed to create dirlink",
                      p.getProperty("test.record.dirlink.created"));

        assertNotNull("Failed to create dirlink2",
                      p.getProperty("test.record.dirlink2.created"));

        assertNotNull("Couldn't record links in dir1",
                      p.getProperty("test.record.dir1.recorded"));

        assertNotNull("Couldn't record links in dir2",
                      p.getProperty("test.record.dir2.recorded"));

        String dir3rec = p.getProperty("test.record.dir3.recorded");

        if (dir3rec != null) {
            fail(dir3rec);
        }

    }

    @Test
    public void testRecreate() {
        buildRule.executeTarget("test-recreate");
        Project p = buildRule.getProject();
        String link1Rem = p.getProperty("test.recreate.link1.not.removed");
        String link2Rem = p.getProperty("test.recreate.link2.not.removed");
        String link3Rem = p.getProperty("test.recreate.link3.not.removed");
        String dirlinkRem = p.getProperty("test.recreate.dirlink.not.removed");
        if (link1Rem != null) {
            fail(link1Rem);
        }
        if (link2Rem != null) {
            fail(link2Rem);
        }
        if (link3Rem != null) {
            fail(link3Rem);
        }
        if (dirlinkRem != null) {
            fail(dirlinkRem);
        }
        assertNotNull("Failed to recreate link1",
                      p.getProperty("test.recreate.link1.recreated"));
        assertNotNull("Failed to recreate link2",
                      p.getProperty("test.recreate.link2.recreated"));
        assertNotNull("Failed to recreate link3",
                      p.getProperty("test.recreate.link3.recreated"));
        assertNotNull("Failed to recreate dirlink",
                      p.getProperty("test.recreate.dirlink.recreated"));

        String doubleRecreate = p.getProperty("test.recreate.dirlink2.recreated.twice");

        if (doubleRecreate != null) {
            fail(doubleRecreate);
        }

        assertNotNull("Failed to alter dirlink3",
                      p.getProperty("test.recreate.dirlink3.was.altered"));
    }

    @Test
    public void testSymbolicLinkUtilsMethods() throws Exception {

        buildRule.executeTarget("test-fileutils");
        SymbolicLinkUtils su = SymbolicLinkUtils.getSymbolicLinkUtils();

        java.io.File f = new File(buildRule.getOutputDir(), "file1");
        assertTrue(f.exists());
        assertFalse(f.isDirectory());
        assertTrue(f.isFile());
        assertFalse(su.isSymbolicLink(f.getAbsolutePath()));
        assertFalse(su.isSymbolicLink(f.getParentFile(),
                                      f.getName()));
        assertFalse(su.isDanglingSymbolicLink(f.getAbsolutePath()));
        assertFalse(su.isDanglingSymbolicLink(f.getParentFile(),
                                              f.getName()));

        f = new File(buildRule.getOutputDir(), "dir1");
        assertTrue(f.exists());
        assertTrue(f.isDirectory());
        assertFalse(f.isFile());
        assertFalse(su.isSymbolicLink(f.getAbsolutePath()));
        assertFalse(su.isSymbolicLink(f.getParentFile(),
                                      f.getName()));
        assertFalse(su.isDanglingSymbolicLink(f.getAbsolutePath()));
        assertFalse(su.isDanglingSymbolicLink(f.getParentFile(),
                                              f.getName()));

        f = new File(buildRule.getOutputDir(), "file2");
        assertFalse(f.exists());
        assertFalse(f.isDirectory());
        assertFalse(f.isFile());
        assertFalse(su.isSymbolicLink(f.getAbsolutePath()));
        assertFalse(su.isSymbolicLink(f.getParentFile(),
                                      f.getName()));
        assertFalse(su.isDanglingSymbolicLink(f.getAbsolutePath()));
        assertFalse(su.isDanglingSymbolicLink(f.getParentFile(),
                                              f.getName()));

        f = new File(buildRule.getOutputDir(), "dir2");
        assertFalse(f.exists());
        assertFalse(f.isDirectory());
        assertFalse(f.isFile());
        assertFalse(su.isSymbolicLink(f.getAbsolutePath()));
        assertFalse(su.isSymbolicLink(f.getParentFile(),
                                      f.getName()));
        assertFalse(su.isDanglingSymbolicLink(f.getAbsolutePath()));
        assertFalse(su.isDanglingSymbolicLink(f.getParentFile(),
                                              f.getName()));


        f = new File(buildRule.getOutputDir(), "file.there");
        assertTrue(f.exists());
        assertFalse(f.isDirectory());
        assertTrue(f.isFile());
        assertTrue(su.isSymbolicLink(f.getAbsolutePath()));
        assertTrue(su.isSymbolicLink(f.getParentFile(),
                                     f.getName()));
        assertFalse(su.isDanglingSymbolicLink(f.getAbsolutePath()));
        assertFalse(su.isDanglingSymbolicLink(f.getParentFile(),
                                              f.getName()));

        f = new File(buildRule.getOutputDir(), "dir.there");
        assertTrue(f.exists());
        assertTrue(f.isDirectory());
        assertFalse(f.isFile());
        assertTrue(su.isSymbolicLink(f.getAbsolutePath()));
        assertTrue(su.isSymbolicLink(f.getParentFile(),
                                     f.getName()));
        assertFalse(su.isDanglingSymbolicLink(f.getAbsolutePath()));
        assertFalse(su.isDanglingSymbolicLink(f.getParentFile(),
                                              f.getName()));

        // it is not possible to find out that symbolic links pointing
        // to inexistent files or directories are symbolic links
        // it used to be possible to detect this on Mac
        // this is not true under Snow Leopard and JDK 1.5
        // Removing special handling of MacOS until someone shouts
        // Antoine
        f = new File(buildRule.getOutputDir(), "file.notthere");
        assertFalse(f.exists());
        assertFalse(f.isDirectory());
        assertFalse(f.isFile());
        assertTrue(su.isSymbolicLink(f.getAbsolutePath()) == false);
        assertTrue(su.isSymbolicLink(f.getParentFile(), f.getName()) == false);
        assertTrue(su.isDanglingSymbolicLink(f.getAbsolutePath()));
        assertTrue(su.isDanglingSymbolicLink(f.getParentFile(),
                                             f.getName()));

        f = new File(buildRule.getOutputDir(), "dir.notthere");
        assertFalse(f.exists());
        assertFalse(f.isDirectory());
        assertFalse(f.isFile());
        assertTrue(su.isSymbolicLink(f.getAbsolutePath()) == false);
        assertTrue(su.isSymbolicLink(f.getParentFile(), f.getName()) == false);
        assertTrue(su.isDanglingSymbolicLink(f.getAbsolutePath()));
        assertTrue(su.isDanglingSymbolicLink(f.getParentFile(),
                                             f.getName()));

    }

    @After
    public void tearDown() {
        if (buildRule.getProject() != null) {
            buildRule.executeTarget("tearDown");
        }
    }

}
