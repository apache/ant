/*
 * Copyright  2000-2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
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
import java.io.IOException;
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

public class JUnitTestRunner implements TestListener {

    /**
     * No problems with this test.
     */
    public static final int SUCCESS = 0;

    /**
     * Some tests failed.
     */
    public static final int FAILURES = 1;

    /**
     * An error occurred.
     */
    public static final int ERRORS = 2;

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
                "org.apache.tools.ant."
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
     * The corresponding testsuite.
     */
    private Test suite = null;

    /**
     * Exception caught in constructor.
     */
    private Exception exception;

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

    /**
     * Constructor for fork=true or when the user hasn't specified a
     * classpath.
     */
    public JUnitTestRunner(JUnitTest test, boolean haltOnError,
                           boolean filtertrace, boolean haltOnFailure) {
        this(test, haltOnError, filtertrace, haltOnFailure, false);
    }

    /**
     * Constructor for fork=true or when the user hasn't specified a
     * classpath.
     */
    public JUnitTestRunner(JUnitTest test, boolean haltOnError,
                           boolean filtertrace, boolean haltOnFailure,
                           boolean showOutput) {
        this(test, haltOnError, filtertrace, haltOnFailure, showOutput, null);
    }

    /**
     * Constructor to use when the user has specified a classpath.
     */
    public JUnitTestRunner(JUnitTest test, boolean haltOnError,
                           boolean filtertrace, boolean haltOnFailure,
                           ClassLoader loader) {
        this(test, haltOnError, filtertrace, haltOnFailure, false, loader);
    }

    /**
     * Constructor to use when the user has specified a classpath.
     */
    public JUnitTestRunner(JUnitTest test, boolean haltOnError,
                           boolean filtertrace, boolean haltOnFailure,
                           boolean showOutput, ClassLoader loader) {
        JUnitTestRunner.filtertrace = filtertrace;
        this.junitTest = test;
        this.haltOnError = haltOnError;
        this.haltOnFailure = haltOnFailure;
        this.showOutput = showOutput;

        try {
            Class testClass = null;
            if (loader == null) {
                testClass = Class.forName(test.getName());
            } else {
                testClass = Class.forName(test.getName(), true, loader);
            }

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
                // try to extract a test suite automatically
                // this will generate warnings if the class is no suitable Test
                suite = new TestSuite(testClass);
            }

        } catch (Exception e) {
            retCode = ERRORS;
            exception = e;
        }
    }

    public void run() {
        res = new TestResult();
        res.addListener(this);
        for (int i = 0; i < formatters.size(); i++) {
            res.addListener((TestListener) formatters.elementAt(i));
        }

        long start = System.currentTimeMillis();

        fireStartTestSuite();
        if (exception != null) { // had an exception in the constructor
            for (int i = 0; i < formatters.size(); i++) {
                ((TestListener) formatters.elementAt(i)).addError(null,
                                                                  exception);
            }
            junitTest.setCounts(1, 0, 1);
            junitTest.setRunTime(0);
        } else {


            ByteArrayOutputStream errStrm = new ByteArrayOutputStream();
            systemError = new PrintStream(errStrm);

            ByteArrayOutputStream outStrm = new ByteArrayOutputStream();
            systemOut = new PrintStream(outStrm);

            PrintStream savedOut = null;
            PrintStream savedErr = null;

            if (forked) {
                savedOut = System.out;
                savedErr = System.err;
                if (!showOutput) {
                    System.setOut(systemOut);
                    System.setErr(systemError);
                } else {
                    System.setOut(new PrintStream(
                                      new TeeOutputStream(savedOut, systemOut)
                                      )
                                  );
                    System.setErr(new PrintStream(
                                      new TeeOutputStream(savedErr,
                                                          systemError)
                                      )
                                  );
                }
                perm = null;
            } else {
                if (perm != null) {
                    perm.setSecurityManager();
                }
            }


            try {
                suite.run(res);
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
                sendOutAndErr(new String(outStrm.toByteArray()),
                              new String(errStrm.toByteArray()));

                junitTest.setCounts(res.runCount(), res.failureCount(),
                                    res.errorCount());
                junitTest.setRunTime(System.currentTimeMillis() - start);
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
     */
    public void startTest(Test t) {
    }

    /**
     * Interface TestListener.
     *
     * <p>A Test is finished.
     */
    public void endTest(Test test) {
    }

    /**
     * Interface TestListener for JUnit &lt;= 3.4.
     *
     * <p>A Test failed.
     */
    public void addFailure(Test test, Throwable t) {
        if (haltOnFailure) {
            res.stop();
        }
    }

    /**
     * Interface TestListener for JUnit &gt; 3.4.
     *
     * <p>A Test failed.
     */
    public void addFailure(Test test, AssertionFailedError t) {
        addFailure(test, (Throwable) t);
    }

    /**
     * Interface TestListener.
     *
     * <p>An error occurred while running the test.
     */
    public void addError(Test test, Throwable t) {
        if (haltOnError) {
            res.stop();
        }
    }

    /**
     * Permissions for the test run.
     * @since Ant 1.6
     * @param permissions
     */
    public void setPermissions(Permissions permissions) {
        perm = permissions;
    }

    protected void handleOutput(String output) {
        if (systemOut != null) {
            systemOut.print(output);
        }
    }

    /**
     * @see org.apache.tools.ant.Task#handleInput(byte[], int, int)
     *
     * @since Ant 1.6
     */
    protected int handleInput(byte[] buffer, int offset, int length)
        throws IOException {
        return -1;
    }

    protected void handleErrorOutput(String output) {
        if (systemError != null) {
            systemError.print(output);
        }
    }

    protected void handleFlush(String output) {
        if (systemOut != null) {
            systemOut.print(output);
        }
    }

    protected void handleErrorFlush(String output) {
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

    public void addFormatter(JUnitResultFormatter f) {
        formatters.addElement(f);
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
     * </table>
     */
    public static void main(String[] args) throws IOException {
        boolean haltError = false;
        boolean haltFail = false;
        boolean stackfilter = true;
        Properties props = new Properties();
        boolean showOut = false;

        if (args.length == 0) {
            System.err.println("required argument TestClassName missing");
            System.exit(ERRORS);
        }

        if (args[0].startsWith("testsfile=")) {
            multipleTests = true;
            args[0] = args[0].substring(10 /* "testsfile=".length() */);
        }

        for (int i = 1; i < args.length; i++) {
            if (args[i].startsWith("haltOnError=")) {
                haltError = Project.toBoolean(args[i].substring(12));
            } else if (args[i].startsWith("haltOnFailure=")) {
                haltFail = Project.toBoolean(args[i].substring(14));
            } else if (args[i].startsWith("filtertrace=")) {
                stackfilter = Project.toBoolean(args[i].substring(12));
            } else if (args[i].startsWith("formatter=")) {
                try {
                    createAndStoreFormatter(args[i].substring(10));
                } catch (BuildException be) {
                    System.err.println(be.getMessage());
                    System.exit(ERRORS);
                }
            } else if (args[i].startsWith("propsfile=")) {
                FileInputStream in = new FileInputStream(args[i]
                                                         .substring(10));
                props.load(in);
                in.close();
            } else if (args[i].startsWith("showoutput=")) {
                showOut = Project.toBoolean(args[i].substring(11));
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
                boolean errorOccured = false;
                boolean failureOccured = false;
                String line = null;
                while ((line = reader.readLine()) != null) {
                    StringTokenizer st = new StringTokenizer(line, ",");
                    testCaseName = st.nextToken();
                    JUnitTest t = new JUnitTest(testCaseName);
                    t.setTodir(new File(st.nextToken()));
                    t.setOutfile(st.nextToken());
                    code = launch(t, haltError, stackfilter, haltFail, 
                                  showOut, props);
                    errorOccured = (code == ERRORS);
                    failureOccured = (code != SUCCESS);
                    if (errorOccured || failureOccured ) {
                        if ((errorOccured && haltError) 
                            || (failureOccured && haltFail)) {
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
            } catch(IOException e) {
                e.printStackTrace();
            }
        } else {
            returnCode = launch(new JUnitTest(args[0]), haltError,
                                stackfilter, haltFail, showOut, props);
        }

        System.exit(returnCode);
    }

    private static Vector fromCmdLine = new Vector();

    private static void transferFormatters(JUnitTestRunner runner,
                                           JUnitTest test) {
        for (int i = 0; i < fromCmdLine.size(); i++) {
            FormatterElement fe = (FormatterElement) fromCmdLine.elementAt(i);
            if (multipleTests && fe.getUseFile()) {
                File destFile = 
                    new File(test.getTodir(), 
                             test.getOutfile() + fe.getExtension());
                fe.setOutfile(destFile);
            }
            runner.addFormatter(fe.createFormatter());
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
            }
        }
        fromCmdLine.addElement(fe);
    }

    /**
     * Returns a filtered stack trace.
     * This is ripped out of junit.runner.BaseTestRunner.
     */
    public static String getFilteredTrace(Throwable t) {
        String trace = StringUtils.getStackTrace(t);
        return JUnitTestRunner.filterStack(trace);
    }

    /**
     * Filters stack frames from internal JUnit and Ant classes
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
        } catch (Exception IOException) {
            return stack; // return the stack unfiltered
        }
        return sw.toString();
    }

    private static boolean filterLine(String line) {
        for (int i = 0; i < DEFAULT_TRACE_FILTERS.length; i++) {
            if (line.indexOf(DEFAULT_TRACE_FILTERS[i]) > 0) {
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
                              boolean showOut, Properties props) {
        t.setProperties(props);
        JUnitTestRunner runner = 
            new JUnitTestRunner(t, haltError, stackfilter, haltFail, showOut);
        runner.forked = true;
        transferFormatters(runner, t);

        runner.run();
        return runner.getRetCode();
     }
} // JUnitTestRunner
