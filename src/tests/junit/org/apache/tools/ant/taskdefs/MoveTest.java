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

import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.FileUtilities;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests the Move task.
 *
 */
public class MoveTest {

    @Rule
    public final BuildFileRule buildRule = new BuildFileRule();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/move.xml");
        buildRule.executeTarget("setUp");
    }

    @Test
    public void testFilterSet() throws IOException {
        buildRule.executeTarget("testFilterSet");
        File tmp  = new File(buildRule.getProject().getProperty("output"), "move.filterset.tmp");
        File check  = new File(buildRule.getProject().getBaseDir(), "expected/copy.filterset.filtered");
        assertTrue(tmp.exists());
        assertEquals(FileUtilities.getFileContents(check), FileUtilities.getFileContents(tmp));
    }

    @Test
    public void testFilterChain() throws IOException {
        buildRule.executeTarget("testFilterChain");
        File tmp  = new File(buildRule.getProject().getProperty("output"), "move.filterchain.tmp");
        File check  = new File(buildRule.getProject().getBaseDir(), "expected/copy.filterset.filtered");
        assertTrue(tmp.exists());
        assertEquals(FileUtilities.getFileContents(check), FileUtilities.getFileContents(tmp));
    }

    /** Bugzilla Report 11732 */
    @Test
    public void testDirectoryRemoval() {

        buildRule.executeTarget("testDirectoryRemoval");
        String output = buildRule.getProject().getProperty("output");
        assertFalse(new File(output, "E/B/1").exists());
        assertTrue(new File(output, "E/C/2").exists());
        assertTrue(new File(output, "E/D/3").exists());
        assertTrue(new File(output, "A/B/1").exists());
        assertFalse(new File(output, "A/C/2").exists());
        assertFalse(new File(output, "A/D/3").exists());
        assertFalse(new File(output, "A/C").exists());
        assertFalse(new File(output, "A/D").exists());
    }

    /** Bugzilla Report 18886 */
    @Test
    public void testDirectoryRetaining() {
        buildRule.executeTarget("testDirectoryRetaining");
        String output = buildRule.getProject().getProperty("output");
        assertTrue(new File(output, "E").exists());
        assertTrue(new File(output, "E/1").exists());
        assertFalse(new File(output, "A/1").exists());
        assertTrue(new File(output, "A").exists());
    }

    @Test
    public void testCompleteDirectoryMove() {
        testCompleteDirectoryMove("testCompleteDirectoryMove");
    }

    @Test
    public void testCompleteDirectoryMove2() {
        testCompleteDirectoryMove("testCompleteDirectoryMove2");
    }

    private void testCompleteDirectoryMove(String target) {
        buildRule.executeTarget(target);
        String output = buildRule.getProject().getProperty("output");
        assertTrue(new File(output, "E").exists());
        assertTrue(new File(output, "E/1").exists());
        assertFalse(new File(output, "A/1").exists());
        // <path> swallows the basedir, it seems
        //assertFalse(new File(getOutputDir(), "A").exists());
    }

    @Test
    public void testPathElementMove() {
        buildRule.executeTarget("testPathElementMove");
        String output = buildRule.getProject().getProperty("output");
        assertTrue(new File(output, "E").exists());
        assertTrue(new File(output, "E/1").exists());
        assertFalse(new File(output, "A/1").exists());
        assertTrue(new File(output, "A").exists());
    }

    @Test
    public void testMoveFileAndFileset() {
        buildRule.executeTarget("testMoveFileAndFileset");
    }

    @Test
    public void testCompleteDirectoryMoveToExistingDir() {
        buildRule.executeTarget("testCompleteDirectoryMoveToExistingDir");
    }

    @Test
    public void testCompleteDirectoryMoveFileToFile() {
        buildRule.executeTarget("testCompleteDirectoryMoveFileToFile");
    }

    @Test
    public void testCompleteDirectoryMoveFileToDir() {
        buildRule.executeTarget("testCompleteDirectoryMoveFileToDir");
    }

    @Test
    public void testCompleteDirectoryMoveFileAndFileset() {
        buildRule.executeTarget("testCompleteDirectoryMoveFileAndFileset");
    }

    @Test
    public void testCompleteDirectoryMoveFileToExistingFile() {
        buildRule.executeTarget("testCompleteDirectoryMoveFileToExistingFile");
    }

    @Test
    public void testCompleteDirectoryMoveFileToExistingDir() {
        buildRule.executeTarget("testCompleteDirectoryMoveFileToExistingDir");
    }

    @Test
    public void testCompleteDirectoryMoveFileToDirWithExistingFile() {
        buildRule.executeTarget("testCompleteDirectoryMoveFileToDirWithExistingFile");
    }

    @Test
    public void testCompleteDirectoryMoveFileToDirWithExistingDir() {
        buildRule.executeTarget("testCompleteDirectoryMoveFileToDirWithExistingDir");
    }

}
