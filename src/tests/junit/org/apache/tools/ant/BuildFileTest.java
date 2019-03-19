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

package org.apache.tools.ant;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;

import junit.framework.TestCase;
import org.apache.tools.ant.util.ProcessUtil;

/**
 * A BuildFileTest is a TestCase which executes targets from an Ant buildfile
 * for testing.
 *
 * This class provides a number of utility methods for particular build file
 * tests which extend this class.
 *
 * @deprecated as of 1.9.4. Use BuildFileRule, Assert, AntAssert and JUnit4 annotations to drive tests instead
 * @see org.apache.tools.ant.BuildFileRule
 */
@Deprecated
public abstract class BuildFileTest extends TestCase {

    protected Project project;

    private StringBuffer logBuffer;
    private StringBuffer fullLogBuffer;
    private StringBuffer outBuffer;
    private StringBuffer errBuffer;
    private BuildException buildException;

    /**
     * Default constructor for the BuildFileTest object.
     */
    public BuildFileTest() {
        super();
    }

    /**
     * Constructor for the BuildFileTest object.
     *
     * @param  name string to pass up to TestCase constructor
     */
    public BuildFileTest(String name) {
        super(name);
    }

    /**
     * Automatically calls the target called "tearDown"
     * from the build file tested if it exits.
     *
     * This allows to use Ant tasks directly in the build file
     * to clean up after each test. Note that no "setUp" target
     * is automatically called, since it's trivial to have a
     * test target depend on it.
     *
     * @throws Exception this implementation doesn't throw any
     * exception but we've added it to the signature so that
     * subclasses can throw whatever they need.
     */
    protected void tearDown() throws Exception {
        if (project == null) {
            /*
             * Maybe the BuildFileTest was subclassed and there is
             * no initialized project. So we could avoid getting a
             * NPE.
             * If there is an initialized project getTargets() does
             * not return null as it is initialized by an empty
             * HashSet.
             */
            return;
        }
        final String tearDown = "tearDown";
        if (project.getTargets().containsKey(tearDown)) {
            project.executeTarget(tearDown);
        }
    }

    /**
     * run a target, expect for any build exception
     *
     * @param  target target to run
     * @param  cause  information string to reader of report
     */
    public void expectBuildException(String target, String cause) {
        expectSpecificBuildException(target, cause, null);
    }

    /**
     * Assert that only the given message has been logged with a
     * priority &lt;= INFO when running the given target.
     *
     * @param target String
     * @param log String
     */
    public void expectLog(String target, String log) {
        executeTarget(target);
        String realLog = getLog();
        assertEquals(log, realLog);
    }

    /**
     * Assert that the given substring is in the log messages.
     *
     * @param substring String
     */
    public void assertLogContaining(String substring) {
        String realLog = getLog();
        assertTrue("expecting log to contain \"" + substring + "\" log was \""
                   + realLog + "\"",
                realLog.contains(substring));
    }

    /**
     * Assert that the given substring is not in the log messages.
     *
     * @param substring String
     */
    public void assertLogNotContaining(String substring) {
        String realLog = getLog();
        assertFalse("didn't expect log to contain \"" + substring + "\" log was \""
                    + realLog + "\"",
                realLog.contains(substring));
    }

    /**
     * Assert that the given substring is in the output messages.
     *
     * @param substring String
     * @since Ant1.7
     */
    public void assertOutputContaining(String substring) {
        assertOutputContaining(null, substring);
    }

    /**
     * Assert that the given substring is in the output messages.
     *
     * @param message Print this message if the test fails. Defaults to
     *                a meaningful text if <code>null</code> is passed.
     * @param substring String
     * @since Ant1.7
     */
    public void assertOutputContaining(String message, String substring) {
        String realOutput = getOutput();
        String realMessage = (message != null)
            ? message
            : "expecting output to contain \"" + substring + "\" output was \"" + realOutput + "\"";
        assertTrue(realMessage, realOutput.contains(substring));
    }

    /**
     * Assert that the given substring is not in the output messages.
     *
     * @param message Print this message if the test fails. Defaults to
     *                a meaningful text if <code>null</code> is passed.
     * @param substring String
     * @since Ant1.7
     */
    public void assertOutputNotContaining(String message, String substring) {
        String realOutput = getOutput();
        String realMessage = (message != null)
            ? message
            : "expecting output to not contain \"" + substring + "\" output was \"" + realOutput + "\"";
        assertFalse(realMessage, realOutput.contains(substring));
    }

    /**
     * Assert that the given message has been logged with a priority &lt;= INFO when running the
     * given target.
     *
     * @param target String
     * @param log String
     */
    public void expectLogContaining(String target, String log) {
        executeTarget(target);
        assertLogContaining(log);
    }

    /**
     * Assert that the given message has not been logged with a
     * priority &lt;= INFO when running the given target.
     *
     * @param target String
     * @param log String
     */
    public void expectLogNotContaining(String target, String log) {
        executeTarget(target);
        assertLogNotContaining(log);
    }

    /**
     * Gets the log the BuildFileTest object.
     * Only valid if configureProject() has been called.
     *
     * @pre logBuffer!=null
     * @return The log value
     */
    public String getLog() {
        return logBuffer.toString();
    }

    /**
     * Assert that the given message has been logged with a priority
     * &gt;= VERBOSE when running the given target.
     *
     * @param target String
     * @param log String
     */
    public void expectDebuglog(String target, String log) {
        executeTarget(target);
        String realLog = getFullLog();
        assertEquals(log, realLog);
    }

    /**
     * Assert that the given substring is in the log messages.
     *
     * @param substring String
     */
    public void assertDebuglogContaining(String substring) {
        String realLog = getFullLog();
        assertTrue("expecting debug log to contain \"" + substring
                   + "\" log was \""
                   + realLog + "\"",
                realLog.contains(substring));
    }

    /**
     * Gets the log the BuildFileTest object.
     *
     * Only valid if configureProject() has been called.
     *
     * @pre fullLogBuffer!=null
     * @return    The log value
     */
    public String getFullLog() {
        return fullLogBuffer.toString();
    }

    /**
     * execute the target, verify output matches expectations
     *
     * @param target  target to execute
     * @param output  output to look for
     */
    public void expectOutput(String target, String output) {
        executeTarget(target);
        String realOutput = getOutput();
        assertEquals(output, realOutput.trim());
    }

    /**
     * Executes the target, verify output matches expectations
     * and that we got the named error at the end
     *
     * @param target  target to execute
     * @param output  output to look for
     * @param error   Description of Parameter
     */
    public void expectOutputAndError(String target, String output, String error) {
        executeTarget(target);
        String realOutput = getOutput();
        assertEquals(output, realOutput);
        String realError = getError();
        assertEquals(error, realError);
    }

    public String getOutput() {
        return cleanBuffer(outBuffer);
    }

    public String getError() {
        return cleanBuffer(errBuffer);
    }

    public BuildException getBuildException() {
        return buildException;
    }

    private String cleanBuffer(StringBuffer buffer) {
        StringBuilder cleanedBuffer = new StringBuilder();
        for (int i = 0; i < buffer.length(); i++) {
            char ch = buffer.charAt(i);
            if (ch != '\r') {
                cleanedBuffer.append(ch);
            }
        }
        return cleanedBuffer.toString();
    }

    /**
     * Sets up to run the named project
     *
     * @param filename name of project file to run
     */
    public void configureProject(String filename) throws BuildException {
        configureProject(filename, Project.MSG_DEBUG);
    }

    /**
     * Sets up to run the named project
     *
     * @param filename name of project file to run
     * @param logLevel int
     */
    public void configureProject(String filename, int logLevel)
        throws BuildException {
        logBuffer = new StringBuffer();
        fullLogBuffer = new StringBuffer();
        project = new Project();
        if (Boolean.getBoolean(MagicTestNames.TEST_BASEDIR_IGNORE)) {
            System.clearProperty(MagicNames.PROJECT_BASEDIR);
        }
        project.init();
        File antFile = new File(System.getProperty(MagicTestNames.TEST_ROOT_DIRECTORY), filename);
        project.setUserProperty(MagicNames.ANT_FILE, antFile.getAbsolutePath());
        // set two new properties to allow to build unique names when running multithreaded tests
        project.setProperty(MagicTestNames.TEST_PROCESS_ID, ProcessUtil.getProcessId("<Process>"));
        project.setProperty(MagicTestNames.TEST_THREAD_NAME, Thread.currentThread().getName());
        project.addBuildListener(new AntTestListener(logLevel));
        ProjectHelper.configureProject(project, antFile);
    }

    /**
     * Executes a target we have set up
     *
     * @pre configureProject has been called
     * @param targetName  target to run
     */
    public void executeTarget(String targetName) {
        PrintStream sysOut = System.out;
        PrintStream sysErr = System.err;
        try {
            sysOut.flush();
            sysErr.flush();
            outBuffer = new StringBuffer();
            PrintStream out = new PrintStream(new AntOutputStream(outBuffer));
            System.setOut(out);
            errBuffer = new StringBuffer();
            PrintStream err = new PrintStream(new AntOutputStream(errBuffer));
            System.setErr(err);
            logBuffer = new StringBuffer();
            fullLogBuffer = new StringBuffer();
            buildException = null;
            project.executeTarget(targetName);
        } finally {
            System.setOut(sysOut);
            System.setErr(sysErr);
        }

    }

    /**
     * Get the project which has been configured for a test.
     *
     * @return the Project instance for this test.
     */
    public Project getProject() {
        return project;
    }

    /**
     * Gets the directory of the project.
     *
     * @return the base dir of the project
     */
    public File getProjectDir() {
        return project.getBaseDir();
    }

    /**
     * get location of temporary directory pointed to by property "output"
     * @return location of temporary directory pointed to by property "output"
     * @since Ant 1.9.4
     */
    public File getOutputDir() {
        return new File(project.getProperty("output"));
    }

    /**
     * Runs a target, wait for a build exception.
     *
     * @param target target to run
     * @param cause  information string to reader of report
     * @param msg    the message value of the build exception we are waiting
     *               for set to null for any build exception to be valid
     */
    public void expectSpecificBuildException(String target, String cause, String msg) {
        try {
            executeTarget(target);
        } catch (BuildException ex) {
            buildException = ex;
            assertTrue("Should throw BuildException because '" + cause
                    + "' with message '" + msg + "' (actual message '"
                            + ex.getMessage() + "' instead)",
                    msg == null || ex.getMessage().equals(msg));
            return;
        }
        fail("Should throw BuildException because: " + cause);
    }

    /**
     * run a target, expect an exception string
     * containing the substring we look for (case sensitive match)
     *
     * @param target target to run
     * @param cause  information string to reader of report
     * @param contains  substring of the build exception to look for
     */
    public void expectBuildExceptionContaining(String target, String cause, String contains) {
        try {
            executeTarget(target);
        } catch (BuildException ex) {
            buildException = ex;
            assertTrue("Should throw BuildException because '" + cause
                    + "' with message containing '" + contains + "' (actual message '"
                            + ex.getMessage() + "' instead)",
                    null == contains || ex.getMessage().contains(contains));
            return;
        }
        fail("Should throw BuildException because: " + cause);
    }

    /**
     * call a target, verify property is as expected
     *
     * @param target build file target
     * @param property property name
     * @param value expected value
     */
    public void expectPropertySet(String target, String property, String value) {
        executeTarget(target);
        assertPropertyEquals(property, value);
    }

    /**
     * assert that a property equals a value; comparison is case sensitive.
     *
     * @param property property name
     * @param value expected value
     */
    public void assertPropertyEquals(String property, String value) {
        String result = project.getProperty(property);
        assertEquals("property " + property, value, result);
    }

    /**
     * assert that a property equals "true".
     *
     * @param property property name
     */
    public void assertPropertySet(String property) {
        assertPropertyEquals(property, "true");
    }

    /**
     * assert that a property is null.
     *
     * @param property property name
     */
    public void assertPropertyUnset(String property) {
        String result = project.getProperty(property);
        assertNull("Expected property " + property
                + " to be unset, but it is set to the value: " + result, result);
    }

    /**
     * call a target, verify named property is "true".
     *
     * @param target build file target
     * @param property property name
     */
    public void expectPropertySet(String target, String property) {
        expectPropertySet(target, property, "true");
    }

    /**
     * Call a target, verify property is null.
     *
     * @param target build file target
     * @param property property name
     */
    public void expectPropertyUnset(String target, String property) {
        expectPropertySet(target, property, null);
    }

    /**
     * Retrieve a resource from the caller classloader to avoid
     * assuming a vm working directory. The resource path must be
     * relative to the package name or absolute from the root path.
     *
     * @param resource the resource to retrieve its url.
     * @return URL ditto
     */
    public URL getResource(String resource) {
        URL url = getClass().getResource(resource);
        assertNotNull("Could not find resource :" + resource, url);
        return url;
    }

    /**
     * an output stream which saves stuff to our buffer.
     */
    protected static class AntOutputStream extends OutputStream {
        private StringBuffer buffer;

        public AntOutputStream(StringBuffer buffer) {
            this.buffer = buffer;
        }

        public void write(int b) {
            buffer.append((char) b);
        }
    }

    /**
     * Our own personal build listener.
     */
    private class AntTestListener implements BuildListener {
        private int logLevel;

        /**
         * Constructs a test listener which will ignore log events
         * above the given level.
         */
        public AntTestListener(int logLevel) {
            this.logLevel = logLevel;
        }

        /**
         * Fired before any targets are started.
         */
        public void buildStarted(BuildEvent event) {
        }

        /**
         * Fired after the last target has finished. This event
         * will still be thrown if an error occurred during the build.
         *
         * @see BuildEvent#getException()
         */
        public void buildFinished(BuildEvent event) {
        }

        /**
         * Fired when a target is started.
         *
         * @see BuildEvent#getTarget()
         */
        public void targetStarted(BuildEvent event) {
            //System.out.println("targetStarted " + event.getTarget().getName());
        }

        /**
         * Fired when a target has finished. This event will
         * still be thrown if an error occurred during the build.
         *
         * @see BuildEvent#getException()
         */
        public void targetFinished(BuildEvent event) {
            //System.out.println("targetFinished " + event.getTarget().getName());
        }

        /**
         * Fired when a task is started.
         *
         * @see BuildEvent#getTask()
         */
        public void taskStarted(BuildEvent event) {
            //System.out.println("taskStarted " + event.getTask().getTaskName());
        }

        /**
         * Fired when a task has finished. This event will still
         * be throw if an error occurred during the build.
         *
         * @see BuildEvent#getException()
         */
        public void taskFinished(BuildEvent event) {
            //System.out.println("taskFinished " + event.getTask().getTaskName());
        }

        /**
         * Fired whenever a message is logged.
         *
         * @see BuildEvent#getMessage()
         * @see BuildEvent#getPriority()
         */
        public void messageLogged(BuildEvent event) {
            if (event.getPriority() > logLevel) {
                // ignore event
                return;
            }

            if (event.getPriority() == Project.MSG_INFO
                || event.getPriority() == Project.MSG_WARN
                || event.getPriority() == Project.MSG_ERR) {
                logBuffer.append(event.getMessage());
            }
            fullLogBuffer.append(event.getMessage());
        }
    }

}
