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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeNotNull;

import java.io.File;
import java.util.GregorianCalendar;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.MagicNames;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.util.FileUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

/**
 * Unit test for the &lt;exec&gt; task.
 */
public class ExecTaskTest {

    @Rule
    public BuildFileRule buildRule = new BuildFileRule();

    private static final int TIME_TO_WAIT = 1;
    /** maximum time allowed for the build in milliseconds */
    private static final int MAX_BUILD_TIME = 6000;
    private static final int SECURITY_MARGIN = 4000; // wait 4 second extras
    // the test failed with 100 ms of margin on cvs.apache.org on August 1st, 2003
    // the test randomly failed with 3 s of margin on Windows Jenkins slaves on during July 2014

    /** Utilities used for file operations */
    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    private volatile boolean buildFinished = false;

    @Before
    public void setUp() {
        buildRule.configureProject("src/etc/testcases/taskdefs/exec/exec.xml");
    }

    @Test
    public void testspawn() throws InterruptedException {
        buildRule.getProject().executeTarget("setUp");
        assumeNotNull(buildRule.getProject().getProperty("test.can.run"));
        MonitoredBuild myBuild = new MonitoredBuild(new File(
                buildRule.getProject().getProperty(MagicNames.ANT_FILE)), "spawn");
        File logFile = FILE_UTILS.createTempFile("spawn", "log",
                new File(buildRule.getProject().getProperty("output")), false, false);
        // this is guaranteed by FileUtils#createTempFile
        assertFalse("log file not existing", logFile.exists());
        // make the spawned process run 1 seconds
        myBuild.setTimeToWait(TIME_TO_WAIT);
        myBuild.setLogFile(logFile.getAbsolutePath());
        myBuild.addBuildListener(new MonitoredBuildListener());
        myBuild.start();
        GregorianCalendar startwait = new GregorianCalendar();
        // this loop runs parallel to the build
        while (!buildFinished) {
            Thread.sleep(10);
            GregorianCalendar now = new GregorianCalendar();
            // security
            if (now.getTime().getTime() - startwait.getTime().getTime() > MAX_BUILD_TIME) {
                System.out.println("aborting wait, too long "
                        + (now.getTime().getTime() - startwait.getTime().getTime())
                        + "milliseconds");
                break;
            }
        }
        // now wait until the spawned process is finished
        Thread.sleep((TIME_TO_WAIT) * 1000 + SECURITY_MARGIN);
        // time of the build in milli seconds
        long elapsed = myBuild.getTimeElapsed();
        assertTrue("we waited more than the process lasted",
                TIME_TO_WAIT * 1000 + SECURITY_MARGIN > elapsed);
        logFile = new File(logFile.getAbsolutePath());
        assertTrue("log file found after spawn", logFile.exists());
    }

    @Test
    @Ignore("#50507 - fails at least on Linux")
    /* TODO #50507 - fails at least on Linux */
    public void testOutAndErr() {
        buildRule.getProject().executeTarget("test-out-and-err");
    }

    private static class MonitoredBuild implements Runnable {
        private Thread worker;
        private File myBuildFile = null;
        private String target = null;
        private Project project = null;
        private GregorianCalendar timeStarted = null;
        private GregorianCalendar timeFinished = null;

        public void setLogFile(String logFile) {
            project.setProperty("logFile", logFile);
        }

        public void setTimeToWait(int timeToWait) {
            project.setProperty("timeToWait", Long.toString(timeToWait));
        }

        public void addBuildListener(BuildListener bl) {
            project.addBuildListener(bl);
        }

        public MonitoredBuild(File buildFile, String target) {
            myBuildFile = buildFile;
            this.target = target;
            project = new Project();
            project.init();
            project.setUserProperty(MagicNames.ANT_FILE, myBuildFile.getAbsolutePath());
            ProjectHelper.configureProject(project, myBuildFile);
        }

        /**
         *
         * @return time in millis of the build
         */
        public long getTimeElapsed() {
            return timeFinished.getTime().getTime() - timeStarted.getTime().getTime();
        }

        public void start() {
            worker = new Thread(this, myBuildFile.toString() + "/" + target);
            worker.start();
        }

        public void run() {
            startProject();
        }

        private void startProject() {
            timeStarted = new GregorianCalendar();
            project.executeTarget(target);
            timeFinished = new GregorianCalendar();
        }
    }

    private class MonitoredBuildListener implements BuildListener {
        public void buildStarted(BuildEvent event) {
        }

        public void buildFinished(BuildEvent event) {
        }

        public void targetStarted(BuildEvent event) {
        }

        public void targetFinished(BuildEvent event) {
            if (event.getTarget().getName().equals("spawn")) {
                buildFinished = true;
            }
        }

        public void taskStarted(BuildEvent event) {
        }

        public void taskFinished(BuildEvent event) {
        }

        public void messageLogged(BuildEvent event) {
        }
    }

}
