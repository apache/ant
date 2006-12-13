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
package org.apache.tools.ant.taskdefs.optional.rjunit.remote;

import java.io.Serializable;

import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestListener;
import junit.framework.TestResult;

/**
 * A helpful test summary that is somewhat similar to <tt>TestResult</tt>.
 * Here the difference is that this test summary should register to
 * the test result the time you wan to collect information.
 *
 */
public final class TestSummary implements Serializable, TestListener {

    /** time elapsed during tests run  in ms */
    private long elapsedTime;

    /** number of errors */
    private int errorCount;

    /** number of successes */
    private int successCount;

    /** number of failures */
    private int failureCount;

    /** number of runs */
    private int runCount;

    /** bean constructor */
    public TestSummary() {
    }

    /**
     * @return the number of errors that occurred in this test.
     */
    public int errorCount() {
        return errorCount;
    }

    /**
     * @return the number of successes that occurred in this test.
     */
    public int successCount() {
        return successCount;
    }

    /**
     * @return the number of failures that occurred in this test.
     */
    public int failureCount() {
        return failureCount;
    }

    /**
     * @return the number of runs that occurred in this test.
     * a run is the sum of failures + errors + successes.
     */
    public int runCount() {
        return runCount;
    }

    /**
     * @return the elapsed time in ms
     */
    public long elapsedTime() {
        return elapsedTime;
    }

//
    /**
     * register to the <tt>TestResult</tt> and starts the time counter.
     * @param result the instance to register to.
     * @see #stop(TestResult)
     */
    public void start(TestResult result){
        elapsedTime = System.currentTimeMillis();
        result.addListener(this);
    }

    /**
     * unregister from the <tt>TestResult</tt> and stops the time counter.
     * @param result the instance to unregister from.
     * @see #start(TestResult)
     */
    public void stop(TestResult result){
        elapsedTime = System.currentTimeMillis() - elapsedTime;
        result.removeListener(this);
    }

// test listener implementation

    public void addError(Test test, Throwable throwable) {
        errorCount++;
    }

    public void addFailure(Test test, AssertionFailedError error) {
        failureCount++;
    }

    public void endTest(Test test) {
        successCount++;
    }

    public void startTest(Test test) {
        runCount++;
    }

    public String toString(){
        StringBuffer buf = new StringBuffer();
        buf.append("run: ").append(runCount);
        buf.append(" success: ").append(successCount);
        buf.append(" failures: ").append(failureCount);
        buf.append(" errors: ").append(errorCount);
        buf.append(" elapsed: ").append(elapsedTime);
        return buf.toString();
    }
}
