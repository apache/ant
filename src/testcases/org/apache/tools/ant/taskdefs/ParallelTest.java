/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
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
import java.io.PrintStream;

import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.DemuxOutputStream;
import org.apache.tools.ant.Project;

/**
 * Test of the parallel TaskContainer
 *
 * @author Conor MacNeill
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

