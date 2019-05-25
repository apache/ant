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
import org.apache.tools.ant.util.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertTrue;

/**
 */
public class RecorderTest {

    @Rule
    public final BuildFileRule buildRule = new BuildFileRule();

    private static final String REC_IN = "recorder/";

    /** Utilities used for file operations */
    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/recorder.xml");
        buildRule.executeTarget("setUp");
    }

    @Test
    public void testNoAppend() throws IOException {
        buildRule.executeTarget("noappend");
        assertTrue(FILE_UTILS
                   .contentEquals(buildRule.getProject().resolveFile(REC_IN
                                                      + "rectest1.result"),
                                  new File(buildRule.getOutputDir(),
                                                      "rectest1.log"), true));
    }

    @Test
    public void testAppend() throws IOException {
        buildRule.executeTarget("append");
        assertTrue(FILE_UTILS
                   .contentEquals(buildRule.getProject().resolveFile(REC_IN
                                                      + "rectest2.result"),
                           new File(buildRule.getOutputDir(),
                                                      "rectest2.log"), true));
    }

    @Test
    public void testRestart() throws IOException {
        buildRule.executeTarget("restart");
        assertTrue(FILE_UTILS
                   .contentEquals(buildRule.getProject().resolveFile(REC_IN
                                                      + "rectest3.result"),
                           new File(buildRule.getOutputDir(), "rectest3.log"), true));
    }

    @Test
    public void testDeleteRestart() throws IOException {
        buildRule.executeTarget("deleterestart");
        assertTrue(FILE_UTILS
                   .contentEquals(buildRule.getProject().resolveFile(REC_IN
                                                      + "rectest4.result"),
                           new File(buildRule.getOutputDir(),
                                                      "rectest4.log"), true));
    }

    @Test
    public void testSubBuild() throws IOException {
        buildRule.executeTarget("subbuild");
        assertTrue(FILE_UTILS
                   .contentEquals(buildRule.getProject().resolveFile(REC_IN
                                                      + "rectest5.result"),
                           new File(buildRule.getOutputDir(), "rectest5.log"), true));
        assertTrue(FILE_UTILS
                   .contentEquals(buildRule.getProject().resolveFile(REC_IN
                                                      + "rectest6.result"),
                           new File(buildRule.getOutputDir(), "rectest6.log"), true));

    }

}
