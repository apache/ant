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

import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Date;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.optional.rjunit.JUnitHelper;
import org.apache.tools.ant.taskdefs.optional.rjunit.remote.TestRunEvent;
import org.apache.tools.ant.taskdefs.optional.rjunit.remote.TestSummary;
import org.apache.tools.ant.taskdefs.optional.rjunit.remote.ExceptionData;
import org.apache.tools.ant.util.DOMElementWriter;
import org.apache.tools.ant.util.DateUtils;
import org.apache.tools.ant.util.StringUtils;

/**
 * XML Formatter. Due to the nature of the XML we are forced to store
 * everything in memory until it is finished. It might be resource
 * intensive when running lots of testcases.
 *
 * <testsuites stop="true">
 *  <testsuite name="" time="">
 *    <testcase name="" time="">
 *      <error/>
 *    </testcase>
 *    <testcase name="" time="">
 *      <failure/>
 *    </testcase>
 *  </testsuite>
 * </testsuites>
 *
 */
public class XMLFormatter extends BaseStreamFormatter {

    /** the testsuites element for the aggregate document */
    public final static String TESTSUITES = "testsuites";

    /** the testsuite element */
    public final static String TESTSUITE = "testsuite";

    /** the testcase element */
    public final static String TESTCASE = "testcase";

    /** the error element */
    public final static String ERROR = "error";

    /** the failure element */
    public final static String FAILURE = "failure";

    /** the system-err element */
    public final static String SYSTEM_ERR = "system-err";

    /** the system-out element */
    public final static String SYSTEM_OUT = "system-out";

    /** package attribute for the aggregate document */
    public final static String ATTR_PACKAGE = "package";

    /** name attribute for property, testcase and testsuite elements */
    public final static String ATTR_NAME = "name";

    /** time attribute for testcase and testsuite elements */
    public final static String ATTR_TIME = "time";

    /** errors attribute for testsuite elements */
    public final static String ATTR_ERRORS = "errors";

    /** failures attribute for testsuite elements */
    public final static String ATTR_FAILURES = "failures";

    /** tests attribute for testsuite elements */
    public final static String ATTR_TESTS = "tests";

    /** type attribute for failure and error elements */
    public final static String ATTR_TYPE = "type";

    /** message attribute for failure elements */
    public final static String ATTR_MESSAGE = "message";

    /** the properties element */
    public final static String PROPERTIES = "properties";

    /** the property element */
    public final static String PROPERTY = "property";

    /** value attribute for property elements */
    public final static String ATTR_VALUE = "value";

    /** The XML document. */
    private Document doc = getDocumentBuilder().newDocument();

    /**  The wrapper for the whole testsuite. */
    private Element rootElement = doc.createElement(TESTSUITES);

    private Element lastTestElement = null;
    private TestRunEvent lastTestEvent = null;
    private Element lastSuiteElement = null;
    private long programStart;

    public void onSuiteStarted(TestRunEvent evt) {
        String fullclassname = evt.getName();
        int pos = fullclassname.lastIndexOf('.');

        // a missing . might imply no package at all. Don't get fooled.
        String pkgName = (pos == -1) ? "" : fullclassname.substring(0, pos);
        String classname = (pos == -1) ? fullclassname : fullclassname.substring(pos + 1);

        Element suite = doc.createElement(TESTSUITE);
        suite.setAttribute(ATTR_NAME, classname);
        suite.setAttribute(ATTR_PACKAGE, pkgName);
        rootElement.appendChild(suite);
        lastSuiteElement = suite;
    }

    public void onSuiteEnded(TestRunEvent evt) {
        Element suite = lastSuiteElement;
        TestSummary summary = evt.getSummary();
        suite.setAttribute(ATTR_TIME, String.valueOf(summary.elapsedTime()/1000.0f));
        suite.setAttribute(ATTR_TESTS, String.valueOf(summary.runCount()));
        suite.setAttribute(ATTR_FAILURES, String.valueOf(summary.failureCount()));
        suite.setAttribute(ATTR_ERRORS, String.valueOf(summary.errorCount()));
        lastSuiteElement = null;
    }

    public void onRunEnded(TestRunEvent evt) {
        final String elapsedTime = String.valueOf(evt.getTimeStamp() - programStart);
        rootElement.setAttribute("elapsed_time", elapsedTime);
        // Output properties
        final Element propsElement = doc.createElement(PROPERTIES);
        rootElement.appendChild(propsElement);
        final Properties props = evt.getProperties();
        if (props != null) {
            Enumeration e = props.propertyNames();
            while (e.hasMoreElements()) {
                String name = (String) e.nextElement();
                Element propElement = doc.createElement(PROPERTY);
                propElement.setAttribute(ATTR_NAME, name);
                propElement.setAttribute(ATTR_VALUE, props.getProperty(name));
                propsElement.appendChild(propElement);
            }
        }
        close();
    }

    public void onRunStarted(TestRunEvent evt) {
        programStart = evt.getTimeStamp();
        final String date = DateUtils.format(programStart, DateUtils.ISO8601_DATETIME_PATTERN);
        rootElement.setAttribute("program_start", date);
    }

    public void onRunStopped(TestRunEvent evt) {
        rootElement.setAttribute("stopped", "true");
        onRunEnded(evt);
    }

    public void onTestStarted(TestRunEvent evt) {
        Element test = doc.createElement(TESTCASE);
        String name = JUnitHelper.getTestName(evt.getName());
        test.setAttribute(ATTR_NAME, name);
        String suiteName = JUnitHelper.getSuiteName(evt.getName());
        String lastSuiteName = lastSuiteElement.getAttribute(ATTR_PACKAGE)
                + "." + lastSuiteElement.getAttribute(ATTR_NAME);
        if ( !suiteName.equals(lastSuiteName) ){
            throw new BuildException("Received testcase from test "
                    + suiteName + " and was expecting "
                    + lastSuiteElement.getAttribute("name"));
        }
        lastSuiteElement.appendChild(test);
        lastTestElement = test;
        lastTestEvent = evt;
    }

    public void onTestEnded(TestRunEvent evt) {
        // with a TestSetup, startTest and endTest are not called.
        if (lastTestEvent == null) {
            onTestStarted(evt);
        }
        float time = (evt.getTimeStamp() - lastTestEvent.getTimeStamp()) / 1000.0f;
        lastTestElement.setAttribute(ATTR_TIME, Float.toString(time));
        lastTestElement = null;
        lastTestEvent = null;
    }

    public void onTestError(TestRunEvent evt) {
        onTestFailure(evt);
    }

    public void onTestFailure(TestRunEvent evt) {
        String type = evt.getType() == TestRunEvent.TEST_FAILURE ? FAILURE : ERROR;
        Element nested = doc.createElement(type);
        lastTestElement.appendChild(nested);
        ExceptionData error = evt.getError();
        nested.setAttribute(ATTR_MESSAGE, error.getMessage());
        nested.setAttribute(ATTR_TYPE, error.getType());
        Text text = doc.createTextNode(error.getStackTrace());
        nested.appendChild(text);
        onTestEnded(evt);
    }

    protected void close() {
        // the underlying writer uses UTF8 encoding
        getWriter().println("<?xml version='1.0' encoding='UTF-8' ?>");
        String now = DateUtils.format(new Date(), DateUtils.ISO8601_DATETIME_PATTERN);
        rootElement.setAttribute("snapshot_created", now);
        try {
            final DOMElementWriter domWriter = new DOMElementWriter();
            domWriter.write(rootElement, getWriter(), 0, "  ");
        } catch (IOException e) {
            throw new BuildException(e);
        } finally {
            super.close();
        }
    }

    private static DocumentBuilder getDocumentBuilder() {
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (Exception exc) {
            throw new ExceptionInInitializerError(exc);
        }
    }

}
