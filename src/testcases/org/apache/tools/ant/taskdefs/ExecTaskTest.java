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

import org.apache.tools.ant.*;
import org.apache.tools.ant.util.FileUtils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.GregorianCalendar;

import junit.framework.ComparisonFailure;

/**
 * Unit test for the &lt;exec&gt; task.
 */
public class ExecTaskTest extends BuildFileTest {
    private static final String BUILD_PATH = "src/etc/testcases/taskdefs/exec/";
    private static final String BUILD_FILE = BUILD_PATH + "exec.xml";
    private static final int TIME_TO_WAIT = 1;
    /** maximum time allowed for the build in milliseconds */
    private static final int MAX_BUILD_TIME = 4000;
    private static final int SECURITY_MARGIN = 2000; // wait 2 second extras
    // the test failed with 100 ms of margin on cvs.apache.org on August 1st, 2003

    /** Utilities used for file operations */
    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

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
        if (logFile != null && logFile.exists()) {
            getProject().setProperty("logFile", logFile.getAbsolutePath());
        }
        executeTarget("cleanup");
    }

    public void testNoRedirect() {
        executeTarget("no-redirect");
        if (getProject().getProperty("test.can.run") == null) {
            return;
        }
        assertEquals("unexpected log content",
            getProject().getProperty("ant.file") + " out"
            + getProject().getProperty("ant.file") + " err", getLog());
    }

    public void testRedirect1() throws IOException {
        executeTarget("redirect1");
        if (getProject().getProperty("test.can.run") == null) {
            return;
        }
        String expectedOut = getProject().getProperty("ant.file") + " out\n"
            + getProject().getProperty("ant.file") + " err\n";

        assertEquals("unexpected output",
            expectedOut, getFileString("redirect.out"));
    }

    public void testRedirect2() throws IOException {
        executeTarget("redirect2");
        if (getProject().getProperty("test.can.run") == null) {
            return;
        }

        assertEquals("unexpected output",
            getProject().getProperty("ant.file") + " out\n",
            getFileString("redirect.out"));
        assertEquals("unexpected error output",
            getProject().getProperty("ant.file") + " err\n",
            getFileString("redirect.err"));
    }

    public void testRedirect3() throws IOException {
        executeTarget("redirect3");
        if (getProject().getProperty("test.can.run") == null) {
            return;
        }
        assertEquals("unexpected log content",
            getProject().getProperty("ant.file") + " err", getLog());
        String expectedOut = getProject().getProperty("ant.file") + " out\n";

        assertEquals("unexpected output",
            expectedOut, getFileString("redirect.out"));
        assertPropertyEquals("redirect.out", expectedOut.trim());
    }

    public void testRedirect4() throws IOException {
        executeTarget("redirect4");
        if (getProject().getProperty("test.can.run") == null) {
            return;
        }
        String expectedOut = getProject().getProperty("ant.file") + " out\n";
        String expectedErr = getProject().getProperty("ant.file") + " err\n";

        assertEquals("unexpected output",
            expectedOut, getFileString("redirect.out"));
        assertPropertyEquals("redirect.out", expectedOut.trim());
        assertEquals("unexpected error output",
            expectedErr, getFileString("redirect.err"));
        assertPropertyEquals("redirect.err", expectedErr.trim());
    }

    public void testRedirect5() throws IOException {
        testRedirect5or6("redirect5");
    }

    public void testRedirect6() throws IOException {
        testRedirect5or6("redirect6");
    }

    public void testRedirect5or6(String target) throws IOException {
        executeTarget(target);
        if (getProject().getProperty("wc.can.run") == null) {
            return;
        }

        assertEquals("unexpected output", "3", getFileString("redirect.out").trim());
        assertEquals("property redirect.out", "3",
            getProject().getProperty("redirect.out").trim());
        assertNull("unexpected error output", getFileString("redirect.err"));
        assertPropertyEquals("redirect.err", "");
    }

    public void testRedirect7() throws IOException {
        executeTarget("redirect7");
        if (getProject().getProperty("wc.can.run") == null) {
            return;
        }

        assertEquals("unexpected output", "3", getFileString("redirect.out").trim());
        assertEquals("property redirect.out", "3",
            getProject().getProperty("redirect.out").trim());
        assertNull("unexpected error output", getFileString("redirect.err"));
    }

    public void testRedirector1() {
        executeTarget("init");
        if (getProject().getProperty("test.can.run") == null) {
            return;
        }
        expectBuildException("redirector1", "cannot have > 1 nested <redirector>s");
    }

    public void testRedirector2() throws IOException {
        executeTarget("redirector2");
        if (getProject().getProperty("test.can.run") == null) {
            return;
        }

        assertEquals("unexpected output",
            getProject().getProperty("ant.file") + " out\n"
            + getProject().getProperty("ant.file") + " err\n",
            getFileString("redirector.out"));
    }

    public void testRedirector3() throws IOException {
        executeTarget("redirector3");
        if (getProject().getProperty("test.can.run") == null) {
            return;
        }

        assertEquals("unexpected output",
            getProject().getProperty("ant.file") + " out\n",
            getFileString("redirector.out"));
        assertEquals("unexpected error output",
            getProject().getProperty("ant.file") + " err\n",
            getFileString("redirector.err"));
    }

    public void testRedirector4() throws IOException {
        executeTarget("redirector4");
        if (getProject().getProperty("test.can.run") == null) {
            return;
        }
        String expectedOut = getProject().getProperty("ant.file") + " out\n";

        assertEquals("unexpected log content",
            getProject().getProperty("ant.file") + " err", getLog());
        assertEquals("unexpected output", expectedOut,
            getFileString("redirector.out"));
        assertPropertyEquals("redirector.out", expectedOut.trim());
    }

    public void testRedirector5() throws IOException {
        testRedirector5or6("redirector5");
    }

    public void testRedirector6() throws IOException {
        testRedirector5or6("redirector6");
    }

    private void testRedirector5or6(String target) throws IOException {
        executeTarget(target);
        if (getProject().getProperty("test.can.run") == null) {
            return;
        }
        String expectedOut = getProject().getProperty("ant.file") + " out\n";
        String expectedErr = getProject().getProperty("ant.file") + " err\n";

        assertEquals("unexpected output", expectedOut,
            getFileString("redirector.out"));
        assertPropertyEquals("redirector.out", expectedOut.trim());
        assertEquals("unexpected error output", expectedErr,
            getFileString("redirector.err"));
        assertPropertyEquals("redirector.err", expectedErr.trim());
    }

    public void testRedirector7() throws IOException {
        executeTarget("redirector7");
        if (getProject().getProperty("test.can.run") == null) {
            return;
        }
        String expectedOut = getProject().getProperty("ant.file") + " out\n";
        String expectedErr = getProject().getProperty("ant.file") + " ERROR!!!\n";

        assertEquals("unexpected output", expectedOut,
            getFileString("redirector.out"));
        assertPropertyEquals("redirector.out", expectedOut.trim());
        assertEquals("unexpected error output", expectedErr,
            getFileString("redirector.err"));
        assertPropertyEquals("redirector.err", expectedErr.trim());
    }

    public void testRedirector8() throws IOException {
        executeTarget("redirector8");
        if (getProject().getProperty("wc.can.run") == null) {
            return;
        }

        assertEquals("unexpected output", "3", getFileString("redirector.out").trim());
        assertEquals("property redirector.out", "3",
            getProject().getProperty("redirector.out").trim());
        assertNull("unexpected error output", getFileString("redirector.err"));
        assertPropertyEquals("redirector.err", "");
    }

    public void testRedirector9() throws IOException {
        testRedirector9Thru12("redirector9");
    }

    public void testRedirector10() throws IOException {
        testRedirector9Thru12("redirector10");
    }

    public void testRedirector11() throws IOException {
        testRedirector9Thru12("redirector11");
    }

    public void testRedirector12() throws IOException {
        testRedirector9Thru12("redirector12");
    }

    private void testRedirector9Thru12(String target) throws IOException {
        executeTarget(target);
        if (getProject().getProperty("cat.can.run") == null) {
            return;
        }
        String expectedOut = "blah after blah";

        assertEquals("unexpected output",
            expectedOut, getFileString("redirector.out").trim());
        assertPropertyEquals("redirector.out", expectedOut.trim());
        assertNull("unexpected error output", getFileString("redirector.err"));
        assertPropertyEquals("redirector.err", "");
    }

    public void testRedirector13() {
        executeTarget("redirector13");
        if (getProject().getProperty("test.can.run") == null) {
            return;
        }
        String antfile = getProject().getProperty("ant.file");
        try {
            //no point in setting a message
            assertEquals(antfile + " OUTPUT???" + antfile + " ERROR!!!", getLog());
        } catch (ComparisonFailure cf) {
            assertEquals("unexpected log content",
                antfile + " ERROR!!!" + antfile + " OUTPUT???", getLog());
        }
    }

    public void testRedirector14() {
        executeTarget("redirector14");
        if (getProject().getProperty("cat.can.run") == null) {
            return;
        }
        assertEquals("unexpected log output", "blah after blah", getLog());
    }

    public void testRedirector15() throws IOException {
        executeTarget("redirector15");
        if (getProject().getProperty("cat.can.run") == null) {
            return;
        }
        assertTrue("error with transcoding",
            FILE_UTILS.contentEquals(
            getProject().resolveFile("expected/utf-8"),
            getProject().resolveFile("redirector.out")));
    }

    public void testRedirector16() {
        executeTarget("redirector16");
    }

    public void testRedirector17() {
        executeTarget("redirector17");
    }

    public void testRedirector18() {
        if (getProject().getProperty("test.can.run") == null) {
            return;
        }
        expectLog("redirector18", getProject().getProperty("ant.file")
            + " out" + getProject().getProperty("ant.file") + " err");
    }

    public void testspawn() {
        project.executeTarget("init");
        if (project.getProperty("test.can.run") == null) {
            return;
        }
        myBuild = new MonitoredBuild(new File(System.getProperty("root"), BUILD_FILE), "spawn");
        logFile = FILE_UTILS.createTempFile("spawn","log", project.getBaseDir());
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

    public void testExecUnknownOS() {
        executeTarget("testExecUnknownOS");
    }

    public void testExecOSFamily() {
        executeTarget("testExecOSFamily");
    }

    public void testExecInconsistentSettings() {
        executeTarget("testExecInconsistentSettings");
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

    //borrowed from TokenFilterTest
    private String getFileString(String filename) throws IOException {
        String result = null;
        FileReader reader = null;
        try {
            reader = new FileReader(getProject().resolveFile(filename));
            result = FileUtils.readFully(reader);
        } catch (IOException eyeOhEx) {
        } finally {
            FileUtils.close(reader);
        }
        return result;
    }

}
