/*
 * Copyright  2002-2004 The Apache Software Foundation
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
import java.io.PrintStream;
import junit.framework.AssertionFailedError;
import org.apache.tools.ant.BuildException;

import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.DemuxOutputStream;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

/**
 * Test of the parallel TaskContainer
 *
 * @created 21 February 2002
 */
public class ParallelTest extends BuildFileTest {
    /** Standard property value for the basic test */
    public final static String DIRECT_MESSAGE = "direct";
    /** Standard property value for the basic and fail test */
    public final static String DELAYED_MESSAGE = "delayed";
    /** Standard property value for the fail test */
    public final static String FAILURE_MESSAGE = "failure";

    /** the build fiel associated with this test */
    public final static String TEST_BUILD_FILE
         = "src/etc/testcases/taskdefs/parallel.xml";

    /**
     * Constructor for the ParallelTest object
     *
     * @param name name of the test
     */
    public ParallelTest(String name) {
        super(name);
    }

    /** The JUnit setup method */
    public void setUp() {
        configureProject(TEST_BUILD_FILE);
    }

    /** tests basic operation of the parallel task */
    public void testBasic() {
        // should get no output at all
        Project project = getProject();
        project.setUserProperty("test.direct", DIRECT_MESSAGE);
        project.setUserProperty("test.delayed", DELAYED_MESSAGE);
        expectOutputAndError("testBasic", "", "");
        String log = getLog();
        assertEquals("parallel tasks didn't output correct data", log,
            DIRECT_MESSAGE + DELAYED_MESSAGE);

    }

    /** tests basic operation of the parallel task */
    public void testThreadCount() {
        // should get no output at all
        Project project = getProject();
        project.setUserProperty("test.direct", DIRECT_MESSAGE);
        project.setUserProperty("test.delayed", DELAYED_MESSAGE);
        expectOutputAndError("testThreadCount", "", "");
        String log = getLog();
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
     *@returns -1 no more tests
     *          # start pos of next test
     *@throws AssertionFailedException when a constraint is invalid
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
                    throw new AssertionFailedError("Only expect '+-' in result count, found "
                        + s.charAt(--pos) + " at position " + pos);
            }
            if (current > max) {
                throw new AssertionFailedError("Number of executing threads exceeded number allowed: "
                    + current + " > " + max);
            }
        }
        return lastPipe;
    }


    /** tests the failure of a task within a parallel construction */
    public void testFail() {
        // should get no output at all
        Project project = getProject();
        project.setUserProperty("test.failure", FAILURE_MESSAGE);
        project.setUserProperty("test.delayed", DELAYED_MESSAGE);
        expectBuildExceptionContaining("testFail",
            "fail task in one parallel branch", FAILURE_MESSAGE);
    }

    /** tests the demuxing of output streams in a multithreaded situation */
    public void testDemux() {
        Project project = getProject();
        project.addTaskDefinition("demuxtest", DemuxOutputTask.class);
        PrintStream out = System.out;
        PrintStream err = System.err;
        System.setOut(new PrintStream(new DemuxOutputStream(project, false)));
        System.setErr(new PrintStream(new DemuxOutputStream(project, true)));

        try {
            project.executeTarget("testDemux");
        } finally {
            System.setOut(out);
            System.setErr(err);
        }
    }

}

