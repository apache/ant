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
 * 4. The names "The Jakarta Project", "Tomcat", and "Apache Software
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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.CharacterIterator;
import java.text.NumberFormat;
import java.text.StringCharacterIterator;
import junit.framework.Test;
import junit.framework.TestCase;

/**
 * Prints XML output of the test to a specified Writer.
 *
 * @author <a href="mailto:stefan.bodewig@megabit.net">Stefan Bodewig</a>
 */

public class XMLJUnitResultFormatter implements JUnitResultFormatter {

    /**
     * OutputStream for XML output.
     */
    private PrintWriter out;
    /**
     * Collects output during the test run.
     */
    private StringBuffer results = new StringBuffer();
    /**
     * platform independent line separator.
     */
    private static String newLine = System.getProperty("line.separator");
    /**
     * Formatter for timings.
     */
    private NumberFormat nf = NumberFormat.getInstance();
    /**
     * Timing helper.
     */
    private long lastTestStart = 0;

    public XMLJUnitResultFormatter(PrintWriter out) {
        this.out = out;
    }

    /**
     * The whole testsuite ended.
     */
    public void endTestSuite(JUnitTest suite) {
        out.println("<?xml version=\"1.0\"?>");
        out.print("<testsuite name=\"");
        out.print(suite.getName());
        out.print("\" tests=\"");
        out.print(suite.runCount());
        out.print("\" failures=\"");
        out.print(suite.failureCount());
        out.print("\" errors=\"");
        out.print(suite.errorCount());
        out.print("\" time=\"");
        out.print(nf.format(suite.getRunTime()/1000.0));
        out.println(" sec\">");
        out.print(results.toString());
        out.println("</testsuite>");
        out.flush();
        out.close();
    }

    /**
     * The whole testsuite started.
     */
    public void startTestSuite(JUnitTest suite) {
    }

    /**
     * Interface TestListener.
     *
     * <p>A new Test is started.
     */
    public void startTest(Test t) {
        lastTestStart = System.currentTimeMillis();
    }

    /**
     * Interface TestListener.
     *
     * <p>A Test is finished.
     */
    public void endTest(Test test) {
        formatTestCaseOpenTag(test);
        results.append("  </testcase>");
        results.append(newLine);
    }

    /**
     * Interface TestListener.
     *
     * <p>A Test failed.
     */
    public void addFailure(Test test, Throwable t) {
        formatError("failure", test, t);
    }

    /**
     * Interface TestListener.
     *
     * <p>An error occured while running the test.
     */
    public void addError(Test test, Throwable t) {
        formatError("error", test, t);
    }

    /**
     * Translates <, & and > to corresponding entities.
     */
    private String xmlEscape(String orig) {
        if (orig == null) return "";
        StringBuffer temp = new StringBuffer();
        StringCharacterIterator sci = new StringCharacterIterator(orig);
        for (char c = sci.first(); c != CharacterIterator.DONE;
             c = sci.next()) {

            switch (c) {
            case '<':
                temp.append("&lt;");
                break;
            case '>':
                temp.append("&gt;");
                break;
            case '&':
                temp.append("&amp;");
                break;
            default:
                temp.append(c);
                break;
            }
        }
        return temp.toString();
    }

    private void formatTestCaseOpenTag(Test test) {
        results.append("  <testcase");
        if (test != null && test instanceof TestCase) {
            results.append(" name=\"");
            results.append(((TestCase) test).name());
            results.append("\"");
        }
        results.append(" time=\"");
        results.append(nf.format((System.currentTimeMillis()-lastTestStart)
                                 / 1000.0));
        results.append("\">");
        results.append(newLine);
    }

    private void formatError(String type, Test test, Throwable t) {
        formatTestCaseOpenTag(test);
        results.append("    <");
        results.append(type);
        results.append(" message=\"");
        results.append(xmlEscape(t.getMessage()));
        results.append("\" type=\"");
        results.append(t.getClass().getName());
        results.append("\">");
        results.append(newLine);

        results.append("<![CDATA[");
        results.append(newLine);
        StringWriter swr = new StringWriter();
        t.printStackTrace(new PrintWriter(swr, true));
        results.append(swr.toString());
        results.append("]]>");
        results.append(newLine);

        results.append("    </");
        results.append(type);
        results.append(">");
        results.append(newLine);

        results.append("  </testcase>");
        results.append(newLine);
    }


} // XMLJUnitResultFormatter
