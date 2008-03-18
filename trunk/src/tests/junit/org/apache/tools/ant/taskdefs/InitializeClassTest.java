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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.util.FileUtils;

/**
 * Test to see if static initializers are invoked the same way
 * when <java> is invoked in forked and unforked modes.
 *
 */
public class InitializeClassTest extends BuildFileTest {

    /** Utilities used for file operations */
    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    private File f1 = new File(System.getProperty("root"), "src/etc/testcases/taskdefs/forkedout");
    private File f2 = new File(System.getProperty("root"), "src/etc/testcases/taskdefs/unforkedout");

    public InitializeClassTest(String name) {
        super(name);
    }

    public void setUp() {
        configureProject("src/etc/testcases/taskdefs/initializeclass.xml");
    }

    public void testAll() throws IOException {
        executeTarget("forked");
        PrintStream ps = System.out;
        PrintStream newps = new PrintStream(new FileOutputStream(f2));
        System.setOut(newps);
        project.executeTarget("unforked");
        System.setOut(ps);
        newps.close();
        assertTrue("Forked - non-forked mismatch", FILE_UTILS.contentEquals(f1, f2));
    }

    public void tearDown() {
        f1.delete();
        f2.delete();
    }
}
