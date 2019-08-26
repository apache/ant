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

import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.JavaEnvUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.File;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeNotNull;
import static org.junit.Assume.assumeTrue;

/**
 * Tests the Image task.
 *
 * @since     Ant 1.5
 */
public class ImageTest {

    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();
    private static final String LARGEIMAGE = "largeimage.jpg";

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        /* JAI depends on internal API removed in Java 9 */
        assumeFalse(JavaEnvUtils.isAtLeastJavaVersion(JavaEnvUtils.JAVA_9));
        buildRule.configureProject("src/etc/testcases/taskdefs/optional/image/image.xml");
    }


    @Test
    public void testEchoToLog() {
        buildRule.executeTarget("testEchoToLog");
        assumeNotNull("JPEG codec is unavailable in classpath",
                buildRule.getProject().getProperty("jpeg.codec.available"));
        assertThat(buildRule.getLog(), containsString("Processing File"));
    }

    @Test
    public void testSimpleScale() {
        buildRule.executeTarget("testSimpleScale");
        assumeNotNull("JPEG codec is unavailable in classpath",
                buildRule.getProject().getProperty("jpeg.codec.available"));
        assertThat(buildRule.getLog(), containsString("Processing File"));
        File f = new File(buildRule.getOutputDir(), LARGEIMAGE);
        assertTrue("Did not create " + f.getAbsolutePath(), f.exists());
    }

    @Test
    public void testOverwriteTrue() {
        buildRule.executeTarget("testSimpleScale");
        assumeNotNull("JPEG codec is unavailable in classpath",
                buildRule.getProject().getProperty("jpeg.codec.available"));
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
    public void testOverwriteFalse() {
        buildRule.executeTarget("testSimpleScale");
        assumeNotNull("JPEG codec is unavailable in classpath",
                buildRule.getProject().getProperty("jpeg.codec.available"));
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
        assumeNotNull("JPEG codec is unavailable in classpath",
                buildRule.getProject().getProperty("jpeg.codec.available"));
        assertThat(buildRule.getLog(), containsString("Processing File"));
        File f = new File(buildRule.getOutputDir(), "scaled-" + LARGEIMAGE);
        assertTrue("Did not create " + f.getAbsolutePath(), f.exists());
    }

    @Test
    @Ignore("Previously named in a manner to prevent execution")
    public void testFailOnError() {
        final String message = "Unable to render RenderedOp for this operation.";
        thrown.expect(RuntimeException.class);
        thrown.expectMessage(message);
        try {
            buildRule.executeTarget("testFailOnError");
        } finally {
            assertThat(buildRule.getLog(), containsString(message));
        }
    }

}
