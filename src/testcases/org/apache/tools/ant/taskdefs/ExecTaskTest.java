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

import org.apache.tools.ant.*;
import org.apache.tools.ant.util.FileUtils;

import java.io.File;
import java.io.OutputStream;
import java.util.GregorianCalendar;

import junit.framework.Assert;

/**
 * @author <a href="antoine@antbuild.com">Antoine Levy-Lambert</a>
 */
public class ExecTaskTest extends BuildFileTest {
    private static final String BUILD_PATH = "src/etc/testcases/taskdefs/exec/";
    private static final String BUILD_FILE = BUILD_PATH + "exec.xml";
    private final int TIME_TO_WAIT = 4;
    /** maximum time allowed for the build in milliseconds */
    private final int MAX_BUILD_TIME = 4000;
    private final int SECURITY_MARGIN = 100; // wait 100 millis extras
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
        if (logFile != null) {
            logFile.delete();
        }
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
        assertTrue("we waited more than the process lasted", TIME_TO_WAIT * 1000 > elapsed);
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
