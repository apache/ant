/*
 * Copyright  2002,2004 The Apache Software Foundation
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.util.FileUtils;

/**
 * Test to see if static initializers are invoked the same way
 * when <java> is invoked in forked and unforked modes.
 *
 * @author Magesh Umasankar
 */
public class InitializeClassTest extends BuildFileTest {

    public InitializeClassTest(String name) {
        super(name);
    }

    public void setUp() {
        configureProject("src/etc/testcases/taskdefs/initializeclass.xml");
    }

    public void testAll() throws IOException {
        executeTarget("forked");
        PrintStream ps = System.out;
        File f1 = new File("src/etc/testcases/taskdefs/forkedout");
        File f2 = new File("src/etc/testcases/taskdefs/unforkedout");
        PrintStream newps = new PrintStream(new FileOutputStream(f2));
        System.setOut(newps);
        project.executeTarget("unforked");
        System.setOut(ps);
        newps.close();
        FileUtils fu = FileUtils.newFileUtils();
        assertTrue("Forked - non-forked mismatch", fu.contentEquals(f1, f2));
    }

    public void tearDown() {
        File f1 = new File("src/etc/testcases/taskdefs/forkedout");
        File f2 = new File("src/etc/testcases/taskdefs/unforkedout");
        f1.delete();
        f2.delete();
    }
}
