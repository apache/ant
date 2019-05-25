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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.FileUtilities;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Test to see if static initializers are invoked the same way
 * when <java> is invoked in forked and unforked modes.
 *
 */
public class InitializeClassTest {

    @Rule
    public final BuildFileRule buildRule = new BuildFileRule();

    private File f1;
    private File f2;

    @Before
    public void setUp() {
        assertNotNull("build.tests.value not set", System.getProperty("build.tests.value"));
        buildRule.configureProject("src/etc/testcases/taskdefs/initializeclass.xml");
        f1 = buildRule.getProject().resolveFile("forkedout");
        f2 = buildRule.getProject().resolveFile("unforkedout");
    }

    @Test
    public void testAll() throws IOException {
        buildRule.executeTarget("forked");
        synchronized (System.out) {
            PrintStream ps = System.out;
            try (PrintStream newps = new PrintStream(new FileOutputStream(f2))) {
                System.setOut(newps);
                buildRule.getProject().executeTarget("unforked");
            } finally {
                System.setOut(ps);
            }
        }
        assertEquals(FileUtilities.getFileContents(f1), FileUtilities.getFileContents(f2));
    }

    @After
    public void tearDown() {
        f1.delete();
        f2.delete();
    }
}
