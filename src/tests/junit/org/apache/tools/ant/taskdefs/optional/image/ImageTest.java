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

package org.apache.tools.ant.taskdefs.optional.image;

import org.apache.tools.ant.AntAssert;
import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.util.FileUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;


/**
 * Tests the Image task.
 *
 * @since     Ant 1.5
 */
public class ImageTest {

    private final static String TASKDEFS_DIR = 
        "src/etc/testcases/taskdefs/optional/image/";
    private final static String LARGEIMAGE = "largeimage.jpg";

    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    @Before
    public void setUp() {
        buildRule.configureProject(TASKDEFS_DIR + "image.xml");
    }


    @Test
    public void testEchoToLog() {
        buildRule.executeTarget("testEchoToLog");
        AntAssert.assertContains("Processing File", buildRule.getLog());
    }

    @Test
    public void testSimpleScale(){
        buildRule.executeTarget("testSimpleScale");
        AntAssert.assertContains("Processing File", buildRule.getLog());

        File f = new File(buildRule.getOutputDir(), LARGEIMAGE);
        assertTrue(
                   "Did not create "+f.getAbsolutePath(),
                   f.exists());

    }

    @Test
    public void testOverwriteTrue() throws InterruptedException {
        buildRule.executeTarget("testSimpleScale");
        AntAssert.assertContains("Processing File", buildRule.getLog());
        File f = new File(buildRule.getOutputDir(), LARGEIMAGE);
        assumeTrue("Could not change file modificaiton date",
                f.setLastModified(f.lastModified() - (FILE_UTILS.getFileTimestampGranularity() * 2)));
        long lastModified = f.lastModified();
        buildRule.executeTarget("testOverwriteTrue");
        AntAssert.assertContains("Processing File", buildRule.getLog());
        f = new File(buildRule.getOutputDir(), LARGEIMAGE);
        long overwrittenLastModified = f.lastModified();
        assertTrue("File was not overwritten.",
                   lastModified < overwrittenLastModified);
    }

    @Test
    public void testOverwriteFalse() {
        buildRule.executeTarget("testSimpleScale");
        AntAssert.assertContains("Processing File", buildRule.getLog());
        File f = new File(buildRule.getOutputDir(), LARGEIMAGE);
        long lastModified = f.lastModified();
        buildRule.executeTarget("testOverwriteFalse");
        AntAssert.assertContains("Processing File", buildRule.getLog());
        f = new File(buildRule.getOutputDir(), LARGEIMAGE);
        long overwrittenLastModified = f.lastModified();
        assertTrue("File was overwritten.",
                   lastModified == overwrittenLastModified);
    }

    @Test
    public void testSimpleScaleWithMapper() {
        buildRule.executeTarget("testSimpleScaleWithMapper");
        AntAssert.assertContains("Processing File", buildRule.getLog());
        File f = new File(buildRule.getOutputDir(), "scaled-" + LARGEIMAGE);
        assertTrue(
                   "Did not create "+f.getAbsolutePath(),
                   f.exists());

    }

    @Test
    @Ignore("Previously named in a manner to prevent execution")
    public void testFailOnError() {
        try {
            buildRule.executeTarget("testFailOnError");
            AntAssert.assertContains("Unable to process image stream", buildRule.getLog());
        }
        catch (RuntimeException re){
            assertTrue("Run time exception should say "
                       + "'Unable to process image stream'. :" 
                       + re.toString(),
                       re.toString()
                       .indexOf("Unable to process image stream") > -1);
        }
    }

}

