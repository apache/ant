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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import org.apache.tools.ant.util.JavaEnvUtils;
import org.junit.AssumptionViolatedException;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Simple testcase for the ExecuteWatchdog class.
 *
 */
public class ExecuteWatchdogTest {

    private static final String TEST_CLASSPATH = getTestClassPath();

    private static final long TIME_OUT = 5000;
    private static final int CLOCK_ERROR = 200;
    private static final long TIME_OUT_TEST = TIME_OUT - CLOCK_ERROR;

    private ExecuteWatchdog watchdog;

    @Before
    public void setUp() {
        watchdog = new ExecuteWatchdog(TIME_OUT);
    }

    /**
     * Dangerous method to obtain the classpath for the test. This is
     * severely tied to the build.xml properties.
     */
    private static String getTestClassPath() {
        String classpath = System.getProperty("build.tests.value");
        if (classpath == null) {
            System.err.println("WARNING: 'build.tests.value' property is not available!");
            classpath = System.getProperty("java.class.path");
        }

        return classpath;
    }

    private Process getProcess(long timetorun) throws Exception {
        String[] cmdArray = {
            JavaEnvUtils.getJreExecutable("java"), "-classpath", TEST_CLASSPATH,
            TimeProcess.class.getName(), String.valueOf(timetorun)
        };
        //System.out.println("Testing with classpath: " + System.getProperty("java.class.path"));
        return Runtime.getRuntime().exec(cmdArray);
    }

    private String getErrorOutput(Process p) {
        BufferedReader err = new BufferedReader(new InputStreamReader(p.getErrorStream()));
        return err.lines().collect(Collectors.joining());
    }

    private int waitForEnd(Process p) throws Exception {
        int retcode = p.waitFor();
        if (retcode != 0) {
            String err = getErrorOutput(p);
            if (!err.isEmpty()) {
                System.err.println("ERROR:");
                System.err.println(err);
            }
        }
        return retcode;
    }

    @Test
    public void testNoTimeOut() throws Exception {
        Process process = getProcess(TIME_OUT / 2);
        watchdog.start(process);
        int retCode = waitForEnd(process);
        assertFalse("process should not have been killed", watchdog.killedProcess());
        assertFalse(Execute.isFailure(retCode));
    }

    // test that the watchdog ends the process
    @Test
    public void testTimeOut() throws Exception {
        Process process = getProcess(TIME_OUT * 2);
        long now = System.currentTimeMillis();
        watchdog.start(process);
        @SuppressWarnings("unused")
    int retCode = process.waitFor();
        long elapsed = System.currentTimeMillis() - now;
        assertTrue("process should have been killed", watchdog.killedProcess());
        // assertNotEquals("return code is invalid: " + retCode, retCode, 0);
        assertTrue("elapse time of " + elapsed + " ms is less than timeout value of " + TIME_OUT_TEST + " ms",
                elapsed >= TIME_OUT_TEST);
        assertTrue("elapse time of " + elapsed + " ms is greater than run value of " + (TIME_OUT * 2) + " ms",
                elapsed < TIME_OUT * 2);
    }

    // test a process that runs and failed
    @Test
    public void testFailed() throws Exception {
        Process process = getProcess(-1); // process should abort
        watchdog.start(process);
        int retCode = process.waitFor();
        assertFalse("process should not have been killed", watchdog.killedProcess());
        assertNotEquals("return code is invalid: " + retCode, 0, retCode);
    }

    @Test
    public void testManualStop() throws Exception {
        final Process process = getProcess(TIME_OUT * 2);
        watchdog.start(process);

        // I assume that starting this takes less than TIME_OUT/2 ms...
        Thread thread = new Thread(() -> {
            try {
                process.waitFor();
            } catch (InterruptedException e) {
                // not very nice but will do the job
                throw new AssumptionViolatedException("process interrupted in thread", e);
            }
        });
        thread.start();

        // wait for TIME_OUT / 2, there should be about TIME_OUT / 2 ms remaining before timeout
        thread.join(TIME_OUT / 2);

         // now stop the watchdog.
        watchdog.stop();

        // wait for the thread to die, should be the end of the process
        thread.join();

        // process should be dead and well finished
        assertEquals(0, process.exitValue());
        assertFalse("process should not have been killed", watchdog.killedProcess());
    }
}
