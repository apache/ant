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

package org.apache.tools.ant.taskdefs.optional.i18n;

import org.apache.tools.ant.BuildFileRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import static org.junit.Assert.assertTrue;

/**
 * Tests the Translate task.
 *
 * @since     Ant 1.6
 */
public class TranslateTest {

    @Rule
    public final BuildFileRule buildRule = new BuildFileRule();

    private static final int BUF_SIZE = 32768;

    private static final String TASKDEFS_DIR = "src/etc/testcases/taskdefs/optional/i18n/translate";

    @Before
    public void setUp() {
        buildRule.configureProject(TASKDEFS_DIR + "/translate.xml");
    }

    @Test
    public void test1() throws IOException {
        buildRule.executeTarget("test1");
        assertTrue("translation of " + TASKDEFS_DIR + "/input/template.txt",
                compareFiles(new File(buildRule.getProject().getBaseDir(), "expected/de/template.txt"),
                        new File(buildRule.getOutputDir(), "de/template.txt")));
    }

    private boolean compareFiles(File file1, File file2) throws IOException {
        if (!file1.exists() || !file2.exists()) {
            return false;
        }

        if (file1.length() != file2.length()) {
            return false;
        }

        // byte - byte compare
        byte[] buffer1 = new byte[BUF_SIZE];
        byte[] buffer2 = new byte[BUF_SIZE];

        try (FileInputStream fis1 = new FileInputStream(file1);
             FileInputStream fis2 = new FileInputStream(file2)) {
            int read = 0;
            while ((read = fis1.read(buffer1)) != -1) {
                fis2.read(buffer2);
                for (int i = 0; i < read; ++i) {
                    if (buffer1[i] != buffer2[i]) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
