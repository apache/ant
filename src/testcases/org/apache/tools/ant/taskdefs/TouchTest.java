/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "Ant" and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
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
