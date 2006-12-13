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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.Random;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestListener;
import junit.framework.TestResult;
import junit.framework.TestSuite;

import org.apache.tools.ant.taskdefs.optional.rjunit.JUnitHelper;
import org.apache.tools.ant.taskdefs.optional.rjunit.formatter.Formatter;
import org.apache.tools.ant.taskdefs.optional.rjunit.formatter.PlainFormatter;
import org.apache.tools.ant.util.StringUtils;

/**
 * TestRunner for running tests and send results to a remote server.
 *
 * <i>
 * This code is based on the code from Erich Gamma made for the
 * JUnit plugin for <a href="http://www.eclipse.org">Eclipse</a> and is
 * merged with code originating from Ant 1.4.x.
 * </i>
 *
 */
public class TestRunner implements TestListener {

    /** unique identifier for the runner */
    private final Integer id = new Integer( (new Random()).nextInt() );

    /** host to connect to */
    private String host = "127.0.0.1";

    /** port to connect to */
    private int port = -1;

    /** handy debug flag */
    private boolean debug = false;

    /** the list of test class names to run */
    private final ArrayList testClassNames = new ArrayList();

    /** result of the current test */
    private TestResult testResult;

    /** client socket to communicate with the server */
    private Socket clientSocket;

    /** writer to send message to the server */
    private Messenger messenger;

    /** helpful formatter to debug events directly here */
    private final Formatter debugFormatter = new PlainFormatter();

    /** bean constructor */
    public TestRunner() {
        Properties props = new Properties();
        props.setProperty("file", "rjunit-client-debug.log");
        debugFormatter.init(props);
    }

    /**
     * Set the debug mode.
     * @param debug true to set to debug mode otherwise false.
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    /**
     * Set the port to connect to the server
     * @param port a valid port number.
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * Set the hostname of the server
     * @param host the hostname or ip of the server
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * Add a test class name to be executed by this runner.
     * @param classname the class name of the test to run.
     */
    public void addTestClassName(String classname) {
        testClassNames.add(classname);
    }

    /**
     * Thread listener for a shutdown from the server
     * Note that it will stop any running test.
     */
    private class StopThread extends Thread {
        public void run() {
            try {
                TestRunEvent evt = null;
                if ((evt = messenger.read()) != null) {
                    if (evt.getType() == TestRunEvent.RUN_STOP) {
                        TestRunner.this.stop();
                    }
                }
            } catch (Exception e) {
                TestRunner.this.stop();
            }
        }
    }

    /**
     * Entry point for command line.
     * Usage:
     * <pre>
     * TestRunner -classnames <classnames> -port <port> -host <host> -debug
     * -file
     * -classnames <list of whitespace separated classnames to run>
     * -port       <port to connect to>
     * -host       <host to connect to>
     * -debug      to run in debug mode
     * </pre>
     */
    public static void main(String[] args) throws Exception {
        TestRunner testRunServer = new TestRunner();
        testRunServer.init(args);
        testRunServer.run();
    }

    /**
     * Parses the arguments of command line.
     * testClassNames, host, port, listeners and debug mode are set
     * @see  #main(String[])
     */
    protected void init(String[] args) throws Exception {
        for (int i = 0; i < args.length; i++) {
            if ("-file".equalsIgnoreCase(args[i])) {
                // @fixme if you mix file and other options it will be a mess,
                // not important right now.
                FileInputStream fis = new FileInputStream(args[++i]);
                Properties props = new Properties();
                props.load(fis);
                fis.close();
                init(props);
            }
            if ("-classnames".equalsIgnoreCase(args[i])) {
                for (int j = ++i; j < args.length; j++) {
                    if (args[j].startsWith("-"))
                        break;
                    addTestClassName(args[j]);
                }
            }
            if ("-port".equalsIgnoreCase(args[i])) {
                setPort(Integer.parseInt(args[++i]));
            }
            if ("-host".equalsIgnoreCase(args[i])) {
                setHost(args[++i]);
            }
            if ("-debug".equalsIgnoreCase(args[i])) {
                setDebug(true);
            }
        }
    }

    /**
     * Initialize the TestRunner from properties.
     * @param props the properties containing configuration data.
     * @see #init(String[])
     */
    protected void init(Properties props) {
        if (props.getProperty("debug") != null) {
            setDebug(true);
        }
        String port = props.getProperty("port");
        if (port != null) {
            setPort(Integer.parseInt(port));
        }
        String host = props.getProperty("host");
        if (host != null) {
            setHost(host);
        }
        String classnames = props.getProperty("classnames");
        if (classnames != null) {
            StringTokenizer st = new StringTokenizer(classnames);
            while (st.hasMoreTokens()) {
                addTestClassName(st.nextToken());
            }
        }
    }

    public final void run() throws Exception {
        if (testClassNames.size() == 0) {
            throw new IllegalArgumentException("No TestCase specified");
        }
        connect();

        testResult = new TestResult();
        testResult.addListener(this);
        runTests();

        testResult.removeListener(this);
        if (testResult != null) {
            testResult.stop();
            testResult = null;
        }
    }

    /**
     * Transform all classnames into instantiated <tt>Test</tt>.
     * @throws Exception a generic exception that can be thrown while
     * instantiating a test case.
     */
    protected Map getSuites() throws Exception {
        final int count = testClassNames.size();
        log("Extracting testcases from " + count + " classnames...");
        final Map suites = new HashMap();
        for (int i = 0; i < count; i++) {
            String classname = (String) testClassNames.get(i);
            try {
                Test test = JUnitHelper.getTest(null, classname);
                if (test != null) {
                    suites.put(classname, test);
                }
            } catch (Exception e) {
                // notify log error instead ?
                log("Could not get Test instance from " + classname);
                log(e);
            }
        }
        log("Extracted " + suites.size() + " testcases.");
        return suites;
    }

    private void runTests() throws Exception {

        Map suites = getSuites();

        // count all testMethods and inform TestRunListeners
        int count = countTests(suites.values());
        log("Total tests to run: " + count);
        TestRunEvent evt = new TestRunEvent(id, TestRunEvent.RUN_STARTED);
        if (debug){
            debugFormatter.onRunStarted(evt);
        }
        fireEvent(evt);

        TestSummary runSummary = new TestSummary();
        runSummary.start(testResult);
        for (Iterator it = suites.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry)it.next();
            String name = (String)entry.getKey();
            Test test = (Test)entry.getValue();
            if (test instanceof TestCase) {
                test = new TestSuite(name);
            }
            runTest(test, name);
        }
        runSummary.stop(testResult);

        // inform TestRunListeners of test end
        int type = (testResult == null || testResult.shouldStop()) ?
            TestRunEvent.RUN_STOPPED : TestRunEvent.RUN_ENDED;
        evt = new TestRunEvent(id, type, System.getProperties(), runSummary);
        if (debug){
            debugFormatter.onRunEnded(evt);
        }
        fireEvent(evt);
        log("Finished after " + runSummary.elapsedTime() + "ms");
        shutDown();
    }

    /**
     * run a single suite and dispatch its results.
     * @param test the instance of the testsuite to run.
     * @param name the name of the testsuite (classname)
     */
    private void runTest(Test test, String name){
        TestRunEvent evt = new TestRunEvent(id, TestRunEvent.SUITE_STARTED, name);
        if (debug){
            debugFormatter.onSuiteStarted(evt);
        }
        fireEvent(evt);
        TestSummary suiteSummary = new TestSummary();
        suiteSummary.start(testResult);
        try {
            test.run(testResult);
        } finally {
            suiteSummary.stop(testResult);
            evt = new TestRunEvent(id, TestRunEvent.SUITE_ENDED, name, suiteSummary);
            if (debug){
                debugFormatter.onSuiteEnded(evt);
            }
            fireEvent(evt);
        }
    }

    /**
     * count the number of test methods in all tests
     */
    private final int countTests(Collection tests) {
        int count = 0;
        for (Iterator it = tests.iterator(); it.hasNext(); ) {
            Test test = (Test)it.next();
            count = count + test.countTestCases();
        }
        return count;
    }

    protected void stop() {
        if (testResult != null) {
            testResult.stop();
        }
    }

    /**
     * connect to the specified host and port.
     * @throws IOException if any error occurs during connection.
     */
    protected void connect() throws IOException {
        log("Connecting to " + host + " on port " + port + "...");
        clientSocket = new Socket(host, port);
        messenger = new Messenger(clientSocket.getInputStream(), clientSocket.getOutputStream());
        new StopThread().start();
    }


    protected void shutDown() {
        try {
            if (messenger != null) {
                messenger.close();
                messenger = null;
            }
        } catch (IOException e) {
            log(e);
        }

        try {
            if (clientSocket != null) {
                clientSocket.close();
                clientSocket = null;
            }
        } catch (IOException e) {
            log(e);
        }
    }

    protected void fireEvent(TestRunEvent evt){
        try {
            messenger.writeEvent(evt);
        } catch (IOException e){
            log(e);
        }
    }

// -------- JUnit TestListener implementation


    public void startTest(Test test) {
        String testName = test.toString();
        TestRunEvent evt = new TestRunEvent(id, TestRunEvent.TEST_STARTED, testName);
        if (debug){
            debugFormatter.onTestStarted(evt);
        }
        fireEvent(evt);
    }

    public void addError(Test test, Throwable t) {
        String testName = test.toString();
        TestRunEvent evt = new TestRunEvent(id, TestRunEvent.TEST_ERROR, testName, t);
        if (debug){
            debugFormatter.onTestError(evt);
        }
        fireEvent(evt);
    }

    /**
     * this implementation is for JUnit &lt; 3.4
     * @see #addFailure(Test, Throwable)
     */
    public void addFailure(Test test, AssertionFailedError afe) {
        addFailure(test, (Throwable) afe);
    }

    /**
     * This implementation is for JUnit &lt;= 3.4
     * @see #addFailure(Test, AssertionFailedError)
     */
    public void addFailure(Test test, Throwable t) {
        String testName = test.toString();
        TestRunEvent evt = new TestRunEvent(id, TestRunEvent.TEST_FAILURE, testName, t);
        if (debug){
            debugFormatter.onTestFailure(evt);
        }
        fireEvent(evt);
    }

    public void endTest(Test test) {
        String testName = test.toString();
        TestRunEvent evt = new TestRunEvent(id, TestRunEvent.TEST_ENDED, testName);
        if (debug){
            debugFormatter.onTestEnded(evt);
        }
        fireEvent(evt);
    }

    public void log(String msg) {
        if (debug) {
            System.out.println(msg);
        }
    }

    public void log(Throwable t) {
        if (debug) {
            t.printStackTrace();
        }
    }
}

