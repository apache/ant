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

import java.util.Hashtable;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

/**
 * XML Formatter. Due to the nature of the XML we are forced to store
 * everything in memory until it is finished. It might be resource
 * intensive when running lots of testcases.
 *
 * @author <a href="mailto:sbailliez@apache.org">Stephane Bailliez</a>
 */
public class XMLFormatter extends BaseFormatter {

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
    private Document doc;

    /**  The wrapper for the whole testsuite. */
    private Element rootElement;

    /** Element for the current test. */
    private Hashtable testElements = new Hashtable();

    /** Timing helper. */
    private Hashtable testStarts = new Hashtable();

    public void onTestStarted(String testname) {
        //@fixme, eh, a testname only can obviouslly be a duplicate...
        testStarts.put(testname, new Long(System.currentTimeMillis()));
        Element currentTest = doc.createElement(TESTCASE);
        currentTest.setAttribute(ATTR_NAME, testname);
        rootElement.appendChild(currentTest);
        testElements.put(testname, currentTest);
        super.onTestStarted(testname);
    }

    public void onTestEnded(String testname) {
        Element currentTest = (Element) testElements.get(testname);
        // with a TestSetup, startTest and endTest are not called.
        if (currentTest == null) {
            onTestStarted(testname);
            currentTest = (Element) testElements.get(testname);
        }
        Long l = (Long) testStarts.get(testname);
        float time = ((System.currentTimeMillis() - l.longValue()) / 1000.0f);
        currentTest.setAttribute(ATTR_TIME, Float.toString(time));
        super.onTestEnded(testname);
        // remove the test objects
        testStarts.remove(testname);
        testElements.remove(testname);
    }

    public void onTestFailed(int status, String testname, String trace) {
        if (testname != null) {
            onTestEnded(testname);
        }
        String type = status == STATUS_FAILURE ? FAILURE : ERROR;
        Element nested = doc.createElement(type);
        Element currentTest = null;
        if (testname != null) {
            currentTest = (Element) testElements.get(testname);
        } else {
            currentTest = rootElement;
        }

        currentTest.appendChild(nested);

        String[] args = parseFirstLine(trace);
        if (args[1] != null && args[1].length() > 0) {
            nested.setAttribute(ATTR_MESSAGE, args[1]);
        }
        nested.setAttribute(ATTR_TYPE, args[0]);
        Text text = doc.createTextNode(trace);
        nested.appendChild(text);
        super.onTestFailed(status, testname, trace);
    }

    public void onTestRunStarted(int testcount) {
        super.onTestRunStarted(testcount);
    }

    public void onTestRunEnded(long elapsedtime) {
        super.onTestRunEnded(elapsedtime);
    }

    public void onTestRunStopped(long elapsedtime) {
        super.onTestRunStopped(elapsedtime);
    }

    private static DocumentBuilder getDocumentBuilder() {
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (Exception exc) {
            throw new ExceptionInInitializerError(exc);
        }
    }

    protected static String[] parseFirstLine(String trace) {
        int pos = trace.indexOf('\n');
        if (pos == -1) {
            return new String[]{trace, ""};
        }
        String line = trace.substring(0, pos);
        pos = line.indexOf(':');
        if (pos != -1) {
            String classname = line.substring(0, pos).trim();
            String message = line.substring(pos + 1).trim();
            return new String[]{classname, message};
        }
        return new String[]{trace, ""};
    }
}
