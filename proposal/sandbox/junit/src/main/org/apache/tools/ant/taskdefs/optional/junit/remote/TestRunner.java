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
package org.apache.tools.ant.taskdefs.optional.junit.remote;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.Random;

import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestListener;
import junit.framework.TestResult;
import junit.framework.TestSuite;

import org.apache.tools.ant.taskdefs.optional.junit.JUnitHelper;
import org.apache.tools.ant.taskdefs.optional.junit.TestRunListener;
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
 * @author <a href="mailto:sbailliez@apache.org">Stephane Bailliez</a>
 */
public class TestRunner implements TestListener {

    /** unique identifier for the runner */
    private final Integer id = new Integer( (new Random()).nextInt() );

    /** host to connect to */
    private String host = "127.0.0.1";

    /** port to connect to */
    private int port = -1;

    private boolean debug = false;

    /** the list of test class names to run */
    private Vector testClassNames = new Vector();

    /** result of the current test */
    private TestResult testResult;

    /** client socket to communicate with the server */
    private Socket clientSocket;

    /** writer to send message to the server */
    private Messenger messenger;

    /** bean constructor */
    public TestRunner() {
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
        testClassNames.addElement(classname);
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
     * @param the properties containing configuration data.
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
    protected Test[] getSuites() throws Exception {
        final int count = testClassNames.size();
        log("Extracting testcases from " + count + " classnames...");
        final Vector suites = new Vector(count);
        for (int i = 0; i < count; i++) {
            String classname = (String) testClassNames.elementAt(i);
            try {
                Test test = JUnitHelper.getTest(null, classname);
                if (test != null) {
                    suites.addElement(test);
                }
            } catch (Exception e) {
                // notify log error instead ?
                log("Could not get Test instance from " + classname);
                log(e);
            }
        }
        log("Extracted " + suites.size() + " testcases.");
        Test[] array = new Test[suites.size()];
        suites.copyInto(array);
        return array;
    }

    /**
     * @param testClassNames String array of full qualified class names of test classes
     */
    private void runTests() throws Exception {

        Test[] suites = getSuites();

        // count all testMethods and inform TestRunListeners
        int count = countTests(suites);
        log("Total tests to run: " + count);
        fireEvent(new TestRunEvent(id, TestRunEvent.RUN_STARTED));

        long startTime = System.currentTimeMillis();
        for (int i = 0; i < suites.length; i++) {
            String name = suites[i].getClass().getName();
            if (suites[i] instanceof TestCase) {
                suites[i] = new TestSuite(name);
            }
            log("running suite: " + suites[i]);
            fireEvent(new TestRunEvent(id, TestRunEvent.SUITE_STARTED, name));
            suites[i].run(testResult);
            fireEvent(new TestRunEvent(id, TestRunEvent.SUITE_ENDED, name));
        }

        // inform TestRunListeners of test end
        long elapsedTime = System.currentTimeMillis() - startTime;
        if (testResult == null || testResult.shouldStop()) {
            fireEvent(new TestRunEvent(id, TestRunEvent.RUN_STOPPED, System.getProperties()));
        } else {
            fireEvent(new TestRunEvent(id, TestRunEvent.RUN_ENDED, System.getProperties()));
        }
        log("Finished after " + elapsedTime + "ms");
        shutDown();
    }

    /** count the number of test methods in all tests */
    private final int countTests(Test[] tests) {
        int count = 0;
        for (int i = 0; i < tests.length; i++) {
            count = count + tests[i].countTestCases();
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
        log("starting test: " + test);
        fireEvent(new TestRunEvent(id, TestRunEvent.TEST_STARTED, testName));
    }

    public void addError(Test test, Throwable t) {
        log("Adding error for test: " + test);
        String testName = test.toString();
        fireEvent(new TestRunEvent(id, TestRunEvent.TEST_ERROR, testName, t));
    }

    /**
     * this implementation is for JUnit &lt; 3.4
     * @see addFailure(Test, Throwable)
     */
    public void addFailure(Test test, AssertionFailedError afe) {
        addFailure(test, (Throwable) afe);
    }

    /**
     * This implementation is for JUnit &lt;= 3.4
     * @see addFailure(Test, AssertionFailedError)
     */
    public void addFailure(Test test, Throwable t) {
        log("Adding failure for test: " + test);
        String testName = test.toString();
        fireEvent(new TestRunEvent(id, TestRunEvent.TEST_FAILURE, testName, t));
    }

    public void endTest(Test test) {
        log("Ending test: " + test);
        String testName = test.toString();
        fireEvent(new TestRunEvent(id, TestRunEvent.TEST_ENDED, testName));
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

