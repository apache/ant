/*
 * Copyright  2003-2004 Apache Software Foundation
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

import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.util.FileUtils;

import java.io.File;

public class TouchTest extends BuildFileTest {

    private static String touchfile="src/etc/testcases/taskdefs/touchtest";

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

        File file = new File(touchfile);
        if(!file.exists()) {
            throw new BuildException("failed to touch file "+touchfile);
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
        touchFile("testMillis", 1234567);
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
    public void test1970() {
        touchFile("test1970", 0);
    }

    /**
     * test the file list
     */
    public void testFilelist() {
        touchFile("testFilelist", 100000);
    }

    /**
     * test the file set
     */
    public void testFileset() {
        touchFile("testFileset", 200000);
    }

    /**
     * run a target to touch the test file; verify the timestamp is as expected
     * @param targetName
     * @param timestamp
     */
    private void touchFile(String targetName, long timestamp) {
        executeTarget(targetName);
        long time = getTargetTime();
        assertTimesNearlyMatch(timestamp,time);
    }

    /**
     * assert that two times are within the current FS granularity;
     * @param timestamp
     * @param time
     */
    public void assertTimesNearlyMatch(long timestamp,long time) {
        long granularity= FileUtils.newFileUtils().getFileTimestampGranularity();
        assertTimesNearlyMatch(timestamp, time, granularity);
    }

    /**
     * assert that two times are within a specified range
     * @param timestamp
     * @param time
     * @param range
     */
    private void assertTimesNearlyMatch(long timestamp, long time, long range) {
        assertTrue("Time "+timestamp+" is not within "+range+" ms of "+time,
                Math.abs(time-timestamp)<=range);
    }
}
