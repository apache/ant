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

import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.util.FileUtils;
import java.io.File;
import java.io.IOException;

/**
 * Tests the Move task.
 *
 */
public class MoveTest extends BuildFileTest {

    /** Utilities used for file operations */
    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    public MoveTest(String name) {
        super(name);
    }

    public void setUp() {
        configureProject("src/etc/testcases/taskdefs/move.xml");
    }

    public void tearDown() {
        executeTarget("cleanup");
    }

    public void testFilterSet() throws IOException {
        executeTarget("testFilterSet");
        File tmp  = new File(getProjectDir(), "move.filterset.tmp");
        File check  = new File(getProjectDir(), "expected/copy.filterset.filtered");
        assertTrue(tmp.exists());
        assertTrue(FILE_UTILS.contentEquals(tmp, check));
    }

    public void testFilterChain() throws IOException {
        executeTarget("testFilterChain");
        File tmp  = new File(getProjectDir(), "move.filterchain.tmp");
        File check  = new File(getProjectDir(), "expected/copy.filterset.filtered");
        assertTrue(tmp.exists());
        assertTrue(FILE_UTILS.contentEquals(tmp, check));
    }

    /** Bugzilla Report 11732 */
    public void testDirectoryRemoval() throws IOException {
        executeTarget("testDirectoryRemoval");
        assertTrue(!getProject().resolveFile("E/B/1").exists());
        assertTrue(getProject().resolveFile("E/C/2").exists());
        assertTrue(getProject().resolveFile("E/D/3").exists());
        assertTrue(getProject().resolveFile("A/B/1").exists());
        assertTrue(!getProject().resolveFile("A/C/2").exists());
        assertTrue(!getProject().resolveFile("A/D/3").exists());
        assertTrue(!getProject().resolveFile("A/C").exists());
        assertTrue(!getProject().resolveFile("A/D").exists());
    }

    /** Bugzilla Report 18886 */
    public void testDirectoryRetaining() throws IOException {
        executeTarget("testDirectoryRetaining");
        assertTrue(getProject().resolveFile("E").exists());
        assertTrue(getProject().resolveFile("E/1").exists());
        assertTrue(!getProject().resolveFile("A/1").exists());
        assertTrue(getProject().resolveFile("A").exists());
    }

    public void testCompleteDirectoryMove() throws IOException {
        testCompleteDirectoryMove("testCompleteDirectoryMove");
    }

    public void testCompleteDirectoryMove2() throws IOException {
        testCompleteDirectoryMove("testCompleteDirectoryMove2");
    }

    private void testCompleteDirectoryMove(String target) throws IOException {
        executeTarget(target);
        assertTrue(getProject().resolveFile("E").exists());
        assertTrue(getProject().resolveFile("E/1").exists());
        assertTrue(!getProject().resolveFile("A/1").exists());
        // <path> swallows the basedir, it seems
        //assertTrue(!getProject().resolveFile("A").exists());
    }

    public void testPathElementMove() throws IOException {
        executeTarget("testPathElementMove");
        assertTrue(getProject().resolveFile("E").exists());
        assertTrue(getProject().resolveFile("E/1").exists());
        assertTrue(!getProject().resolveFile("A/1").exists());
        assertTrue(getProject().resolveFile("A").exists());
    }

    public void testMoveFileAndFileset() {
        executeTarget("testMoveFileAndFileset");
    }

    public void testCompleteDirectoryMoveToExistingDir() {
        executeTarget("testCompleteDirectoryMoveToExistingDir");
    }

    public void testCompleteDirectoryMoveFileToFile() {
        executeTarget("testCompleteDirectoryMoveFileToFile");
    }

    public void testCompleteDirectoryMoveFileToDir() {
        executeTarget("testCompleteDirectoryMoveFileToDir");
    }

    public void testCompleteDirectoryMoveFileAndFileset() {
        executeTarget("testCompleteDirectoryMoveFileAndFileset");
    }

    public void testCompleteDirectoryMoveFileToExistingFile() {
        executeTarget("testCompleteDirectoryMoveFileToExistingFile");
    }

    public void testCompleteDirectoryMoveFileToExistingDir() {
        executeTarget("testCompleteDirectoryMoveFileToExistingDir");
    }

    public void testCompleteDirectoryMoveFileToDirWithExistingFile() {
        executeTarget("testCompleteDirectoryMoveFileToDirWithExistingFile");
    }

    public void testCompleteDirectoryMoveFileToDirWithExistingDir() {
        executeTarget("testCompleteDirectoryMoveFileToDirWithExistingDir");
    }

}
