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
package org.apache.tools.ant.taskdefs.optional.junit.formatter;

import java.util.Properties;

import org.apache.tools.ant.BuildException;

/**
 * Provide a common set of attributes and methods to factorize
 *
 * @author <a href="mailto:sbailliez@apache.org">Stephane Bailliez</a>
 */
public abstract class BaseFormatter implements Formatter {

    /** number of errors */
    private int errorCount;

    /** number of failures */
    private int failureCount;

    /** number of runs (success + failure + error) */
    private int runCount;

    public void init(Properties props) throws BuildException {
    }

    protected void finalize() throws Throwable {
        super.finalize();
        close();
    }

    public void onTestStdOutLine(String testname, String line) {
    }

    public void onTestStdErrLine(String testname, String line) {
    }

    public void onTestRunSystemProperties(Properties props) {
    }

    public void onTestStarted(String testname) {
    }

    public void onTestEnded(String testname) {
    }

    public void onTestFailed(int status, String testname, String trace) {
        if (status == STATUS_ERROR) {
            errorCount++;
        } else if (status == STATUS_FAILURE) {
            failureCount++;
        }
    }

    public void onTestRunStarted(int testcount) {
        runCount = testcount;
    }

    public void onTestRunEnded(long elapsedtime) {
        finished(elapsedtime);
    }

    public void onTestRunStopped(long elapsedtime) {
        finished(elapsedtime);
    }

    protected void finished(long elapsedtime) {
        close();
    }

    /** @return the number of errors */
    protected final int getErrorCount() {
        return errorCount;
    }

    /** @return the number of failures */
    protected final int getFailureCount() {
        return failureCount;
    }

    /** @return the number of runs */
    protected final int getRunCount() {
        return runCount;
    }

    /** helper method to flush and close the stream */
    protected void close() {
    }
}
