/*
 * Copyright  2004 The Apache Software Foundation
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

import java.io.IOException;

/**
 * @version $Revision$
 */
public class RecorderTest extends BuildFileTest {

    private static final String REC_DIR = "recorder-out";

    public RecorderTest(String name) {
        super(name);
    }

    public void setUp() {
        configureProject("src/etc/testcases/taskdefs/recorder.xml");
        executeTarget("prepare");
    }

    public void tearDown() {
        executeTarget("cleanup");
    }

    public void testNoAppend() throws IOException {
        FileUtils fileUtils = FileUtils.newFileUtils();
        executeTarget("noappend");
        assertTrue(fileUtils
                   .contentEquals(project.resolveFile(REC_DIR 
                                                      + "rectest1.result"),
                                  project.resolveFile(REC_DIR 
                                                      + "rectest1.log")));
    }

    public void testAppend() throws IOException {
        FileUtils fileUtils = FileUtils.newFileUtils();
        executeTarget("append");
        assertTrue(fileUtils
                   .contentEquals(project.resolveFile(REC_DIR 
                                                      + "rectest2.result"),
                                  project.resolveFile(REC_DIR 
                                                      + "rectest2.log")));
    }

    public void testRestart() throws IOException {
        FileUtils fileUtils = FileUtils.newFileUtils();
        executeTarget("restart");
        assertTrue(fileUtils
                   .contentEquals(project.resolveFile(REC_DIR 
                                                      + "rectest3.result"),
                                  project.resolveFile(REC_DIR 
                                                      + "rectest3.log")));
    }

    public void testDeleteRestart() throws IOException {
        FileUtils fileUtils = FileUtils.newFileUtils();
        executeTarget("deleterestart");
        assertTrue(fileUtils
                   .contentEquals(project.resolveFile(REC_DIR 
                                                      + "rectest4.result"),
                                  project.resolveFile(REC_DIR 
                                                      + "rectest4.log")));
    }

}
