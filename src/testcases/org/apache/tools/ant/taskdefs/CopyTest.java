/* 
 * Copyright  2000-2004 Apache Software Foundation
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

import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.JavaEnvUtils;
import java.io.File;
import java.io.IOException;

/**
 * Tests FileSet using the Copy task.
 *
 * @author David Rees <dave@ubiqsoft.com>
 */
public class CopyTest extends BuildFileTest {

    public CopyTest(String name) {
        super(name);
    }

    public void setUp() {
        configureProject("src/etc/testcases/taskdefs/copy.xml");
    }

    public void test1() {
        executeTarget("test1");
        File f = new File(getProjectDir(), "copytest1.tmp");
        if ( !f.exists()) {
            fail("Copy failed");
        }
    }

    public void tearDown() {
        executeTarget("cleanup");
    }

    public void test2() {
        executeTarget("test2");
        File f = new File(getProjectDir(), "copytest1dir/copy.xml");
        if ( !f.exists()) {
            fail("Copy failed");
        }
    }

    public void test3() {
        executeTarget("test3");
        File file3  = new File(getProjectDir(), "copytest3.tmp");
        assertTrue(file3.exists());
        File file3a = new File(getProjectDir(), "copytest3a.tmp");
        assertTrue(file3a.exists());
        File file3b = new File(getProjectDir(), "copytest3b.tmp");
        assertTrue(file3b.exists());
        File file3c = new File(getProjectDir(), "copytest3c.tmp");
        assertTrue(file3c.exists());

        //file length checks rely on touch generating a zero byte file
        if(file3.length()==0) {
            fail("could not overwrite an existing, older file");
        }
        if(file3c.length()!=0) {
            fail("could not force overwrite an existing, newer file");
        }
        if(file3b.length()==0) {
            fail("unexpectedly overwrote an existing, newer file");
        }

        //file time checks for java1.2+
        if (!JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_1)) {
            assertTrue(file3a.lastModified()==file3.lastModified());
            assertTrue(file3c.lastModified()<file3a.lastModified());
        }

    }

    public void testFilterTest() {
        executeTarget("filtertest");
        assertTrue(getOutput().indexOf("loop in tokens") == -1);
    }

    public void testInfiniteFilter() {
        executeTarget("infinitetest");
        assertTrue(getOutput().indexOf("loop in tokens") != -1);
    }
    
    public void testFilterSet() throws IOException {
        executeTarget("testFilterSet");
        FileUtils fileUtils = FileUtils.newFileUtils();
        File tmp  = new File(getProjectDir(), "copy.filterset.tmp");
        File check  = new File(getProjectDir(), "expected/copy.filterset.filtered");
        assertTrue(tmp.exists());
        assertTrue(fileUtils.contentEquals(tmp, check));
    }

    public void testFilterChain() throws IOException {
        executeTarget("testFilterChain");
        FileUtils fileUtils = FileUtils.newFileUtils();
        File tmp  = new File(getProjectDir(), "copy.filterchain.tmp");
        File check  = new File(getProjectDir(), "expected/copy.filterset.filtered");
        assertTrue(tmp.exists());
        assertTrue(fileUtils.contentEquals(tmp, check));
    }

    public void testSingleFileFileset() {
        executeTarget("test_single_file_fileset");
        File file  = new File(getProjectDir(),
                                        "copytest_single_file_fileset.tmp");
        assertTrue(file.exists());
    }

    public void testTranscoding() throws IOException {
        executeTarget("testTranscoding");
        FileUtils fileUtils = FileUtils.newFileUtils();
        File f1 = getProject().resolveFile("copy/expected/utf-8");
        File f2 = getProject().resolveFile("copytest1.tmp");
        assertTrue(fileUtils.contentEquals(f1, f2));
    }

    public void testMissingFileIgnore() {
        expectLogContaining("testMissingFileIgnore", 
                            "Warning: Could not find file ");        
    }

    public void testMissingFileBail() {
        expectBuildException("testMissingFileBail", "not-there doesn't exist");
        assertTrue(getBuildException().getMessage()
                   .startsWith("Warning: Could not find file "));
    }

    public void testMissingDirIgnore() {
        expectLogContaining("testMissingDirIgnore", "Warning: ");        
    }

    public void testMissingDirBail() {
        expectBuildException("testMissingDirBail", "not-there doesn't exist");
        assertTrue(getBuildException().getMessage().endsWith(" not found."));
    }
}
