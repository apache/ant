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

package org.apache.tools.ant.taskdefs.optional.i18n;

import org.apache.tools.ant.BuildFileTest;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Tests the Translate task.
 *
 * @since     Ant 1.6
 */
public class TranslateTest extends BuildFileTest {
    static private final int BUF_SIZE = 32768;

    private final static String TASKDEFS_DIR = "src/etc/testcases/taskdefs/optional/i18n/translate";

    public TranslateTest(String name) {
        super(name);
    }


    public void setUp() {
        configureProject(TASKDEFS_DIR + "/translate.xml");
    }

    public void test1() {
        executeTarget("test1");
        assertTrue("translation of "+ TASKDEFS_DIR + "/input/template.txt",compareFiles(new File(getProjectDir(), "expected/de/template.txt"),
                new File(getOutputDir(), "de/template.txt")));
    }
    private boolean compareFiles(File file1, File file2) {
        try {
            if (!file1.exists() || !file2.exists()) {
                System.out.println("One or both files do not exist:" + file1.getAbsoluteFile() + ", " + file2.getAbsoluteFile());
                return false;
            }

            if (file1.length() != file2.length()) {
                System.out.println("File size mismatch:" + file1.getAbsoluteFile() + "(" + file1.length() + "), " +
                                   file2.getAbsoluteFile()  + "(" + file2.length() + ")");
                return false;
            }

            // byte - byte compare
            byte[] buffer1 = new byte[BUF_SIZE];
            byte[] buffer2 = new byte[BUF_SIZE];

            FileInputStream fis1 = new FileInputStream(file1);
            FileInputStream fis2 = new FileInputStream(file2);
            int index = 0;
            int read = 0;
            while ((read = fis1.read(buffer1)) != -1) {
                fis2.read(buffer2);
                for (int i = 0; i < read; ++i, ++index) {
                    if (buffer1[i] != buffer2[i]) {
                        System.out.println("Bytes mismatch:" + file1.getAbsoluteFile() + ", " + file2.getAbsoluteFile() +
                                           " at byte " + index);
                        return false;
                    }
                }
            }
            return true;
        }
        catch (IOException e) {
            System.out.println("IOException comparing files: " + file1.getAbsoluteFile() + ", " + file2.getAbsoluteFile());
            return false;
        }
    }
}

