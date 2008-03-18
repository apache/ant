/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.tools.ant.taskdefs.optional.junit;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;
import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestFailure;
import junit.framework.TestListener;
import junit.framework.TestResult;
import junit.framework.TestSuite;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Permissions;
import org.apache.tools.ant.util.StringUtils;
import org.apache.tools.ant.util.TeeOutputStream;

/**
 * Simple Testrunner for JUnit that runs all tests of a testsuite.
 *
 * <p>This TestRunner expects a name of a TestCase class as its
 * argument. If this class provides a static suite() method it will be
 * called and the resulting Test will be run. So, the signature should be
 * <pre><code>
 *     public static junit.framework.Test suite()
 * </code></pre>
 *
 * <p> If no such method exists, all public methods starting with
 * "test" and taking no argument will be run.
 *
 * <p> Summary output is generated at the end.
 *
 * @since Ant 1.2
 */

public class JUnitTestRunner implements TestListener, JUnitTaskMirror.JUnitTestRunnerMirror {

    /**
     * Holds the registered formatters.
     */
    private Vector formatters = new Vector();

    /**
     * Collects TestResults.
     */
    private TestResult res;

    /**
     * Do we filter junit.*.* stack frames out of failure and error exceptions.
     */
    private static boolean filtertrace = true;

    /**
     * Do we send output to System.out/.err in addition to the formatters?
     */
    private boolean showOutput = false;

    private boolean outputToFormatters = true;

    /**
     * The permissions set for the test to run.
     */
    private Permissions perm = null;

    private static final String[] DEFAULT_TRACE_FILTERS = new String[] {
                "junit.framework.TestCase",
                "junit.framework.TestResult",
                "junit.framework.TestSuite",
                "junit.framework.Assert.", // don't filter AssertionFailure
                "junit.swingui.TestRunner",
                "junit.awtui.TestRunner",
                "junit.textui.TestRunner",
                "java.lang.reflect.Method.invoke(",
                "sun.reflect.",
                "org.apache.tools.ant.",
                // JUnit 4 support:
                "org.junit.",
                "junit.framework.JUnit4TestAdapter",
                // See wrapListener for reason:
                "Caused by: java.lang.AssertionError",
                " more",
        };


    /**
     * Do we stop on errors.
     */
    private boolean haltOnError = false;

    /**
     * Do we stop on test failures.
     */
    private boolean haltOnFailure = false;

    /**
     * Returncode
     */
    private int retCode = SUCCESS;

    /**
     * The TestSuite we are currently running.
     */
    private JUnitTest junitTest;

    /** output written during the test */
    private PrintStream systemError;

    /** Error output during the test */
    private PrintStream systemOut;

    /** is this runner running in forked mode? */
    private boolean forked = false;

    /** Running more than one test suite? */
    private static boolean multipleTests = false;

    /** ClassLoader passed in in non-forked mode. */
    private ClassLoader loader;

    /** Do we print TestListener events? */
    private boolean logTestListenerEvents = false;

    /** Turned on if we are using JUnit 4 for this test suite. see #38811 */
    private boolean junit4;

    /**
     * The file used to indicate that the build crashed.
     * File will be empty in case the build did not crash.
     */
    private static String crashFile = null;

    /**
     * Constructor for fork=true or when the user hasn't specified a
     * classpath.
     * @param test the test to run.
     * @param haltOnError whether to stop the run if an error is found.
     * @param filtertrace whether to filter junit.*.* stack frames out of exceptions
     * @param haltOnFailure whether to stop the run if failure is found.
     */
    public JUnitTestRunner(JUnitTest test, boolean haltOnError,
                           boolean filtertrace, boolean haltOnFailure) {
        this(test, haltOnError, filtertrace, haltOnFailure, false);
    }

    /**
     * Constructor for fork=true or when the user hasn't specified a
     * classpath.
     * @param test the test to run.
     * @param haltOnError whether to stop the run if an error is found.
     * @param filtertrace whether to filter junit.*.* stack frames out of exceptions
     * @param haltOnFailure whether to stop the run if failure is found.
     * @param showOutput    whether to send output to System.out/.err as well as formatters.
     */
    public JUnitTestRunner(JUnitTest test, boolean haltOnError,
                           boolean filtertrace, boolean haltOnFailure,
                           boolean showOutput) {
        this(test, haltOnError, filtertrace, haltOnFailure, showOutput, false);
    }

    /**
     * Constructor for fork=true or when the user hasn't specified a
     * classpath.
     * @param test the test to run.
     * @param haltOnError whether to stop the run if an error is found.
     * @param filtertrace whether to filter junit.*.* stack frames out of exceptions
     * @param haltOnFailure whether to stop the run if failure is found.
     * @param showOutput    whether to send output to System.out/.err as well as formatters.
     * @param logTestListenerEvents whether to print TestListener events.
     * @since Ant 1.7
     */
    public JUnitTestRunner(JUnitTest test, boolean haltOnError,
                           boolean filtertrace, boolean haltOnFailure,
                           boolean showOutput, boolean logTestListenerEvents) {
        this(test, haltOnError, filtertrace, haltOnFailure, showOutput,
             logTestListenerEvents, null);
    }

    /**
     * Constructor to use when the user has specified a classpath.
     * @param test the test to run.
     * @param haltOnError whether to stop the run if an error is found.
     * @param filtertrace whether to filter junit.*.* stack frames out of exceptions
     * @param haltOnFailure whether to stop the run if failure is found.
     * @param loader the classloader to use running the test.
     */
    public JUnitTestRunner(JUnitTest test, boolean haltOnError,
                           boolean filtertrace, boolean haltOnFailure,
                           ClassLoader loader) {
        this(test, haltOnError, filtertrace, haltOnFailure, false, loader);
    }

    /**
     * Constructor to use when the user has specified a classpath.
     * @param test the test to run.
     * @param haltOnError whether to stop the run if an error is found.
     * @param filtertrace whether to filter junit.*.* stack frames out of exceptions
     * @param haltOnFailure whether to stop the run if failure is found.
     * @param showOutput    whether to send output to System.out/.err as well as formatters.
     * @param loader the classloader to use running the test.
     */
    public JUnitTestRunner(JUnitTest test, boolean haltOnError,
                           boolean filtertrace, boolean haltOnFailure,
                           boolean showOutput, ClassLoader loader) {
        this(test, haltOnError, filtertrace, haltOnFailure, showOutput,
             false, loader);
    }

    /**
     * Constructor to use when the user has specified a classpath.
     * @param test the test to run.
     * @param haltOnError whether to stop the run if an error is found.
     * @param filtertrace whether to filter junit.*.* stack frames out of exceptions
     * @param haltOnFailure whether to stop the run if failure is found.
     * @param showOutput    whether to send output to System.out/.err as well as formatters.
     * @param logTestListenerEvents whether to print TestListener events.
     * @param loader the classloader to use running the test.
     * @since Ant 1.7
     */
    public JUnitTestRunner(JUnitTest test, boolean haltOnError,
                           boolean filtertrace, boolean haltOnFailure,
                           boolean showOutput, boolean logTestListenerEvents,
                           ClassLoader loader) {
        JUnitTestRunner.filtertrace = filtertrace;
        this.junitTest = test;
        this.haltOnError = haltOnError;
        this.haltOnFailure = haltOnFailure;
        this.showOutput = showOutput;
        this.logTestListenerEvents = logTestListenerEvents;
        this.loader = loader;
    }

    private PrintStream savedOut = null;
    private PrintStream savedErr = null;

    private PrintStream createEmptyStream() {
        return new PrintStream(
            new OutputStream() {
                public void write(int b) {
                }
            });
    }

    private PrintStream createTeePrint(PrintStream ps1, PrintStream ps2) {
        return new PrintStream(new TeeOutputStream(ps1, ps2));
    }

    private void setupIOStreams(ByteArrayOutputStream o,
                                ByteArrayOutputStream e) {
        systemOut = new PrintStream(o);
        systemError = new PrintStream(e);

        if (forked) {
            if (!outputToFormatters) {
                if (!showOutput) {
                    savedOut = System.out;
                    savedErr = System.err;
                    System.setOut(createEmptyStream());
                    System.setErr(createEmptyStream());
                }
            } else {
                savedOut = System.out;
                savedErr = System.err;
                if (!showOutput) {
                    System.setOut(systemOut);
                    System.setErr(systemError);
                } else {
                    System.setOut(createTeePrint(savedOut, systemOut));
                    System.setErr(createTeePrint(savedErr, systemError));
                }
                perm = null;
            }
        } else {
            if (perm != null) {
                perm.setSecurityManager();
            }
        }
    }

    /**
     * Run the test.
     */
    public void run() {
        res = new TestResult();
        res.addListener(wrapListener(this));
        for (int i = 0; i < formatters.size(); i++) {
            res.addListener(wrapListener((TestListener) formatters.elementAt(i)));
        }

        ByteArrayOutputStream errStrm = new ByteArrayOutputStream();
        ByteArrayOutputStream outStrm = new ByteArrayOutputStream();

        setupIOStreams(outStrm, errStrm);

        Test suite = null;
        Throwable exception = null;
        boolean startTestSuiteSuccess = false;

        try {

            try {
                Class testClass = null;
                if (loader == null) {
                    testClass = Class.forName(junitTest.getName());
                } else {
                    testClass = Class.forName(junitTest.getName(), true,
                                              loader);
                }

                // check for a static suite method first, even when using
                // JUnit 4
                Method suiteMethod = null;
                try {
                    // check if there is a suite method
                    suiteMethod = testClass.getMethod("suite", new Class[0]);
                } catch (NoSuchMethodException e) {
                    // no appropriate suite method found. We don't report any
                    // error here since it might be perfectly normal.
                }

                if (suiteMethod != null) {
                    // if there is a suite method available, then try
                    // to extract the suite from it. If there is an error
                    // here it will be caught below and reported.
                    suite = (Test) suiteMethod.invoke(null, new Class[0]);

                } else {
                    Class junit4TestAdapterClass = null;

                    // Check for JDK 5 first. Will *not* help on JDK 1.4
                    // if only junit-4.0.jar in CP because in that case
                    // linkage of whole task will already have failed! But
                    // will help if CP has junit-3.8.2.jar:junit-4.0.jar.

                    // In that case first C.fN will fail with CNFE and we
                    // will avoid UnsupportedClassVersionError.

                    try {
                        Class.forName("java.lang.annotation.Annotation");
                        if (loader == null) {
                            junit4TestAdapterClass =
                                Class.forName("junit.framework.JUnit4TestAdapter");
                        } else {
                            junit4TestAdapterClass =
                                Class.forName("junit.framework.JUnit4TestAdapter",
                                              true, loader);
                        }
                    } catch (ClassNotFoundException e) {
                        // OK, fall back to JUnit 3.
                    }
                    junit4 = junit4TestAdapterClass != null;

                    if (junit4) {
                        // Let's use it!
                        suite =
                            (Test) junit4TestAdapterClass
                            .getConstructor(new Class[] {Class.class}).
                            newInstance(new Object[] {testClass});
                    } else {
                        // Use JUnit 3.

                        // try to extract a test suite automatically this
                        // will generate warnings if the class is no
                        // suitable Test
                        suite = new TestSuite(testClass);
                    }

                }

            } catch (Throwable e) {
                retCode = ERRORS;
                exception = e;
            }

            long start = System.currentTimeMillis();

            fireStartTestSuite();
            startTestSuiteSuccess = true;
            if (exception != null) { // had an exception constructing suite
                for (int i = 0; i < formatters.size(); i++) {
                    ((TestListener) formatters.elementAt(i))
                        .addError(null, exception);
                }
                junitTest.setCounts(1, 0, 1);
                junitTest.setRunTime(0);
            } else {
                try {
                    logTestListenerEvent("tests to run: " + suite.countTestCases());
                    suite.run(res);
                } finally {
                    if (junit4) {
                        int[] cnts = findJUnit4FailureErrorCount(res);
                        junitTest.setCounts(res.runCount(), cnts[0], cnts[1]);
                    } else {
                        junitTest.setCounts(res.runCount(), res.failureCount(),
                                res.errorCount());
                    }
                    junitTest.setRunTime(System.currentTimeMillis() - start);
                }
            }
        } finally {
            if (perm != null) {
                perm.restoreSecurityManager();
            }
            if (savedOut != null) {
                System.setOut(savedOut);
            }
            if (savedErr != null) {
                System.setErr(savedErr);
            }

            systemError.close();
            systemError = null;
            systemOut.close();
            systemOut = null;
            if (startTestSuiteSuccess) {
                sendOutAndErr(new String(outStrm.toByteArray()),
                              new String(errStrm.toByteArray()));
            }
        }
        fireEndTestSuite();

        if (retCode != SUCCESS || res.errorCount() != 0) {
            retCode = ERRORS;
        } else if (res.failureCount() != 0) {
            retCode = FAILURES;
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
     * @param t the test.
     */
    public void startTest(Test t) {
        String testName = JUnitVersionHelper.getTestCaseName(t);
        logTestListenerEvent("startTest(" + testName + ")");
    }

    /**
     * Interface TestListener.
     *
     * <p>A Test is finished.
     * @param test the test.
     */
    public void endTest(Test test) {
        String testName = JUnitVersionHelper.getTestCaseName(test);
        logTestListenerEvent("endTest(" + testName + ")");
    }

    private void logTestListenerEvent(String msg) {
        PrintStream out = savedOut != null ? savedOut : System.out;
        if (logTestListenerEvents) {
            out.flush();
            out.println(JUnitTask.TESTLISTENER_PREFIX + msg);
            out.flush();
        }
    }

    /**
     * Interface TestListener for JUnit &lt;= 3.4.
     *
     * <p>A Test failed.
     * @param test the test.
     * @param t    the exception thrown by the test.
     */
    public void addFailure(Test test, Throwable t) {
        String testName = JUnitVersionHelper.getTestCaseName(test);
        logTestListenerEvent("addFailure(" + testName + ", " + t.getMessage() + ")");
        if (haltOnFailure) {
            res.stop();
        }
    }

    /**
     * Interface TestListener for JUnit &gt; 3.4.
     *
     * <p>A Test failed.
     * @param test the test.
     * @param t    the assertion thrown by the test.
     */
    public void addFailure(Test test, AssertionFailedError t) {
        addFailure(test, (Throwable) t);
    }

    /**
     * Interface TestListener.
     *
     * <p>An error occurred while running the test.
     * @param test the test.
     * @param t    the error thrown by the test.
     */
    public void addError(Test test, Throwable t) {
        String testName = JUnitVersionHelper.getTestCaseName(test);
        logTestListenerEvent("addError(" + testName + ", " + t.getMessage() + ")");
        if (haltOnError) {
            res.stop();
        }
    }

    /**
     * Permissions for the test run.
     * @since Ant 1.6
     * @param permissions the permissions to use.
     */
    public void setPermissions(Permissions permissions) {
        perm = permissions;
    }

    /**
     * Handle a string destined for standard output.
     * @param output the string to output
     */
    public void handleOutput(String output) {
        if (!logTestListenerEvents && output.startsWith(JUnitTask.TESTLISTENER_PREFIX)) {
            // ignore
        } else if (systemOut != null) {
            systemOut.print(output);
        }
    }

    /**
     * Handle input.
     * @param buffer not used.
     * @param offset not used.
     * @param length not used.
     * @return -1 always.
     * @throws IOException never.
     * @see org.apache.tools.ant.Task#handleInput(byte[], int, int)
     *
     * @since Ant 1.6
     */
    public int handleInput(byte[] buffer, int offset, int length)
        throws IOException {
        return -1;
    }

    /** {@inheritDoc}. */
    public void handleErrorOutput(String output) {
        if (systemError != null) {
            systemError.print(output);
        }
    }

    /** {@inheritDoc}. */
    public void handleFlush(String output) {
        if (systemOut != null) {
            systemOut.print(output);
        }
    }

    /** {@inheritDoc}. */
    public void handleErrorFlush(String output) {
        if (systemError != null) {
            systemError.print(output);
        }
    }

    private void sendOutAndErr(String out, String err) {
        for (int i = 0; i < formatters.size(); i++) {
            JUnitResultFormatter formatter =
                ((JUnitResultFormatter) formatters.elementAt(i));

            formatter.setSystemOutput(out);
            formatter.setSystemError(err);
        }
    }

    private void fireStartTestSuite() {
        for (int i = 0; i < formatters.size(); i++) {
            ((JUnitResultFormatter) formatters.elementAt(i))
                .startTestSuite(junitTest);
        }
    }

    private void fireEndTestSuite() {
        for (int i = 0; i < formatters.size(); i++) {
            ((JUnitResultFormatter) formatters.elementAt(i))
                .endTestSuite(junitTest);
        }
    }

    /**
     * Add a formatter.
     * @param f the formatter to add.
     */
    public void addFormatter(JUnitResultFormatter f) {
        formatters.addElement(f);
    }

    /** {@inheritDoc}. */
    public void addFormatter(JUnitTaskMirror.JUnitResultFormatterMirror f) {
        formatters.addElement((JUnitResultFormatter) f);
    }

    /**
     * Entry point for standalone (forked) mode.
     *
     * Parameters: testcaseclassname plus parameters in the format
     * key=value, none of which is required.
     *
     * <table cols="4" border="1">
     * <tr><th>key</th><th>description</th><th>default value</th></tr>
     *
     * <tr><td>haltOnError</td><td>halt test on
     * errors?</td><td>false</td></tr>
     *
     * <tr><td>haltOnFailure</td><td>halt test on
     * failures?</td><td>false</td></tr>
     *
     * <tr><td>formatter</td><td>A JUnitResultFormatter given as
     * classname,filename. If filename is ommitted, System.out is
     * assumed.</td><td>none</td></tr>
     *
     * <tr><td>showoutput</td><td>send output to System.err/.out as
     * well as to the formatters?</td><td>false</td></tr>
     *
     * <tr><td>logtestlistenerevents</td><td>log TestListener events to
     * System.out.</td><td>false</td></tr>
     *
     * </table>
     * @param args the command line arguments.
     * @throws IOException on error.
     */
    public static void main(String[] args) throws IOException {
        boolean haltError = false;
        boolean haltFail = false;
        boolean stackfilter = true;
        Properties props = new Properties();
        boolean showOut = false;
        boolean outputToFormat = true;
        boolean logTestListenerEvents = false;


        if (args.length == 0) {
            System.err.println("required argument TestClassName missing");
            System.exit(ERRORS);
        }

        if (args[0].startsWith(Constants.TESTSFILE)) {
            multipleTests = true;
            args[0] = args[0].substring(Constants.TESTSFILE.length());
        }

        for (int i = 1; i < args.length; i++) {
            if (args[i].startsWith(Constants.HALT_ON_ERROR)) {
                haltError = Project.toBoolean(args[i].substring(Constants.HALT_ON_ERROR.length()));
            } else if (args[i].startsWith(Constants.HALT_ON_FAILURE)) {
                haltFail = Project.toBoolean(args[i].substring(Constants.HALT_ON_FAILURE.length()));
            } else if (args[i].startsWith(Constants.FILTERTRACE)) {
                stackfilter = Project.toBoolean(args[i].substring(Constants.FILTERTRACE.length()));
            } else if (args[i].startsWith(Constants.CRASHFILE)) {
                crashFile = args[i].substring(Constants.CRASHFILE.length());
                registerTestCase(Constants.BEFORE_FIRST_TEST);
            } else if (args[i].startsWith(Constants.FORMATTER)) {
                try {
                    createAndStoreFormatter(args[i].substring(Constants.FORMATTER.length()));
                } catch (BuildException be) {
                    System.err.println(be.getMessage());
                    System.exit(ERRORS);
                }
            } else if (args[i].startsWith(Constants.PROPSFILE)) {
                FileInputStream in = new FileInputStream(args[i]
                                                         .substring(Constants.PROPSFILE.length()));
                props.load(in);
                in.close();
            } else if (args[i].startsWith(Constants.SHOWOUTPUT)) {
                showOut = Project.toBoolean(args[i].substring(Constants.SHOWOUTPUT.length()));
            } else if (args[i].startsWith(Constants.LOGTESTLISTENEREVENTS)) {
                logTestListenerEvents = Project.toBoolean(
                    args[i].substring(Constants.LOGTESTLISTENEREVENTS.length()));
            } else if (args[i].startsWith(Constants.OUTPUT_TO_FORMATTERS)) {
                outputToFormat = Project.toBoolean(
                    args[i].substring(Constants.OUTPUT_TO_FORMATTERS.length()));
            }
        }

        // Add/overlay system properties on the properties from the Ant project
        Hashtable p = System.getProperties();
        for (Enumeration e = p.keys(); e.hasMoreElements();) {
            Object key = e.nextElement();
            props.put(key, p.get(key));
        }

        int returnCode = SUCCESS;
        if (multipleTests) {
            try {
                java.io.BufferedReader reader =
                    new java.io.BufferedReader(new java.io.FileReader(args[0]));
                String testCaseName;
                int code = 0;
                boolean errorOccurred = false;
                boolean failureOccurred = false;
                String line = null;
                while ((line = reader.readLine()) != null) {
                    StringTokenizer st = new StringTokenizer(line, ",");
                    testCaseName = st.nextToken();
                    JUnitTest t = new JUnitTest(testCaseName);
                    t.setTodir(new File(st.nextToken()));
                    t.setOutfile(st.nextToken());
                    t.setProperties(props);
                    code = launch(t, haltError, stackfilter, haltFail,
                                  showOut, outputToFormat,
                                  logTestListenerEvents);
                    errorOccurred = (code == ERRORS);
                    failureOccurred = (code != SUCCESS);
                    if (errorOccurred || failureOccurred) {
                        if ((errorOccurred && haltError)
                            || (failureOccurred && haltFail)) {
                            registerNonCrash();
                            System.exit(code);
                        } else {
                            if (code > returnCode) {
                                returnCode = code;
                            }
                            System.out.println("TEST " + t.getName()
                                               + " FAILED");
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            JUnitTest t = new JUnitTest(args[0]);
            t.setProperties(props);
            returnCode = launch(
                t, haltError, stackfilter, haltFail,
                showOut, outputToFormat, logTestListenerEvents);
        }

        registerNonCrash();
        System.exit(returnCode);
    }

    private static Vector fromCmdLine = new Vector();

    private static void transferFormatters(JUnitTestRunner runner,
                                           JUnitTest test) {
        runner.addFormatter(new JUnitResultFormatter() {

            public void startTestSuite(JUnitTest suite) throws BuildException {
            }

            public void endTestSuite(JUnitTest suite) throws BuildException {
            }

            public void setOutput(OutputStream out) {
            }

            public void setSystemOutput(String out) {
            }

            public void setSystemError(String err) {
            }

            public void addError(Test arg0, Throwable arg1) {
            }

            public void addFailure(Test arg0, AssertionFailedError arg1) {
            }

            public void endTest(Test arg0) {
            }

            public void startTest(Test arg0) {
                registerTestCase(JUnitVersionHelper.getTestCaseName(arg0));
            }
        });
        for (int i = 0; i < fromCmdLine.size(); i++) {
            FormatterElement fe = (FormatterElement) fromCmdLine.elementAt(i);
            if (multipleTests && fe.getUseFile()) {
                File destFile =
                    new File(test.getTodir(),
                             test.getOutfile() + fe.getExtension());
                fe.setOutfile(destFile);
            }
            runner.addFormatter((JUnitResultFormatter) fe.createFormatter());
        }
    }

    /**
     * Line format is: formatter=<classname>(,<pathname>)?
     */
    private static void createAndStoreFormatter(String line)
        throws BuildException {
        FormatterElement fe = new FormatterElement();
        int pos = line.indexOf(',');
        if (pos == -1) {
            fe.setClassname(line);
            fe.setUseFile(false);
        } else {
            fe.setClassname(line.substring(0, pos));
            fe.setUseFile(true);
            if (!multipleTests) {
                fe.setOutfile(new File(line.substring(pos + 1)));
            } else {
                int fName = line.indexOf(IGNORED_FILE_NAME);
                if (fName > -1) {
                    fe.setExtension(line
                                    .substring(fName
                                               + IGNORED_FILE_NAME.length()));
                }
            }
        }
        fromCmdLine.addElement(fe);
    }

    /**
     * Returns a filtered stack trace.
     * This is ripped out of junit.runner.BaseTestRunner.
     * @param t the exception to filter.
     * @return the filtered stack trace.
     */
    public static String getFilteredTrace(Throwable t) {
        String trace = StringUtils.getStackTrace(t);
        return JUnitTestRunner.filterStack(trace);
    }

    /**
     * Filters stack frames from internal JUnit and Ant classes
     * @param stack the stack trace to filter.
     * @return the filtered stack.
     */
    public static String filterStack(String stack) {
        if (!filtertrace) {
            return stack;
        }
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        StringReader sr = new StringReader(stack);
        BufferedReader br = new BufferedReader(sr);

        String line;
        try {
            while ((line = br.readLine()) != null) {
                if (!filterLine(line)) {
                    pw.println(line);
                }
            }
        } catch (Exception e) {
            return stack; // return the stack unfiltered
        }
        return sw.toString();
    }

    private static boolean filterLine(String line) {
        for (int i = 0; i < DEFAULT_TRACE_FILTERS.length; i++) {
            if (line.indexOf(DEFAULT_TRACE_FILTERS[i]) != -1) {
                return true;
            }
        }
        return false;
    }

    /**
     * @since Ant 1.6.2
     */
    private static int launch(JUnitTest t, boolean haltError,
                              boolean stackfilter, boolean haltFail,
                              boolean showOut, boolean outputToFormat,
                              boolean logTestListenerEvents) {
        JUnitTestRunner runner =
            new JUnitTestRunner(t, haltError, stackfilter, haltFail, showOut,
                                logTestListenerEvents, null);
        runner.forked = true;
        runner.outputToFormatters = outputToFormat;
        transferFormatters(runner, t);

        runner.run();
        return runner.getRetCode();
     }

    /**
     * @since Ant 1.7
     */
    private static void registerNonCrash()
            throws IOException {
        if (crashFile != null) {
            FileWriter out = null;
            try {
                out = new FileWriter(crashFile);
                out.write(Constants.TERMINATED_SUCCESSFULLY + "\n");
                out.flush();
            } finally {
                if (out != null) {
                    out.close();
                }
            }
        }
    }

    private static void registerTestCase(String testCase) {
        if (crashFile != null) {
            try {
                FileWriter out = null;
                try {
                    out = new FileWriter(crashFile);
                    out.write(testCase + "\n");
                    out.flush();
                } finally {
                    if (out != null) {
                        out.close();
                    }
                }
            } catch (IOException e) {
                // ignored.
            }
        }
    }

    /**
     * Modifies a TestListener when running JUnit 4: treats AssertionFailedError
     * as a failure not an error.
     *
     * @since Ant 1.7
     */
    private TestListener wrapListener(final TestListener testListener) {
        return new TestListener() {
            public void addError(Test test, Throwable t) {
                if (junit4 && t instanceof AssertionFailedError) {
                    // JUnit 4 does not distinguish between errors and failures
                    // even in the JUnit 3 adapter.
                    // So we need to help it a bit to retain compatibility for JUnit 3 tests.
                    testListener.addFailure(test, (AssertionFailedError) t);
                } else if (junit4 && t.getClass().getName().equals("java.lang.AssertionError")) {
                    // Not strictly necessary but probably desirable.
                    // JUnit 4-specific test GUIs will show just "failures".
                    // But Ant's output shows "failures" vs. "errors".
                    // We would prefer to show "failure" for things that logically are.
                    try {
                        String msg = t.getMessage();
                        AssertionFailedError failure = msg != null
                            ? new AssertionFailedError(msg) : new AssertionFailedError();
                        // To compile on pre-JDK 4 (even though this should always succeed):
                        Method initCause = Throwable.class.getMethod(
                            "initCause", new Class[] {Throwable.class});
                        initCause.invoke(failure, new Object[] {t});
                        testListener.addFailure(test, failure);
                    } catch (Exception e) {
                        // Rats.
                        e.printStackTrace(); // should not happen
                        testListener.addError(test, t);
                    }
                } else {
                    testListener.addError(test, t);
                }
            }
            public void addFailure(Test test, AssertionFailedError t) {
                testListener.addFailure(test, t);
            }
            public void addFailure(Test test, Throwable t) { // pre-3.4
                if (t instanceof AssertionFailedError) {
                    testListener.addFailure(test, (AssertionFailedError) t);
                } else {
                    testListener.addError(test, t);
                }
            }
            public void endTest(Test test) {
                testListener.endTest(test);
            }
            public void startTest(Test test) {
                testListener.startTest(test);
            }
        };
    }

    /**
     * Use instead of TestResult.get{Failure,Error}Count on JUnit 4,
     * since the adapter claims that all failures are errors.
     * @since Ant 1.7
     */
    private int[] findJUnit4FailureErrorCount(TestResult res) {
        int failures = 0;
        int errors = 0;
        Enumeration e = res.failures();
        while (e.hasMoreElements()) {
            e.nextElement();
            failures++;
        }
        e = res.errors();
        while (e.hasMoreElements()) {
            Throwable t = ((TestFailure) e.nextElement()).thrownException();
            if (t instanceof AssertionFailedError
                || t.getClass().getName().equals("java.lang.AssertionError")) {
                failures++;
            } else {
                errors++;
            }
        }
        return new int[] {failures, errors};
    }

} // JUnitTestRunner
