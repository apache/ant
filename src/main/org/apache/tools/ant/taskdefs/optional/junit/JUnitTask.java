/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2003 The Apache Software Foundation.  All rights
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
 * 4. The names "Ant" and "Apache Software
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;
import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.ExecuteWatchdog;
import org.apache.tools.ant.taskdefs.LogOutputStream;
import org.apache.tools.ant.taskdefs.LogStreamHandler;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.CommandlineJava;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.Environment;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.PropertySet;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.LoaderUtils;
import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestResult;

/**
 * Runs JUnit tests.
 *
 * <p> JUnit is a framework to create unit test. It has been initially
 * created by Erich Gamma and Kent Beck.  JUnit can be found at <a
 * href="http://www.junit.org">http://www.junit.org</a>.
 *
 * <p> <code>JUnitTask</code> can run a single specific
 * <code>JUnitTest</code> using the <code>test</code> element.</p>
 * For example, the following target <code><pre>
 *   &lt;target name="test-int-chars" depends="jar-test"&gt;
 *       &lt;echo message="testing international characters"/&gt;
 *       &lt;junit printsummary="no" haltonfailure="yes" fork="false"&gt;
 *           &lt;classpath refid="classpath"/&gt;
 *           &lt;formatter type="plain" usefile="false" /&gt;
 *           &lt;test name="org.apache.ecs.InternationalCharTest" /&gt;
 *       &lt;/junit&gt;
 *   &lt;/target&gt;
 * </pre></code>
 * <p>runs a single junit test
 * (<code>org.apache.ecs.InternationalCharTest</code>) in the current
 * VM using the path with id <code>classpath</code> as classpath and
 * presents the results formatted using the standard
 * <code>plain</code> formatter on the command line.</p>
 *
 * <p> This task can also run batches of tests.  The
 * <code>batchtest</code> element creates a <code>BatchTest</code>
 * based on a fileset.  This allows, for example, all classes found in
 * directory to be run as testcases.</p>
 *
 * <p>For example,</p><code><pre>
 * &lt;target name="run-tests" depends="dump-info,compile-tests" if="junit.present"&gt;
 *   &lt;junit printsummary="no" haltonfailure="yes" fork="${junit.fork}"&gt;
 *     &lt;jvmarg value="-classic"/&gt;
 *     &lt;classpath refid="tests-classpath"/&gt;
 *     &lt;sysproperty key="build.tests" value="${build.tests}"/&gt;
 *     &lt;formatter type="brief" usefile="false" /&gt;
 *     &lt;batchtest&gt;
 *       &lt;fileset dir="${tests.dir}"&gt;
 *         &lt;include name="**&#047;*Test*" /&gt;
 *       &lt;/fileset&gt;
 *     &lt;/batchtest&gt;
 *   &lt;/junit&gt;
 * &lt;/target&gt;
 * </pre></code>
 * <p>this target finds any classes with a <code>test</code> directory
 * anywhere in their path (under the top <code>${tests.dir}</code>, of
 * course) and creates <code>JUnitTest</code>'s for each one.</p>
 *
 * <p> Of course, <code>&lt;junit&gt;</code> and
 * <code>&lt;batch&gt;</code> elements can be combined for more
 * complex tests. For an example, see the ant <code>build.xml</code>
 * target <code>run-tests</code> (the second example is an edited
 * version).</p>
 *
 * <p> To spawn a new Java VM to prevent interferences between
 * different testcases, you need to enable <code>fork</code>.  A
 * number of attributes and elements allow you to set up how this JVM
 * runs.
 * @author Thomas Haas
 * @author Stefan Bodewig
 * @author <a href="mailto:sbailliez@imediation.com">Stephane Bailliez</a>
 * @author <a href="mailto:Gerrit.Riessen@web.de">Gerrit Riessen</a>
 * @author <a href="mailto:ehatcher@apache.org">Erik Hatcher</a>
 * @author <a href="mailto:martijn@kruithof.xs4all.nl">Martijn Kruithof></a>
 *
 * @version $Revision$
 *
 * @since Ant 1.2
 *
 * @see JUnitTest
 * @see BatchTest
 */
public class JUnitTask extends Task {

    private CommandlineJava commandline = new CommandlineJava();
    private Vector tests = new Vector();
    private Vector batchTests = new Vector();
    private Vector formatters = new Vector();
    private File dir = null;

    private Integer timeout = null;
    private boolean summary = false;
    private String summaryValue = "";
    private JUnitTestRunner runner = null;

    private boolean newEnvironment = false;
    private Environment env = new Environment();

    private boolean includeAntRuntime = true;
    private Path antRuntimeClasses = null;

    private boolean showOutput = false;
    private File tmpDir;

    /**
     * If true, smartly filter the stack frames of
     * JUnit errors and failures before reporting them.
     *
     * <p>This property is applied on all BatchTest (batchtest) and
     * JUnitTest (test) however it can possibly be overridden by their
     * own properties.</p>
     * @param value <tt>false</tt> if it should not filter, otherwise
     * <tt>true<tt>
     *
     * @since Ant 1.5
     */
    public void setFiltertrace(boolean value) {
        Enumeration enum = allTests();
        while (enum.hasMoreElements()) {
            BaseTest test = (BaseTest) enum.nextElement();
            test.setFiltertrace(value);
        }
    }

    /**
     * If true, stop the build process when there is an error in a test.
     * This property is applied on all BatchTest (batchtest) and JUnitTest
     * (test) however it can possibly be overridden by their own
     * properties.
     * @param value <tt>true</tt> if it should halt, otherwise
     * <tt>false</tt>
     *
     * @since Ant 1.2
     */
    public void setHaltonerror(boolean value) {
        Enumeration enum = allTests();
        while (enum.hasMoreElements()) {
            BaseTest test = (BaseTest) enum.nextElement();
            test.setHaltonerror(value);
        }
    }

    /**
     * Property to set to "true" if there is a error in a test.
     *
     * <p>This property is applied on all BatchTest (batchtest) and
     * JUnitTest (test), however, it can possibly be overriden by
     * their own properties.</p>
     * @param propertyName the name of the property to set in the
     * event of an error.
     *
     * @since Ant 1.4
     */
    public void setErrorProperty(String propertyName) {
        Enumeration enum = allTests();
        while (enum.hasMoreElements()) {
            BaseTest test = (BaseTest) enum.nextElement();
            test.setErrorProperty(propertyName);
        }
    }

    /**
     * If true, stop the build process if a test fails
     * (errors are considered failures as well).
     * This property is applied on all BatchTest (batchtest) and
     * JUnitTest (test) however it can possibly be overridden by their
     * own properties.
     * @param value <tt>true</tt> if it should halt, otherwise
     * <tt>false</tt>
     *
     * @since Ant 1.2
     */
    public void setHaltonfailure(boolean value) {
        Enumeration enum = allTests();
        while (enum.hasMoreElements()) {
            BaseTest test = (BaseTest) enum.nextElement();
            test.setHaltonfailure(value);
        }
    }

    /**
     * Property to set to "true" if there is a failure in a test.
     *
     * <p>This property is applied on all BatchTest (batchtest) and
     * JUnitTest (test), however, it can possibly be overriden by
     * their own properties.</p>
     * @param propertyName the name of the property to set in the
     * event of an failure.
     *
     * @since Ant 1.4
     */
    public void setFailureProperty(String propertyName) {
        Enumeration enum = allTests();
        while (enum.hasMoreElements()) {
            BaseTest test = (BaseTest) enum.nextElement();
            test.setFailureProperty(propertyName);
        }
    }

    /**
     * If true, JVM should be forked for each test.
     *
     * <p>It avoids interference between testcases and possibly avoids
     * hanging the build.  this property is applied on all BatchTest
     * (batchtest) and JUnitTest (test) however it can possibly be
     * overridden by their own properties.</p>
     * @param value <tt>true</tt> if a JVM should be forked, otherwise
     * <tt>false</tt>
     * @see #setTimeout
     *
     * @since Ant 1.2
     */
    public void setFork(boolean value) {
        Enumeration enum = allTests();
        while (enum.hasMoreElements()) {
            BaseTest test = (BaseTest) enum.nextElement();
            test.setFork(value);
        }
    }

    /**
     * If true, print one-line statistics for each test, or "withOutAndErr"
     * to also show standard output and error.
     *
     * Can take the values on, off, and withOutAndErr.
     * @param value <tt>true</tt> to print a summary,
     * <tt>withOutAndErr</tt> to include the test&apos;s output as
     * well, <tt>false</tt> otherwise.
     * @see SummaryJUnitResultFormatter
     *
     * @since Ant 1.2
     */
    public void setPrintsummary(SummaryAttribute value) {
        summaryValue = value.getValue();
        summary = value.asBoolean();
    }

    /**
     * Print summary enumeration values.
     */
    public static class SummaryAttribute extends EnumeratedAttribute {
        public String[] getValues() {
            return new String[] {"true", "yes", "false", "no",
                                 "on", "off", "withOutAndErr"};
        }

        public boolean asBoolean() {
            String value = getValue();
            return "true".equals(value)
                || "on".equals(value)
                || "yes".equals(value)
                || "withOutAndErr".equals(value);
        }
    }

    /**
     * Set the timeout value (in milliseconds).
     *
     * <p>If the test is running for more than this value, the test
     * will be canceled. (works only when in 'fork' mode).</p>
     * @param value the maximum time (in milliseconds) allowed before
     * declaring the test as 'timed-out'
     * @see #setFork(boolean)
     *
     * @since Ant 1.2
     */
    public void setTimeout(Integer value) {
        timeout = value;
    }

    /**
     * Set the maximum memory to be used by all forked JVMs.
     * @param   max     the value as defined by <tt>-mx</tt> or <tt>-Xmx</tt>
     *                  in the java command line options.
     *
     * @since Ant 1.2
     */
    public void setMaxmemory(String max) {
        commandline.setMaxmemory(max);
    }

    /**
     * The command used to invoke the Java Virtual Machine,
     * default is 'java'. The command is resolved by
     * java.lang.Runtime.exec(). Ignored if fork is disabled.
     *
     * @param   value   the new VM to use instead of <tt>java</tt>
     * @see #setFork(boolean)
     *
     * @since Ant 1.2
     */
    public void setJvm(String value) {
        commandline.setVm(value);
    }

    /**
     * Adds a JVM argument; ignored if not forking.
     *
     * @return create a new JVM argument so that any argument can be
     * passed to the JVM.
     * @see #setFork(boolean)
     *
     * @since Ant 1.2
     */
    public Commandline.Argument createJvmarg() {
        return commandline.createVmArgument();
    }

    /**
     * The directory to invoke the VM in. Ignored if no JVM is forked.
     * @param   dir     the directory to invoke the JVM from.
     * @see #setFork(boolean)
     *
     * @since Ant 1.2
     */
    public void setDir(File dir) {
        this.dir = dir;
    }

    /**
     * Adds a system property that tests can access.
     * This might be useful to tranfer Ant properties to the
     * testcases when JVM forking is not enabled.
     *
     * @since Ant 1.3
     */
    public void addSysproperty(Environment.Variable sysp) {
        commandline.addSysproperty(sysp);
    }

    /**
     * Adds a set of properties that will be used as system properties
     * that tests can access.
     *
     * This might be useful to tranfer Ant properties to the
     * testcases when JVM forking is not enabled.
     *
     * @since Ant 1.6
     */
    public void addSyspropertyset(PropertySet sysp) {
        commandline.addSyspropertyset(sysp);
    }

    /**
     * Adds path to classpath used for tests.
     *
     * @since Ant 1.2
     */
    public Path createClasspath() {
        return commandline.createClasspath(getProject()).createPath();
    }

    /**
     * Adds a path to the bootclasspath.
     * @since Ant 1.6
     */
    public Path createBootclasspath() {
        return commandline.createBootclasspath(getProject()).createPath();
    }

    /**
     * Adds an environment variable; used when forking.
     *
     * <p>Will be ignored if we are not forking a new VM.</p>
     *
     * @since Ant 1.5
     */
    public void addEnv(Environment.Variable var) {
        env.addVariable(var);
    }

    /**
     * If true, use a new environment when forked.
     *
     * <p>Will be ignored if we are not forking a new VM.</p>
     *
     * @since Ant 1.5
     */
    public void setNewenvironment(boolean newenv) {
        newEnvironment = newenv;
    }

    /**
     * Add a new single testcase.
     * @param   test    a new single testcase
     * @see JUnitTest
     *
     * @since Ant 1.2
     */
    public void addTest(JUnitTest test) {
        tests.addElement(test);
    }

    /**
     * Adds a set of tests based on pattern matching.
     *
     * @return  a new instance of a batch test.
     * @see BatchTest
     *
     * @since Ant 1.2
     */
    public BatchTest createBatchTest() {
        BatchTest test = new BatchTest(getProject());
        batchTests.addElement(test);
        return test;
    }

    /**
     * Add a new formatter to all tests of this task.
     *
     * @since Ant 1.2
     */
    public void addFormatter(FormatterElement fe) {
        formatters.addElement(fe);
    }

    /**
     * If true, include ant.jar, optional.jar and junit.jar in the forked VM.
     *
     * @since Ant 1.5
     */
    public void setIncludeantruntime(boolean b) {
        includeAntRuntime = b;
    }

    /**
     * If true, send any output generated by tests to Ant's logging system
     * as well as to the formatters.
     * By default only the formatters receive the output.
     *
     * <p>Output will always be passed to the formatters and not by
     * shown by default.  This option should for example be set for
     * tests that are interactive and prompt the user to do
     * something.</p>
     *
     * @since Ant 1.5
     */
    public void setShowOutput(boolean showOutput) {
        this.showOutput = showOutput;
    }

    /**
     * Creates a new JUnitRunner and enables fork of a new Java VM.
     *
     * @since Ant 1.2
     */
    public JUnitTask() throws Exception {
        commandline
            .setClassname("org.apache.tools.ant.taskdefs.optional.junit.JUnitTestRunner");
    }

    /**
     * Where Ant should place temporary files.
     *
     * @since Ant 1.6
     */
    public void setTempdir(File tmpDir) {
        this.tmpDir = tmpDir;
    }

    /**
     * Adds the jars or directories containing Ant, this task and
     * JUnit to the classpath - this should make the forked JVM work
     * without having to specify them directly.
     *
     * @since Ant 1.4
     */
    public void init() {
        antRuntimeClasses = new Path(getProject());
        addClasspathEntry("/junit/framework/TestCase.class");
        addClasspathEntry("/org/apache/tools/ant/Task.class");
        addClasspathEntry("/org/apache/tools/ant/taskdefs/optional/junit/JUnitTestRunner.class");
    }

    /**
     * Runs the testcase.
     *
     * @since Ant 1.2
     */
    public void execute() throws BuildException {
        Enumeration list = getIndividualTests();
        while (list.hasMoreElements()) {
            JUnitTest test = (JUnitTest) list.nextElement();
            if (test.shouldRun(getProject())) {
                execute(test);
            }
        }
    }

    /**
     * Run the tests.
     */
    protected void execute(JUnitTest arg) throws BuildException {
        JUnitTest test = (JUnitTest) arg.clone();
        // set the default values if not specified
        //@todo should be moved to the test class instead.
        if (test.getTodir() == null) {
            test.setTodir(getProject().resolveFile("."));
        }

        if (test.getOutfile() == null) {
            test.setOutfile("TEST-" + test.getName());
        }

        // execute the test and get the return code
        int exitValue = JUnitTestRunner.ERRORS;
        boolean wasKilled = false;
        if (!test.getFork()) {
            exitValue = executeInVM(test);
        } else {
            ExecuteWatchdog watchdog = createWatchdog();
            exitValue = executeAsForked(test, watchdog);
            // null watchdog means no timeout, you'd better not check with null
            if (watchdog != null) {
                wasKilled = watchdog.killedProcess();
            }
        }

        // if there is an error/failure and that it should halt, stop
        // everything otherwise just log a statement
        boolean errorOccurredHere = exitValue == JUnitTestRunner.ERRORS;
        boolean failureOccurredHere = exitValue != JUnitTestRunner.SUCCESS;
        if (errorOccurredHere || failureOccurredHere) {
            if ((errorOccurredHere && test.getHaltonerror())
                || (failureOccurredHere && test.getHaltonfailure())) {
                throw new BuildException("Test " + test.getName() + " failed"
                    + (wasKilled ? " (timeout)" : ""), getLocation());
            } else {
                log("TEST " + test.getName() + " FAILED"
                    + (wasKilled ? " (timeout)" : ""), Project.MSG_ERR);
                if (errorOccurredHere && test.getErrorProperty() != null) {
                    getProject().setNewProperty(test.getErrorProperty(), "true");
                }
                if (failureOccurredHere && test.getFailureProperty() != null) {
                    getProject().setNewProperty(test.getFailureProperty(), "true");
                }
            }
        }
    }

    /**
     * Execute a testcase by forking a new JVM. The command will block until
     * it finishes. To know if the process was destroyed or not, use the
     * <tt>killedProcess()</tt> method of the watchdog class.
     * @param  test       the testcase to execute.
     * @param  watchdog   the watchdog in charge of cancelling the test if it
     * exceeds a certain amount of time. Can be <tt>null</tt>, in this case
     * the test could probably hang forever.
     */
    private int executeAsForked(JUnitTest test, ExecuteWatchdog watchdog)
        throws BuildException {

        CommandlineJava cmd = (CommandlineJava) commandline.clone();

        cmd.setClassname("org.apache.tools.ant.taskdefs.optional.junit.JUnitTestRunner");
        cmd.createArgument().setValue(test.getName());
        cmd.createArgument().setValue("filtertrace=" + test.getFiltertrace());
        cmd.createArgument().setValue("haltOnError=" + test.getHaltonerror());
        cmd.createArgument().setValue("haltOnFailure="
                                      + test.getHaltonfailure());
        if (includeAntRuntime) {
            log("Implicitly adding " + antRuntimeClasses + " to CLASSPATH",
                Project.MSG_VERBOSE);
            cmd.createClasspath(getProject()).createPath()
                .append(antRuntimeClasses);
        }

        if (summary) {
            log("Running " + test.getName(), Project.MSG_INFO);
            cmd.createArgument()
                .setValue("formatter=org.apache.tools.ant.taskdefs.optional.junit.SummaryJUnitResultFormatter");
        }

        cmd.createArgument().setValue("showoutput="
                                      + String.valueOf(showOutput));

        StringBuffer formatterArg = new StringBuffer(128);
        final FormatterElement[] feArray = mergeFormatters(test);
        for (int i = 0; i < feArray.length; i++) {
            FormatterElement fe = feArray[i];
            formatterArg.append("formatter=");
            formatterArg.append(fe.getClassname());
            File outFile = getOutput(fe, test);
            if (outFile != null) {
                formatterArg.append(",");
                formatterArg.append(outFile);
            }
            cmd.createArgument().setValue(formatterArg.toString());
            formatterArg = new StringBuffer();
        }

        // Create a temporary file to pass the Ant properties to the
        // forked test
        File propsFile =
            FileUtils.newFileUtils().createTempFile("junit", ".properties",
                                                    tmpDir != null ? tmpDir :
                                                    getProject().getBaseDir());
        cmd.createArgument().setValue("propsfile="
                                      + propsFile.getAbsolutePath());
        Hashtable p = getProject().getProperties();
        Properties props = new Properties();
        for (Enumeration enum = p.keys(); enum.hasMoreElements();) {
            Object key = enum.nextElement();
            props.put(key, p.get(key));
        }
        try {
            FileOutputStream outstream = new FileOutputStream(propsFile);
            props.save(outstream, "Ant JUnitTask generated properties file");
            outstream.close();
        } catch (java.io.IOException e) {
            propsFile.delete();
            throw new BuildException("Error creating temporary properties "
                                     + "file.", e, getLocation());
        }

        Execute execute = new Execute(new LogStreamHandler(this,
                                                           Project.MSG_INFO,
                                                           Project.MSG_WARN),
                                      watchdog);
        execute.setCommandline(cmd.getCommandline());
        execute.setAntRun(getProject());
        if (dir != null) {
            execute.setWorkingDirectory(dir);
        }

        String[] environment = env.getVariables();
        if (environment != null) {
            for (int i = 0; i < environment.length; i++) {
                log("Setting environment variable: " + environment[i],
                    Project.MSG_VERBOSE);
            }
        }
        execute.setNewenvironment(newEnvironment);
        execute.setEnvironment(environment);

        log(cmd.describeCommand(), Project.MSG_VERBOSE);
        int retVal;
        try {
            retVal = execute.execute();
        } catch (IOException e) {
            throw new BuildException("Process fork failed.", e, getLocation());
        } finally {
            if (watchdog != null && watchdog.killedProcess()) {
                logTimeout(feArray, test);
            }

            if (!propsFile.delete()) {
                throw new BuildException("Could not delete temporary "
                                         + "properties file.");
            }
        }

        return retVal;
    }


    /**
     * Pass output sent to System.out to the TestRunner so it can
     * collect ot for the formatters.
     *
     * @since Ant 1.5
     */
    protected void handleOutput(String line) {
        if (runner != null) {
            runner.handleOutput(line);
            if (showOutput) {
                super.handleOutput(line);
            }
        } else {
            super.handleOutput(line);
        }
    }

    /**
     * @see Task#handleInput(byte[], int, int)
     * 
     * @since Ant 1.6
     */
    protected int handleInput(byte[] buffer, int offset, int length) 
        throws IOException {
        if (runner != null) {
            return runner.handleInput(buffer, offset, length);
        } else {
            return super.handleInput(buffer, offset, length);
        }
    }
    
    
    /**
     * Pass output sent to System.out to the TestRunner so it can
     * collect ot for the formatters.
     *
     * @since Ant 1.5.2
     */
    protected void handleFlush(String line) {
        if (runner != null) {
            runner.handleFlush(line);
            if (showOutput) {
                super.handleFlush(line);
            }
        } else {
            super.handleFlush(line);
        }
    }

    /**
     * Pass output sent to System.err to the TestRunner so it can
     * collect ot for the formatters.
     *
     * @since Ant 1.5
     */
    public void handleErrorOutput(String line) {
        if (runner != null) {
            runner.handleErrorOutput(line);
            if (showOutput) {
                super.handleErrorOutput(line);
            }
        } else {
            super.handleErrorOutput(line);
        }
    }


    /**
     * Pass output sent to System.err to the TestRunner so it can
     * collect ot for the formatters.
     *
     * @since Ant 1.5.2
     */
    public void handleErrorFlush(String line) {
        if (runner != null) {
            runner.handleErrorFlush(line);
            if (showOutput) {
                super.handleErrorFlush(line);
            }
        } else {
            super.handleErrorFlush(line);
        }
    }

    // in VM is not very nice since it could probably hang the
    // whole build. IMHO this method should be avoided and it would be best
    // to remove it in future versions. TBD. (SBa)

    /**
     * Execute inside VM.
     */
    private int executeInVM(JUnitTest arg) throws BuildException {
        JUnitTest test = (JUnitTest) arg.clone();
        test.setProperties(getProject().getProperties());
        if (dir != null) {
            log("dir attribute ignored if running in the same VM",
                Project.MSG_WARN);
        }

        if (newEnvironment || null != env.getVariables()) {
            log("Changes to environment variables are ignored if running in "
                + "the same VM.", Project.MSG_WARN);
        }

        if (commandline.getBootclasspath() != null) {
            log("bootclasspath is ignored if running in the same VM.", 
                Project.MSG_WARN);
        }

        CommandlineJava.SysProperties sysProperties =
            commandline.getSystemProperties();
        if (sysProperties != null) {
            sysProperties.setSystem();
        }
        AntClassLoader cl = null;
        try {
            log("Using System properties " + System.getProperties(),
                Project.MSG_VERBOSE);
            cl = createClassLoader();
            if (cl != null) {
                cl.setThreadContextLoader();
            }
            runner = new JUnitTestRunner(test, test.getHaltonerror(),
                                         test.getFiltertrace(),
                                         test.getHaltonfailure(), cl);
            if (summary) {
                log("Running " + test.getName(), Project.MSG_INFO);

                SummaryJUnitResultFormatter f =
                    new SummaryJUnitResultFormatter();
                f.setWithOutAndErr("withoutanderr"
                                   .equalsIgnoreCase(summaryValue));
                f.setOutput(getDefaultOutput());
                runner.addFormatter(f);
            }

            final FormatterElement[] feArray = mergeFormatters(test);
            for (int i = 0; i < feArray.length; i++) {
                FormatterElement fe = feArray[i];
                File outFile = getOutput(fe, test);
                if (outFile != null) {
                    fe.setOutfile(outFile);
                } else {
                    fe.setOutput(getDefaultOutput());
                }
                runner.addFormatter(fe.createFormatter(cl));
            }

            runner.run();
            return runner.getRetCode();
        } finally{
            if (sysProperties != null) {
                sysProperties.restoreSystem();
            }
            if (cl != null) {
                cl.resetThreadContextLoader();
            }
        }
    }

    /**
     * @return <tt>null</tt> if there is a timeout value, otherwise the
     * watchdog instance.
     *
     * @since Ant 1.2
     */
    protected ExecuteWatchdog createWatchdog() throws BuildException {
        if (timeout == null){
            return null;
        }
        return new ExecuteWatchdog(timeout.intValue());
    }

    /**
     * Get the default output for a formatter.
     *
     * @since Ant 1.3
     */
    protected OutputStream getDefaultOutput(){
        return new LogOutputStream(this, Project.MSG_INFO);
    }

    /**
     * Merge all individual tests from the batchtest with all individual tests
     * and return an enumeration over all <tt>JUnitTest</tt>.
     *
     * @since Ant 1.3
     */
    protected Enumeration getIndividualTests(){
        final int count = batchTests.size();
        final Enumeration[] enums = new Enumeration[ count + 1];
        for (int i = 0; i < count; i++) {
            BatchTest batchtest = (BatchTest) batchTests.elementAt(i);
            enums[i] = batchtest.elements();
        }
        enums[enums.length - 1] = tests.elements();
        return Enumerations.fromCompound(enums);
    }

    /**
     * @since Ant 1.3
     */
    protected Enumeration allTests() {
        Enumeration[] enums = { tests.elements(), batchTests.elements() };
        return Enumerations.fromCompound(enums);
    }

    /**
     * @since Ant 1.3
     */
    private FormatterElement[] mergeFormatters(JUnitTest test){
        Vector feVector = (Vector) formatters.clone();
        test.addFormattersTo(feVector);
        FormatterElement[] feArray = new FormatterElement[feVector.size()];
        feVector.copyInto(feArray);
        return feArray;
    }

    /**
     * If the formatter sends output to a file, return that file.
     * null otherwise.
     *
     * @since Ant 1.3
     */
    protected File getOutput(FormatterElement fe, JUnitTest test){
        if (fe.getUseFile()) {
            String filename = test.getOutfile() + fe.getExtension();
            File destFile = new File(test.getTodir(), filename);
            String absFilename = destFile.getAbsolutePath();
            return getProject().resolveFile(absFilename);
        }
        return null;
    }

    /**
     * Search for the given resource and add the directory or archive
     * that contains it to the classpath.
     *
     * <p>Doesn't work for archives in JDK 1.1 as the URL returned by
     * getResource doesn't contain the name of the archive.</p>
     *
     * @since Ant 1.4
     */
    protected void addClasspathEntry(String resource) {
        /* 
         * pre Ant 1.6 this method used to call getClass().getResource
         * while Ant 1.6 will call ClassLoader.getResource().
         *
         * The difference is that Class.getResource expects a leading
         * slash for "absolute" resources and will strip it before
         * delegating to ClassLoader.getResource - so we now have to
         * emulate Class's behavior.
         */
        if (resource.startsWith("/")) {
            resource = resource.substring(1);
        } else {
            resource = "org/apache/tools/ant/taskdefs/optional/junit/"
                + resource;
        }
        
        File f = LoaderUtils.getResourceSource(getClass().getClassLoader(),
                                               resource);
        if (f != null) {
            log("Found " + f.getAbsolutePath(), Project.MSG_DEBUG);
            antRuntimeClasses.createPath().setLocation(f);
        } else {
            log("Couldn\'t find " + resource, Project.MSG_DEBUG);
        }
    }

    /**
     * Take care that some output is produced in report files if the
     * watchdog kills the test.
     *
     * @since Ant 1.5.2
     */

    private void logTimeout(FormatterElement[] feArray, JUnitTest test) {
        AntClassLoader cl = createClassLoader();
        for (int i = 0; i < feArray.length; i++) {
            FormatterElement fe = feArray[i];
            File outFile = getOutput(fe, test);
            JUnitResultFormatter formatter = fe.createFormatter(cl);
            if (outFile != null && formatter != null) {
                try {
                    OutputStream out = new FileOutputStream(outFile);
                    formatter.setOutput(out);
                    formatter.startTestSuite(test);
                    test.setCounts(0,0,1);
                    Test t = new Test() {
                        public int countTestCases() { return 0; }
                        public void run(TestResult r) {
                            throw new AssertionFailedError("Timeout occurred");
                        }
                    };
                    formatter.startTest(t);
                    formatter
                        .addError(t,
                                  new AssertionFailedError("Timeout occurred"));

                    formatter.endTestSuite(test);
                } catch (IOException e) {}
            }
        }
    }

    /**
     * Creates and configures an AntClassLoader instance from the
     * nested classpath element.
     *
     * @return null if there is no user-specified classpath.
     *
     * @since Ant 1.6
     */
    private AntClassLoader createClassLoader() {
        AntClassLoader cl = null;
        Path userClasspath = commandline.getClasspath();
        if (userClasspath != null) {
            Path classpath = (Path) userClasspath.clone();
            if (includeAntRuntime) {
                log("Implicitly adding " + antRuntimeClasses 
                    + " to CLASSPATH", Project.MSG_VERBOSE);
                classpath.append(antRuntimeClasses);
            }

            cl = getProject().createClassLoader(classpath);
            cl.setParentFirst(false);
            cl.addJavaLibraries();
            log("Using CLASSPATH " + cl.getClasspath(), Project.MSG_VERBOSE);

            // make sure the test will be accepted as a TestCase
            cl.addSystemPackageRoot("junit");
            // will cause trouble in JDK 1.1 if omitted
            cl.addSystemPackageRoot("org.apache.tools.ant");
        }
        return cl;
    }
}
