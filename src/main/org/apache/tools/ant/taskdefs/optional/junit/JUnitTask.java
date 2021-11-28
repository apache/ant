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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.nio.file.Files;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.MagicNames;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.ExecuteWatchdog;
import org.apache.tools.ant.taskdefs.LogOutputStream;
import org.apache.tools.ant.taskdefs.PumpStreamHandler;
import org.apache.tools.ant.types.Assertions;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.CommandlineJava;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.Environment;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Permissions;
import org.apache.tools.ant.types.PropertySet;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.LoaderUtils;
import org.apache.tools.ant.util.SplitClassLoader;
import org.apache.tools.ant.util.StringUtils;

/**
 * Runs JUnit tests.
 *
 * <p>JUnit is a framework to create unit tests. It has been initially
 * created by Erich Gamma and Kent Beck.  JUnit can be found at <a
 * href="https://www.junit.org">https://www.junit.org</a>.
 *
 * <p><code>JUnitTask</code> can run a single specific
 * <code>JUnitTest</code> using the <code>test</code> element.</p>
 * For example, the following target <pre>
 *   &lt;target name="test-int-chars" depends="jar-test"&gt;
 *       &lt;echo message="testing international characters"/&gt;
 *       &lt;junit printsummary="no" haltonfailure="yes" fork="false"&gt;
 *           &lt;classpath refid="classpath"/&gt;
 *           &lt;formatter type="plain" usefile="false" /&gt;
 *           &lt;test name="org.apache.ecs.InternationalCharTest" /&gt;
 *       &lt;/junit&gt;
 *   &lt;/target&gt;
 * </pre>
 * <p>runs a single junit test
 * (<code>org.apache.ecs.InternationalCharTest</code>) in the current
 * VM using the path with id <code>classpath</code> as classpath and
 * presents the results formatted using the standard
 * <code>plain</code> formatter on the command line.</p>
 *
 * <p>This task can also run batches of tests.  The
 * <code>batchtest</code> element creates a <code>BatchTest</code>
 * based on a fileset.  This allows, for example, all classes found in
 * directory to be run as testcases.</p>
 *
 * <p>For example,</p><pre>
 * &lt;target name="run-tests" depends="dump-info,compile-tests" if="junit.present"&gt;
 *   &lt;junit printsummary="no" haltonfailure="yes" fork="${junit.fork}"&gt;
 *     &lt;jvmarg value="-classic"/&gt;
 *     &lt;classpath refid="tests-classpath"/&gt;
 *     &lt;sysproperty key="build.tests.value" value="${build.tests.value}"/&gt;
 *     &lt;formatter type="brief" usefile="false" /&gt;
 *     &lt;batchtest&gt;
 *       &lt;fileset dir="${tests.dir}"&gt;
 *         &lt;include name="**&#047;*Test*" /&gt;
 *       &lt;/fileset&gt;
 *     &lt;/batchtest&gt;
 *   &lt;/junit&gt;
 * &lt;/target&gt;
 * </pre>
 * <p>this target finds any classes with a <code>test</code> directory
 * anywhere in their path (under the top <code>${tests.dir}</code>, of
 * course) and creates <code>JUnitTest</code>'s for each one.</p>
 *
 * <p>Of course, <code>&lt;junit&gt;</code> and
 * <code>&lt;batch&gt;</code> elements can be combined for more
 * complex tests. For an example, see the ant <code>build.xml</code>
 * target <code>run-tests</code> (the second example is an edited
 * version).</p>
 *
 * <p>To spawn a new Java VM to prevent interferences between
 * different testcases, you need to enable <code>fork</code>.  A
 * number of attributes and elements allow you to set up how this JVM
 * runs.
 *
 *
 * @since Ant 1.2
 *
 * @see JUnitTest
 * @see BatchTest
 */
public class JUnitTask extends Task {

    private static final String CLASSPATH = "CLASSPATH";

    private static final int STRING_BUFFER_SIZE = 128;
    /**
     * @since Ant 1.7
     */
    public static final String TESTLISTENER_PREFIX =
        "junit.framework.TestListener: ";

    /**
     * Name of magic property that enables test listener events.
     */
    public static final String ENABLE_TESTLISTENER_EVENTS =
        "ant.junit.enabletestlistenerevents";

    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    private CommandlineJava commandline;
    private final List<JUnitTest> tests = new Vector<>();
    private final List<BatchTest> batchTests = new Vector<>();
    private final Vector<FormatterElement> formatters = new Vector<>();
    private File dir = null;

    private Integer timeout = null;
    private boolean summary = false;
    private boolean reloading = true;
    private String summaryValue = "";
    private JUnitTaskMirror.JUnitTestRunnerMirror runner = null;

    private boolean newEnvironment = false;
    private final Environment env = new Environment();

    private boolean includeAntRuntime = true;
    private Path antRuntimeClasses = null;

    // Do we send output to System.out/.err in addition to the formatters?
    private boolean showOutput = false;

    // Do we send output to the formatters ?
    private boolean outputToFormatters = true;

    private boolean logFailedTests = true;

    private File tmpDir;
    private AntClassLoader classLoader = null;
    private Permissions perm = null;
    private ForkMode forkMode = new ForkMode("perTest");

    private boolean splitJUnit = false;
    private boolean enableTestListenerEvents = false;
    private JUnitTaskMirror delegate;
    private ClassLoader mirrorLoader;

    /** A boolean on whether to get the forked path for ant classes */
    private boolean forkedPathChecked = false;

    /* set when a test fails/errs with haltonfailure/haltonerror and >1 thread to stop other threads */
    private volatile BuildException caughtBuildException = null;

    //   Attributes for basetest
    private boolean haltOnError = false;
    private boolean haltOnFail  = false;
    private boolean filterTrace = true;
    private boolean fork        = false;
    private int     threads     = 1;
    private String  failureProperty;
    private String  errorProperty;

    /**
     * If true, force ant to re-classload all classes for each JUnit TestCase
     *
     * @param value force class reloading for each test case
     */
    public void setReloading(final boolean value) {
        reloading = value;
    }

    /**
     * If true, smartly filter the stack frames of
     * JUnit errors and failures before reporting them.
     *
     * <p>This property is applied on all BatchTest (batchtest) and
     * JUnitTest (test) however it can possibly be overridden by their
     * own properties.</p>
     * @param value <code>false</code> if it should not filter, otherwise
     * <code>true</code>
     *
     * @since Ant 1.5
     */
    public void setFiltertrace(final boolean value) {
        this.filterTrace = value;
    }

    /**
     * If true, stop the build process when there is an error in a test.
     * This property is applied on all BatchTest (batchtest) and JUnitTest
     * (test) however it can possibly be overridden by their own
     * properties.
     * @param value <code>true</code> if it should halt, otherwise
     * <code>false</code>
     *
     * @since Ant 1.2
     */
    public void setHaltonerror(final boolean value) {
        this.haltOnError = value;
    }

    /**
     * Property to set to "true" if there is a error in a test.
     *
     * <p>This property is applied on all BatchTest (batchtest) and
     * JUnitTest (test), however, it can possibly be overridden by
     * their own properties.</p>
     * @param propertyName the name of the property to set in the
     * event of an error.
     *
     * @since Ant 1.4
     */
    public void setErrorProperty(final String propertyName) {
        this.errorProperty = propertyName;
    }

    /**
     * If true, stop the build process if a test fails
     * (errors are considered failures as well).
     * This property is applied on all BatchTest (batchtest) and
     * JUnitTest (test) however it can possibly be overridden by their
     * own properties.
     * @param value <code>true</code> if it should halt, otherwise
     * <code>false</code>
     *
     * @since Ant 1.2
     */
    public void setHaltonfailure(final boolean value) {
        this.haltOnFail = value;
    }

    /**
     * Property to set to "true" if there is a failure in a test.
     *
     * <p>This property is applied on all BatchTest (batchtest) and
     * JUnitTest (test), however, it can possibly be overridden by
     * their own properties.</p>
     * @param propertyName the name of the property to set in the
     * event of an failure.
     *
     * @since Ant 1.4
     */
    public void setFailureProperty(final String propertyName) {
        this.failureProperty = propertyName;
    }

    /**
     * If true, JVM should be forked for each test.
     *
     * <p>It avoids interference between testcases and possibly avoids
     * hanging the build.  this property is applied on all BatchTest
     * (batchtest) and JUnitTest (test) however it can possibly be
     * overridden by their own properties.</p>
     * @param value <code>true</code> if a JVM should be forked, otherwise
     * <code>false</code>
     * @see #setTimeout
     *
     * @since Ant 1.2
     */
    public void setFork(final boolean value) {
        this.fork = value;
    }

    /**
     * Set the behavior when {@link #setFork fork} fork has been enabled.
     *
     * <p>Possible values are "once", "perTest" and "perBatch".  If
     * set to "once", only a single Java VM will be forked for all
     * tests, with "perTest" (the default) each test will run in a
     * fresh Java VM and "perBatch" will run all tests from the same
     * &lt;batchtest&gt; in the same Java VM.</p>
     *
     * <p>This attribute will be ignored if tests run in the same VM
     * as Ant.</p>
     *
     * <p>Only tests with the same configuration of haltonerror,
     * haltonfailure, errorproperty, failureproperty and filtertrace
     * can share a forked Java VM, so even if you set the value to
     * "once", Ant may need to fork multiple VMs.</p>
     * @param mode the mode to use.
     * @since Ant 1.6.2
     */
    public void setForkMode(final ForkMode mode) {
        this.forkMode = mode;
    }

    /**
     * Set the number of test threads to be used for parallel test
     * execution.  The default is 1, which is the same behavior as
     * before parallel test execution was possible.
     *
     * <p>This attribute will be ignored if tests run in the same VM
     * as Ant.</p>
     *
     * @param threads int
     * @since Ant 1.9.4
     */
    public void setThreads(final int threads) {
        if (threads >= 0) {
            this.threads = threads;
        }
    }

    /**
     * If true, print one-line statistics for each test, or "withOutAndErr"
     * to also show standard output and error.
     *
     * Can take the values on, off, and withOutAndErr.
     *
     * @param value <code>true</code> to print a summary,
     * <code>withOutAndErr</code> to include the test's output as
     * well, <code>false</code> otherwise.
     * @see SummaryJUnitResultFormatter
     * @since Ant 1.2
     */
    public void setPrintsummary(final SummaryAttribute value) {
        summaryValue = value.getValue();
        summary = value.asBoolean();
    }

    /**
     * Print summary enumeration values.
     */
    public static class SummaryAttribute extends EnumeratedAttribute {
        /**
         * list the possible values
         * @return array of allowed values
         */
        @Override
        public String[] getValues() {
            return new String[] {"true", "yes", "false", "no", "on", "off", "withOutAndErr"};
        }

        /**
         * gives the boolean equivalent of the authorized values
         * @return boolean equivalent of the value
         */
        public boolean asBoolean() {
            final String v = getValue();
            return "true".equals(v)
                || "on".equals(v)
                || "yes".equals(v)
                || "withOutAndErr".equals(v);
        }
    }

    /**
     * Set the timeout value (in milliseconds).
     *
     * <p>If the test is running for more than this value, the test
     * will be canceled. (works only when in 'fork' mode).</p>
     *
     * @param value the maximum time (in milliseconds) allowed before
     * declaring the test as 'timed-out'
     * @see #setFork(boolean)
     * @since Ant 1.2
     */
    public void setTimeout(final Integer value) {
        timeout = value;
    }

    /**
     * Set the maximum memory to be used by all forked JVMs.
     *
     * @param   max     the value as defined by <code>-mx</code> or <code>-Xmx</code>
     *                  in the java command line options.
     * @since Ant 1.2
     */
    public void setMaxmemory(final String max) {
        getCommandline().setMaxmemory(max);
    }

    /**
     * The command used to invoke the Java Virtual Machine,
     * default is 'java'. The command is resolved by
     * java.lang.Runtime.exec(). Ignored if fork is disabled.
     *
     * @param   value   the new VM to use instead of <code>java</code>
     * @see #setFork(boolean)
     *
     * @since Ant 1.2
     */
    public void setJvm(final String value) {
        getCommandline().setVm(value);
    }

    /**
     * Adds a JVM argument; ignored if not forking.
     *
     * @return create a new JVM argument so that any argument can be
     * passed to the JVM.
     * @see #setFork(boolean)
     * @since Ant 1.2
     */
    public Commandline.Argument createJvmarg() {
        return getCommandline().createVmArgument();
    }

    /**
     * The directory to invoke the VM in. Ignored if no JVM is forked.
     *
     * @param   dir     the directory to invoke the JVM from.
     * @see #setFork(boolean)
     * @since Ant 1.2
     */
    public void setDir(final File dir) {
        this.dir = dir;
    }

    /**
     * Adds a system property that tests can access.
     * This might be useful to transfer Ant properties to the
     * testcases when JVM forking is not enabled.
     *
     * @since Ant 1.3
     * @deprecated since Ant 1.6
     * @param sysp environment variable to add
     */
    @Deprecated
    public void addSysproperty(final Environment.Variable sysp) {

        getCommandline().addSysproperty(sysp);
    }

    /**
     * Adds a system property that tests can access.
     * This might be useful to transfer Ant properties to the
     * testcases when JVM forking is not enabled.
     *
     * @param sysp new environment variable to add
     * @since Ant 1.6
     */
    public void addConfiguredSysproperty(final Environment.Variable sysp) {
        // get a build exception if there is a missing key or value
        // see bugzilla report 21684
        final String testString = sysp.getContent();
        getProject().log("sysproperty added : " + testString, Project.MSG_DEBUG);
        getCommandline().addSysproperty(sysp);
    }

    /**
     * Adds a set of properties that will be used as system properties
     * that tests can access.
     *
     * <p>This might be useful to transfer Ant properties to the
     * testcases when JVM forking is not enabled.</p>
     *
     * @param sysp set of properties to be added
     * @since Ant 1.6
     */
    public void addSyspropertyset(final PropertySet sysp) {
        getCommandline().addSyspropertyset(sysp);
    }

    /**
     * Adds path to classpath used for tests.
     *
     * @return reference to the classpath in the embedded java command line
     * @since Ant 1.2
     */
    public Path createClasspath() {
        return getCommandline().createClasspath(getProject()).createPath();
    }

    /**
     * Adds a path to the bootclasspath.
     *
     * @return reference to the bootclasspath in the embedded java command line
     * @since Ant 1.6
     */
    public Path createBootclasspath() {
        return getCommandline().createBootclasspath(getProject()).createPath();
    }

    /**
     * Add a path to the modulepath.
     *
     * @return created modulepath.
     * @since 1.9.8
     */
    public Path createModulepath() {
        return getCommandline().createModulepath(getProject()).createPath();
    }

    /**
     * Add a path to the upgrademodulepath.
     *
     * @return created upgrademodulepath.
     * @since 1.9.8
     */
    public Path createUpgrademodulepath() {
        return getCommandline().createUpgrademodulepath(getProject()).createPath();
    }

    /**
     * Adds an environment variable; used when forking.
     *
     * <p>Will be ignored if we are not forking a new VM.</p>
     *
     * @param var environment variable to be added
     * @since Ant 1.5
     */
    public void addEnv(final Environment.Variable var) {
        env.addVariable(var);
    }

    /**
     * If true, use a new environment when forked.
     *
     * <p>Will be ignored if we are not forking a new VM.</p>
     *
     * @param newenv boolean indicating if setting a new environment is wished
     * @since Ant 1.5
     */
    public void setNewenvironment(final boolean newenv) {
        newEnvironment = newenv;
    }

    /**
     * Preset the attributes of the test
     * before configuration in the build
     * script.
     * This allows attributes in the &lt;junit&gt; task
     * be be defaults for the tests, but allows
     * individual tests to override the defaults.
     *
     * @param test BaseTest
     */
    private void preConfigure(final BaseTest test) {
        test.setFiltertrace(filterTrace);
        test.setHaltonerror(haltOnError);
        if (errorProperty != null) {
            test.setErrorProperty(errorProperty);
        }
        test.setHaltonfailure(haltOnFail);
        if (failureProperty != null) {
            test.setFailureProperty(failureProperty);
        }
        test.setFork(fork);
    }

    /**
     * Add a new single testcase.
     * @param   test    a new single testcase
     * @see JUnitTest
     * @since Ant 1.2
     */
    public void addTest(final JUnitTest test) {
        tests.add(test);
        preConfigure(test);
    }

    /**
     * Adds a set of tests based on pattern matching.
     *
     * @return  a new instance of a batch test.
     * @see BatchTest
     * @since Ant 1.2
     */
    public BatchTest createBatchTest() {
        final BatchTest test = new BatchTest(getProject());
        batchTests.add(test);
        preConfigure(test);
        return test;
    }

    /**
     * Add a new formatter to all tests of this task.
     *
     * @param fe formatter element
     * @since Ant 1.2
     */
    public void addFormatter(final FormatterElement fe) {
        formatters.add(fe);
    }

    /**
     * If true, include ant.jar, optional.jar and junit.jar in the forked VM.
     *
     * @param b include ant run time yes or no
     * @since Ant 1.5
     */
    public void setIncludeantruntime(final boolean b) {
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
     * @param showOutput if true, send output to Ant's logging system too
     * @since Ant 1.5
     */
    public void setShowOutput(final boolean showOutput) {
        this.showOutput = showOutput;
    }

    /**
     * If true, send any output generated by tests to the formatters.
     *
     * @param outputToFormatters if true, send output to formatters (Default
     *                           is true).
     * @since Ant 1.7.0
     */
    public void setOutputToFormatters(final boolean outputToFormatters) {
        this.outputToFormatters = outputToFormatters;
    }

    /**
     * If true, write a single "FAILED" line for failed tests to Ant's
     * log system.
     *
     * @param logFailedTests boolean
     * @since Ant 1.8.0
     */
    public void setLogFailedTests(final boolean logFailedTests) {
        this.logFailedTests = logFailedTests;
    }

    /**
     * Assertions to enable in this program (if fork=true)
     *
     * @since Ant 1.6
     * @param asserts assertion set
     */
    public void addAssertions(final Assertions asserts) {
        if (getCommandline().getAssertions() != null) {
            throw new BuildException("Only one assertion declaration is allowed");
        }
        getCommandline().setAssertions(asserts);
    }

    /**
     * Sets the permissions for the application run inside the same JVM.
     *
     * @since Ant 1.6
     * @return Permissions
     */
    public Permissions createPermissions() {
        if (perm == null) {
            perm = new Permissions();
        }
        return perm;
    }

    /**
     * If set, system properties will be copied to the cloned VM - as
     * well as the bootclasspath unless you have explicitly specified
     * a bootclasspath.
     *
     * <p>Doesn't have any effect unless fork is true.</p>
     *
     * @param cloneVm a <code>boolean</code> value.
     * @since Ant 1.7
     */
    public void setCloneVm(final boolean cloneVm) {
        getCommandline().setCloneVm(cloneVm);
    }

    /**
     * Creates a new JUnitRunner and enables fork of a new Java VM.
     *
     * @throws Exception never
     * @since Ant 1.2
     */
    public JUnitTask() throws Exception { //NOSONAR
    }

    /**
     * Where Ant should place temporary files.
     *
     * @param tmpDir location where temporary files should go to
     * @since Ant 1.6
     */
    public void setTempdir(final File tmpDir) {
        if (tmpDir != null && (!tmpDir.exists() || !tmpDir.isDirectory())) {
            throw new BuildException("%s is not a valid temp directory",
                tmpDir);
        }
        this.tmpDir = tmpDir;
    }

    /**
     * Whether test listener events shall be generated.
     *
     * <p>Defaults to false.</p>
     *
     * <p>This value will be overridden by the magic property
     * ant.junit.enabletestlistenerevents if it has been set.</p>
     *
     * @param b boolean
     * @since Ant 1.8.2
     */
    public void setEnableTestListenerEvents(final boolean b) {
        enableTestListenerEvents = b;
    }

    /**
     * Whether test listener events shall be generated.
     *
     * @return boolean
     * @since Ant 1.8.2
     */
    public boolean getEnableTestListenerEvents() {
        final String e = getProject().getProperty(ENABLE_TESTLISTENER_EVENTS);
        if (e != null) {
            return Project.toBoolean(e);
        }
        return enableTestListenerEvents;
    }

    /**
     * Adds the jars or directories containing Ant, this task and
     * JUnit to the classpath - this should make the forked JVM work
     * without having to specify them directly.
     *
     * @since Ant 1.4
     */
    @Override
    public void init() {
        antRuntimeClasses = new Path(getProject());
        splitJUnit = !addClasspathResource("/junit/framework/TestCase.class");
        addClasspathEntry("/org/apache/tools/ant/launch/AntMain.class");
        addClasspathEntry("/org/apache/tools/ant/Task.class");
        addClasspathEntry("/org/apache/tools/ant/taskdefs/optional/junit/JUnitTestRunner.class");
        addClasspathEntry("/org/apache/tools/ant/taskdefs/optional/junit/JUnit4TestMethodAdapter.class");
    }

    private static JUnitTaskMirror createMirror(final JUnitTask task, final ClassLoader loader) {
        try {
            loader.loadClass("junit.framework.Test"); // sanity check
        } catch (final ClassNotFoundException e) {
            throw new BuildException(
                    "The <classpath> or <modulepath> for <junit> must include junit.jar "
                    + "if not in Ant's own classpath",
                    e, task.getLocation());
        }
        try {
            final Class<? extends JUnitTaskMirror> c = loader.loadClass(JUnitTaskMirror.class.getName() + "Impl").asSubclass(JUnitTaskMirror.class);
            if (c.getClassLoader() != loader) {
                throw new BuildException("Overdelegating loader", task.getLocation());
            }
            final Constructor<? extends JUnitTaskMirror> cons = c.getConstructor(JUnitTask.class);
            return cons.newInstance(task);
        } catch (final Exception e) {
            throw new BuildException(e, task.getLocation());
        }
    }

    /**
     * Sets up the delegate that will actually run the tests.
     *
     * <p>Will be invoked implicitly once the delegate is needed.</p>
     *
     * @since Ant 1.7.1
     */
    protected void setupJUnitDelegate() {
        final ClassLoader myLoader = JUnitTask.class.getClassLoader();
        if (splitJUnit) {
            final Path path = new Path(getProject());
            path.add(antRuntimeClasses);
            Path extra = getCommandline().getClasspath();
            if (extra != null) {
                path.add(extra);
            }
            extra = getCommandline().getModulepath();
            if (extra != null && !hasJunit(path)) {
                path.add(expandModulePath(extra));
            }
            mirrorLoader = AccessController.doPrivileged(
                (PrivilegedAction<ClassLoader>) () -> new SplitClassLoader(
                    myLoader, path, getProject(),
                    new String[] {"BriefJUnitResultFormatter", "JUnit4TestMethodAdapter",
                            "JUnitResultFormatter", "JUnitTaskMirrorImpl", "JUnitTestRunner",
                            "JUnitVersionHelper", "OutErrSummaryJUnitResultFormatter",
                            "PlainJUnitResultFormatter", "SummaryJUnitResultFormatter",
                            "TearDownOnVmCrash", "XMLJUnitResultFormatter", "IgnoredTestListener",
                            "IgnoredTestResult", "CustomJUnit4TestAdapterCache",
                            "TestListenerWrapper"}));
        } else {
            mirrorLoader = myLoader;
        }
        delegate = createMirror(this, mirrorLoader);
    }

    /**
     * Runs the testcase.
     *
     * @throws BuildException in case of test failures or errors
     * @since Ant 1.2
     */
    @Override
    public void execute() throws BuildException {
        checkMethodLists();
        checkModules();
        setupJUnitDelegate();

        final List<List<JUnitTest>> testLists = new ArrayList<>();
        /* parallel test execution is only supported for multi-process execution */
        final int threads = !fork || forkMode.getValue().equals(ForkMode.ONCE) ? 1 : this.threads;

        final boolean forkPerTest = ForkMode.PER_TEST.equals(forkMode.getValue());
        if (forkPerTest || ForkMode.ONCE.equals(forkMode.getValue())) {
            testLists.addAll(executeOrQueue(getIndividualTests(),
                                            forkPerTest));
        } else { /* forkMode.getValue().equals(ForkMode.PER_BATCH) */
            batchTests.stream().map(b -> executeOrQueue(b.elements(), false))
                .forEach(testLists::addAll);
            testLists.addAll(
                executeOrQueue(Collections.enumeration(tests), forkPerTest));
        }
        try {
            /* prior to parallel the code in 'oneJunitThread' used to be here. */
            runTestsInThreads(testLists, threads);
        } finally {
            cleanup();
        }
    }

    /*
     * When the list of tests is established, an array of threads is created to pick the
     * tests off the list one at a time and execute them until the list is empty.  Tests are
     * not assigned to threads until the thread is available.
     *
     * This class is the runnable thread subroutine that takes care of passing the shared
     * list iterator and the handle back to the main class to the test execution subroutine
     * code 'runTestsInThreads'.  One object is created for each thread and each one gets
     * a unique thread id that can be useful for tracing test starts and stops.
     *
     * Because the threads are picking tests off the same list, it is the list *iterator*
     * that must be shared, not the list itself - and the iterator must have a thread-safe
     * ability to pop the list - hence the synchronized 'getNextTest'.
     */
    private class JunitTestThread implements Runnable {

        JunitTestThread(final JUnitTask master, final Iterator<List<JUnitTest>> iterator, final int id) {
            this.masterTask = master;
            this.iterator = iterator;
            this.id = id;
        }

        @Override
        public void run() {
            try {
                masterTask.oneJunitThread(iterator, id);
            } catch (final BuildException b) {
                /* saved to rethrow in main thread to be like single-threaded case */
                caughtBuildException = b;
            }
        }

        private final JUnitTask masterTask;
        private final Iterator<List<JUnitTest>> iterator;
        private final int id;
    }

    /*
     * Because the threads are picking tests off the same list, it is the list *iterator*
     * that must be shared, not the list itself - and the iterator must have a thread-safe
     * ability to pop the list - hence the synchronized 'getNextTest'.  We can't have two
     * threads get the same test, or two threads simultaneously pop the list so that a test
     * gets skipped!
     */
    private List<JUnitTest> getNextTest(final Iterator<List<JUnitTest>> iter) {
        synchronized (iter) {
            if (iter.hasNext()) {
                return iter.next();
            }
            return null;
        }
    }

    /*
     * This code loops keeps executing the next test or test bunch (depending on fork mode)
     * on the list of test cases until none are left.  Basically this body of code used to
     * be in the execute routine above; now, several copies (one for each test thread) execute
     * simultaneously.  The while loop was modified to call the new thread-safe atomic list
     * popping subroutine and the logging messages were added.
     *
     * If one thread aborts due to a BuildException (haltOnError, haltOnFailure, or any other
     * fatal reason, no new tests/batches will be started but the running threads will be
     * permitted to complete.  Additional tests may start in already-running batch-test threads.
     */
    private void oneJunitThread(final Iterator<List<JUnitTest>> iter, final int threadId) {

        List<JUnitTest> l;
        log("Starting test thread " + threadId, Project.MSG_VERBOSE);
        while ((caughtBuildException == null) && ((l = getNextTest(iter)) != null)) {
            log("Running test " + l.get(0).toString() + "(" + l.size() + ") in thread " + threadId, Project.MSG_VERBOSE);
            if (l.size() == 1) {
                execute(l.get(0), threadId);
            } else {
                execute(l, threadId);
            }
        }
       log("Ending test thread " + threadId, Project.MSG_VERBOSE);
    }


    private void runTestsInThreads(final List<List<JUnitTest>> testList, final int numThreads) {

        Iterator<List<JUnitTest>> iter = testList.iterator();

        if (numThreads == 1) {
            /* with just one thread just run the test - don't create any threads */
            oneJunitThread(iter, 0);
        } else {
            final Thread[] threads = new Thread[numThreads];
            int i;
            boolean exceptionOccurred;

            /* Need to split apart tests, which are still grouped in batches */
            /* is there a simpler Java mechanism to do this? */
            /* I assume we don't want to do this with "per batch" forking. */
            List<List<JUnitTest>> newlist = new ArrayList<>();
            if (forkMode.getValue().equals(ForkMode.PER_TEST)) {
                for (List<JUnitTest> list : testList) {
                    if (list.size() == 1) {
                        newlist.add(list);
                    } else {
                        for (JUnitTest test : list) {
                            newlist.add(Collections.singletonList(test));
                        }
                    }
                }
            } else {
                newlist = testList;
            }
            iter = newlist.iterator();

            /* create 1 thread using the passthrough class, and let each thread start */
            for (i = 0; i < numThreads; i++) {
                threads[i] = new Thread(new JunitTestThread(this, iter, i + 1));
                threads[i].start();
            }

            /* wait for all of the threads to complete.  Not sure if the exception can actually occur in this use case. */
            do {
                exceptionOccurred = false;

                try {
                    for (i = 0; i < numThreads; i++) {
                        threads[i].join();
                    }
                } catch (final InterruptedException e) {
                    exceptionOccurred = true;
                }
            } while (exceptionOccurred);

           /* an exception occurred in one of the threads - usually a haltOnError/Failure.
              throw the exception again so it behaves like the single-thread case */
           if (caughtBuildException != null) {
               throw new BuildException(caughtBuildException);
           }

            /* all threads are completed - that's all there is to do. */
            /* control will flow back to the test cleanup call and then execute is done. */
        }
    }

    /**
     * Run the tests.
     * @param arg one JUnitTest
     * @param thread Identifies which thread is test running in (0 for single-threaded runs)
     * @throws BuildException in case of test failures or errors
     */
    protected void execute(final JUnitTest arg, final int thread) throws BuildException {
        validateTestName(arg.getName());

        final JUnitTest test = (JUnitTest) arg.clone();
        test.setThread(thread);

        // set the default values if not specified
        //@todo should be moved to the test class instead.
        if (test.getTodir() == null) {
            test.setTodir(getProject().resolveFile("."));
        }

        if (test.getOutfile() == null) {
            test.setOutfile("TEST-" + test.getName());
        }

        // execute the test and get the return code
        TestResultHolder result;
        if (test.getFork()) {
            final ExecuteWatchdog watchdog = createWatchdog();
            result = executeAsForked(test, watchdog, null);
            // null watchdog means no timeout, you'd better not check with null
        } else {
            result = executeInVM(test);
        }
        actOnTestResult(result, test, "Test " + test.getName());
    }

    /**
     * Run the tests.
     * @param arg one JUnitTest
     * @throws BuildException in case of test failures or errors
     */
    protected void execute(final JUnitTest arg) throws BuildException {
        execute(arg, 0);
    }

    /**
     * Throws a <code>BuildException</code> if the given test name is invalid.
     * Validity is defined as not <code>null</code>, not empty, and not the
     * string &quot;null&quot;.
     * @param testName the test name to be validated
     * @throws BuildException if <code>testName</code> is not a valid test name
     */
    private void validateTestName(final String testName) throws BuildException {
        if (testName == null || testName.isEmpty()
            || "null".equals(testName)) {
            throw new BuildException("test name must be specified");
        }
    }

    /**
     * Execute a list of tests in a single forked Java VM.
     * @param testList the list of tests to execute.
     * @param thread Identifies which thread is test running in (0 for single-threaded runs)
     * @throws BuildException on error.
     */
    protected void execute(final List<JUnitTest> testList, final int thread) throws BuildException {
        // Create a temporary file to pass the test cases to run to
        // the runner (one test case per line)
        final File casesFile = createTempPropertiesFile("junittestcases");
        try (BufferedWriter writer =
            new BufferedWriter(new FileWriter(casesFile))) {

            log("Creating casesfile '" + casesFile.getAbsolutePath()
                + "' with content: ", Project.MSG_VERBOSE);
            final PrintStream logWriter =
                new PrintStream(new LogOutputStream(this, Project.MSG_VERBOSE));

            JUnitTest test = null;
            for (JUnitTest t : testList) {
                test = t;
                test.setThread(thread);
                printDual(writer, logWriter, test.getName());
                if (test.getMethods() != null) {
                    printDual(writer, logWriter, ":" + test.getMethodsString().replace(',', '+'));
                }
                if (test.getTodir() == null) {
                    printDual(writer, logWriter,
                              "," + getProject().resolveFile("."));
                } else {
                    printDual(writer, logWriter, "," + test.getTodir());
                }

                if (test.getOutfile() == null) {
                    printlnDual(writer, logWriter,
                                "," + "TEST-" + test.getName());
                } else {
                    printlnDual(writer, logWriter, "," + test.getOutfile());
                }
            }
            writer.flush();

            // execute the test and get the return code
            final ExecuteWatchdog watchdog = createWatchdog();
            final TestResultHolder result =
                executeAsForked(test, watchdog, casesFile);
            actOnTestResult(result, test, "Tests");
        } catch (final IOException e) {
            log(e.toString(), Project.MSG_ERR);
            throw new BuildException(e);
        } finally {
            try {
                FILE_UTILS.tryHardToDelete(casesFile);
            } catch (final Exception e) {
                log(e.toString(), Project.MSG_ERR);
            }
        }
    }

    /**
     * Execute a list of tests in a single forked Java VM.
     * @param testList the list of tests to execute.
     * @throws BuildException on error.
     */
    protected void execute(final List<JUnitTest> testList) throws BuildException {
        execute(testList, 0);
    }

    /**
     * Execute a testcase by forking a new JVM. The command will block
     * until it finishes. To know if the process was destroyed or not
     * or whether the forked Java VM exited abnormally, use the
     * attributes of the returned holder object.
     * @param  test       the testcase to execute.
     * @param  watchdog   the watchdog in charge of cancelling the test if it
     * exceeds a certain amount of time. Can be <code>null</code>, in this case
     * the test could probably hang forever.
     * @param casesFile list of test cases to execute. Can be <code>null</code>,
     * in this case only one test is executed.
     * @return the test results from the JVM itself.
     * @throws BuildException in case of error creating a temporary property file,
     * or if the junit process can not be forked
     */
    private TestResultHolder executeAsForked(JUnitTest test,
                                             final ExecuteWatchdog watchdog,
                                             final File casesFile)
        throws BuildException {

        if (perm != null) {
            log("Permissions ignored when running in forked mode!",
                Project.MSG_WARN);
        }

        CommandlineJava cmd;
        try {
            cmd = (CommandlineJava) getCommandline().clone();
        } catch (final CloneNotSupportedException e) {
            throw new BuildException("This shouldn't happen", e, getLocation());
        }
        if (casesFile == null) {
            cmd.createArgument().setValue(test.getName());
            if (test.getMethods() != null) {
                cmd.createArgument().setValue(Constants.METHOD_NAMES + test.getMethodsString());
            }
        } else {
            log("Running multiple tests in the same VM", Project.MSG_VERBOSE);
            cmd.createArgument().setValue(Constants.TESTSFILE + casesFile);
        }

        cmd.createArgument().setValue(Constants.SKIP_NON_TESTS + test.isSkipNonTests());
        cmd.createArgument().setValue(Constants.FILTERTRACE + test.getFiltertrace());
        cmd.createArgument().setValue(Constants.HALT_ON_ERROR + test.getHaltonerror());
        cmd.createArgument().setValue(Constants.HALT_ON_FAILURE
                                      + test.getHaltonfailure());
        checkIncludeAntRuntime(cmd);

        checkIncludeSummary(cmd);

        cmd.createArgument().setValue(Constants.SHOWOUTPUT
                                      + showOutput);
        cmd.createArgument().setValue(Constants.OUTPUT_TO_FORMATTERS
                                      + outputToFormatters);
        cmd.createArgument().setValue(Constants.LOG_FAILED_TESTS
                                      + logFailedTests);
        cmd.createArgument().setValue(Constants.THREADID
                                      + test.getThread());

        // #31885
        cmd.createArgument().setValue(Constants.LOGTESTLISTENEREVENTS
                                      + getEnableTestListenerEvents());

        StringBuilder formatterArg = new StringBuilder(STRING_BUFFER_SIZE);
        final FormatterElement[] feArray = mergeFormatters(test);
        for (final FormatterElement fe : feArray) {
            if (fe.shouldUse(this)) {
                formatterArg.append(Constants.FORMATTER);
                formatterArg.append(fe.getClassname());
                final File outFile = getOutput(fe, test);
                if (outFile != null) {
                    formatterArg.append(",");
                    formatterArg.append(outFile);
                }
                cmd.createArgument().setValue(formatterArg.toString());
                formatterArg = new StringBuilder();
            }
        }

        final File vmWatcher = createTempPropertiesFile("junitvmwatcher");
        cmd.createArgument().setValue(Constants.CRASHFILE
                                      + vmWatcher.getAbsolutePath());
        final File propsFile = createTempPropertiesFile("junit");
        cmd.createArgument().setValue(Constants.PROPSFILE
                                      + propsFile.getAbsolutePath());
        final Hashtable<String, Object> p = getProject().getProperties();
        final Properties props = new Properties();
        props.putAll(p);
        try {
            final OutputStream outstream = Files.newOutputStream(propsFile.toPath());
            props.store(outstream, "Ant JUnitTask generated properties file");
            outstream.close();
        } catch (final IOException e) {
            FILE_UTILS.tryHardToDelete(propsFile);
            throw new BuildException("Error creating temporary properties "
                                     + "file.", e, getLocation());
        }

        final Execute execute = new Execute(
            new JUnitLogStreamHandler(
                this,
                Project.MSG_INFO,
                Project.MSG_WARN),
            watchdog);
        execute.setCommandline(cmd.getCommandline());
        execute.setAntRun(getProject());
        if (dir != null) {
            execute.setWorkingDirectory(dir);
        }

        final String[] environment = env.getVariables();
        if (environment != null) {
            for (String variable : environment) {
                log("Setting environment variable: " + variable,
                        Project.MSG_VERBOSE);
            }
        }
        execute.setNewenvironment(newEnvironment);
        execute.setEnvironment(environment);

        log(cmd.describeCommand(), Project.MSG_VERBOSE);

        checkForkedPath(cmd);

        final TestResultHolder result = new TestResultHolder();
        boolean success = false;
        try {
            result.exitCode = execute.execute();
            success = true;
        } catch (final IOException e) {
            throw new BuildException("Process fork failed.", e, getLocation());
        } finally {
            String vmCrashString = "unknown";
            BufferedReader br = null;
            try {
                if (vmWatcher.exists()) {
                    br = new BufferedReader(new FileReader(vmWatcher));
                    vmCrashString = br.readLine();
                } else {
                    vmCrashString = "Monitor file ("
                            + vmWatcher.getAbsolutePath()
                            + ") missing, location not writable,"
                            + " testcase not started or mixing ant versions?";
                }
            } catch (final Exception e) {
                log(StringUtils.getStackTrace(e), Project.MSG_INFO);
                // ignored.
            } finally {
                FileUtils.close(br);
                if (vmWatcher.exists()) {
                    FILE_UTILS.tryHardToDelete(vmWatcher);
                }
            }

            final boolean crash = (watchdog != null && watchdog.killedProcess())
                || !Constants.TERMINATED_SUCCESSFULLY.equals(vmCrashString);

            if (casesFile != null && crash) {
                test = createDummyTestForBatchTest(test);
            }

            if (watchdog != null && watchdog.killedProcess()) {
                result.timedOut = true;
                logTimeout(feArray, test, vmCrashString);
            } else if (crash) {
                result.crashed = true;
                logVmCrash(feArray, test, vmCrashString);
            }

            if (!FILE_UTILS.tryHardToDelete(propsFile)) {
                String msg = "Could not delete temporary properties file '"
                    + propsFile.getAbsolutePath() + "'.";
                if (success) {
                    throw new BuildException(msg); //NOSONAR
                }
                // don't hide inner exception
                log(msg, Project.MSG_ERR);
            }
        }

        return result;
    }

    /**
     * Adding ant runtime.
     * @param cmd command to run
     */
    private void checkIncludeAntRuntime(final CommandlineJava cmd) {
        if (includeAntRuntime) {
            final Map<String, String> env = Execute.getEnvironmentVariables();
            final String cp = env.get(CLASSPATH);
            if (cp != null) {
                cmd.createClasspath(getProject()).createPath()
                    .append(new Path(getProject(), cp));
            }
            log("Implicitly adding " + antRuntimeClasses + " to CLASSPATH",
                Project.MSG_VERBOSE);
            cmd.createClasspath(getProject()).createPath()
                .append(antRuntimeClasses);
        }
    }

    /**
     * check for the parameter being "withoutanderr" in a locale-independent way.
     * @param summaryOption the summary option -can be null
     * @return true if the run should be withoutput and error
     */
    private boolean equalsWithOutAndErr(final String summaryOption) {
        return "withoutanderr".equalsIgnoreCase(summaryOption);
    }

    private void checkIncludeSummary(final CommandlineJava cmd) {
        if (summary) {
            String prefix = "";
            if (equalsWithOutAndErr(summaryValue)) {
                prefix = "OutErr";
            }
            cmd.createArgument()
                .setValue(Constants.FORMATTER
                          + "org.apache.tools.ant.taskdefs.optional.junit."
                          + prefix + "SummaryJUnitResultFormatter");
        }
    }

    /**
     * Check the path for multiple different versions of
     * ant.
     * @param cmd command to execute
     */
    private void checkForkedPath(final CommandlineJava cmd) {
        if (forkedPathChecked) {
            return;
        }
        forkedPathChecked = true;
        if (!cmd.haveClasspath()) {
            return;
        }
        try (AntClassLoader loader =
            AntClassLoader.newAntClassLoader(null, getProject(),
                                             cmd.createClasspath(getProject()),
                                             false)) {
            loader.setIsolated(true);
            final String projectResourceName =
                LoaderUtils.classNameToResource(Project.class.getName());
            URL previous = null;
            try {
                for (final URL current : Collections.list(loader.getResources(projectResourceName))) {
                    if (previous != null && !urlEquals(current, previous)) {
                        log(String.format(
                                "WARNING: multiple versions of ant detected in path for junit%n"
                                        + "         %s%n     and %s", previous, current),
                                Project.MSG_WARN);
                        return;
                    }
                    previous = current;
                }
            } catch (final Exception ex) {
                // Ignore exception
            }
        }
    }

    /**
     * Compares URLs for equality but takes case-sensitivity into
     * account when comparing file URLs and ignores the jar specific
     * part of the URL if present.
     */
    private static boolean urlEquals(final URL u1, final URL u2) {
        final String url1 = maybeStripJarAndClass(u1);
        final String url2 = maybeStripJarAndClass(u2);
        if (url1.startsWith("file:") && url2.startsWith("file:")) {
            return new File(FILE_UTILS.fromURI(url1))
                .equals(new File(FILE_UTILS.fromURI(url2)));
        }
        return url1.equals(url2);
    }

    private static String maybeStripJarAndClass(final URL u) {
        String s = u.toString();
        if (s.startsWith("jar:")) {
            final int pling = s.indexOf('!');
            s = s.substring(4, pling == -1 ? s.length() : pling);
        }
        return s;
    }

    /**
     * Create a temporary file to pass the properties to a new process.
     * Will auto-delete on (graceful) exit.
     * The file will be in the project basedir unless tmpDir declares
     * something else.
     * @param prefix String
     * @return created file
     */
    private File createTempPropertiesFile(final String prefix) {
        return FILE_UTILS.createTempFile(getProject(), prefix, ".properties",
            tmpDir != null ? tmpDir : getProject().getBaseDir(), true, true);
    }


    /**
     * Pass output sent to System.out to the TestRunner so it can
     * collect it for the formatters.
     *
     * @param output output coming from System.out
     * @since Ant 1.5
     */
    @Override
    protected void handleOutput(final String output) {
        if (output.startsWith(TESTLISTENER_PREFIX)) {
            log(output, Project.MSG_VERBOSE);
        } else if (runner != null) {
            if (outputToFormatters) {
                runner.handleOutput(output);
            }
            if (showOutput) {
                super.handleOutput(output);
            }
        } else {
            super.handleOutput(output);
        }
    }

    /**
     * Handle an input request by this task.
     * @see Task#handleInput(byte[], int, int)
     * This implementation delegates to a runner if it
     * present.
     * @param buffer the buffer into which data is to be read.
     * @param offset the offset into the buffer at which data is stored.
     * @param length the amount of data to read.
     *
     * @return the number of bytes read.
     * @exception IOException if the data cannot be read.
     *
     * @since Ant 1.6
     */
    @Override
    protected int handleInput(final byte[] buffer, final int offset, final int length)
        throws IOException {
        if (runner != null) {
            return runner.handleInput(buffer, offset, length);
        }
        return super.handleInput(buffer, offset, length);
    }


    /**
     * Pass output sent to System.out to the TestRunner so it can
     * collect ot for the formatters.
     *
     * @param output output coming from System.out
     * @since Ant 1.5.2
     */
    @Override
    protected void handleFlush(final String output) {
        if (runner != null) {
            runner.handleFlush(output);
            if (showOutput) {
                super.handleFlush(output);
            }
        } else {
            super.handleFlush(output);
        }
    }

    /**
     * Pass output sent to System.err to the TestRunner so it can
     * collect it for the formatters.
     *
     * @param output output coming from System.err
     * @since Ant 1.5
     */
    @Override
    public void handleErrorOutput(final String output) {
        if (runner != null) {
            runner.handleErrorOutput(output);
            if (showOutput) {
                super.handleErrorOutput(output);
            }
        } else {
            super.handleErrorOutput(output);
        }
    }


    /**
     * Pass output sent to System.err to the TestRunner so it can
     * collect it for the formatters.
     *
     * @param output coming from System.err
     * @since Ant 1.5.2
     */
    @Override
    public void handleErrorFlush(final String output) {
        if (runner != null) {
            runner.handleErrorFlush(output);
            if (showOutput) {
                super.handleErrorFlush(output);
            }
        } else {
            super.handleErrorFlush(output);
        }
    }

    // in VM is not very nice since it could probably hang the
    // whole build. IMHO this method should be avoided and it would be best
    // to remove it in future versions. TBD. (SBa)

    /**
     * Execute inside VM.
     * @param arg one JUnitTest
     * @throws BuildException under unspecified circumstances
     * @return the results
     */
    private TestResultHolder executeInVM(final JUnitTest arg) throws BuildException {
        if (delegate == null) {
            setupJUnitDelegate();
        }

        final JUnitTest test = (JUnitTest) arg.clone();
        test.setProperties(getProject().getProperties());
        if (dir != null) {
            log("dir attribute ignored if running in the same VM",
                Project.MSG_WARN);
        }

        if (newEnvironment || null != env.getVariables()) {
            log("Changes to environment variables are ignored if running in "
                + "the same VM.", Project.MSG_WARN);
        }

        if (getCommandline().getBootclasspath() != null) {
            log("bootclasspath is ignored if running in the same VM.",
                Project.MSG_WARN);
        }

        final CommandlineJava.SysProperties sysProperties =
                getCommandline().getSystemProperties();
        if (sysProperties != null) {
            sysProperties.setSystem();
        }

        try {
            log("Using System properties " + System.getProperties(),
                Project.MSG_VERBOSE);
            if (splitJUnit) {
                classLoader = (AntClassLoader) delegate.getClass().getClassLoader();
            } else {
                createClassLoader();
            }
            if (classLoader != null) {
                classLoader.setThreadContextLoader();
            }
            runner = delegate.newJUnitTestRunner(test, test.getMethods(), test.getHaltonerror(),
                                         test.getFiltertrace(),
                                         test.getHaltonfailure(), false,
                                         getEnableTestListenerEvents(),
                                         classLoader);
            if (summary) {

                final JUnitTaskMirror.SummaryJUnitResultFormatterMirror f =
                    delegate.newSummaryJUnitResultFormatter();
                f.setWithOutAndErr(equalsWithOutAndErr(summaryValue));
                f.setOutput(getDefaultOutput());
                runner.addFormatter(f);
            }

            runner.setPermissions(perm);

            for (final FormatterElement fe : mergeFormatters(test)) {
                if (fe.shouldUse(this)) {
                    final File outFile = getOutput(fe, test);
                    if (outFile != null) {
                        fe.setOutfile(outFile);
                    } else {
                        fe.setOutput(getDefaultOutput());
                    }
                    runner.addFormatter(fe.createFormatter(classLoader));
                }
            }

            runner.run();
            final TestResultHolder result = new TestResultHolder();
            result.exitCode = runner.getRetCode();
            return result;
        } finally {
            if (sysProperties != null) {
                sysProperties.restoreSystem();
            }
            if (classLoader != null) {
                classLoader.resetThreadContextLoader();
            }
        }
    }

    /**
     * @return <code>null</code> if there is a timeout value, otherwise the
     * watchdog instance.
     *
     * @throws BuildException under unspecified circumstances
     * @since Ant 1.2
     */
    protected ExecuteWatchdog createWatchdog() throws BuildException {
        if (timeout == null) {
            return null;
        }
        return new ExecuteWatchdog((long) timeout);
    }

    /**
     * Get the default output for a formatter.
     *
     * @return default output stream for a formatter
     * @since Ant 1.3
     */
    protected OutputStream getDefaultOutput() {
        return new LogOutputStream(this, Project.MSG_INFO);
    }

    /**
     * Merge all individual tests from the batchtest with all individual tests
     * and return an enumeration over all <code>JUnitTest</code>.
     *
     * @return enumeration over individual tests
     * @since Ant 1.3
     */
    protected Enumeration<JUnitTest> getIndividualTests() {
        return Collections.enumeration(Stream.concat(batchTests.stream()
                .flatMap(b -> Collections.list(b.elements()).stream()), tests.stream())
                .collect(Collectors.toList()));
    }

    /**
     * Verifies all <code>test</code> elements having the <code>methods</code>
     * attribute specified and having the <code>if</code>-condition resolved
     * to true, that the value of the <code>methods</code> attribute is valid.
     * @exception BuildException if some of the tests matching the described
     *                           conditions has invalid value of the
     *                           <code>methods</code> attribute
     * @since 1.8.2
     */
    private void checkMethodLists() throws BuildException {
        if (tests.isEmpty()) {
            return;
        }
        tests.stream().filter(test -> test.hasMethodsSpecified() && test.shouldRun(getProject()))
                .forEach(JUnitTest::resolveMethods);
    }

    /**
     * Checks a validity of module specific options.
     * @since 1.9.8
     */
    private void checkModules() {
        if (hasPath(getCommandline().getModulepath())
                || hasPath(getCommandline().getUpgrademodulepath())) {
            if (!batchTests.stream().allMatch(BaseTest::getFork)
                    || !tests.stream().allMatch(BaseTest::getFork)) {
                throw new BuildException(
                    "The module path requires fork attribute to be set to true.");
            }
        }
    }

    /**
     * Checks is a junit is on given path.
     * @param path the {@link Path} to check
     * @return true when given {@link Path} contains junit
     * @since 1.9.8
     */
    private boolean hasJunit(final Path path) {
        try (AntClassLoader loader = AntClassLoader.newAntClassLoader(
                null,
                getProject(),
                path,
                true)) {
            try {
                loader.loadClass("junit.framework.Test");
                return true;
            } catch (final Exception ex) {
                return false;
            }
        }
    }

    /**
     * Expands a module path to flat path of jars and root folders usable by classloader.
     * @param modulePath to be expanded
     * @return the expanded path
     * @since 1.9.8
     */
    private Path expandModulePath(Path modulePath) {
        final Path expanded = new Path(getProject());
        for (String path : modulePath.list()) {
            final File modulePathEntry = getProject().resolveFile(path);
            if (modulePathEntry.isDirectory() && !hasModuleInfo(modulePathEntry)) {
                final File[] modules = modulePathEntry.listFiles((dir,
                    name) -> name.toLowerCase(Locale.ENGLISH).endsWith(".jar"));
                if (modules != null) {
                    for (File module : modules) {
                        expanded.add(new Path(getProject(), String.format(
                                "%s%s%s",   //NOI18N
                                path,
                                File.separator,
                                module.getName())));
                    }
                }
            } else {
                expanded.add(new Path(getProject(), path));
            }
        }
        return expanded;
    }

    /**
     * return an enumeration listing each test, then each batchtest
     * @return enumeration
     * @since Ant 1.3
     */
    protected Enumeration<BaseTest> allTests() {
        return Collections.enumeration(Stream.concat(tests.stream(), batchTests.stream())
                .collect(Collectors.toList()));
    }

    /**
     * @param test junit test
     * @return array of FormatterElement
     * @since Ant 1.3
     */
    private FormatterElement[] mergeFormatters(final JUnitTest test) {
        @SuppressWarnings("unchecked")
        final Vector<FormatterElement> feVector = (Vector<FormatterElement>) formatters.clone();
        test.addFormattersTo(feVector);
        final FormatterElement[] feArray = new FormatterElement[feVector.size()];
        feVector.copyInto(feArray);
        return feArray;
    }

    /**
     * If the formatter sends output to a file, return that file.
     * null otherwise.
     * @param fe  formatter element
     * @param test one JUnit test
     * @return file reference
     * @since Ant 1.3
     */
    protected File getOutput(final FormatterElement fe, final JUnitTest test) {
        if (fe.getUseFile()) {
            String base = test.getOutfile();
            if (base == null) {
                base = JUnitTaskMirror.JUnitTestRunnerMirror.IGNORED_FILE_NAME;
            }
            final String filename = base + fe.getExtension();
            final File destFile = new File(test.getTodir(), filename);
            final String absFilename = destFile.getAbsolutePath();
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
     * @param resource resource that one wants to lookup
     * @since Ant 1.4
     */
    protected void addClasspathEntry(final String resource) {
        addClasspathResource(resource);
    }

    /**
     * Implementation of addClasspathEntry.
     *
     * @param resource resource that one wants to lookup
     * @return true if something was in fact added
     * @since Ant 1.7.1
     */
    private boolean addClasspathResource(String resource) {
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

        final File f = LoaderUtils.getResourceSource(JUnitTask.class.getClassLoader(),
                                               resource);
        if (f != null) {
            log("Found " + f.getAbsolutePath(), Project.MSG_DEBUG);
            antRuntimeClasses.createPath().setLocation(f);
            return true;
        } else {
            log("Couldn't find " + resource, Project.MSG_DEBUG);
            return false;
        }
    }

    static final String TIMEOUT_MESSAGE =
        "Timeout occurred. Please note the time in the report does"
        + " not reflect the time until the timeout.";

    /**
     * Take care that some output is produced in report files if the
     * watchdog kills the test.
     *
     * @since Ant 1.5.2
     */
    private void logTimeout(final FormatterElement[] feArray, final JUnitTest test,
                            final String testCase) {
        logVmExit(feArray, test, TIMEOUT_MESSAGE, testCase);
    }

    /**
     * Take care that some output is produced in report files if the
     * forked machine exited before the test suite finished but the
     * reason is not a timeout.
     *
     * @since Ant 1.7
     */
    private void logVmCrash(final FormatterElement[] feArray, final JUnitTest test, final String testCase) {
        logVmExit(
            feArray, test,
            "Forked Java VM exited abnormally. Please note the time in the report"
            + " does not reflect the time until the VM exit.",
            testCase);
    }

    /**
     * Take care that some output is produced in report files if the
     * forked machine terminated before the test suite finished
     *
     * @since Ant 1.7
     */
    private void logVmExit(final FormatterElement[] feArray, final JUnitTest test,
                           final String message, final String testCase) {
        if (delegate == null) {
            setupJUnitDelegate();
        }

        try {
            log("Using System properties " + System.getProperties(),
                Project.MSG_VERBOSE);
            if (splitJUnit) {
                classLoader = (AntClassLoader) delegate.getClass().getClassLoader();
            } else {
                createClassLoader();
            }
            if (classLoader != null) {
                classLoader.setThreadContextLoader();
            }

            test.setCounts(1, 0, 1, 0);
            test.setProperties(getProject().getProperties());
            for (final FormatterElement fe : feArray) {
                if (fe.shouldUse(this)) {
                    final JUnitTaskMirror.JUnitResultFormatterMirror formatter =
                        fe.createFormatter(classLoader);
                    if (formatter != null) {
                        OutputStream out = null;
                        final File outFile = getOutput(fe, test);
                        if (outFile != null) {
                            try {
                                out = Files.newOutputStream(outFile.toPath());
                            } catch (final IOException e) {
                                // ignore
                            }
                        }
                        if (out == null) {
                            out = getDefaultOutput();
                        }
                        delegate.addVmExit(test, formatter, out, message,
                                           testCase);
                    }
                }
            }
            if (summary) {
                final JUnitTaskMirror.SummaryJUnitResultFormatterMirror f =
                    delegate.newSummaryJUnitResultFormatter();
                f.setWithOutAndErr(equalsWithOutAndErr(summaryValue));
                delegate.addVmExit(test, f, getDefaultOutput(), message, testCase);
            }
        } finally {
            if (classLoader != null) {
                classLoader.resetThreadContextLoader();
            }
        }
    }

    /**
     * Creates and configures an AntClassLoader instance from the
     * nested classpath element.
     *
     * @since Ant 1.6
     */
    private void createClassLoader() {
        final Path userClasspath = getCommandline().getClasspath();
        final Path userModulepath = getCommandline().getModulepath();
        if (userClasspath != null || userModulepath != null) {
            if (reloading || classLoader == null) {
                deleteClassLoader();
                final Path path = new Path(getProject());
                if (userClasspath != null) {
                    path.add((Path) userClasspath.clone());
                }
                if (userModulepath != null && !hasJunit(path)) {
                    path.add(expandModulePath(userModulepath));
                }
                if (includeAntRuntime) {
                    log("Implicitly adding " + antRuntimeClasses
                        + " to CLASSPATH", Project.MSG_VERBOSE);
                    path.append(antRuntimeClasses);
                }
                classLoader = getProject().createClassLoader(path);
                if (getClass().getClassLoader() != null
                    && getClass().getClassLoader() != Project.class.getClassLoader()) {
                    classLoader.setParent(getClass().getClassLoader());
                }
                classLoader.setParentFirst(false);
                classLoader.addJavaLibraries();
                log("Using CLASSPATH " + classLoader.getClasspath(),
                    Project.MSG_VERBOSE);
                // make sure the test will be accepted as a TestCase
                classLoader.addSystemPackageRoot("junit");
                // make sure the test annotation are accepted
                classLoader.addSystemPackageRoot("org.junit");
                // will cause trouble in JDK 1.1 if omitted
                classLoader.addSystemPackageRoot(MagicNames.ANT_CORE_PACKAGE);
            }
        }
    }

    /**
     * Removes resources.
     *
     * <p>Is invoked in {@link #execute execute}.  Subclasses that
     * don't invoke execute should invoke this method in a finally
     * block.</p>
     *
     * @since Ant 1.7.1
     */
    protected void cleanup() {
        deleteClassLoader();
        delegate = null;
    }

    /**
     * Removes a classloader if needed.
     * @since Ant 1.7
     */
    private void deleteClassLoader() {
        if (classLoader != null) {
            classLoader.cleanup();
            classLoader = null;
        }
        if (mirrorLoader instanceof SplitClassLoader) {
            ((SplitClassLoader) mirrorLoader).cleanup();
        }
        mirrorLoader = null;
    }

    /**
     * Get the command line used to run the tests.
     * @return the command line.
     * @since Ant 1.6.2
     */
    protected CommandlineJava getCommandline() {
        if (commandline == null) {
            commandline = new CommandlineJava();
            commandline.setClassname("org.apache.tools.ant.taskdefs.optional.junit.JUnitTestRunner");
        }
        return commandline;
    }

    /**
     * Forked test support
     * @since Ant 1.6.2
     */
    private static final class ForkedTestConfiguration {
        private final boolean filterTrace;
        private final boolean haltOnError;
        private final boolean haltOnFailure;
        private final String errorProperty;
        private final String failureProperty;

        /**
         * constructor for forked test configuration
         * @param filterTrace boolean
         * @param haltOnError boolean
         * @param haltOnFailure boolean
         * @param errorProperty String
         * @param failureProperty String
         */
        ForkedTestConfiguration(final boolean filterTrace, final boolean haltOnError,
                                final boolean haltOnFailure, final String errorProperty,
                                final String failureProperty) {
            this.filterTrace = filterTrace;
            this.haltOnError = haltOnError;
            this.haltOnFailure = haltOnFailure;
            this.errorProperty = errorProperty;
            this.failureProperty = failureProperty;
        }

        /**
         * configure from a test; sets member variables to attributes of the test
         * @param test JUnitTest
         */
        ForkedTestConfiguration(final JUnitTest test) {
            this(test.getFiltertrace(),
                    test.getHaltonerror(),
                    test.getHaltonfailure(),
                    test.getErrorProperty(),
                    test.getFailureProperty());
        }

        /**
         * equality test checks all the member variables
         * @param other object to compare
         * @return true if everything is equal
         */
        @Override
        public boolean equals(final Object other) {
            if (other == null
                || other.getClass() != ForkedTestConfiguration.class) {
                return false;
            }
            final ForkedTestConfiguration o = (ForkedTestConfiguration) other;
            return filterTrace == o.filterTrace
                && haltOnError == o.haltOnError
                && haltOnFailure == o.haltOnFailure
                && ((errorProperty == null && o.errorProperty == null)
                    ||
                    (errorProperty != null
                     && errorProperty.equals(o.errorProperty)))
                && ((failureProperty == null && o.failureProperty == null)
                    ||
                    (failureProperty != null
                     && failureProperty.equals(o.failureProperty)));
        }

        /**
         * hashcode is based only on the boolean members, and returns a value
         * in the range 0-7.
         * @return hash code value
         */
        @Override
        public int hashCode() {
            // CheckStyle:MagicNumber OFF
            return (filterTrace ? 1 : 0)
                + (haltOnError ? 2 : 0)
                + (haltOnFailure ? 4 : 0);
            // CheckStyle:MagicNumber ON
        }
    }

    /**
     * These are the different forking options
     * @since 1.6.2
     */
    public static final class ForkMode extends EnumeratedAttribute {

        /**
         * fork once only
         */
        public static final String ONCE = "once";
        /**
         * fork once per test class
         */
        public static final String PER_TEST = "perTest";
        /**
         * fork once per batch of tests
         */
        public static final String PER_BATCH = "perBatch";

        /** No arg constructor. */
        public ForkMode() {
            super();
        }

        /**
         * Constructor using a value.
         * @param value the value to use - once, perTest or perBatch.
         */
        public ForkMode(final String value) {
            super();
            setValue(value);
        }

        /** {@inheritDoc}. */
        @Override
        public String[] getValues() {
            return new String[] {ONCE, PER_TEST, PER_BATCH};
        }
    }

    /**
     * Executes all tests that don't need to be forked (or all tests
     * if the runIndividual argument is true.  Returns a collection of
     * lists of tests that share the same VM configuration and haven't
     * been executed yet.
     * @param testList the list of tests to be executed or queued.
     * @param runIndividual if true execute each test individually.
     * @return a list of tasks to be executed.
     * @since 1.6.2
     */
    protected Collection<List<JUnitTest>> executeOrQueue(
        final Enumeration<JUnitTest> testList, final boolean runIndividual) {
        final Map<ForkedTestConfiguration, List<JUnitTest>> testConfigurations =
            new HashMap<>();
        for (final JUnitTest test : Collections.list(testList)) {
            if (test.shouldRun(getProject())) {
                /* with multi-threaded runs need to defer execution of even */
                /* individual tests so the threads can pick tests off the queue. */
                if ((runIndividual || !test.getFork()) && threads == 1) {
                    execute(test, 0);
                } else {
                    testConfigurations
                        .computeIfAbsent(new ForkedTestConfiguration(test),
                            k -> new ArrayList<>())
                        .add(test);
                }
            }
        }
        return testConfigurations.values();
    }

    /**
     * Logs information about failed tests, potentially stops
     * processing (by throwing a BuildException) if a failure/error
     * occurred or sets a property.
     * @param exitValue the exitValue of the test.
     * @param wasKilled if true, the test had been killed.
     * @param test      the test in question.
     * @param name      the name of the test.
     * @since Ant 1.6.2
     */
    protected void actOnTestResult(final int exitValue, final boolean wasKilled,
                                   final JUnitTest test, final String name) {
        final TestResultHolder t = new TestResultHolder();
        t.exitCode = exitValue;
        t.timedOut = wasKilled;
        actOnTestResult(t, test, name);
    }

    /**
     * Logs information about failed tests, potentially stops
     * processing (by throwing a BuildException) if a failure/error
     * occurred or sets a property.
     * @param result    the result of the test.
     * @param test      the test in question.
     * @param name      the name of the test.
     * @since Ant 1.7
     */
    protected void actOnTestResult(final TestResultHolder result, final JUnitTest test,
                                   final String name) {
        // if there is an error/failure and that it should halt, stop
        // everything otherwise just log a statement
        final boolean fatal = result.timedOut || result.crashed;
        final boolean errorOccurredHere =
            result.exitCode == JUnitTaskMirror.JUnitTestRunnerMirror.ERRORS || fatal;
        final boolean failureOccurredHere =
            result.exitCode != JUnitTaskMirror.JUnitTestRunnerMirror.SUCCESS || fatal;
        if (errorOccurredHere || failureOccurredHere) {
            if ((errorOccurredHere && test.getHaltonerror())
                || (failureOccurredHere && test.getHaltonfailure())) {
                throw new BuildException(name + " failed"
                    + (result.timedOut ? " (timeout)" : "")
                    + (result.crashed ? " (crashed)" : ""), getLocation());
            } else {
                if (logFailedTests) {
                    log(name + " FAILED"
                        + (result.timedOut ? " (timeout)" : "")
                        + (result.crashed ? " (crashed)" : ""),
                        Project.MSG_ERR);
                }
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
     * A value class that contains the result of a test.
     */
    protected static class TestResultHolder {
        // CheckStyle:VisibilityModifier OFF - bc
        /** the exit code of the test. */
        public int exitCode = JUnitTaskMirror.JUnitTestRunnerMirror.ERRORS;
        /** true if the test timed out */
        public boolean timedOut = false;
        /** true if the test crashed */
        public boolean crashed = false;
        // CheckStyle:VisibilityModifier ON
    }

    /**
     * A stream handler for handling the junit task.
     * @since Ant 1.7
     */
    protected static class JUnitLogOutputStream extends LogOutputStream {
        private final Task task; // local copy since LogOutputStream.task is private

        /**
         * Constructor.
         * @param task the task being logged.
         * @param level the log level used to log data written to this stream.
         */
        public JUnitLogOutputStream(final Task task, final int level) {
            super(task, level);
            this.task = task;
        }

        /**
         * Logs a line.
         * If the line starts with junit.framework.TestListener: set the level
         * to MSG_VERBOSE.
         * @param line the line to log.
         * @param level the logging level to use.
         */
        @Override
        protected void processLine(final String line, final int level) {
            if (line.startsWith(TESTLISTENER_PREFIX)) {
                task.log(line, Project.MSG_VERBOSE);
            } else {
                super.processLine(line, level);
            }
        }
    }

    /**
     * A log stream handler for junit.
     * @since Ant 1.7
     */
    protected static class JUnitLogStreamHandler extends PumpStreamHandler {
        /**
         * Constructor.
         * @param task the task to log.
         * @param outlevel the level to use for standard output.
         * @param errlevel the level to use for error output.
         */
        public JUnitLogStreamHandler(final Task task, final int outlevel, final int errlevel) {
            super(new JUnitLogOutputStream(task, outlevel),
                  new LogOutputStream(task, errlevel));
        }
    }

    static final String NAME_OF_DUMMY_TEST = "Batch-With-Multiple-Tests";

    /**
     * Creates a JUnitTest instance that shares all flags with the
     * passed in instance but has a more meaningful name.
     *
     * <p>If a VM running multiple tests crashes, we don't know which
     * test failed.  Prior to Ant 1.8.0 Ant would log the error with
     * the last test of the batch test, which caused some confusion
     * since the log might look as if a test had been executed last
     * that was never started.  With Ant 1.8.0 the test's name will
     * indicate that something went wrong with a test inside the batch
     * without giving it a real name.</p>
     *
     * @see "https://issues.apache.org/bugzilla/show_bug.cgi?id=45227"
     */
    private static JUnitTest createDummyTestForBatchTest(final JUnitTest test) {
        final JUnitTest t = (JUnitTest) test.clone();
        final int index = test.getName().lastIndexOf('.');
        // make sure test looks as if it was in the same "package" as
        // the last test of the batch
        final String pack = index > 0 ? test.getName().substring(0, index + 1) : "";
        t.setName(pack + NAME_OF_DUMMY_TEST);
        return t;
    }

    private static void printDual(final BufferedWriter w, final PrintStream s, final String text)
        throws IOException {
        w.write(String.valueOf(text));
        s.print(text);
    }

    private static void printlnDual(final BufferedWriter w, final PrintStream s, final String text)
        throws IOException {
        w.write(String.valueOf(text));
        w.newLine();
        s.println(text);
    }

    /**
     * Checks if a path exists and is non empty.
     * @param path to be checked
     * @return true if the path is non <code>null</code> and non empty.
     * @since 1.9.8
     */
    private static boolean hasPath(final Path path) {
        return path != null && path.size() > 0;
    }

    /**
     * Checks if a given folder is an unpacked module.
     * @param root the folder to be checked
     * @return true if the root is an unpacked module
     * @since 1.9.8
     */
    private static boolean hasModuleInfo(final File root) {
        return new File(root, "module-info.class").exists();    //NOI18N
    }
}
