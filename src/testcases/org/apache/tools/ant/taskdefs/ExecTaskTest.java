/*
 * Copyright  2003-2004 The Apache Software Foundation.
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

import org.apache.tools.ant.*;
import org.apache.tools.ant.util.FileUtils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.GregorianCalendar;

import junit.framework.ComparisonFailure;

/**
 * @author <a href="antoine@antbuild.com">Antoine Levy-Lambert</a>
 * @author Matt Benson
 */
public class ExecTaskTest extends BuildFileTest {
    private static final String BUILD_PATH = "src/etc/testcases/taskdefs/exec/";
    private static final String BUILD_FILE = BUILD_PATH + "exec.xml";
    private final int TIME_TO_WAIT = 1;
    /** maximum time allowed for the build in milliseconds */
    private final int MAX_BUILD_TIME = 4000;
    private final int SECURITY_MARGIN = 2000; // wait 2 second extras
    // the test failed with 100 ms of margin on cvs.apache.org on August 1st, 2003
    private File logFile;
    private MonitoredBuild myBuild = null;
    volatile private boolean buildFinished = false;
    public ExecTaskTest(String name) {
        super(name);
    }

    public void setUp() {
        configureProject(BUILD_FILE);
    }

    public void tearDown() {
        executeTarget("cleanup");
        if (logFile != null && logFile.exists()) {
            logFile.delete();
        }
    }

    public void testNoRedirect() {
        executeTarget("no-redirect");
        if (getProject().getProperty("test.can.run") == null) {
            return;
        }
        assertEquals(getProject().getProperty("ant.file") + " out"
            + getProject().getProperty("ant.file") + " err", getLog());
    }

    public void testRedirect1() {
        executeTarget("redirect1");
        if (getProject().getProperty("test.can.run") == null) {
            return;
        }
        String expectedOut = getProject().getProperty("ant.file") + " out\n"
            + getProject().getProperty("ant.file") + " err\n";

        String actualOut = null;
        try {
            actualOut = FileUtils.newFileUtils().readFully(new FileReader(
                getProject().resolveFile("redirect.out")));
        } catch (IOException eyeOhEx) {
        }
        assertEquals("unexpected output", expectedOut, actualOut);
    }

    public void testRedirect2() {
        executeTarget("redirect2");
        if (getProject().getProperty("test.can.run") == null) {
            return;
        }
        String expectedOut = getProject().getProperty("ant.file") + " out\n";
        String expectedErr = getProject().getProperty("ant.file") + " err\n";

        String actualOut = null;
        String actualErr = null;
        try {
            actualOut = FileUtils.newFileUtils().readFully(new FileReader(
                getProject().resolveFile("redirect.out")));
            actualErr = FileUtils.newFileUtils().readFully(new FileReader(
                getProject().resolveFile("redirect.err")));
        } catch (IOException eyeOhEx) {
        }
        assertEquals("unexpected output", expectedOut, actualOut);
        assertEquals("unexpected error output", expectedErr, actualErr);
    }

    public void testRedirect3() {
        executeTarget("redirect3");
        if (getProject().getProperty("test.can.run") == null) {
            return;
        }
        assertEquals(getProject().getProperty("ant.file") + " err", getLog());
        String expectedOut = getProject().getProperty("ant.file") + " out\n";

        String actualOut = null;
        try {
            actualOut = FileUtils.newFileUtils().readFully(new FileReader(
                getProject().resolveFile("redirect.out")));
        } catch (IOException eyeOhEx) {
        }
        assertEquals("unexpected output", expectedOut, actualOut);
        assertPropertyEquals("redirect.out", expectedOut.trim());
    }

    public void testRedirect4() {
        executeTarget("redirect4");
        if (getProject().getProperty("test.can.run") == null) {
            return;
        }
        String expectedOut = getProject().getProperty("ant.file") + " out\n";
        String expectedErr = getProject().getProperty("ant.file") + " err\n";

        String actualOut = null;
        String actualErr = null;
        try {
            actualOut = FileUtils.newFileUtils().readFully(new FileReader(
                getProject().resolveFile("redirect.out")));
            actualErr = FileUtils.newFileUtils().readFully(new FileReader(
                getProject().resolveFile("redirect.err")));
        } catch (IOException eyeOhEx) {
        }
        assertEquals("unexpected output", expectedOut, actualOut);
        assertPropertyEquals("redirect.out", expectedOut.trim());
        assertEquals("unexpected error output", expectedErr, actualErr);
        assertPropertyEquals("redirect.err", expectedErr.trim());
    }

    public void testRedirect5() {
        testRedirect5or6("redirect5");
    }

    public void testRedirect6() {
        testRedirect5or6("redirect6");
    }

    public void testRedirect5or6(String target) {
        executeTarget(target);
        if (getProject().getProperty("wc.can.run") == null) {
            return;
        }

        String expectedOut = getProject().getProperty("ant.file") + " out\n";

        String actualOut = null;
        String actualErr = null;
        try {
            actualOut = FileUtils.newFileUtils().readFully(new FileReader(
                getProject().resolveFile("redirect.out")));
            actualErr = FileUtils.newFileUtils().readFully(new FileReader(
                getProject().resolveFile("redirect.err")));
        } catch (IOException eyeOhEx) {
        }
        assertEquals("unexpected output", "3", actualOut.trim());
        assertEquals(getProject().getProperty("redirect.out").trim(), "3");
        assertEquals("unexpected error output", null, actualErr);
        assertPropertyEquals("redirect.err", "");
    }

    public void testRedirect7() {
        executeTarget("redirect7");
        if (getProject().getProperty("wc.can.run") == null) {
            return;
        }

        String expectedOut = getProject().getProperty("ant.file") + " out\n";

        String actualOut = null;
        String actualErr = null;
        try {
            actualOut = FileUtils.newFileUtils().readFully(new FileReader(
                getProject().resolveFile("redirect.out")));
            actualErr = FileUtils.newFileUtils().readFully(new FileReader(
                getProject().resolveFile("redirect.err")));
        } catch (IOException eyeOhEx) {
        }
        assertEquals("unexpected output", "3", actualOut.trim());
        assertEquals(getProject().getProperty("redirect.out").trim(), "3");
        assertEquals("unexpected error output", null, actualErr);
    }

    public void testspawn() {
        project.executeTarget("init");
        if (project.getProperty("test.can.run") == null) {
            return;
        }
        myBuild = new MonitoredBuild(new File(BUILD_FILE), "spawn");
        FileUtils fileutils  = FileUtils.newFileUtils();
        logFile = fileutils.createTempFile("spawn","log", project.getBaseDir());
        // this is guaranteed by FileUtils#createTempFile
        assertTrue("log file not existing", !logFile.exists());
        // make the spawned process run 4 seconds
        myBuild.setTimeToWait(TIME_TO_WAIT);
        myBuild.setLogFile(logFile.getAbsolutePath());
        myBuild.addBuildListener(new MonitoredBuildListener());
        myBuild.start();
        GregorianCalendar startwait = new GregorianCalendar();
        // this loop runs parallel to the build
        while (!buildFinished) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                System.out.println("my sleep was interrupted");
            }
            GregorianCalendar now = new GregorianCalendar();
            // security
            if (now.getTime().getTime() - startwait.getTime().getTime() > MAX_BUILD_TIME) {
                System.out.println("aborting wait, too long " + (now.getTime().getTime() - startwait.getTime().getTime()) + "milliseconds");
                break;
            }
        }
        // now wait until the spawned process is finished
        try {
            Thread.sleep((TIME_TO_WAIT) * 1000 + SECURITY_MARGIN);
        } catch (InterruptedException e) {
            System.out.println("my sleep was interrupted");
        }
        // time of the build in milli seconds
        long elapsed = myBuild.getTimeElapsed();
        assertTrue("we waited more than the process lasted", TIME_TO_WAIT * 1000
                + SECURITY_MARGIN > elapsed);
        logFile = new File(logFile.getAbsolutePath());
        assertTrue("log file found after spawn", logFile.exists());
    }

    private static class MonitoredBuild implements Runnable {
        private Thread worker;
        private File myBuildFile = null;
        private String target = null;
        private Project project = null;
        private int timeToWait = 0;
        private String logFile = null;
        private GregorianCalendar timeStarted = null;
        private GregorianCalendar timeFinished = null;

        public void setLogFile(String logFile) {
            this.logFile = logFile;
            project.setProperty("logFile", logFile);
        }


        public void setTimeToWait(int timeToWait) {
            this.timeToWait = timeToWait;
            project.setProperty("timeToWait", Long.toString(timeToWait));
        }

        public void addBuildListener(BuildListener bl) {
            project.addBuildListener(bl);
        }
        public MonitoredBuild(File buildFile, String target) {
            myBuildFile = buildFile;
            this.target = target;
            project=new Project();
            project = new Project();
            project.init();
            project.setUserProperty( "ant.file" , myBuildFile.getAbsolutePath() );
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
