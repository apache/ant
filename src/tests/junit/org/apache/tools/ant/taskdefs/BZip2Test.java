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
import org.apache.tools.bzip2.CBZip2InputStream;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import static org.hamcrest.Matchers.endsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 */
public class BZip2Test {

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    private File outputDir;

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/bzip2.xml");
        outputDir = new File(buildRule.getProject().getProperty("output"));
        buildRule.executeTarget("prepare");
    }

    @Test
    public void testRealTest() throws IOException {
        buildRule.executeTarget("realTest");

        // doesn't work: Depending on the compression engine used,
        // compressed bytes may differ. False errors would be
        // reported.
        // assertTrue("File content mismatch",
        // FILE_UTILS.contentEquals(project.resolveFile("expected/asf-logo-huge.tar.bz2"),
        // project.resolveFile("asf-logo-huge.tar.bz2")));

        // We have to compare the decompressed content instead:

        File originalFile =
            buildRule.getProject().resolveFile("expected/asf-logo-huge.tar.bz2");
        File actualFile   = new File(outputDir, "asf-logo-huge.tar.bz2");

        InputStream originalIn =
            new BufferedInputStream(Files.newInputStream(originalFile.toPath()));
        assertEquals((byte) 'B', originalIn.read());
        assertEquals((byte) 'Z', originalIn.read());

        InputStream actualIn =
            new BufferedInputStream(Files.newInputStream(actualFile.toPath()));
        assertEquals((byte) 'B', actualIn.read());
        assertEquals((byte) 'Z', actualIn.read());

        originalIn = new CBZip2InputStream(originalIn);
        actualIn   = new CBZip2InputStream(actualIn);

        while (true) {
            int expected = originalIn.read();
            int actual   = actualIn.read();
            assertEquals("File content mismatch", expected, actual);
            if (expected < 0) {
                break;
            }
        }

        originalIn.close();
        actualIn.close();
    }

    @Test
    public void testResource() {
        buildRule.executeTarget("realTestWithResource");
    }

    @Test
    public void testDateCheck() {
        buildRule.executeTarget("testDateCheck");
        String log = buildRule.getLog();
        assertThat("Expecting message ending with 'asf-logo.gif.bz2 is up to date.' but got '"
                        + log + "'", log, endsWith("asf-logo.gif.bz2 is up to date."));
    }

}
