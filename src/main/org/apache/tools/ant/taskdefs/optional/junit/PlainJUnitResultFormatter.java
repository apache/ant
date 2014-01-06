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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.text.NumberFormat;
import java.util.Hashtable;

import junit.framework.AssertionFailedError;
import junit.framework.Test;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.StringUtils;


/**
 * Prints plain text output of the test to a specified Writer.
 *
 */

public class PlainJUnitResultFormatter implements JUnitResultFormatter, IgnoredTestListener {

    private static final double ONE_SECOND = 1000.0;

    /**
     * Formatter for timings.
     */
    private NumberFormat nf = NumberFormat.getInstance();
    /**
     * Timing helper.
     */
    private Hashtable testStarts = new Hashtable();
    /**
     * Where to write the log to.
     */
    private OutputStream out;
    /**
     * Helper to store intermediate output.
     */
    private StringWriter inner;
    /**
     * Convenience layer on top of {@link #inner inner}.
     */
    private BufferedWriter wri;
    /**
     * Suppress endTest if testcase failed.
     */
    private Hashtable failed = new Hashtable();

    private String systemOutput = null;
    private String systemError = null;

    /** No arg constructor */
    public PlainJUnitResultFormatter() {
        inner = new StringWriter();
        wri = new BufferedWriter(inner);
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
     * The whole testsuite started.
     * @param suite the test suite
     * @throws BuildException if unable to write the output
     */
    public void startTestSuite(JUnitTest suite) throws BuildException {
        if (out == null) {
            return; // Quick return - no output do nothing.
        }
        StringBuffer sb = new StringBuffer("Testsuite: ");
        sb.append(suite.getName());
        sb.append(StringUtils.LINE_SEP);
        try {
            out.write(sb.toString().getBytes());
            out.flush();
        } catch (IOException ex) {
            throw new BuildException("Unable to write output", ex);
        }
    }

    /**
     * The whole testsuite ended.
     * @param suite the test suite
     * @throws BuildException if unable to write the output
     */
    public void endTestSuite(JUnitTest suite) throws BuildException {
        try {
            StringBuffer sb = new StringBuffer("Tests run: ");
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
            sb.append(StringUtils.LINE_SEP);
            write(sb.toString());

            // write the err and output streams to the log
            if (systemOutput != null && systemOutput.length() > 0) {
                write("------------- Standard Output ---------------");
                write(StringUtils.LINE_SEP);
                write(systemOutput);
                write("------------- ---------------- ---------------");
                write(StringUtils.LINE_SEP);
            }

            if (systemError != null && systemError.length() > 0) {
                write("------------- Standard Error -----------------");
                write(StringUtils.LINE_SEP);
                write(systemError);
                write("------------- ---------------- ---------------");
                write(StringUtils.LINE_SEP);
            }

            write(StringUtils.LINE_SEP);
            if (out != null) {
                try {
                    wri.flush();
                    write(inner.toString());
                } catch (IOException ioex) {
                    throw new BuildException("Unable to write output", ioex);
                }
            }
        } finally {
            if (out != null) {
                try {
                    wri.close();
                } catch (IOException ioex) {
                    throw new BuildException("Unable to flush output", ioex);
                } finally {
                    if (out != System.out && out != System.err) {
                        FileUtils.close(out);
                    }
                    wri = null;
                    out = null;
                }
            }
        }
    }

    /**
     * Interface TestListener.
     *
     * <p>A new Test is started.
     * @param t the test.
     */
    public void startTest(Test t) {
        testStarts.put(t, new Long(System.currentTimeMillis()));
        failed.put(t, Boolean.FALSE);
    }

    /**
     * Interface TestListener.
     *
     * <p>A Test is finished.
     * @param test the test.
     */
    public void endTest(Test test) {
        if (Boolean.TRUE.equals(failed.get(test))) {
            return;
        }
        synchronized (wri) {
            try {
                wri.write("Testcase: "
                          + JUnitVersionHelper.getTestCaseName(test));
                Long l = (Long) testStarts.get(test);
                double seconds = 0;
                // can be null if an error occurred in setUp
                if (l != null) {
                    seconds =
                        (System.currentTimeMillis() - l.longValue()) / ONE_SECOND;
                }

                wri.write(" took " + nf.format(seconds) + " sec");
                wri.newLine();
            } catch (IOException ex) {
                throw new BuildException(ex);
            }
        }
    }

    /**
     * Interface TestListener for JUnit &lt;= 3.4.
     *
     * <p>A Test failed.
     * @param test the test.
     * @param t the exception.
     */
    public void addFailure(Test test, Throwable t) {
        formatError("\tFAILED", test, t);
    }

    /**
     * Interface TestListener for JUnit &gt; 3.4.
     *
     * <p>A Test failed.
     * @param test the test.
     * @param t  the assertion that failed.
     */
    public void addFailure(Test test, AssertionFailedError t) {
        addFailure(test, (Throwable) t);
    }

    /**
     * Interface TestListener.
     *
     * <p>An error occurred while running the test.
     * @param test the test.
     * @param t    the exception.
     */
    public void addError(Test test, Throwable t) {
        formatError("\tCaused an ERROR", test, t);
    }

    private void formatError(String type, Test test, Throwable t) {
        synchronized (wri) {
            if (test != null) {
                endTest(test);
                failed.put(test, Boolean.TRUE);
            }

            try {
                wri.write(type);
                wri.newLine();
                wri.write(String.valueOf(t.getMessage()));
                wri.newLine();
                String strace = JUnitTestRunner.getFilteredTrace(t);
                wri.write(strace);
                wri.newLine();
            } catch (IOException ex) {
                throw new BuildException(ex);
            }
        }
    }

    public void testIgnored(Test test) {
        formatSkip(test, JUnitVersionHelper.getIgnoreMessage(test));
    }


    public void formatSkip(Test test, String message) {
        if (test != null) {
            endTest(test);
        }

        try {
            wri.write("\tSKIPPED");
            if (message != null) {
                wri.write(": ");
                wri.write(message);
            }
            wri.newLine();
        } catch (IOException ex) {
            throw new BuildException(ex);
        }

    }

    public void testAssumptionFailure(Test test, Throwable throwable) {
        formatSkip(test, throwable.getMessage());
    }

    /**
     * Print out some text, and flush the output stream; encoding in the platform
     * local default encoding.
     * @param text text to write.
     * @throws BuildException on IO Problems.
     */
    private void write(String text) {
        if (out == null) {
            return;
        }
        try {
            out.write(text.getBytes());
            out.flush();
        } catch (IOException ex) {
            throw new BuildException("Unable to write output " + ex, ex);
        }
    }
} // PlainJUnitResultFormatter
