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

package org.apache.tools.ant.taskdefs.optional.image;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.util.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.File;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

/**
 * Tests ImageIOTask.
 *
 * @since     Ant 1.10.6
 */
public class ImageIOTest {

    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();
    private static final String LARGEIMAGE = "largeimage.jpg";

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/optional/image/imageio.xml");
    }


    @Test
    public void testEchoToLog() {
        buildRule.executeTarget("testEchoToLog");
        assertThat(buildRule.getLog(), containsString("Processing File"));
    }

    @Test
    public void testSimpleScale() {
        buildRule.executeTarget("testSimpleScale");
        assertThat(buildRule.getLog(), containsString("Processing File"));

        File f = new File(buildRule.getOutputDir(), LARGEIMAGE);
        assertTrue("Did not create " + f.getAbsolutePath(), f.exists());
    }

    @Test
    public void testOverwriteTrue() {
        buildRule.executeTarget("testSimpleScale");
        assertThat(buildRule.getLog(), containsString("Processing File"));
        File f = new File(buildRule.getOutputDir(), LARGEIMAGE);
        assumeTrue("Could not change file modification date",
                f.setLastModified(f.lastModified() - FILE_UTILS.getFileTimestampGranularity() * 2));
        long lastModified = f.lastModified();
        buildRule.executeTarget("testOverwriteTrue");
        assertThat(buildRule.getLog(), containsString("Processing File"));
        f = new File(buildRule.getOutputDir(), LARGEIMAGE);
        long overwrittenLastModified = f.lastModified();
        assertTrue("File was not overwritten.", lastModified < overwrittenLastModified);
    }

    @Test
    public void testDrawOverwriteTrue() {
        buildRule.executeTarget("testSimpleScale");
        assertThat(buildRule.getLog(), containsString("Processing File"));
        File f = new File(buildRule.getOutputDir(), LARGEIMAGE);
        assumeTrue("Could not change file modification date",
                f.setLastModified(f.lastModified() - FILE_UTILS.getFileTimestampGranularity() * 2));
        long lastModified = f.lastModified();
        buildRule.executeTarget("testDrawOverwriteTrue");
        assertThat(buildRule.getLog(), containsString("Processing File"));
        f = new File(buildRule.getOutputDir(), LARGEIMAGE);
        long overwrittenLastModified = f.lastModified();
        assertTrue("File was not overwritten.", lastModified < overwrittenLastModified);
    }

    @Test
    public void testOverwriteFalse() {
        buildRule.executeTarget("testSimpleScale");
        assertThat(buildRule.getLog(), containsString("Processing File"));
        File f = new File(buildRule.getOutputDir(), LARGEIMAGE);
        long lastModified = f.lastModified();
        buildRule.executeTarget("testOverwriteFalse");
        assertThat(buildRule.getLog(), containsString("Processing File"));
        f = new File(buildRule.getOutputDir(), LARGEIMAGE);
        long overwrittenLastModified = f.lastModified();
        assertEquals("File was overwritten.", lastModified, overwrittenLastModified);
    }

    @Test
    public void testSimpleScaleWithMapper() {
        buildRule.executeTarget("testSimpleScaleWithMapper");
        assertThat(buildRule.getLog(), containsString("Processing File"));
        File f = new File(buildRule.getOutputDir(), "scaled-" + LARGEIMAGE);
        assertTrue("Did not create " + f.getAbsolutePath(), f.exists());
    }

    @Test
    public void testFlip() {
        buildRule.executeTarget("testFlip");
        assertThat(buildRule.getFullLog(), containsString("Flipping an image"));
        File f = new File(buildRule.getOutputDir(), LARGEIMAGE);
        assertTrue("Did not create " + f.getAbsolutePath(), f.exists());
    }

    @Test
    public void testFailOnError() {
        final String message = "Unsupported Image Type";
        thrown.expect(BuildException.class);
        thrown.expectMessage(message);
        try {
            buildRule.executeTarget("testFailOnError");
        } finally {
            assertThat(buildRule.getLog(), containsString(message));
        }
    }

}
