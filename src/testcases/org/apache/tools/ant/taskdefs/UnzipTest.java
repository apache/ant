/* 
 * Copyright  2000-2001,2003-2004 Apache Software Foundation
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
import org.apache.tools.ant.util.FileUtils;

/**
 * @author Nico Seessle <nico@seessle.de> 
 * @author Stefan Bodewig
 */
public class UnzipTest extends BuildFileTest { 
    
    public UnzipTest(String name) { 
        super(name);
    }    
    
    public void setUp() { 
        configureProject("src/etc/testcases/taskdefs/unzip.xml");
    }
    
    public void tearDown() {
        executeTarget("cleanup");
    }

    public void test1() { 
        expectBuildException("test1", "required argument not specified");
    }

    public void test2() { 
        expectBuildException("test2", "required argument not specified");
    }

    public void test3() { 
        expectBuildException("test3", "required argument not specified");
    }


    public void testRealTest() throws java.io.IOException {
        FileUtils fileUtils = FileUtils.newFileUtils();
        executeTarget("realTest");
        assertTrue(fileUtils.contentEquals(project.resolveFile("../asf-logo.gif"),
                                           project.resolveFile("asf-logo.gif")));
    }
    
    public void testTestZipTask() throws java.io.IOException {
        FileUtils fileUtils = FileUtils.newFileUtils();
        executeTarget("testZipTask");
        assertTrue(fileUtils.contentEquals(project.resolveFile("../asf-logo.gif"),
                                           project.resolveFile("asf-logo.gif")));
    }
    
    public void testTestUncompressedZipTask() throws java.io.IOException {
        FileUtils fileUtils = FileUtils.newFileUtils();
        executeTarget("testUncompressedZipTask");
        assertTrue(fileUtils.contentEquals(project.resolveFile("../asf-logo.gif"),
                                           project.resolveFile("asf-logo.gif")));
    }
    
    /*
     * PR 11100
     */
    public void testPatternSetExcludeOnly() {
        executeTarget("testPatternSetExcludeOnly");
        assertTrue("1/foo is excluded",
                   !getProject().resolveFile("unziptestout/1/foo").exists());
        assertTrue("2/bar is not excluded",
                   getProject().resolveFile("unziptestout/2/bar").exists());
    }

    /*
     * PR 11100
     */
    public void testPatternSetIncludeOnly() {
        executeTarget("testPatternSetIncludeOnly");
        assertTrue("1/foo is not included",
                   !getProject().resolveFile("unziptestout/1/foo").exists());
        assertTrue("2/bar is included",
                   getProject().resolveFile("unziptestout/2/bar").exists());
    }

    /*
     * PR 11100
     */
    public void testPatternSetIncludeAndExclude() {
        executeTarget("testPatternSetIncludeAndExclude");
        assertTrue("1/foo is not included",
                   !getProject().resolveFile("unziptestout/1/foo").exists());
        assertTrue("2/bar is excluded",
                   !getProject().resolveFile("unziptestout/2/bar").exists());
    }

    /*
     * PR 16213
     */
    public void testSelfExtractingArchive() {
        executeTarget("selfExtractingArchive");
    }


    /*
     * PR 20969
     */
    public void testPatternSetSlashOnly() {
        executeTarget("testPatternSetSlashOnly");
        assertTrue("1/foo is not included",
                   !getProject().resolveFile("unziptestout/1/foo").exists());
        assertTrue("2/bar is included",
                   getProject().resolveFile("unziptestout/2/bar").exists());
    }

    /*
     * PR 10504
     */
    public void testEncoding() {
        executeTarget("encodingTest");
        assertTrue("foo has been properly named",
                   getProject().resolveFile("unziptestout/foo").exists());
    }

}
