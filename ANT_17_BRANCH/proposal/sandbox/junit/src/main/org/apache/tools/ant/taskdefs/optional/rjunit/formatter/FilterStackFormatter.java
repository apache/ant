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
package org.apache.tools.ant.taskdefs.optional.rjunit.formatter;

import java.util.StringTokenizer;

import org.apache.tools.ant.util.StringUtils;
import org.apache.tools.ant.taskdefs.optional.rjunit.remote.TestRunEvent;
import org.apache.tools.ant.taskdefs.optional.rjunit.remote.ExceptionData;

/**
 * Filtered Formatter that strips out unwanted stack frames from the full
 * stack trace, for instance it will filter the lines containing the
 * following matches:
 * <pre>
 *   junit.framework.TestCase
 *   junit.framework.TestResult
 *   junit.framework.TestSuite
 *   junit.framework.Assert.
 *   junit.swingui.TestRunner
 *   junit.awtui.TestRunner
 *   junit.textui.TestRunner
 *   java.lang.reflect.Method.invoke(
 *   org.apache.tools.ant.
 * </pre>
 * Removing all the above will help to make stacktrace more readable.
 *
 */
public class FilterStackFormatter extends FilterFormatter {

    /** the set of matches to look for in a stack trace */
    private final static String[] DEFAULT_TRACE_FILTERS = new String[]{
        "junit.framework.TestCase",
        "junit.framework.TestResult",
        "junit.framework.TestSuite",
        "junit.framework.Assert.", // don't filter AssertionFailure
        "junit.swingui.TestRunner",
        "junit.awtui.TestRunner",
        "junit.textui.TestRunner",
        "java.lang.reflect.Method.invoke(",
        "org.apache.tools.ant."
    };

    private final String[] filters = getFilters();

    /**
     * Creates a new <tt>FilterStackFormatter</tt>
     * @param formatter the formatter to be filtered.
     */
    public FilterStackFormatter(Formatter formatter) {
        super(formatter);
    }

    public void onTestFailure(TestRunEvent evt) {
        filterEvent(evt);
        super.onTestFailure(evt);
    }

    public void onTestError(TestRunEvent evt) {
        filterEvent(evt);
        super.onTestFailure(evt);
    }

    protected void filterEvent(TestRunEvent evt){
        String filteredTrace = filter(evt.getError().getStackTrace());
        ExceptionData error = new ExceptionData(
                evt.getError().getType(),
                evt.getError().getMessage(),
                filteredTrace);
        evt.setError(error);
    }

    protected String filter(String trace){
        StringTokenizer st = new StringTokenizer(trace, "\r\n");
        StringBuffer buf = new StringBuffer(trace.length());
        while (st.hasMoreTokens()) {
            String line = st.nextToken();
            if (accept(line)) {
                buf.append(line).append(StringUtils.LINE_SEP);
            }
        }
        return buf.toString();
    }
    /**
     * Check whether or not the line should be accepted.
     * @param line the line to be check for acceptance.
     * @return <tt>true</tt> if the line is accepted, <tt>false</tt> if not.
     */
    protected boolean accept(String line) {
        for (int i = 0; i < filters.length; i++) {
            if (line.indexOf(filters[i]) > 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return the filters to use for this
     */
    protected static String[] getFilters(){
        // @fixme hack for now, need something better.
        // using configuration properties ?
        String filters = System.getProperty("ant.rjunit.stacktrace.filters");
        if (filters == null){
            return DEFAULT_TRACE_FILTERS;
        }
        StringTokenizer st = new StringTokenizer(filters, ",");
        String[] results = new String[ st.countTokens() ];
        int i = 0;
        while (st.hasMoreTokens()){
            results[i++] = st.nextToken();
        }
        return results;
    }

}
