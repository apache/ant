/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000 The Apache Software Foundation.  All rights
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

import org.apache.tools.ant.BuildException;

import java.io.*;
import java.text.NumberFormat;

import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestCase;

/**
 * Prints plain text output of the test to a specified Writer.
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 */

public class PlainJUnitResultFormatter implements JUnitResultFormatter {

    /**
     * Formatter for timings.
     */
    private NumberFormat nf = NumberFormat.getInstance();
    /**
     * Timing helper.
     */
    private long lastTestStart = 0;
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
    private PrintWriter wri;
    /**
     * Suppress endTest if testcase failed.
     */
    private boolean failed = true;

    private String systemOutput = null;
    private String systemError = null;

    public PlainJUnitResultFormatter() {
        inner = new StringWriter();
        wri = new PrintWriter(inner);
    }

    public void setOutput(OutputStream out) {
        this.out = out;
    }

    public void setSystemOutput(String out) {
        systemOutput = out;
    }

    public void setSystemError(String err) {
        systemError = err;
    }

    /**
     * Empty.
     */
    public void startTestSuite(JUnitTest suite) {
    }

    /**
     * The whole testsuite ended.
     */
    public void endTestSuite(JUnitTest suite) throws BuildException {
        String newLine = System.getProperty("line.separator");
        StringBuffer sb = new StringBuffer("Testsuite: ");
        sb.append(suite.getName());
        sb.append(newLine);
        sb.append("Tests run: ");
        sb.append(suite.runCount());
        sb.append(", Failures: ");
        sb.append(suite.failureCount());
        sb.append(", Errors: ");
        sb.append(suite.errorCount());
        sb.append(", Time elapsed: ");
        sb.append(nf.format(suite.getRunTime()/1000.0));
        sb.append(" sec");
        sb.append(newLine);
        
        // append the err and output streams to the log
        if (systemOutput != null && systemOutput.length() > 0) {
            sb.append("------------- Standard Output ---------------" )
                .append(newLine)
                .append(systemOutput)
                .append("------------- ---------------- ---------------" )
                .append(newLine);
        }
        
        if (systemError != null && systemError.length() > 0) {
            sb.append("------------- Standard Error -----------------" )
                .append(newLine)
                .append(systemError)
                .append("------------- ---------------- ---------------" )
                .append(newLine);
        }

        sb.append(newLine);

        if (out != null) {
            try {
                out.write(sb.toString().getBytes());
                wri.close();
                out.write(inner.toString().getBytes());
                out.flush();
            } catch (IOException ioex) {
                throw new BuildException("Unable to write output", ioex);
            } finally {
                if (out != System.out && out != System.err) {
                    try {
                        out.close();
                    } catch (IOException e) {}
                }
            }
        }
    }

    /**
     * Interface TestListener.
     *
     * <p>A new Test is started.
     */
    public void startTest(Test t) {
        lastTestStart = System.currentTimeMillis();
        wri.print("Testcase: " + ((TestCase) t).name());
        failed = false;
    }

    /**
     * Interface TestListener.
     *
     * <p>A Test is finished.
     */
    public void endTest(Test test) {
        if (failed) return;
        wri.println(" took " 
                    + nf.format((System.currentTimeMillis()-lastTestStart)
                                / 1000.0)
                    + " sec");
    }

    /**
     * Interface TestListener for JUnit &lt;= 3.4.
     *
     * <p>A Test failed.
     */
    public void addFailure(Test test, Throwable t) {
        formatError("\tFAILED", test, t);
    }

    /**
     * Interface TestListener for JUnit &gt; 3.4.
     *
     * <p>A Test failed.
     */
    public void addFailure(Test test, AssertionFailedError t) {
        addFailure(test, (Throwable) t);
    }

    /**
     * Interface TestListener.
     *
     * <p>An error occured while running the test.
     */
    public void addError(Test test, Throwable t) {
        formatError("\tCaused an ERROR", test, t);
    }

    private void formatError(String type, Test test, Throwable t) {
        if (test != null) {
            endTest(test);
        }
        failed = true;

        wri.println(type);
        wri.println(t.getMessage());
        t.printStackTrace(wri);
        wri.println("");
    }
} // PlainJUnitResultFormatter
