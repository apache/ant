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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;

import junit.framework.AssertionFailedError;
import junit.framework.Test;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.DataType;
import org.apache.tools.ant.util.FileUtils;

/**
 * <p>Collects all failing test <i>cases</i> and creates a new JUnit test class containing
 * a suite() method which calls these failed tests.</p>
 * <p>Having classes <i>A</i> ... <i>D</i> with each several testcases you could earn a new
 * test class like
 * <pre>
 * // generated on: 2007.08.06 09:42:34,555
 * import junit.framework.*;
 * public class FailedTests extends TestCase {
 *     public FailedTests(String testname) {
 *         super(testname);
 *     }
 *     public static Test suite() {
 *         TestSuite suite = new TestSuite();
 *         suite.addTest( new B("test04") );
 *         suite.addTest( new org.D("test10") );
 *         return suite;
 *     }
 * }
 * </pre>
 *
 * Because each running test case gets its own formatter, we collect
 * the failing test cases in a static list. Because we dont have a finalizer
 * method in the formatters "lifecycle", we register this formatter as
 * BuildListener and generate the new java source on taskFinished event.
 *
 * @since Ant 1.8.0
 */
public class FailureRecorder extends DataType implements JUnitResultFormatter, BuildListener {

    /**
     * This is the name of a magic System property ({@value}). The value of this
     * <b>System</b> property should point to the location where to store the
     * generated class (without suffix).
     * Default location and name is defined in DEFAULT_CLASS_LOCATION.
     * @see #DEFAULT_CLASS_LOCATION
     */
    public static final String MAGIC_PROPERTY_CLASS_LOCATION
        = "ant.junit.failureCollector";

    /** Default location and name for the generated JUnit class file. {@value} */
    public static final String DEFAULT_CLASS_LOCATION
        = System.getProperty("java.io.tmpdir") + "FailedTests";

    /** Prefix for logging. {@value} */
    private static final String LOG_PREFIX = "    [junit]";

    /** Class names of failed tests without duplicates. */
    private static SortedSet/*<TestInfos>*/ failedTests = new TreeSet();

    /** A writer for writing the generated source to. */
    private PrintWriter writer;

    /**
     * Location and name of the generated JUnit class.
     * Lazy instantiated via getLocationName().
     */
    private static String locationName;

    /**
     * Returns the (lazy evaluated) location for the collector class.
     * Order for evaluation: System property > Ant property > default value
     * @return location for the collector class
     * @see #MAGIC_PROPERTY_CLASS_LOCATION
     * @see #DEFAULT_CLASS_LOCATION
     */
    private String getLocationName() {
        if (locationName == null) {
            String syspropValue = System.getProperty(MAGIC_PROPERTY_CLASS_LOCATION);
            String antpropValue = getProject().getProperty(MAGIC_PROPERTY_CLASS_LOCATION);

            if (syspropValue != null) {
                locationName = syspropValue;
                verbose("System property '" + MAGIC_PROPERTY_CLASS_LOCATION + "' set, so use "
                        + "its value '" + syspropValue + "' as location for collector class.");
            } else if (antpropValue != null) {
                locationName = antpropValue;
                verbose("Ant property '" + MAGIC_PROPERTY_CLASS_LOCATION + "' set, so use "
                        + "its value '" + antpropValue + "' as location for collector class.");
            } else {
                locationName = DEFAULT_CLASS_LOCATION;
                verbose("System property '" + MAGIC_PROPERTY_CLASS_LOCATION + "' not set, so use "
                        + "value as location for collector class: '"
                        + DEFAULT_CLASS_LOCATION + "'");
            }

            File locationFile = new File(locationName);
            if (!locationFile.isAbsolute()) {
                File f = new File(getProject().getBaseDir(), locationName);
                locationName = f.getAbsolutePath();
                verbose("Location file is relative (" + locationFile + ")"
                        + " use absolute path instead (" + locationName + ")");
            }
        }

        return locationName;
    }

    /**
     * This method is called by the Ant runtime by reflection. We use the project reference for
     * registration of this class as BuildListener.
     *
     * @param project
     *            project reference
     */
    public void setProject(Project project) {
        // store project reference for logging
        super.setProject(project);
        // check if already registered
        boolean alreadyRegistered = false;
        Vector allListeners = project.getBuildListeners();
        for (int i = 0; i < allListeners.size(); i++) {
            Object listener = allListeners.get(i);
            if (listener instanceof FailureRecorder) {
                alreadyRegistered = true;
                continue;
            }
        }
        // register if needed
        if (!alreadyRegistered) {
            verbose("Register FailureRecorder (@" + this.hashCode() + ") as BuildListener");
            project.addBuildListener(this);
        }
    }

    // ===== JUnitResultFormatter =====

    /**
     * Not used
     * {@inheritDoc}
     */
    public void endTestSuite(JUnitTest suite) throws BuildException {
    }

    /**
     * Add the failed test to the list.
     * @param test the test that errored.
     * @param throwable the reason it errored.
     * @see junit.framework.TestListener#addError(junit.framework.Test, java.lang.Throwable)
     */
    public void addError(Test test, Throwable throwable) {
        failedTests.add(new TestInfos(test));
    }

    // CheckStyle:LineLengthCheck OFF - @see is long
    /**
     * Add the failed test to the list.
     * @param test the test that failed.
     * @param error the assertion that failed.
     * @see junit.framework.TestListener#addFailure(junit.framework.Test, junit.framework.AssertionFailedError)
     */
    // CheckStyle:LineLengthCheck ON
    public void addFailure(Test test, AssertionFailedError error) {
        failedTests.add(new TestInfos(test));
    }

    /**
     * Not used
     * {@inheritDoc}
     */
    public void setOutput(OutputStream out) {
    }

    /**
     * Not used
     * {@inheritDoc}
     */
    public void setSystemError(String err) {
    }

    /**
     * Not used
     * {@inheritDoc}
     */
    public void setSystemOutput(String out) {
    }

    /**
     * Not used
     * {@inheritDoc}
     */
    public void startTestSuite(JUnitTest suite) throws BuildException {
    }

    /**
     * Not used
     * {@inheritDoc}
     */
    public void endTest(Test test) {
    }

    /**
     * Not used
     * {@inheritDoc}
     */
    public void startTest(Test test) {
    }

    // ===== "Templates" for generating the JUnit class =====

    private void writeJavaClass() {
        try {
            File sourceFile = new File((getLocationName() + ".java"));
            verbose("Write collector class to '" + sourceFile.getAbsolutePath() + "'");

            sourceFile.delete();
            writer = new PrintWriter(new FileOutputStream(sourceFile));

            createClassHeader();
            createSuiteMethod();
            createClassFooter();

            FileUtils.close(writer);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void createClassHeader() {
        String className = getLocationName().replace('\\', '/');
        if (className.indexOf('/') > -1) {
            className = className.substring(className.lastIndexOf('/') + 1);
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss,SSS");
        writer.print("// generated on: ");
        writer.println(sdf.format(new Date()));
        writer.println("import junit.framework.*;");
        writer.print("public class ");
        writer.print(className);
        // If this class does not extend TC, Ant doesnt run these
        writer.println(" extends TestCase {");
        // standard String-constructor
        writer.print("    public ");
        writer.print(className);
        writer.println("(String testname) {");
        writer.println("        super(testname);");
        writer.println("    }");
    }

    private void createSuiteMethod() {
        writer.println("    public static Test suite() {");
        writer.println("        TestSuite suite = new TestSuite();");
        for (Iterator iter = failedTests.iterator(); iter.hasNext();) {
            TestInfos testInfos = (TestInfos) iter.next();
            writer.print("        suite.addTest(");
            writer.print(testInfos);
            writer.println(");");
        }
        writer.println("        return suite;");
        writer.println("    }");
    }

    private void createClassFooter() {
        writer.println("}");
    }

    // ===== Helper classes and methods =====

    /**
     * Logging facade in INFO-mode.
     */
    public void log(String message) {
        getProject().log(LOG_PREFIX + " " + message, Project.MSG_INFO);
    }

    /**
     * Logging facade in VERBOSE-mode.
     */
    public void verbose(String message) {
        getProject().log(LOG_PREFIX + " " + message, Project.MSG_VERBOSE);
    }

    /**
     * TestInfos holds information about a given test for later use.
     */
    public class TestInfos implements Comparable {

        /** The class name of the test. */
        private String className;

        /** The method name of the testcase. */
        private String methodName;

        /**
         * This constructor extracts the needed information from the given test.
         * @param test Test to analyze
         */
        public TestInfos(Test test) {
            className = test.getClass().getName();
            methodName = test.toString();
            methodName = methodName.substring(0, methodName.indexOf('('));
        }

        /**
         * This String-Representation can directly be used for instantiation of
         * the JUnit testcase.
         * @return the string representation.
         * @see java.lang.Object#toString()
         * @see FailureRecorder#createSuiteMethod()
         */
        public String toString() {
            return "new " + className + "(\"" + methodName + "\")";
        }

        /**
         * The SortedMap needs comparable elements.
         * @param other the object to compare to.
         * @return the result of the comparison.
         * @see java.lang.Comparable#compareTo(T)
         * @see SortedSet#comparator()
         */
        public int compareTo(Object other) {
            if (other instanceof TestInfos) {
                TestInfos otherInfos = (TestInfos) other;
                return toString().compareTo(otherInfos.toString());
            } else {
                return -1;
            }
        }
    }

    // ===== BuildListener =====

    /**
     * Not used
     * {@inheritDoc}
     */
    public void buildFinished(BuildEvent event) {
    }

    /**
     * Not used
     * {@inheritDoc}
     */
    public void buildStarted(BuildEvent event) {
    }

    /**
     * Not used
     * {@inheritDoc}
     */
    public void messageLogged(BuildEvent event) {
    }

    /**
     * Not used
     * {@inheritDoc}
     */
    public void targetFinished(BuildEvent event) {
    }

    /**
     * Not used
     * {@inheritDoc}
     */
    public void targetStarted(BuildEvent event) {
    }

    /**
     * The task outside of this JUnitResultFormatter is the <junit> task. So all tests passed
     * and we could create the new java class.
     * @param event  not used
     * @see org.apache.tools.ant.BuildListener#taskFinished(org.apache.tools.ant.BuildEvent)
     */
    public void taskFinished(BuildEvent event) {
        if (!failedTests.isEmpty()) {
            writeJavaClass();
        }
    }

    /**
     * Not used
     * {@inheritDoc}
     */
    public void taskStarted(BuildEvent event) {
    }

}
