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

import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.util.FileUtils;

import java.io.File;

public class TouchTest extends BuildFileTest {

    private static String TOUCH_FILE = "src/etc/testcases/taskdefs/touchtest";

    /** Utilities used for file operations */
    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    public TouchTest(String name) {
        super(name);
    }

    public void setUp() {
        configureProject("src/etc/testcases/taskdefs/touch.xml");
    }

    public void tearDown() {
        executeTarget("cleanup");
    }

    public long getTargetTime() {

        File file = new File(System.getProperty("root"), TOUCH_FILE);
        if(!file.exists()) {
            throw new BuildException("failed to touch file " + file);
        }
        return file.lastModified();
    }

    /**
     * No real test, simply checks whether the dateformat without
     * seconds is accepted - by erroring out otherwise.
     */
    public void testNoSeconds() {
        executeTarget("noSeconds");
        long time = getTargetTime();
    }

    /**
     * No real test, simply checks whether the dateformat with
     * seconds is accepted - by erroring out otherwise.
     */
    public void testSeconds() {
        executeTarget("seconds");
        long time=getTargetTime();
    }
    /**
     * verify that the millis test sets things up
     */
    public void testMillis() {
        touchFile("testMillis", 662256000000L);
    }

    /**
     * verify that the default value defaults to now
     */
    public void testNow() {
        long now=System.currentTimeMillis();
        executeTarget("testNow");
        long time = getTargetTime();
        assertTimesNearlyMatch(time,now,5000);
    }
    /**
     * verify that the millis test sets things up
     */
    public void test2000() {
        touchFile("test2000", 946080000000L);
    }

    /**
     * test the file list
     */
    public void testFilelist() {
        touchFile("testFilelist", 662256000000L);
    }

    /**
     * test the file set
     */
    public void testFileset() {
        touchFile("testFileset", 946080000000L);
    }

    /**
     * test the resource collection
     */
    public void testResourceCollection() {
        touchFile("testResourceCollection", 1662256000000L);
    }

    /**
     * test the mapped file set
     */
    public void testMappedFileset() {
        executeTarget("testMappedFileset");
    }

    /**
     * test the explicit mapped file set
     */
    public void testExplicitMappedFileset() {
        executeTarget("testExplicitMappedFileset");
    }

    /**
     * test the mapped file list
     */
    public void testMappedFilelist() {
        executeTarget("testMappedFilelist");
    }

    /**
     * test the pattern attribute
     */
    public void testGoodPattern() {
        executeTarget("testGoodPattern");
    }

    /**
     * test the pattern attribute again
     */
    public void testBadPattern() {
        expectBuildExceptionContaining("testBadPattern",
            "No parsing exception thrown", "Unparseable");
    }

    /**
     * run a target to touch the test file; verify the timestamp is as expected
     * @param targetName
     * @param timestamp
     */
    private void touchFile(String targetName, long timestamp) {
        executeTarget(targetName);
        long time = getTargetTime();
        assertTimesNearlyMatch(timestamp, time);
    }

    /**
     * assert that two times are within the current FS granularity;
     * @param timestamp
     * @param time
     */
    public void assertTimesNearlyMatch(long timestamp,long time) {
        long granularity= FILE_UTILS.getFileTimestampGranularity();
        assertTimesNearlyMatch(timestamp, time, granularity);
    }

    /**
     * assert that two times are within a specified range
     * @param timestamp
     * @param time
     * @param range
     */
    private void assertTimesNearlyMatch(long timestamp, long time, long range) {
        assertTrue("Time " + timestamp + " is not within " + range + " ms of "
            + time, (Math.abs(time - timestamp) <= range));
    }
}
