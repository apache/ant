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

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;
import org.apache.tools.ant.Project;

/**
 * <p> Run a single JUnit test.
 *
 * <p> The JUnit test is actually run by {@link JUnitTestRunner}.
 * So read the doc comments for that class :)
 *
 * @since Ant 1.2
 *
 * @see JUnitTask
 * @see JUnitTestRunner
 */
public class JUnitTest extends BaseTest implements Cloneable {

    /** the name of the test case */
    private String name = null;

    /** the name of the result file */
    private String outfile = null;

    // @todo this is duplicating TestResult information. Only the time is not
    // part of the result. So we'd better derive a new class from TestResult
    // and deal with it. (SB)
    private long runs, failures, errors;
    private long runTime;

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
        this.name  = name;
        this.haltOnError = haltOnError;
        this.haltOnFail = haltOnFailure;
        this.filtertrace = filtertrace;
    }

    /**
     * Set the name of the test class.
     * @param value the name to use.
     */
    public void setName(String value) {
        name = value;
    }

    /**
     * Set the name of the output file.
     * @param value the name of the output file to use.
     */
    public void setOutfile(String value) {
        outfile = value;
    }

    /**
     * Get the name of the test class.
     * @return the name of the test.
     */
    public String getName() {
        return name;
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
     * Set the number of runs, failures and errors.
     * @param runs     the number of runs.
     * @param failures the number of failures.
     * @param errors   the number of errors.
     */
    public void setCounts(long runs, long failures, long errors) {
        this.runs = runs;
        this.failures = failures;
        this.errors = errors;
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
    public void setProperties(Hashtable p) {
        props = new Properties();
        for (Enumeration e = p.keys(); e.hasMoreElements();) {
            Object key = e.nextElement();
            props.put(key, p.get(key));
        }
    }

    /**
     * Check if this test should run based on the if and unless
     * attributes.
     * @param p the project to use to check if the if and unless
     *          properties exist in.
     * @return true if this test or testsuite should be run.
     */
    public boolean shouldRun(Project p) {
        if (ifProperty != null && p.getProperty(ifProperty) == null) {
            return false;
        } else if (unlessProperty != null
                    && p.getProperty(unlessProperty) != null) {
            return false;
        }

        return true;
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
    void addFormattersTo(Vector v) {
        final int count = formatters.size();
        for (int i = 0; i < count; i++) {
            v.addElement(formatters.elementAt(i));
        }
    }

    /**
     * @since Ant 1.5
     * @return a clone of this test.
     */
    public Object clone() {
        try {
            JUnitTest t = (JUnitTest) super.clone();
            t.props = props == null ? null : (Properties) props.clone();
            t.formatters = (Vector) formatters.clone();
            return t;
        } catch (CloneNotSupportedException e) {
            // plain impossible
            return this;
        }
    }
}
