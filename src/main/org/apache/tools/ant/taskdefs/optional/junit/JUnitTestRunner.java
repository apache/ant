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
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;

import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestFailure;
import junit.framework.TestListener;
import junit.framework.TestResult;
import junit.framework.TestSuite;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Permissions;
import org.apache.tools.ant.util.FileUtils;
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
    private final Vector<JUnitTaskMirror.JUnitResultFormatterMirror> formatters = new Vector();

    /**
     * Collects TestResults.
     */
    private IgnoredTestResult res;

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

    private static final String JUNIT_4_TEST_ADAPTER
        = "junit.framework.JUnit4TestAdapter";

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
    private final JUnitTest junitTest;

    /** output written during the test */
    private PrintStream systemError;

    /** Error output during the test */
    private PrintStream systemOut;

    /** is this runner running in forked mode? */
    private boolean forked = false;

    /** Running more than one test suite? */
    private static boolean multipleTests = false;

    /** ClassLoader passed in in non-forked mode. */
    private final ClassLoader loader;

    /** Do we print TestListener events? */
    private boolean logTestListenerEvents = false;

    /** Turned on if we are using JUnit 4 for this test suite. see #38811 */
    private boolean junit4;

    /**
     * The file used to indicate that the build crashed.
     * File will be empty in case the build did not crash.
     */
    private static String crashFile = null;

    /** Names of test methods to execute */
    private String[] methods = null;

    /**
     * Constructor for fork=true or when the user hasn't specified a
     * classpath.
     * @param test the test to run.
     * @param haltOnError whether to stop the run if an error is found.
     * @param filtertrace whether to filter junit.*.* stack frames out of exceptions
     * @param haltOnFailure whether to stop the run if failure is found.
     */
    public JUnitTestRunner(final JUnitTest test, final boolean haltOnError,
                           final boolean filtertrace, final boolean haltOnFailure) {
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
    public JUnitTestRunner(final JUnitTest test, final boolean haltOnError,
                           final boolean filtertrace, final boolean haltOnFailure,
                           final boolean showOutput) {
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
    public JUnitTestRunner(final JUnitTest test, final boolean haltOnError,
                           final boolean filtertrace, final boolean haltOnFailure,
                           final boolean showOutput, final boolean logTestListenerEvents) {
        this(test, null, haltOnError, filtertrace, haltOnFailure, showOutput,
             logTestListenerEvents, null);
    }

    /**
     * Constructor for fork=true or when the user hasn't specified a
     * classpath.
     * @param test the test to run.
     * @param methods names of methods of the test to be executed.
     * @param haltOnError whether to stop the run if an error is found.
     * @param filtertrace whether to filter junit.*.* stack frames out of exceptions
     * @param haltOnFailure whether to stop the run if failure is found.
     * @param showOutput    whether to send output to System.out/.err as well as formatters.
     * @param logTestListenerEvents whether to print TestListener events.
     * @since 1.8.2
     */
    public JUnitTestRunner(final JUnitTest test, final String[] methods, final boolean haltOnError,
                           final boolean filtertrace, final boolean haltOnFailure,
                           final boolean showOutput, final boolean logTestListenerEvents) {
        this(test, methods, haltOnError, filtertrace, haltOnFailure, showOutput,
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
    public JUnitTestRunner(final JUnitTest test, final boolean haltOnError,
                           final boolean filtertrace, final boolean haltOnFailure,
                           final ClassLoader loader) {
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
    public JUnitTestRunner(final JUnitTest test, final boolean haltOnError,
                           final boolean filtertrace, final boolean haltOnFailure,
                           final boolean showOutput, final ClassLoader loader) {
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
    public JUnitTestRunner(final JUnitTest test, final boolean haltOnError,
                           final boolean filtertrace, final boolean haltOnFailure,
                           final boolean showOutput, final boolean logTestListenerEvents,
                           final ClassLoader loader) {
        this(test, null, haltOnError, filtertrace, haltOnFailure, showOutput,
             logTestListenerEvents, loader);
    }


    /**
     * Constructor to use when the user has specified a classpath.
     * @since 1.8.2
     */
    public JUnitTestRunner(final JUnitTest test, final String[] methods, final boolean haltOnError,
                           final boolean filtertrace, final boolean haltOnFailure,
                           final boolean showOutput, final boolean logTestListenerEvents,
                           final ClassLoader loader) {
        super();
        JUnitTestRunner.filtertrace = filtertrace; // TODO clumsy, should use instance field somehow
        this.junitTest = test;
        this.haltOnError = haltOnError;
        this.haltOnFailure = haltOnFailure;
        this.showOutput = showOutput;
        this.logTestListenerEvents = logTestListenerEvents;
        this.methods = methods != null ? (String[]) methods.clone() : null;
        this.loader = loader;
    }

    private PrintStream savedOut = null;
    private PrintStream savedErr = null;

    private PrintStream createEmptyStream() {
        return new PrintStream(
            new OutputStream() {
                @Override
                public void write(final int b) {
                }
            });
    }

    private PrintStream createTeePrint(final PrintStream ps1, final PrintStream ps2) {
        return new PrintStream(new TeeOutputStream(ps1, ps2));
    }

    private void setupIOStreams(final ByteArrayOutputStream o,
                                final ByteArrayOutputStream e) {
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
        res = new IgnoredTestResult();
        res.addListener(wrapListener(this));
        final int size = formatters.size();
        for (int i = 0; i < size; i++) {
            res.addListener(wrapListener((TestListener) formatters.elementAt(i)));
        }

        final ByteArrayOutputStream errStrm = new ByteArrayOutputStream();
        final ByteArrayOutputStream outStrm = new ByteArrayOutputStream();

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

                final boolean testMethodsSpecified = (methods != null);

                // check for a static suite method first, even when using
                // JUnit 4
                Method suiteMethod = null;
                if (!testMethodsSpecified) {
                try {
                    // check if there is a suite method
                    suiteMethod = testClass.getMethod("suite", new Class[0]);
                } catch (final NoSuchMethodException e) {
                    // no appropriate suite method found. We don't report any
                    // error here since it might be perfectly normal.
                }
                }

                if (suiteMethod != null) {
                    // if there is a suite method available, then try
                    // to extract the suite from it. If there is an error
                    // here it will be caught below and reported.
                    suite = (Test) suiteMethod.invoke(null, new Object[0]);

                } else {
                    Class junit4TestAdapterClass = null;
                    Class junit4TestAdapterCacheClass = null;
                    boolean useSingleMethodAdapter = false;

                    if (junit.framework.TestCase.class.isAssignableFrom(testClass)) {
                        // Do not use JUnit 4 API for running JUnit 3.x
                        // tests - it is not able to run individual test
                        // methods.
                        //
                        // Technical details:
                        // org.junit.runner.Request.method(Class, String).getRunner()
                        // would return a runner which always executes all
                        // test methods. The reason is that the Runner would be
                        // an instance of class
                        // org.junit.internal.runners.OldTestClassRunner
                        // that does not implement interface Filterable - so it
                        // is unable to filter out test methods not matching
                        // the requested name.
                    } else {
                    // Check for JDK 5 first. Will *not* help on JDK 1.4
                    // if only junit-4.0.jar in CP because in that case
                    // linkage of whole task will already have failed! But
                    // will help if CP has junit-3.8.2.jar:junit-4.0.jar.

                    // In that case first C.fN will fail with CNFE and we
                    // will avoid UnsupportedClassVersionError.

                    try {
                        Class.forName("java.lang.annotation.Annotation");
                        junit4TestAdapterCacheClass = Class.forName("org.apache.tools.ant.taskdefs.optional.junit.CustomJUnit4TestAdapterCache");
                        if (loader == null) {
                            junit4TestAdapterClass =
                                Class.forName(JUNIT_4_TEST_ADAPTER);
                            if (testMethodsSpecified) {
                                /*
                                 * We cannot try to load the JUnit4TestAdapter
                                 * before trying to load JUnit4TestMethodAdapter
                                 * because it might fail with
                                 * NoClassDefFoundException, instead of plain
                                 * ClassNotFoundException.
                                 */
                                junit4TestAdapterClass = Class.forName(
                                    "org.apache.tools.ant.taskdefs.optional.junit.JUnit4TestMethodAdapter");
                                useSingleMethodAdapter = true;
                            }
                        } else {
                            junit4TestAdapterClass =
                                Class.forName(JUNIT_4_TEST_ADAPTER,
                                              true, loader);
                            if (testMethodsSpecified) {
                                junit4TestAdapterClass =
                                    Class.forName(
                                        "org.apache.tools.ant.taskdefs.optional.junit.JUnit4TestMethodAdapter",
                                        true, loader);
                                useSingleMethodAdapter = true;
                            }
                        }
                    } catch (final ClassNotFoundException e) {
                        // OK, fall back to JUnit 3.
                    }
                    }
                    junit4 = junit4TestAdapterClass != null;

                    if (junitTest.isSkipNonTests()) {
                       if (!containsTests(testClass, junit4)) {
                           return;
                       }
                    }


                    if (junit4) {
                        // Let's use it!
                        Class[] formalParams;
                        Object[] actualParams;
                        if (useSingleMethodAdapter) {
                            formalParams = new Class[] {Class.class, String[].class};
                            actualParams = new Object[] {testClass, methods};
                        } else {
                            formalParams = new Class[] {Class.class, Class.forName("junit.framework.JUnit4TestAdapterCache")};
                            actualParams = new Object[] {testClass, junit4TestAdapterCacheClass.getMethod("getInstance").invoke(null)};
                        }
                        suite =
                            (Test) junit4TestAdapterClass
                            .getConstructor(formalParams).
                            newInstance(actualParams);
                    } else {
                        // Use JUnit 3.

                        // try to extract a test suite automatically this
                        // will generate warnings if the class is no
                        // suitable Test
                        if (!testMethodsSpecified) {
                            suite = new TestSuite(testClass);
                        } else if (methods.length == 1) {
                            suite = TestSuite.createTest(testClass, methods[0]);
                        } else {
                            final TestSuite testSuite = new TestSuite(testClass.getName());
                            for (int i = 0; i < methods.length; i++) {
                                testSuite.addTest(
                                    TestSuite.createTest(testClass, methods[i]));
                            }
                            suite = testSuite;
                        }
                    }

                }

            } catch (final Throwable e) {
                retCode = ERRORS;
                exception = e;
            }

            final long start = System.currentTimeMillis();

            fireStartTestSuite();
            startTestSuiteSuccess = true;
            if (exception != null) { // had an exception constructing suite
                final int formatterSize = formatters.size();
                for (int i = 0; i < formatterSize; i++) {
                    ((TestListener) formatters.elementAt(i))
                        .addError(null, exception);
                }
                junitTest.setCounts(1, 0, 1, 0);
                junitTest.setRunTime(0);
            } else {
                try {
                    logTestListenerEvent("tests to run: " + suite.countTestCases());
                    suite.run(res);
                } finally {
                    if (junit4 ||
                        suite.getClass().getName().equals(JUNIT_4_TEST_ADAPTER)) {
                        final int[] cnts = findJUnit4FailureErrorCount(res);
                        junitTest.setCounts(res.runCount() + res.ignoredCount(), cnts[0], cnts[1], res.ignoredCount() + res.skippedCount());
                    } else {
                        junitTest.setCounts(res.runCount() + res.ignoredCount(), res.failureCount(),
                                res.errorCount(), res.ignoredCount() + res.skippedCount());
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
                String out, err;
                try {
                    out = new String(outStrm.toByteArray());
                } catch (final OutOfMemoryError ex) {
                    out = "out of memory on output stream";
                }
                try {
                    err = new String(errStrm.toByteArray());
                } catch (final OutOfMemoryError ex) {
                    err = "out of memory on error stream";
                }
                sendOutAndErr(out, err);
            }
        }
        fireEndTestSuite();

        // junitTest has the correct counts for JUnit4, while res doesn't
        if (retCode != SUCCESS || junitTest.errorCount() != 0) {
            retCode = ERRORS;
        } else if (junitTest.failureCount() != 0) {
            retCode = FAILURES;
        }
    }

    private static boolean containsTests(final Class<?> testClass, final boolean isJUnit4) {
        Class testAnnotation = null;
        Class suiteAnnotation = null;
        Class runWithAnnotation = null;

        try {
            testAnnotation = Class.forName("org.junit.Test");
        } catch (final ClassNotFoundException e) {
            if (isJUnit4) {
                // odd - we think we're JUnit4 but don't support the test annotation. We therefore can't have any tests!
                return false;
            }
            // else... we're a JUnit3 test and don't need the annotation
        }

        try {
            suiteAnnotation = Class.forName("org.junit.Suite.SuiteClasses");
        } catch(final ClassNotFoundException ex) {
            // ignore - we don't have this annotation so make sure we don't check for it
        }
        try {
            runWithAnnotation = Class.forName("org.junit.runner.RunWith");
        } catch(final ClassNotFoundException ex) {
            // also ignore as this annotation doesn't exist so tests can't use it
        }


        if (!isJUnit4 && !TestCase.class.isAssignableFrom(testClass)) {
            //a test we think is JUnit3 but does not extend TestCase. Can't really be a test.
            return false;
        }

        // check if we have any inner classes that contain suitable test methods
        for (final Class<?> innerClass : testClass.getDeclaredClasses()) {
            if (containsTests(innerClass, isJUnit4) || containsTests(innerClass, !isJUnit4)) {
                return true;
            }
        }

        if (Modifier.isAbstract(testClass.getModifiers()) || Modifier.isInterface(testClass.getModifiers())) {
            // can't instantiate class and no inner classes are tests either
            return false;
        }

        if (isJUnit4) {
             if (suiteAnnotation != null && testClass.getAnnotation(suiteAnnotation) != null) {
                // class is marked as a suite. Let JUnit try and work its magic on it.
                return true;
             }
            if (runWithAnnotation != null && testClass.getAnnotation(runWithAnnotation) != null) {
                /* Class is marked with @RunWith. If this class is badly written (no test methods, multiple
                 * constructors, private constructor etc) then the class is automatically run and fails in the
                 * IDEs I've tried... so I'm happy handing the class to JUnit to try and run, and let JUnit
                 * report a failure if a bad test case is provided. Trying to do anything else is likely to
                 * result in us filtering out cases that could be valid for future versions of JUnit so would
                 * just increase future maintenance work.
                 */
                return true;
            }
        }

        for (final Method m : testClass.getMethods()) {
            if (isJUnit4) {
                // check if suspected JUnit4 classes have methods with @Test annotation
                if (m.getAnnotation(testAnnotation) != null) {
                    return true;
                }
            } else {
                // check if JUnit3 class have public or protected no-args methods starting with names starting with test
                if (m.getName().startsWith("test") && m.getParameterTypes().length == 0
                        && (Modifier.isProtected(m.getModifiers()) || Modifier.isPublic(m.getModifiers()))) {
                    return true;
                }
            }
            // check if JUnit3 or JUnit4 test have a public or protected, static,
            // no-args 'suite' method
            if (m.getName().equals("suite") && m.getParameterTypes().length == 0
                    && (Modifier.isProtected(m.getModifiers()) || Modifier.isPublic(m.getModifiers()))
                    && Modifier.isStatic(m.getModifiers())) {
                return true;
            }
        }

        // no test methods found
        return false;
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
    public void startTest(final Test t) {
        final String testName = JUnitVersionHelper.getTestCaseName(t);
        logTestListenerEvent("startTest(" + testName + ")");
    }

    /**
     * Interface TestListener.
     *
     * <p>A Test is finished.
     * @param test the test.
     */
    public void endTest(final Test test) {
        final String testName = JUnitVersionHelper.getTestCaseName(test);
        logTestListenerEvent("endTest(" + testName + ")");
    }

    private void logTestListenerEvent(String msg) {
        if (logTestListenerEvents) {
            final PrintStream out = savedOut != null ? savedOut : System.out;
            out.flush();
            if (msg == null) {
                msg = "null";
            }
            final StringTokenizer msgLines = new StringTokenizer(msg, "\r\n", false);
            while (msgLines.hasMoreTokens()) {
                out.println(JUnitTask.TESTLISTENER_PREFIX
                            + msgLines.nextToken());
            }
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
    public void addFailure(final Test test, final Throwable t) {
        final String testName = JUnitVersionHelper.getTestCaseName(test);
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
    public void addFailure(final Test test, final AssertionFailedError t) {
        addFailure(test, (Throwable) t);
    }

    /**
     * Interface TestListener.
     *
     * <p>An error occurred while running the test.
     * @param test the test.
     * @param t    the error thrown by the test.
     */
    public void addError(final Test test, final Throwable t) {
        final String testName = JUnitVersionHelper.getTestCaseName(test);
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
    public void setPermissions(final Permissions permissions) {
        perm = permissions;
    }

    /**
     * Handle a string destined for standard output.
     * @param output the string to output
     */
    public void handleOutput(final String output) {
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
    public int handleInput(final byte[] buffer, final int offset, final int length)
        throws IOException {
        return -1;
    }

    /** {@inheritDoc}. */
    public void handleErrorOutput(final String output) {
        if (systemError != null) {
            systemError.print(output);
        }
    }

    /** {@inheritDoc}. */
    public void handleFlush(final String output) {
        if (systemOut != null) {
            systemOut.print(output);
        }
    }

    /** {@inheritDoc}. */
    public void handleErrorFlush(final String output) {
        if (systemError != null) {
            systemError.print(output);
        }
    }

    private void sendOutAndErr(final String out, final String err) {
        final int size = formatters.size();
        for (int i = 0; i < size; i++) {
            final JUnitResultFormatter formatter =
                ((JUnitResultFormatter) formatters.elementAt(i));

            formatter.setSystemOutput(out);
            formatter.setSystemError(err);
        }
    }

    private void fireStartTestSuite() {
        final int size = formatters.size();
        for (int i = 0; i < size; i++) {
            ((JUnitResultFormatter) formatters.elementAt(i))
                .startTestSuite(junitTest);
        }
    }

    private void fireEndTestSuite() {
        final int size = formatters.size();
        for (int i = 0; i < size; i++) {
            ((JUnitResultFormatter) formatters.elementAt(i))
                .endTestSuite(junitTest);
        }
    }

    /**
     * Add a formatter.
     * @param f the formatter to add.
     */
    public void addFormatter(final JUnitResultFormatter f) {
        formatters.addElement(f);
    }

    /** {@inheritDoc}. */
    public void addFormatter(final JUnitTaskMirror.JUnitResultFormatterMirror f) {
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
     * classname,filename. If filename is omitted, System.out is
     * assumed.</td><td>none</td></tr>
     *
     * <tr><td>showoutput</td><td>send output to System.err/.out as
     * well as to the formatters?</td><td>false</td></tr>
     *
     * <tr><td>logtestlistenerevents</td><td>log TestListener events to
     * System.out.</td><td>false</td></tr>
     *
     * <tr><td>methods</td><td>Comma-separated list of names of individual
     * test methods to execute.
     * </td><td>null</td></tr>
     *
     * </table>
     * @param args the command line arguments.
     * @throws IOException on error.
     */
    public static void main(final String[] args) throws IOException {
        String[] methods = null;
        boolean haltError = false;
        boolean haltFail = false;
        boolean stackfilter = true;
        final Properties props = new Properties();
        boolean showOut = false;
        boolean outputToFormat = true;
        boolean logFailedTests = true;
        boolean logTestListenerEvents = false;
        boolean skipNonTests = false;
        int antThreadID = 0; /* Ant id of thread running this unit test, 0 in single-threaded mode */

        if (args.length == 0) {
            System.err.println("required argument TestClassName missing");
            System.exit(ERRORS);
        }

        if (args[0].startsWith(Constants.TESTSFILE)) {
            multipleTests = true;
            args[0] = args[0].substring(Constants.TESTSFILE.length());
        }

        for (int i = 1; i < args.length; i++) {
            if (args[i].startsWith(Constants.METHOD_NAMES)) {
                try {
                    final String methodsList = args[i].substring(Constants.METHOD_NAMES.length());
                    methods = JUnitTest.parseTestMethodNamesList(methodsList);
                } catch (final IllegalArgumentException ex) {
                    System.err.println("Invalid specification of test method names: " + args[i]);
                    System.exit(ERRORS);
                }
            } else if (args[i].startsWith(Constants.HALT_ON_ERROR)) {
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
                } catch (final BuildException be) {
                    System.err.println(be.getMessage());
                    System.exit(ERRORS);
                }
            } else if (args[i].startsWith(Constants.PROPSFILE)) {
                final FileInputStream in = new FileInputStream(args[i]
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
            } else if (args[i].startsWith(Constants.LOG_FAILED_TESTS)) {
                logFailedTests = Project.toBoolean(
                    args[i].substring(Constants.LOG_FAILED_TESTS.length()));
            } else if (args[i].startsWith(Constants.SKIP_NON_TESTS)) {
                skipNonTests = Project.toBoolean(
                    args[i].substring(Constants.SKIP_NON_TESTS.length()));
            } else if (args[i].startsWith(Constants.THREADID)) {
                antThreadID = Integer.parseInt(args[i].substring(Constants.THREADID.length()));
            }
        }

        // Add/overlay system properties on the properties from the Ant project
        final Hashtable p = System.getProperties();
        for (final Enumeration e = p.keys(); e.hasMoreElements();) {
            final Object key = e.nextElement();
            props.put(key, p.get(key));
        }

        int returnCode = SUCCESS;
        if (multipleTests) {
            try {
                final java.io.BufferedReader reader =
                    new java.io.BufferedReader(new java.io.FileReader(args[0]));
                String testCaseName;
                String[] testMethodNames;
                int code = 0;
                boolean errorOccurred = false;
                boolean failureOccurred = false;
                String line = null;
                while ((line = reader.readLine()) != null) {
                    final StringTokenizer st = new StringTokenizer(line, ",");
                    final String testListSpec = st.nextToken();
                    final int colonIndex = testListSpec.indexOf(':');
                    if (colonIndex == -1) {
                        testCaseName = testListSpec;
                        testMethodNames = null;
                    } else {
                        testCaseName = testListSpec.substring(0, colonIndex);
                        testMethodNames = JUnitTest.parseTestMethodNamesList(
                                                    testListSpec
                                                    .substring(colonIndex + 1)
                                                    .replace('+', ','));
                    }
                    final JUnitTest t = new JUnitTest(testCaseName);
                    t.setTodir(new File(st.nextToken()));
                    t.setOutfile(st.nextToken());
                    t.setProperties(props);
                    t.setSkipNonTests(skipNonTests);
                    t.setThread(antThreadID);
                    code = launch(t, testMethodNames, haltError, stackfilter, haltFail,
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
                            if (logFailedTests) {
                                System.out.println("TEST " + t.getName()
                                                   + " FAILED");
                            }
                        }
                    }
                }
            } catch (final IOException e) {
                e.printStackTrace();
            }
        } else {
            final JUnitTest t = new JUnitTest(args[0]);
            t.setThread(antThreadID);
            t.setProperties(props);
            t.setSkipNonTests(skipNonTests);
            returnCode = launch(
                t, methods, haltError, stackfilter, haltFail,
                showOut, outputToFormat, logTestListenerEvents);
        }

        registerNonCrash();
        System.exit(returnCode);
    }

    private static Vector fromCmdLine = new Vector();

    private static void transferFormatters(final JUnitTestRunner runner,
                                           final JUnitTest test) {
        runner.addFormatter(new JUnitResultFormatter() {

            public void startTestSuite(final JUnitTest suite) throws BuildException {
            }

            public void endTestSuite(final JUnitTest suite) throws BuildException {
            }

            public void setOutput(final OutputStream out) {
            }

            public void setSystemOutput(final String out) {
            }

            public void setSystemError(final String err) {
            }

            public void addError(final Test arg0, final Throwable arg1) {
            }

            public void addFailure(final Test arg0, final AssertionFailedError arg1) {
            }

            public void endTest(final Test arg0) {
            }

            public void startTest(final Test arg0) {
                registerTestCase(JUnitVersionHelper.getTestCaseName(arg0));
            }
        });
        final int size = fromCmdLine.size();
        for (int i = 0; i < size; i++) {
            final FormatterElement fe = (FormatterElement) fromCmdLine.elementAt(i);
            if (multipleTests && fe.getUseFile()) {
                final File destFile =
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
    private static void createAndStoreFormatter(final String line)
        throws BuildException {
        final FormatterElement fe = new FormatterElement();
        final int pos = line.indexOf(',');
        if (pos == -1) {
            fe.setClassname(line);
            fe.setUseFile(false);
        } else {
            fe.setClassname(line.substring(0, pos));
            fe.setUseFile(true);
            if (!multipleTests) {
                fe.setOutfile(new File(line.substring(pos + 1)));
            } else {
                final int fName = line.indexOf(IGNORED_FILE_NAME);
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
    public static String getFilteredTrace(final Throwable t) {
        final String trace = StringUtils.getStackTrace(t);
        return JUnitTestRunner.filterStack(trace);
    }

    /**
     * Filters stack frames from internal JUnit and Ant classes
     * @param stack the stack trace to filter.
     * @return the filtered stack.
     */
    public static String filterStack(final String stack) {
        if (!filtertrace) {
            return stack;
        }
        final StringWriter sw = new StringWriter();
        final BufferedWriter pw = new BufferedWriter(sw);
        final StringReader sr = new StringReader(stack);
        final BufferedReader br = new BufferedReader(sr);

        String line;
        try {
            boolean firstLine = true;
            while ((line = br.readLine()) != null) {
                if (firstLine || !filterLine(line)) {
                    pw.write(line);
                    pw.newLine();
                }
                firstLine = false;
            }
        } catch (final Exception e) {
            return stack; // return the stack unfiltered
        } finally {
            FileUtils.close(pw);
        }
        return sw.toString();
    }

    private static boolean filterLine(final String line) {
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
    private static int launch(final JUnitTest t, final String[] methods, final boolean haltError,
                              final boolean stackfilter, final boolean haltFail,
                              final boolean showOut, final boolean outputToFormat,
                              final boolean logTestListenerEvents) {
        final JUnitTestRunner runner =
            new JUnitTestRunner(t, methods, haltError, stackfilter, haltFail, showOut,
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
                FileUtils.close(out);
            }
        }
    }

    private static void registerTestCase(final String testCase) {
        if (crashFile != null) {
            try {
                FileWriter out = null;
                try {
                    out = new FileWriter(crashFile);
                    out.write(testCase + "\n");
                    out.flush();
                } finally {
                    FileUtils.close(out);
                }
            } catch (final IOException e) {
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
    private TestListenerWrapper wrapListener(final TestListener testListener) {
        return new TestListenerWrapper(testListener) {
            @Override
            public void addError(final Test test, final Throwable t) {
                if (junit4 && t instanceof AssertionFailedError) {
                    // JUnit 4 does not distinguish between errors and failures
                    // even in the JUnit 3 adapter.
                    // So we need to help it a bit to retain compatibility for JUnit 3 tests.
                    testListener.addFailure(test, (AssertionFailedError) t);
                } else if (junit4 && t instanceof  AssertionError) {
                    // Not strictly necessary but probably desirable.
                    // JUnit 4-specific test GUIs will show just "failures".
                    // But Ant's output shows "failures" vs. "errors".
                    // We would prefer to show "failure" for things that logically are.
                    final String msg = t.getMessage();
                    final AssertionFailedError failure = msg != null
                        ? new AssertionFailedError(msg) : new AssertionFailedError();
                    failure.setStackTrace(t.getStackTrace());
                    testListener.addFailure(test, failure);
                } else {
                    testListener.addError(test, t);
                }
            }
            @Override
            public void addFailure(final Test test, final AssertionFailedError t) {
                testListener.addFailure(test, t);
            }
            public void addFailure(final Test test, final Throwable t) { // pre-3.4
                if (t instanceof AssertionFailedError) {
                    testListener.addFailure(test, (AssertionFailedError) t);
                } else {
                    testListener.addError(test, t);
                }
            }
            @Override
            public void endTest(final Test test) {
                testListener.endTest(test);
            }
            @Override
            public void startTest(final Test test) {
                testListener.startTest(test);
            }
        };
    }

    /**
     * Use instead of TestResult.get{Failure,Error}Count on JUnit 4,
     * since the adapter claims that all failures are errors.
     * @since Ant 1.7
     */
    private int[] findJUnit4FailureErrorCount(final TestResult result) {
        int failures = 0;
        int errors = 0;
        Enumeration e = result.failures();
        while (e.hasMoreElements()) {
            e.nextElement();
            failures++;
        }
        e = result.errors();
        while (e.hasMoreElements()) {
            final Throwable t = ((TestFailure) e.nextElement()).thrownException();
            if (t instanceof AssertionFailedError
                || t instanceof AssertionError) {
                failures++;
            } else {
                errors++;
            }
        }
        return new int[] {failures, errors};
    }

} // JUnitTestRunner
