/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
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
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
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
package org.apache.tools.ant.taskdefs.optional.junit;

import java.util.Properties;
import java.util.Vector;

/**
 * A TestRunListener that stores all events for later check.
 *
 * <p>
 * All the events are stored chronologically in distinct vectors
 * and are made available as public instances
 * </p>
 *
 * @author <a href="mailto:sbailliez@apache.org">Stephane Bailliez</a>
 */
public class TestRunRecorder implements TestRunListener {

    /** wrapper around failure info */
    public static class TestFailedInfo {
        public int status;
        public String testname;
        public String trace;
    }

    /** wrapper around output info */
    public static class TestOutputInfo {
        public String testname;
        public String line;
    }

// all these are public in order for testcases to have access quickly
    public Vector testStarted = new Vector();
    public Vector testEnded = new Vector();
    public Vector testFailed = new Vector();
    public Vector testStdout = new Vector();
    public Vector testStderr = new Vector();
    public Vector sysprops = new Vector();
    public Vector runStarted = new Vector();
    public Vector runEnded = new Vector();
    public Vector runStopped = new Vector();

    public void onTestStarted(String testname) {
        testStarted.addElement(testname);
    }

    public void onTestEnded(String testname) {
        testEnded.addElement(testname);
    }

    public void onTestFailed(int status, String testname, String trace) {
        TestFailedInfo info = new TestFailedInfo();
        info.status = status;
        info.testname = testname;
        info.trace = trace;
        testFailed.addElement(info);
    }

    public void onTestStdOutLine(String testname, String line) {
        TestOutputInfo info = new TestOutputInfo();
        info.testname = testname;
        info.line = line;
        testStdout.addElement(info);
    }

    public void onTestStdErrLine(String testname, String line) {
        TestOutputInfo info = new TestOutputInfo();
        info.testname = testname;
        info.line = line;
        testStderr.addElement(info);
    }

    public void onTestRunSystemProperties(Properties props) {
        sysprops.addElement(props);
    }

    public void onTestRunStarted(int testcount) {
        runStarted.addElement(new Integer(testcount));
    }

    public void onTestRunEnded(long elapsedtime) {
        runEnded.addElement(new Long(elapsedtime));
    }

    public void onTestRunStopped(long elapsedtime) {
        runStopped.addElement(new Long(elapsedtime));
    }
}
