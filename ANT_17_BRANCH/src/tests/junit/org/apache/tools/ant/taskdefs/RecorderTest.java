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

import java.io.IOException;

/**
 */
public class RecorderTest extends BuildFileTest {

    private static final String REC_IN = "recorder/";
    private static final String REC_DIR = "recorder-out/";

    /** Utilities used for file operations */
    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

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
        executeTarget("noappend");
        assertTrue(FILE_UTILS
                   .contentEquals(project.resolveFile(REC_IN
                                                      + "rectest1.result"),
                                  project.resolveFile(REC_DIR
                                                      + "rectest1.log"), true));
    }

    public void testAppend() throws IOException {
        executeTarget("append");
        assertTrue(FILE_UTILS
                   .contentEquals(project.resolveFile(REC_IN
                                                      + "rectest2.result"),
                                  project.resolveFile(REC_DIR
                                                      + "rectest2.log"), true));
    }

    public void testRestart() throws IOException {
        executeTarget("restart");
        assertTrue(FILE_UTILS
                   .contentEquals(project.resolveFile(REC_IN
                                                      + "rectest3.result"),
                                  project.resolveFile(REC_DIR
                                                      + "rectest3.log"), true));
    }

    public void testDeleteRestart() throws IOException {
        executeTarget("deleterestart");
        assertTrue(FILE_UTILS
                   .contentEquals(project.resolveFile(REC_IN
                                                      + "rectest4.result"),
                                  project.resolveFile(REC_DIR
                                                      + "rectest4.log"), true));
    }

}
