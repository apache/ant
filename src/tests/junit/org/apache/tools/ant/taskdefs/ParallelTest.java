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

import java.io.PrintStream;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildFileRule;
import org.apache.tools.ant.DemuxOutputStream;
import org.apache.tools.ant.ExitStatusException;
import org.apache.tools.ant.Project;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Test of the parallel TaskContainer
 */
public class ParallelTest {

    @Rule
    public final BuildFileRule buildRule = new BuildFileRule();

    /** Standard property value for the basic test */
    public final static String DIRECT_MESSAGE = "direct";
    /** Standard property value for the basic and fail test */
    public final static String DELAYED_MESSAGE = "delayed";
    /** Standard property value for the fail test */
    public final static String FAILURE_MESSAGE = "failure";

    /** the build fiel associated with this test */
    public final static String TEST_BUILD_FILE
         = "src/etc/testcases/taskdefs/parallel.xml";


    /** The JUnit setup method */
    @Before
    public void setUp() {
        buildRule.configureProject(TEST_BUILD_FILE);
    }

    /** tests basic operation of the parallel task */
    @Test
    public void testBasic() {
        // should get no output at all
        Project p = buildRule.getProject();
        p.setUserProperty("test.direct", DIRECT_MESSAGE);
        p.setUserProperty("test.delayed", DELAYED_MESSAGE);
        buildRule.executeTarget("testBasic");
        assertEquals("", buildRule.getOutput());
        assertEquals("", buildRule.getError());
        String log = buildRule.getLog();
        assertEquals("parallel tasks didn't output correct data", log,
            DIRECT_MESSAGE + DELAYED_MESSAGE);

    }

    /** tests basic operation of the parallel task */
    @Test
    public void testThreadCount() {
        // should get no output at all
        Project p = buildRule.getProject();
        p.setUserProperty("test.direct", DIRECT_MESSAGE);
        p.setUserProperty("test.delayed", DELAYED_MESSAGE);
        buildRule.executeTarget("testThreadCount");
        assertEquals("", buildRule.getOutput());
        assertEquals("", buildRule.getError());
        String log = buildRule.getLog();
        int pos = 0;
        while (pos > -1) {
            pos = countThreads(log, pos);
        }
    }

    /**
     * the test result string should match the regex
     * <code>^(\|\d+\/(+-)*)+\|$</code> for someting like
     * <code>|3/++--+-|5/+++++-----|</code>
     *
     *@return -1 no more tests
     *          # start pos of next test
     */
    static int countThreads(String s, int start) {
        int firstPipe = s.indexOf('|', start);
        int beginSlash = s.indexOf('/', firstPipe);
        int lastPipe = s.indexOf('|', beginSlash);
        if ((firstPipe == -1) || (beginSlash == -1) || (lastPipe == -1)) {
            return -1;
        }

        int max = Integer.parseInt(s.substring(firstPipe + 1, beginSlash));
        int current = 0;
        int pos = beginSlash + 1;
        while (pos < lastPipe) {
            switch (s.charAt(pos++)) {
                case '+':
                    current++;
                    break;
                case '-':
                    current--;
                    break;
                default:
                    fail("Only expect '+-' in result count, found "
                            + s.charAt(--pos) + " at position " + pos);
            }
            if (current > max) {
                fail("Number of executing threads exceeded number allowed: "
                        + current + " > " + max);
            }
        }
        return lastPipe;
    }


    /** tests the failure of a task within a parallel construction */
    @Test
    public void testFail() {
        // should get no output at all
        Project p = buildRule.getProject();
        p.setUserProperty("test.failure", FAILURE_MESSAGE);
        p.setUserProperty("test.delayed", DELAYED_MESSAGE);
        try {
            buildRule.executeTarget("testFail");
            fail("fail task in one parallel branch");
        } catch (BuildException ex) {
            assertEquals(FAILURE_MESSAGE, ex.getMessage());
        }
    }

    /** tests the demuxing of output streams in a multithreaded situation */
    @Test
    public void testDemux() {
        Project p = buildRule.getProject();
        p.addTaskDefinition("demuxtest", DemuxOutputTask.class);
        synchronized (System.out) {
            PrintStream out = System.out;
            PrintStream err = System.err;
            System.setOut(new PrintStream(new DemuxOutputStream(p, false)));
            System.setErr(new PrintStream(new DemuxOutputStream(p, true)));

            try {
                p.executeTarget("testDemux");
            } finally {
                System.setOut(out);
                System.setErr(err);
            }
        }
    }

    /**
     * @see "https://issues.apache.org/bugzilla/show_bug.cgi?id=55539"
     */
    @Test
    public void testSingleExit() {
        try {
            buildRule.executeTarget("testSingleExit");
            fail("ExitStatusException should have been thrown");
        } catch (ExitStatusException ex) {
            assertEquals(42, ex.getStatus());
        }
    }

    /**
     * @see "https://issues.apache.org/bugzilla/show_bug.cgi?id=55539"
     */
    @Test
    public void testExitAndOtherException() {
        try {
            buildRule.executeTarget("testExitAndOtherException");
            fail("ExitStatusException should have been thrown");
        } catch (ExitStatusException ex) {
            assertEquals(42, ex.getStatus());
        }
    }

}

