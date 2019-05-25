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

package org.apache.tools.ant.taskdefs.optional.junit;

import java.io.IOException;
import java.io.OutputStream;
import java.text.NumberFormat;

import junit.framework.AssertionFailedError;
import junit.framework.Test;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.util.FileUtils;

/**
 * Prints short summary output of the test to Ant's logging system.
 *
 */

public class SummaryJUnitResultFormatter
    implements JUnitResultFormatter, JUnitTaskMirror.SummaryJUnitResultFormatterMirror {

    private static final double ONE_SECOND = 1000.0;

    /**
     * Formatter for timings.
     */
    private NumberFormat nf = NumberFormat.getInstance();
    /**
     * OutputStream to write to.
     */
    private OutputStream out;

    private boolean withOutAndErr = false;
    private String systemOutput = null;
    private String systemError = null;

    /**
     *  Insures that a line of log output is written and flushed as a single
     *  operation, to prevent lines from being spliced into other lines.
     *  (Hopefully this solves the issue of run on lines -
     *  [junit] Tests Run: 2 Failures: 2 [junit] Tests run: 5...
     *  synchronized doesn't seem to be to harsh a penalty since it only
     *  occurs twice per test - at the beginning and end.  Note that message
     *  construction occurs outside the locked block.
     *
     *  @param b data to be written as an unbroken block
     */
    private synchronized void writeOutputLine(byte[] b) {
        try {
            out.write(b);
            out.flush();
        } catch (IOException ioex) {
            throw new BuildException("Unable to write summary output", ioex);
        }
    }

    /**
     * The testsuite started.
     * @param suite the testsuite.
     */
    @Override
    public void startTestSuite(JUnitTest suite) {
        StringBuilder sb = new StringBuilder("Running ");
        int antThreadID = suite.getThread();

        sb.append(suite.getName());
        /* only write thread id in multi-thread mode so default old way doesn't change output */
        if (antThreadID > 0) {
            sb.append(" in thread ");
            sb.append(antThreadID);
        }
        sb.append(System.lineSeparator());
        writeOutputLine(sb.toString().getBytes());
    }
    /**
     * Empty
     * @param t not used.
     */
    @Override
    public void startTest(Test t) {
    }
    /**
     * Empty
     * @param test not used.
     */
    @Override
    public void endTest(Test test) {
    }
    /**
     * Empty
     * @param test not used.
     * @param t not used.
     */
    public void addFailure(Test test, Throwable t) {
    }
    /**
     * Interface TestListener for JUnit &gt; 3.4.
     *
     * <p>A Test failed.
     * @param test not used.
     * @param t not used.
     */
    @Override
    public void addFailure(Test test, AssertionFailedError t) {
        addFailure(test, (Throwable) t);
    }
    /**
     * Empty
     * @param test not used.
     * @param t not used.
     */
    @Override
    public void addError(Test test, Throwable t) {
    }

    /** {@inheritDoc}. */
    @Override
    public void setOutput(OutputStream out) {
        this.out = out;
    }

    /** {@inheritDoc}. */
    @Override
    public void setSystemOutput(String out) {
        systemOutput = out;
    }

    /** {@inheritDoc}. */
    @Override
    public void setSystemError(String err) {
        systemError = err;
    }

    /**
     * Should the output to System.out and System.err be written to
     * the summary.
     * @param value if true write System.out and System.err to the summary.
     */
    @Override
    public void setWithOutAndErr(boolean value) {
        withOutAndErr = value;
    }

    /**
     * The whole testsuite ended.
     * @param suite the testsuite.
     * @throws BuildException if there is an error.
     */
    @Override
    public void endTestSuite(JUnitTest suite) throws BuildException {
        StringBuilder sb = new StringBuilder("Tests run: ");
        sb.append(suite.runCount());
        sb.append(", Failures: ");
        sb.append(suite.failureCount());
        sb.append(", Errors: ");
        sb.append(suite.errorCount());
        sb.append(", Skipped: ");
        sb.append(suite.skipCount());
        sb.append(", Time elapsed: ");
        sb.append(nf.format(suite.getRunTime() / ONE_SECOND));
        sb.append(" sec");

        /* class name needed with multi-threaded execution because
           results line may not appear immediately below start line.
           only write thread id, class name in multi-thread mode so
           the line still looks as much like the old line as possible. */
        if (suite.getThread() > 0) {
            sb.append(", Thread: ");
            sb.append(suite.getThread());
            sb.append(", Class: ");
            sb.append(suite.getName());
        }
        sb.append(System.lineSeparator());

        if (withOutAndErr) {
            if (systemOutput != null && !systemOutput.isEmpty()) {
                sb.append(String.format("Output:%n%s%n", systemOutput));
            }

            if (systemError != null && !systemError.isEmpty()) {
                sb.append(String.format("Error: %n%s%n", systemError));
            }
        }

        try {
            writeOutputLine(sb.toString().getBytes());
        } finally {
            if (out != System.out && out != System.err) {
                FileUtils.close(out);
            }
        }
    }
}
