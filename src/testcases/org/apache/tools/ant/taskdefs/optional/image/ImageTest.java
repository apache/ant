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

import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.util.FileUtils;

import java.io.File;


/**
 * Tests the Image task.
 *
 * @since     Ant 1.5
 */
public class ImageTest extends BuildFileTest {

    private final static String TASKDEFS_DIR = 
        "src/etc/testcases/taskdefs/optional/image/";
    private final static String LARGEIMAGE = "largeimage.jpg";

    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    public ImageTest(String name) {
        super(name);
    }


    public void setUp() {
        configureProject(TASKDEFS_DIR + "image.xml");
    }


    public void tearDown() {
        executeTarget("cleanup");
    }

    public void testEchoToLog() {
        expectLogContaining("testEchoToLog", "Processing File");
    }

    public void testSimpleScale(){
        expectLogContaining("testSimpleScale", "Processing File");
        File f = createRelativeFile("/dest/" + LARGEIMAGE);
        assertTrue(
                   "Did not create "+f.getAbsolutePath(),
                   f.exists());

    }

    public void testOverwriteTrue() {
        expectLogContaining("testSimpleScale", "Processing File");
        File f = createRelativeFile("/dest/" + LARGEIMAGE);
        long lastModified = f.lastModified();
        try {
            Thread.sleep(FILE_UTILS
                         .getFileTimestampGranularity());
        }
        catch (InterruptedException e) {}
        expectLogContaining("testOverwriteTrue", "Processing File");
        f = createRelativeFile("/dest/" + LARGEIMAGE);
        long overwrittenLastModified = f.lastModified();
        assertTrue("File was not overwritten.",
                   lastModified < overwrittenLastModified);
    }

    public void testOverwriteFalse() {
        expectLogContaining("testSimpleScale", "Processing File");
        File f = createRelativeFile("/dest/" + LARGEIMAGE);
        long lastModified = f.lastModified();
        expectLogContaining("testOverwriteFalse", "Processing File");
        f = createRelativeFile("/dest/" + LARGEIMAGE);
        long overwrittenLastModified = f.lastModified();
        assertTrue("File was overwritten.",
                   lastModified == overwrittenLastModified);
    }


    public void off_testFailOnError() {
        try {
            expectLogContaining("testFailOnError", 
                                "Unable to process image stream");
        }
        catch (RuntimeException re){
            assertTrue("Run time exception should say "
                       + "'Unable to process image stream'. :" 
                       + re.toString(),
                       re.toString()
                       .indexOf("Unable to process image stream") > -1);
        }
    }



    protected File createRelativeFile(String filename) {
        if (filename.equals(".")) {
            return getProjectDir();
        }
        // else
        return new File(getProjectDir(), filename);
    }
}

