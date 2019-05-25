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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.FileUtilities;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests CVSLogin task.
 *
 */
public class CVSPassTest {
    private final String EOL = System.lineSeparator();
    private static final String JAKARTA_URL =
        ":pserver:anoncvs@jakarta.apache.org:/home/cvspublic Ay=0=h<Z";
    private static final String XML_URL =
        ":pserver:anoncvs@xml.apache.org:/home/cvspublic Ay=0=h<Z";
    private static final String TIGRIS_URL =
        ":pserver:guest@cvs.tigris.org:/cvs AIbdZ,";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Rule
    public final BuildFileRule buildRule = new BuildFileRule();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/cvspass.xml");
    }

    @After
    public void tearDown() {
        buildRule.executeTarget("cleanup");
    }

    @Test
    public void testNoCVSRoot() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("cvsroot is required");
        buildRule.executeTarget("test1");
    }

    @Test
    public void testNoPassword() {
        thrown.expect(BuildException.class);
        thrown.expectMessage("password is required");
        buildRule.executeTarget("test2");
    }

    @Test
    public void testPassFile() throws Exception {
        buildRule.executeTarget("test3");
        File f = new File(buildRule.getProject().getBaseDir(), "testpassfile.tmp");

        assertTrue("Passfile " + f + " not created", f.exists());

        assertEquals(JAKARTA_URL + EOL, FileUtilities.getFileContents(f));
    }

    @Test
    public void testPassFileDuplicateEntry() throws Exception {
        buildRule.executeTarget("test4");
        File f = new File(buildRule.getProject().getBaseDir(), "testpassfile.tmp");

        assertTrue("Passfile " + f + " not created", f.exists());

        assertEquals(JAKARTA_URL + EOL+ TIGRIS_URL + EOL,
            FileUtilities.getFileContents(f));
    }

    @Test
    public void testPassFileMultipleEntry() throws Exception {
        buildRule.executeTarget("test5");
        File f = new File(buildRule.getProject().getBaseDir(), "testpassfile.tmp");

        assertTrue("Passfile " + f + " not created", f.exists());

        assertEquals(JAKARTA_URL + EOL + XML_URL + EOL+ TIGRIS_URL + EOL,
            FileUtilities.getFileContents(f));
    }
}
