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

import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.util.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;

import static org.apache.tools.ant.AntAssert.assertContains;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TouchTest {
    
    @Rule
    public final BuildFileRule buildRule = new BuildFileRule();

    private static String TOUCH_FILE = "src/etc/testcases/taskdefs/touchtest";

    /** Utilities used for file operations */
    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();


    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/touch.xml");
    }

    @After
    public void tearDown() {
        buildRule.executeTarget("cleanup");
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
    @Test
    public void testNoSeconds() {
        buildRule.executeTarget("noSeconds");
        getTargetTime();
    }

    /**
     * No real test, simply checks whether the dateformat with
     * seconds is accepted - by erroring out otherwise.
     */
    @Test
    public void testSeconds() {
        buildRule.executeTarget("seconds");
        getTargetTime();
    }
    /**
     * verify that the millis test sets things up
     */
    @Test
    public void testMillis() {
        touchFile("testMillis", 662256000000L);
    }

    /**
     * verify that the default value defaults to now
     */
    @Test
    public void testNow() {
        long now=System.currentTimeMillis();
        buildRule.executeTarget("testNow");
        long time = getTargetTime();
        assertTimesNearlyMatch(time,now,5000);
    }
    /**
     * verify that the millis test sets things up
     */
    @Test
    public void test2000() {
        touchFile("test2000", 946080000000L);
    }

    /**
     * test the file list
     */
    @Test
    public void testFilelist() {
        touchFile("testFilelist", 662256000000L);
    }

    /**
     * test the file set
     */
    @Test
    public void testFileset() {
        touchFile("testFileset", 946080000000L);
    }

    /**
     * test the resource collection
     */
    @Test
    public void testResourceCollection() {
        touchFile("testResourceCollection", 1662256000000L);
    }

    /**
     * test the mapped file set
     */
    @Test
    public void testMappedFileset() {
        buildRule.executeTarget("testMappedFileset");
    }

    /**
     * test the explicit mapped file set
     */
    @Test
    public void testExplicitMappedFileset() {
        buildRule.executeTarget("testExplicitMappedFileset");
    }

    /**
     * test the mapped file list
     */
    @Test
    public void testMappedFilelist() {
        buildRule.executeTarget("testMappedFilelist");
    }

    /**
     * test the pattern attribute
     */
    @Test
    public void testGoodPattern() {
        buildRule.executeTarget("testGoodPattern");
    }

    /**
     * test the pattern attribute again
     */
    @Test
    public void testBadPattern() {
        try {
            buildRule.executeTarget("testBadPattern");
            fail("No parsing exception thrown");
        } catch (BuildException ex) {
            assertContains("Unparseable", ex.getMessage());
        }

    }

    /**
     * run a target to touch the test file; verify the timestamp is as expected
     * @param targetName
     * @param timestamp
     */
    private void touchFile(String targetName, long timestamp) {
        buildRule.executeTarget(targetName);
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
