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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.FileUtilities;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.apache.tools.ant.util.FileUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

/**
 * Tests FileSet using the Copy task.
 *
 */
public class CopyTest {

    @Rule
    public final BuildFileRule buildRule = new BuildFileRule();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/copy.xml");
        buildRule.executeTarget("setUp");
    }

    @Test
    public void test1() {
        buildRule.executeTarget("test1");
        File f = new File(buildRule.getProject().getProperty("output"), "copytest1.tmp");
        assertTrue("Copy failed", f.exists());
    }

    @Test
    public void test2() {
        buildRule.executeTarget("test2");
        File f = new File(buildRule.getProject().getProperty("output"), "copytest1dir/copy.xml");
        assertTrue("Copy failed", f.exists());
    }

    @Test
    public void test3() {
        buildRule.executeTarget("test3");
        final String output = buildRule.getProject().getProperty("output");
        final File file3 = new File(output, "copytest3.tmp");
        // rollback file timestamp instead of delaying test
        FileUtilities.rollbackTimestamps(file3, 3);
        buildRule.executeTarget("test3Part2");
        assertTrue(file3.exists());

        File file3a = new File(output, "copytest3a.tmp");
        assertTrue(file3a.exists());
        File file3b = new File(output, "copytest3b.tmp");
        assertTrue(file3b.exists());
        File file3c = new File(output, "copytest3c.tmp");
        assertTrue(file3c.exists());

        // file length checks rely on touch generating a zero byte file

        assertNotEquals("could not overwrite an existing, older file", 0, file3.length());
        assertEquals("could not force overwrite an existing, newer file", 0, file3c.length());
        assertNotEquals("unexpectedly overwrote an existing, newer file", 0, file3b.length());

        // file time checks for java1.2+
        assertEquals(file3a.lastModified(), file3.lastModified());
        assertTrue(file3c.lastModified() < file3a.lastModified());
    }

    @Test
    public void testFilterTest() {
        buildRule.executeTarget("filtertest");
        assertThat(buildRule.getLog(), not(containsString("loop in tokens")));
    }

    @Test
    public void testInfiniteFilter() {
        buildRule.executeTarget("infinitetest");
        assertThat(buildRule.getOutput(), containsString("loop in tokens"));
    }

    @Test
    public void testFilterSet() throws IOException {
        buildRule.executeTarget("testFilterSet");
        File tmp = new File(buildRule.getProject().getProperty("output"), "copy.filterset.tmp");
        File check = new File(buildRule.getProject().getBaseDir(),
                "expected/copy.filterset.filtered");
        assertTrue(tmp.exists());
        assertEquals(FileUtilities.getFileContents(tmp), FileUtilities.getFileContents(check));
    }

    @Test
    public void testFilterChain() throws IOException {
        buildRule.executeTarget("testFilterChain");
        File tmp = new File(buildRule.getProject().getProperty("output"), "copy.filterchain.tmp");
        File check = new File(buildRule.getProject().getBaseDir(),
                "expected/copy.filterset.filtered");
        assertTrue(tmp.exists());
        assertEquals(FileUtilities.getFileContents(tmp), FileUtilities.getFileContents(check));
    }

    @Test
    public void testSingleFileFileset() {
        buildRule.executeTarget("test_single_file_fileset");
        File file = new File(buildRule.getProject().getProperty("output"),
                "copytest_single_file_fileset.tmp");
        assertTrue(file.exists());
    }

    @Test
    public void testSingleFilePath() {
        buildRule.executeTarget("test_single_file_path");
        File file = new File(buildRule.getProject().getProperty("output"),
                "copytest_single_file_path.tmp");
        assertTrue(file.exists());
    }

    @Test
    public void testTranscoding() throws IOException {
        buildRule.executeTarget("testTranscoding");
        File f1 = buildRule.getProject().resolveFile("copy/expected/utf-8");
        File f2 = new File(buildRule.getProject().getProperty("output"), "copytest1.tmp");
        assertEquals(FileUtilities.getFileContents(f1), FileUtilities.getFileContents(f2));
    }

    @Test
    public void testMissingFileIgnore() {
        buildRule.executeTarget("testMissingFileIgnore");
        assertThat(buildRule.getLog(), containsString("Warning: Could not find file"));
    }

    @Test
    public void testMissingFileBail() {
        thrown.expect(BuildException.class);
        thrown.expectMessage(startsWith("Warning: Could not find file "));
        buildRule.executeTarget("testMissingFileBail");
    }

    @Test
    public void testMissingDirIgnore() {
        buildRule.executeTarget("testMissingDirIgnore");
        assertThat(buildRule.getLog(), containsString("Warning: "));
    }

    @Test
    public void testMissingDirBail() {
        thrown.expect(BuildException.class);
        thrown.expectMessage(endsWith(" does not exist."));
        buildRule.executeTarget("testMissingDirBail");
    }

    @Test
    public void testFileResourcePlain() {
        buildRule.executeTarget("testFileResourcePlain");
        final String toDir = buildRule.getProject().getProperty("to.dir");
        assertTrue(new File(toDir + "/file1.txt").exists());
        assertTrue(new File(toDir + "/file2.txt").exists());
        assertTrue(new File(toDir + "/file3.txt").exists());
    }

    @Ignore("Previously ignored by naming convention")
    @Test
    public void testFileResourceWithMapper() {
        buildRule.executeTarget("testFileResourceWithMapper");
        final String toDir = buildRule.getProject().getProperty("to.dir");
        assertTrue(new File(toDir + "/file1.txt.bak").exists());
        assertTrue(new File(toDir + "/file2.txt.bak").exists());
        assertTrue(new File(toDir + "/file3.txt.bak").exists());
    }

    @Test
    public void testFileResourceWithFilter() {
        buildRule.executeTarget("testFileResourceWithFilter");
        File file1 = new File(buildRule.getProject().getProperty("to.dir") + "/fileNR.txt");
        assertTrue(file1.exists());
        try (FileReader f = new FileReader(file1)) {
            String file1Content = FileUtils.readFully(f);
            assertEquals("This is file 42", file1Content);
        } catch (IOException e) {
            // no-op: not a real business error
        }
    }

    @Test
    public void testPathAsResource() {
        buildRule.executeTarget("testPathAsResource");
        final String toDir = buildRule.getProject().getProperty("to.dir");
        assertTrue(new File(toDir + "/file1.txt").exists());
        assertTrue(new File(toDir + "/file2.txt").exists());
        assertTrue(new File(toDir + "/file3.txt").exists());
    }

    @Test
    public void testZipfileset() {
        buildRule.executeTarget("testZipfileset");
        final String toDir = buildRule.getProject().getProperty("to.dir");
        assertTrue(new File(toDir + "/file1.txt").exists());
        assertTrue(new File(toDir + "/file2.txt").exists());
        assertTrue(new File(toDir + "/file3.txt").exists());
    }

    @Test
    public void testDirset() {
        buildRule.executeTarget("testDirset");
    }

    @Ignore("Previously ignored due to naming convention")
    @Test
    public void testResourcePlain() {
        buildRule.executeTarget("testResourcePlain");
    }

    @Ignore("Previously ignored due to naming convention")
    @Test
    public void testResourcePlainWithMapper() {
        buildRule.executeTarget("testResourcePlainWithMapper");
    }

    @Ignore("Previously ignored due to naming convention")
    @Test
    public void testResourcePlainWithFilter() {
        buildRule.executeTarget("testResourcePlainWithFilter");
    }

    @Ignore("Previously ignored due to naming convention")
    @Test
    public void testOnlineResources() {
        buildRule.executeTarget("testOnlineResources");
    }

    /**
     * Tests that the {@code copy} task doesn't corrupt the source file, if the target of the copy operation is a symlink
     * to the source file being copied
     *
     * @throws Exception if something goes wrong
     * @see <a href="https://bz.apache.org/bugzilla/show_bug.cgi?id=60644">issue 60644</a>
     */
    @Test
    public void testCopyToSymlinkedSelf() throws Exception {
        // we are only going to test against systems that support symlinks
        assumeTrue("Symlinks not supported on this operating system", Os.isFamily(Os.FAMILY_UNIX));

        // setup the source files to run copying against
        buildRule.executeTarget("setupSelfCopyTesting");
        final File testDir = new File(buildRule.getProject().getProperty("self.copy.test.root.dir"));
        assertTrue(testDir + " was expected to be a directory", testDir.isDirectory());
        final File srcFile = new File(testDir, "file.txt");
        assertTrue("Source file " + srcFile + " was expected to be a file", srcFile.isFile());
        final long originalFileSize = srcFile.length();
        final String originalContent;
        try (BufferedReader reader = new BufferedReader(new FileReader(srcFile))) {
            originalContent = FileUtils.readFully(reader);
        }
        assertTrue("Content missing in file " + srcFile, originalContent != null && !originalContent.isEmpty());

        // run the copy tests
        buildRule.executeTarget("testSelfCopy");
        // make sure the source file hasn't been impacted by the copy
        assertSizeAndContent(srcFile, originalFileSize, originalContent);
        final File symlinkedFile = new File(testDir, "sylmink-file.txt");
        assertTrue(symlinkedFile + " was expected to be a file", symlinkedFile.isFile());
        assertSizeAndContent(symlinkedFile, originalFileSize, originalContent);

        final File symlinkedTestDir = new File(buildRule.getProject().getProperty("self.copy.test.symlinked.dir"));
        assertTrue(symlinkedTestDir + " was expected to be a directory", symlinkedTestDir.isDirectory());
        assertSizeAndContent(new File(symlinkedTestDir, "file.txt"), originalFileSize, originalContent);
        assertSizeAndContent(new File(symlinkedTestDir, "sylmink-file.txt"), originalFileSize, originalContent);

    }

    private void assertSizeAndContent(final File file, final long expectedSize, final String expectedContent) throws IOException {
        assertTrue(file + " was expected to be a file", file.isFile());
        assertEquals("Unexpected size of file " + file, expectedSize, file.length());
        final String content;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            content = FileUtils.readFully(reader);
        }
        assertEquals("Unexpected content in file " + file, expectedContent, content);
    }
}
