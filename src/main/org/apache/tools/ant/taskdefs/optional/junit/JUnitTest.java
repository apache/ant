/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.tools.ant.taskdefs.optional.junit;

import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.PropertyHelper;

/**
 * Run a single JUnit test.
 *
 * <p>The JUnit test is actually run by {@link JUnitTestRunner}.
 * So read the doc comments for that class :)</p>
 *
 * @since Ant 1.2
 *
 * @see JUnitTask
 * @see JUnitTestRunner
 */
public class JUnitTest extends BaseTest implements Cloneable {

    /** the name of the test case */
    private String name = null;

    /**
     * whether the list of test methods has been specified
     * @see #setMethods(java.lang.String)
     * @see #setMethods(java.lang.String[])
     */
    private boolean methodsSpecified = false;

    /** comma-separated list of names of test methods to execute */
    private String methodsList = null;

    /** the names of test methods to execute */
    private String[] methods = null;

    /** the name of the result file */
    private String outfile = null;

    // @todo this is duplicating TestResult information. Only the time is not
    // part of the result. So we'd better derive a new class from TestResult
    // and deal with it. (SB)
    private long runs, failures, errors;
    /**
    @since Ant 1.9.0
    */
    private long skips;

    private long runTime;

    private int antThreadID;

    // Snapshot of the system properties
    private Properties props = null;

    /** No arg constructor. */
    public JUnitTest() {
    }

    /**
     * Constructor with name.
     * @param name the name of the test.
     */
    public JUnitTest(String name) {
        this.name  = name;
    }

    /**
     * Constructor with options.
     * @param name the name of the test.
     * @param haltOnError if true halt the tests if there is an error.
     * @param haltOnFailure if true halt the tests if there is a failure.
     * @param filtertrace if true filter stack traces.
     */
    public JUnitTest(String name, boolean haltOnError, boolean haltOnFailure,
            boolean filtertrace) {
        this(name, haltOnError, haltOnFailure, filtertrace, null, 0);
    }

    /**
     * Constructor with options.
     * @param name the name of the test.
     * @param haltOnError if true halt the tests if there is an error.
     * @param haltOnFailure if true halt the tests if there is a failure.
     * @param filtertrace if true filter stack traces.
     * @param methods if non-null run only these test methods
     * @since 1.8.2
     */
    public JUnitTest(String name, boolean haltOnError, boolean haltOnFailure,
                     boolean filtertrace, String[] methods) {
        this(name, haltOnError, haltOnFailure, filtertrace, methods, 0);
    }

    /**
     * Constructor with options.
     * @param name the name of the test.
     * @param haltOnError if true halt the tests if there is an error.
     * @param haltOnFailure if true halt the tests if there is a failure.
     * @param filtertrace if true filter stack traces.
     * @param methods if non-null run only these test methods
     * @param thread Ant thread ID in which test is currently running
     * @since 1.9.4
     */
    public JUnitTest(String name, boolean haltOnError, boolean haltOnFailure,
                     boolean filtertrace, String[] methods, int thread) {
        this.name  = name;
        this.haltOnError = haltOnError;
        this.haltOnFail = haltOnFailure;
        this.filtertrace = filtertrace;
        this.methodsSpecified = methods != null;
        this.methods = methodsSpecified ? methods.clone() : null;
        this.antThreadID = thread;
    }

    /**
     * Sets names of individual test methods to be executed.
     * @param value comma-separated list of names of individual test methods
     *              to be executed,
     *              or <code>null</code> if all test methods should be executed
     * @since 1.8.2
     */
    public void setMethods(String value) {
        methodsList = value;
        methodsSpecified = (value != null);
        methods = null;
    }

    /**
     * Sets names of individual test methods to be executed.
     * @param value non-empty array of names of test methods to be executed
     * @see #setMethods(String)
     * @since 1.8.2
     */
    void setMethods(String[] value) {
        methods = value;
        methodsSpecified = (value != null);
        methodsList = null;
    }

    /**
     * Set the name of the test class.
     * @param value the name to use.
     */
    public void setName(String value) {
        name = value;
    }

    /**
     * Set the thread id
     * @param thread the Ant id of the thread running this test
     * (this is not the system process or thread id)
     * (this will be 0 in single-threaded mode).
     * @since Ant 1.9.4
     */
    public void setThread(int thread) {
        this.antThreadID = thread;
    }

    /**
     * Set the name of the output file.
     * @param value the name of the output file to use.
     */
    public void setOutfile(String value) {
        outfile = value;
    }

    /**
     * Informs whether a list of test methods has been specified in this test.
     * @return <code>true</code> if test methods to be executed have been
     *         specified, <code>false</code> otherwise
     * @see #setMethods(java.lang.String)
     * @see #setMethods(java.lang.String[])
     * @since 1.8.2
     */
    boolean hasMethodsSpecified() {
        return methodsSpecified;
    }

    /**
     * Get names of individual test methods to be executed.
     *
     * @return array of names of the individual test methods to be executed,
     *         or <code>null</code> if all test methods in the suite
     *         defined by the test class will be executed
     * @since 1.8.2
     */
    String[] getMethods() {
        if (methodsSpecified && (methods == null)) {
            resolveMethods();
        }
        return methods;
    }

    /**
     * Gets a comma-separated list of names of methods that are to be executed
     * by this test.
     * @return the comma-separated list of test method names, or an empty
     *         string of no method is to be executed, or <code>null</code>
     *         if no method is specified
     * @since 1.8.2
     */
    String getMethodsString() {
        if (methodsList == null && methodsSpecified) {
            methodsList = String.join(",", methods);
        }
        return methodsList;
    }

    /**
     * Computes the value of the {@link #methods} field from the value
     * of the {@link #methodsList} field, if it has not been computed yet.
     * @exception BuildException if the value of the {@link #methodsList} field
     *                           was invalid
     * @since 1.8.2
     */
    void resolveMethods() {
        if (methods == null && methodsSpecified) {
            try {
                methods = parseTestMethodNamesList(methodsList);
            } catch (IllegalArgumentException ex) {
                throw new BuildException(
                        "Invalid specification of test methods: \""
                            + methodsList
                            + "\"; expected: comma-separated list of valid Java identifiers",
                        ex);
            }
        }
    }

    /**
     * Parses a comma-separated list of method names and check their validity.
     * @param methodNames comma-separated list of method names to be parsed
     * @return array of individual test method names
     * @exception  java.lang.IllegalArgumentException
     *             if the given string is <code>null</code> or if it is not
     *             a comma-separated list of valid Java identifiers;
     *             an empty string is acceptable and is handled as an empty
     *             list
     * @since 1.8.2
     */
    public static String[] parseTestMethodNamesList(String methodNames)
                                            throws IllegalArgumentException {
        if (methodNames == null) {
            throw new IllegalArgumentException("methodNames is <null>");
        }

        methodNames = methodNames.trim();

        int length = methodNames.length();
        if (length == 0) {
            return new String[0];
        }

        /* strip the trailing comma, if any */
        if (methodNames.charAt(length - 1) == ',') {
            methodNames = methodNames.substring(0, length - 1).trim();
            length = methodNames.length();
            if (length == 0) {
                throw new IllegalArgumentException("Empty method name");
            }
        }

        final char[] chars = methodNames.toCharArray();
        /* easy detection of one particular case of illegal string: */
        if (chars[0] == ',') {
            throw new IllegalArgumentException("Empty method name");
        }
        /* count number of method names: */
        int wordCount = 1;
        for (int i = 1; i < chars.length; i++) {
            if (chars[i] == ',') {
                wordCount++;
            }
        }
        /* prepare the resulting array: */
        String[] result = new String[wordCount];
        /* parse the string: */
        final int stateBeforeWord = 1;
        final int stateInsideWord = 2;
        final int stateAfterWord = 3;
        //
        int state = stateBeforeWord;
        int wordStartIndex = -1;
        int wordIndex = 0;
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            switch (state) {
                case stateBeforeWord:
                    if (c == ',') {
                        throw new IllegalArgumentException("Empty method name");
                    } else if (c == ' ') {
                        // remain in the same state
                    } else if (Character.isJavaIdentifierStart(c)) {
                        wordStartIndex = i;
                        state = stateInsideWord;
                    } else {
                        throw new IllegalArgumentException("Illegal start of method name: " + c);
                    }
                    break;
                case stateInsideWord:
                    if (c == ',') {
                        result[wordIndex++] = methodNames.substring(wordStartIndex, i);
                        state = stateBeforeWord;
                    } else if (c == ' ') {
                        result[wordIndex++] = methodNames.substring(wordStartIndex, i);
                        state = stateAfterWord;
                    } else if (Character.isJavaIdentifierPart(c)) {
                        // remain in the same state
                    } else {
                        throw new IllegalArgumentException("Illegal character in method name: " + c);
                    }
                    break;
                case stateAfterWord:
                    if (c == ',') {
                        state = stateBeforeWord;
                    } else if (c == ' ') {
                        // remain in the same state
                    } else {
                        throw new IllegalArgumentException("Space in method name");
                    }
                    break;
                default:
                    // this should never happen
            }
        }
        switch (state) {
            case stateBeforeWord:
            case stateAfterWord:
                break;
            case stateInsideWord:
                result[wordIndex++] = methodNames.substring(wordStartIndex, chars.length);
                break;
            default:
                // this should never happen
        }
        return result;
    }

    /**
     * Get the name of the test class.
     * @return the name of the test.
     */
    public String getName() {
        return name;
    }

    /**
     * Get the Ant id of the thread running the test.
     * @return the thread id
     */
    public int getThread() {
        return antThreadID;
    }

    /**
     * Get the name of the output file
     *
     * @return the name of the output file.
     */
    public String getOutfile() {
        return outfile;
    }

    /**
     * Set the number of runs, failures, errors, and skipped tests.
     * @param runs     the number of runs.
     * @param failures the number of failures.
     * @param errors   the number of errors.
     * Kept for backward compatibility with Ant 1.8.4
     */
    public void setCounts(long runs, long failures, long errors) {
        this.runs = runs;
        this.failures = failures;
        this.errors = errors;
    }
    /**
     * Set the number of runs, failures, errors, and skipped tests.
     * @param runs     the number of runs.
     * @param failures the number of failures.
     * @param errors   the number of errors.
     * @param skips   the number of skipped tests.
     * @since Ant 1.9.0
     */
    public void setCounts(long runs, long failures, long errors, long skips) {
        this.runs = runs;
        this.failures = failures;
        this.errors = errors;
        this.skips = skips;
    }

    /**
     * Set the runtime.
     * @param runTime the time in milliseconds.
     */
    public void setRunTime(long runTime) {
        this.runTime = runTime;
    }

    /**
     * Get the number of runs.
     * @return the number of runs.
     */
    public long runCount() {
        return runs;
    }

    /**
     * Get the number of failures.
     * @return the number of failures.
     */
    public long failureCount() {
        return failures;
    }

    /**
     * Get the number of errors.
     * @return the number of errors.
     */
    public long errorCount() {
        return errors;
    }

    /**
     * Get the number of skipped tests.
     * @return the number of skipped tests.
     */
    public long skipCount() {
        return skips;
    }

    /**
     * Get the run time.
     * @return the run time in milliseconds.
     */
    public long getRunTime() {
        return runTime;
    }

    /**
     * Get the properties used in the test.
     * @return the properties.
     */
    public Properties getProperties() {
        return props;
    }

    /**
     * Set the properties to be used in the test.
     * @param p the properties.
     *          This is a copy of the projects ant properties.
     */
    public void setProperties(Hashtable<?, ?> p) {
        props = new Properties();
        props.putAll(p);
    }

    /**
     * Check if this test should run based on the if and unless
     * attributes.
     * @param p the project to use to check if the if and unless
     *          properties exist in.
     * @return true if this test or testsuite should be run.
     */
    public boolean shouldRun(Project p) {
        PropertyHelper ph = PropertyHelper.getPropertyHelper(p);
        return ph.testIfCondition(getIfCondition())
            && ph.testUnlessCondition(getUnlessCondition());
    }

    /**
     * Get the formatters set for this test.
     * @return the formatters as an array.
     */
    public FormatterElement[] getFormatters() {
        FormatterElement[] fes = new FormatterElement[formatters.size()];
        formatters.copyInto(fes);
        return fes;
    }

    /**
     * Convenient method to add formatters to a vector
     */
    void addFormattersTo(Vector<? super FormatterElement> v) {
        final int count = formatters.size();
        for (int i = 0; i < count; i++) {
            v.addElement(formatters.elementAt(i));
        }
    }

    /**
     * @since Ant 1.5
     * @return a clone of this test.
     */
    @SuppressWarnings("unchecked")
    @Override
    public Object clone() {
        try {
            JUnitTest t = (JUnitTest) super.clone();
            t.props = props == null ? null : (Properties) props.clone();
            t.formatters = (Vector<FormatterElement>) formatters.clone();
            return t;
        } catch (CloneNotSupportedException e) {
            // plain impossible
            return this;
        }
    }
}
