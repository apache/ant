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

package org.apache.tools.ant.taskdefs.optional.junit;

import java.io.IOException;
import java.io.OutputStream;
import java.text.NumberFormat;
import junit.framework.AssertionFailedError;
import junit.framework.Test;
import org.apache.tools.ant.BuildException;

/**
 * Prints short summary output of the test to Ant's logging system.
 *
 */

public class SummaryJUnitResultFormatter
    implements JUnitResultFormatter, JUnitTaskMirror.SummaryJUnitResultFormatterMirror {

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
     * Empty
     */
    public SummaryJUnitResultFormatter() {
    }
    /**
     * The testsuite started.
     * @param suite the testsuite.
     */
    public void startTestSuite(JUnitTest suite) {
        String newLine = System.getProperty("line.separator");
        StringBuffer sb = new StringBuffer("Running ");
        sb.append(suite.getName());
        sb.append(newLine);

        try {
            out.write(sb.toString().getBytes());
            out.flush();
        } catch (IOException ioex) {
            throw new BuildException("Unable to write summary output", ioex);
        }
    }
    /**
     * Empty
     * @param t not used.
     */
    public void startTest(Test t) {
    }
    /**
     * Empty
     * @param test not used.
     */
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
    public void addFailure(Test test, AssertionFailedError t) {
        addFailure(test, (Throwable) t);
    }
    /**
     * Empty
     * @param test not used.
     * @param t not used.
     */
    public void addError(Test test, Throwable t) {
    }

    /** {@inheritDoc}. */
    public void setOutput(OutputStream out) {
        this.out = out;
    }

    /** {@inheritDoc}. */
    public void setSystemOutput(String out) {
        systemOutput = out;
    }

    /** {@inheritDoc}. */
    public void setSystemError(String err) {
        systemError = err;
    }

    /**
     * Should the output to System.out and System.err be written to
     * the summary.
     * @param value if true write System.out and System.err to the summary.
     */
    public void setWithOutAndErr(boolean value) {
        withOutAndErr = value;
    }

    /**
     * The whole testsuite ended.
     * @param suite the testsuite.
     * @throws BuildException if there is an error.
     */
    public void endTestSuite(JUnitTest suite) throws BuildException {
        String newLine = System.getProperty("line.separator");
        StringBuffer sb = new StringBuffer("Tests run: ");
        sb.append(suite.runCount());
        sb.append(", Failures: ");
        sb.append(suite.failureCount());
        sb.append(", Errors: ");
        sb.append(suite.errorCount());
        sb.append(", Time elapsed: ");
        sb.append(nf.format(suite.getRunTime() / 1000.0));
        sb.append(" sec");
        sb.append(newLine);

        if (withOutAndErr) {
            if (systemOutput != null && systemOutput.length() > 0) {
                sb.append("Output:").append(newLine).append(systemOutput)
                    .append(newLine);
            }

            if (systemError != null && systemError.length() > 0) {
                sb.append("Error: ").append(newLine).append(systemError)
                    .append(newLine);
            }
        }

        try {
            out.write(sb.toString().getBytes());
            out.flush();
        } catch (IOException ioex) {
            throw new BuildException("Unable to write summary output", ioex);
        } finally {
            if (out != System.out && out != System.err) {
                try {
                    out.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }
}
