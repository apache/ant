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

import org.apache.tools.ant.Project;

import junit.framework.*;
import java.lang.reflect.*;
import java.io.*;
import java.util.Vector;

/**
 * Simple Testrunner for JUnit that runs all tests of a testsuite.
 *
 * <p>This TestRunner expects a name of a TestCase class as its
 * argument. If this class provides a static suite() method it will be
 * called and the resulting Test will be run.
 *
 * <p>Otherwise all public methods starting with "test" and taking no
 * argument will be run.
 *
 * <p>Summary output is generated at the end.
 *
 * @author <a href="mailto:stefan.bodewig@megabit.net">Stefan Bodewig</a>
 */

public class JUnitTestRunner implements TestListener {

    /**
     * Holds the registered formatters.
     */
    private Vector formatters = new Vector();

    /**
     * Collects TestResults.
     */
    private TestResult res;

    /**
     * Flag for endTest.
     */
    private boolean failed = true;

    /**
     * The test I'm going to run.
     */
    private JUnitTest junitTest;

    /**
     * The corresponding testsuite.
     */
    private Test suite = null;

    /**
     * Returncode
     */
    private int retCode = 0;

    public JUnitTestRunner(JUnitTest test) {
        junitTest = test;
        try {
            if (junitTest.getPrintxml()) {
                if (test.getOutfile() != null
                    && test.getOutfile().length() > 0) {

                    addFormatter(new XMLJUnitResultFormatter(
                                     new PrintWriter(
                                         new FileWriter(test.getOutfile(), false)
                                             )
                                         )
                        );
                } else {
                    addFormatter(new XMLJUnitResultFormatter(
                                     new PrintWriter(
                                         new OutputStreamWriter(System.out), true)
                                         )
                        );
                }
            }

            if (junitTest.getPrintsummary()) {
                addFormatter(new SummaryJUnitResultFormatter());
            }

            Class testClass = Class.forName(junitTest.getName());

            try {
                Method suiteMethod= testClass.getMethod("suite", new Class[0]);
                suite = (Test)suiteMethod.invoke(null, new Class[0]);
            } catch(NoSuchMethodException e) {
            } catch(InvocationTargetException e) {
            } catch(IllegalAccessException e) {
            }

            if (suite == null) {
                // try to extract a test suite automatically
                // this will generate warnings if the class is no suitable Test
                suite= new TestSuite(testClass);
            }

            res = new TestResult();
            res.addListener(this);
            for (int i=0; i < formatters.size(); i++) {
                res.addListener((TestListener)formatters.elementAt(i));
            }

        } catch(Exception e) {
            retCode = 2;

            fireStartTestSuite();
            for (int i=0; i < formatters.size(); i++) {
                ((TestListener)formatters.elementAt(i)).addError(null, e);
            }
            junitTest.setCounts(1, 0, 1);
            junitTest.setRunTime(0);
            fireEndTestSuite();
        }
    }

    public void run() {
        long start = System.currentTimeMillis();

        if (retCode != 0) { // had an exception in the constructor
            return;
        }

        fireStartTestSuite();
        suite.run(res);
        junitTest.setRunTime(System.currentTimeMillis()-start);
        junitTest.setCounts(res.runCount(), res.failureCount(),
                            res.errorCount());
        fireEndTestSuite();

        if (res.errorCount() != 0) {
            retCode = 2;
        } else if (res.failureCount() != 0) {
            retCode = 1;
        }
    }

    /**
     * Returns what System.exit() would return in the standalone version.
     *
     * @return 2 if errors occurred, 1 if tests failed else 0.
     */
    public int getRetCode() {
        return retCode;
    }

    /**
     * Interface TestListener.
     *
     * <p>A new Test is started.
     */
    public void startTest(Test t) {
        failed = false;
    }

    /**
     * Interface TestListener.
     *
     * <p>A Test is finished.
     */
    public void endTest(Test test) {
    }

    /**
     * Interface TestListener.
     *
     * <p>A Test failed.
     */
    public void addFailure(Test test, Throwable t) {
        failed = true;

        if (junitTest.getHaltonfailure()) {
            res.stop();
        }
    }

    /**
     * Interface TestListener.
     *
     * <p>An error occured while running the test.
     */
    public void addError(Test test, Throwable t) {
        failed = true;

        if (junitTest.getHaltonerror()) {
            res.stop();
        }
    }

    private void fireStartTestSuite() {
        for (int i=0; i<formatters.size(); i++) {
            ((JUnitResultFormatter)formatters.elementAt(i)).startTestSuite(junitTest);
        }
    }

    private void fireEndTestSuite() {
        for (int i=0; i<formatters.size(); i++) {
            ((JUnitResultFormatter)formatters.elementAt(i)).endTestSuite(junitTest);
        }
    }

    public void addFormatter(JUnitResultFormatter f) {
        formatters.addElement(f);
    }

    /**
     * Entry point for standalone (forked) mode.
     *
     * Parameters: testcaseclassname plus (up to) 6 parameters in the
     * format key=value.
     *
     * <table cols="3" border="1">
     * <tr><th>key</th><th>description</th><th>default value</th></tr>
     *
     * <tr><td>exit</td><td>exit with System.exit after testcase is
     * complete?</td><td>true</td></tr>
     *
     * <tr><td>haltOnError</td><td>halt test on
     * errors?</td><td>false</td></tr>
     *
     * <tr><td>haltOnFailure</td><td>halt test on
     * failures?</td><td>false</td></tr>
     *
     * <tr><td>printSummary</td><td>print summary to System.out?</td>
     * <td>true</td></tr>
     *
     * <tr><td>printXML</td><td>generate XML report?</td>
     * <td>false</td></tr>
     *
     * <tr><td>outfile</td><td>where to print the XML report - a
     * filename</td> <td>System.out</td></tr>
     *
     * </table>
     */
    public static void main(String[] args) throws IOException {
        boolean exitAtEnd = true;
        boolean haltError = false;
        boolean haltFail = false;
        boolean printSummary = true;
        boolean printXml = false;
        PrintWriter out = null;

        if (args.length == 0) {
            System.err.println("required argument TestClassName missing");
            if (exitAtEnd) {
                System.exit(2);
            }
        } else {

            JUnitTest test = new JUnitTest();
            test.setName(args[0]);
            args[0] = null;
            test.setCommandline(args);
            JUnitTestRunner runner = new JUnitTestRunner(test);
            runner.run();

            if (exitAtEnd) {
                System.exit(runner.getRetCode());
            }
        }
    }


    public static int runTest(JUnitTest test) {
        final JUnitTestRunner runner = new JUnitTestRunner(test);
        runner.run();
        return runner.getRetCode();
    }

} // JUnitTestRunner
