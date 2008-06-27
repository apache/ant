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

import org.apache.tools.ant.MagicNames;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.apache.tools.ant.util.JavaEnvUtils;

import junit.framework.TestCase;

/**
 * Simple testcase for the ExecuteJava class - mostly stolen from
 * ExecuteWatchdogTest.
 *
 */
public class ExecuteJavaTest extends TestCase {

    private final static int TIME_OUT = 5000;

    private final static int CLOCK_ERROR=200;
    private final static int TIME_OUT_TEST=TIME_OUT-CLOCK_ERROR;

    private ExecuteJava ej;
    private Project project;
    private Path cp;

    public ExecuteJavaTest(String name) {
        super(name);
    }

    protected void setUp(){
        ej = new ExecuteJava();
        ej.setTimeout(new Long(TIME_OUT));
        project = new Project();
        project.setBasedir(".");
        project.setProperty(MagicNames.ANT_HOME, System.getProperty(MagicNames.ANT_HOME));
        cp = new Path(project, getTestClassPath());
        ej.setClasspath(cp);
    }

    private Commandline getCommandline(int timetorun) throws Exception {
        Commandline cmd = new Commandline();
        cmd.setExecutable(TimeProcess.class.getName());
        cmd.createArgument().setValue(String.valueOf(timetorun));
        return cmd;
    }

    public void testNoTimeOut() throws Exception {
        Commandline cmd = getCommandline(TIME_OUT/2);
        ej.setJavaCommand(cmd);
        ej.execute(project);
        assertTrue("process should not have been killed", !ej.killedProcess());
    }

    // test that the watchdog ends the process
    public void testTimeOut() throws Exception {
        Commandline cmd = getCommandline(TIME_OUT*2);
        ej.setJavaCommand(cmd);
        long now = System.currentTimeMillis();
        ej.execute(project);
        long elapsed = System.currentTimeMillis() - now;
        assertTrue("process should have been killed", ej.killedProcess());

        assertTrue("elapse time of "+elapsed
                   +" ms is less than timeout value of "+TIME_OUT_TEST+" ms",
                   elapsed >= TIME_OUT_TEST);
        assertTrue("elapse time of "+elapsed
                   +" ms is greater than run value of "+(TIME_OUT*2)+" ms",
                   elapsed < TIME_OUT*2);
    }


    public void testNoTimeOutForked() throws Exception {
        Commandline cmd = getCommandline(TIME_OUT/2);
        ej.setJavaCommand(cmd);
        ej.fork(cp);
        assertTrue("process should not have been killed", !ej.killedProcess());
    }

    // test that the watchdog ends the process
    public void testTimeOutForked() throws Exception {
        //process doesn't die properly under this combination,
        //thus test fails.  No workaround?
        if (JavaEnvUtils.isJavaVersion(JavaEnvUtils.JAVA_1_2)
            && Os.isFamily("dos")) {
            return;
        }
        Commandline cmd = getCommandline(TIME_OUT*2);
        ej.setJavaCommand(cmd);
        long now = System.currentTimeMillis();
        ej.fork(cp);
        long elapsed = System.currentTimeMillis() - now;
        assertTrue("process should have been killed", ej.killedProcess());

        assertTrue("elapse time of "+elapsed
                   +" ms is less than timeout value of "+TIME_OUT_TEST+" ms",
                   elapsed >= TIME_OUT_TEST);
        assertTrue("elapse time of "+elapsed
                   +" ms is greater than run value of "+(TIME_OUT*2)+" ms",
                   elapsed < TIME_OUT*2);
    }

    /**
     * Dangerous method to obtain the classpath for the test. This is
     * severely tighted to the build.xml properties.
     */
    private static String getTestClassPath(){
        String classpath = System.getProperty("build.tests");
        if (classpath == null) {
            System.err.println("WARNING: 'build.tests' property is not available !");
            classpath = System.getProperty("java.class.path");
        }

        return classpath;
    }

}
