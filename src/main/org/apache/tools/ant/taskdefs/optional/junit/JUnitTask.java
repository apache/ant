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
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
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

/**
 * Runs JUnit tests.
 *
 * <p> JUnit is a framework to create unit tests. It has been initially
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
 *
 *
 * @since Ant 1.2
 *
 * @see JUnitTest
 * @see BatchTest
 */
public class JUnitTask extends Task {

    private static final String CLASSPATH = "CLASSPATH=";
    private CommandlineJava commandline;
    private Vector tests = new Vector();
    private Vector batchTests = new Vector();
    private Vector formatters = new Vector();
    private File dir = null;

    private Integer timeout = null;
    private boolean summary = false;
    private boolean reloading = true;
    private String summaryValue = "";
    private JUnitTaskMirror.JUnitTestRunnerMirror runner = null;

    private boolean newEnvironment = false;
    private Environment env = new Environment();

    private boolean includeAntRuntime = true;
    private Path antRuntimeClasses = null;

    // Do we send output to System.out/.err in addition to the formatters?
    private boolean showOutput = false;

    // Do we send output to the formatters ?
    private boolean outputToFormatters = true;

    private File tmpDir;
    private AntClassLoader classLoader = null;
    private Permissions perm = null;
    private ForkMode forkMode = new ForkMode("perTest");

    private boolean splitJunit = false;
    private JUnitTaskMirror delegate;

    //   Attributes for basetest
    private boolean haltOnError = false;
    private boolean haltOnFail  = false;
    private boolean filterTrace = true;
    private boolean fork        = false;
    private String  failureProperty;
    private String  errorProperty;

    private static final int STRING_BUFFER_SIZE = 128;
    /**
     * @since Ant 1.7
     */
    public static final String TESTLISTENER_PREFIX =
        "junit.framework.TestListener: ";

    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    /**
     * If true, force ant to re-classload all classes for each JUnit TestCase
     *
     * @param value force class reloading for each test case
     */
    public void setReloading(boolean value) {
        reloading = value;
    }

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
        this.filterTrace = value;
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
        this.haltOnError = value;
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
        this.errorProperty = propertyName;
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
        this.haltOnFail = value;
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
        this.failureProperty = propertyName;
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
     * "once", Ant may need to fork mutliple VMs.</p>
     * @param mode the mode to use.
     * @since Ant 1.6.2
     */
    public void setForkMode(ForkMode mode) {
        this.forkMode = mode;
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
        /**
         * list the possible values
         * @return  array of allowed values
         */
        public String[] getValues() {
            return new String[] {"true", "yes", "false", "no",
                                 "on", "off", "withOutAndErr"};
        }

        /**
         * gives the boolean equivalent of the authorized values
         * @return boolean equivalent of the value
         */
        public boolean asBoolean() {
            String v = getValue();
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
        getCommandline().setMaxmemory(max);
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
        getCommandline().setVm(value);
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
        return getCommandline().createVmArgument();
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
     * @deprecated since ant 1.6
     * @param sysp environment variable to add
     */
    public void addSysproperty(Environment.Variable sysp) {

        getCommandline().addSysproperty(sysp);
    }

    /**
     * Adds a system property that tests can access.
     * This might be useful to tranfer Ant properties to the
     * testcases when JVM forking is not enabled.
     * @param sysp new environment variable to add
     * @since Ant 1.6
     */
    public void addConfiguredSysproperty(Environment.Variable sysp) {
        // get a build exception if there is a missing key or value
        // see bugzilla report 21684
        String testString = sysp.getContent();
        getProject().log("sysproperty added : " + testString, Project.MSG_DEBUG);
        getCommandline().addSysproperty(sysp);
    }

    /**
     * Adds a set of properties that will be used as system properties
     * that tests can access.
     *
     * This might be useful to tranfer Ant properties to the
     * testcases when JVM forking is not enabled.
     *
     * @param sysp set of properties to be added
     * @since Ant 1.6
     */
    public void addSyspropertyset(PropertySet sysp) {
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
     * @return reference to the bootclasspath in the embedded java command line
     * @since Ant 1.6
     */
    public Path createBootclasspath() {
        return getCommandline().createBootclasspath(getProject()).createPath();
    }

    /**
     * Adds an environment variable; used when forking.
     *
     * <p>Will be ignored if we are not forking a new VM.</p>
     * @param var environment variable to be added
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
     * @param newenv boolean indicating if setting a new environment is wished
     * @since Ant 1.5
     */
    public void setNewenvironment(boolean newenv) {
        newEnvironment = newenv;
    }

    /**
     * Preset the attributes of the test
     * before configuration in the build
     * script.
     * This allows attributes in the <junit> task
     * be be defaults for the tests, but allows
     * individual tests to override the defaults.
     */
    private void preConfigure(BaseTest test) {
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
     *
     * @since Ant 1.2
     */
    public void addTest(JUnitTest test) {
        tests.addElement(test);
        preConfigure(test);
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
        preConfigure(test);
        return test;
    }

    /**
     * Add a new formatter to all tests of this task.
     *
     * @param fe formatter element
     * @since Ant 1.2
     */
    public void addFormatter(FormatterElement fe) {
        formatters.addElement(fe);
    }

    /**
     * If true, include ant.jar, optional.jar and junit.jar in the forked VM.
     *
     * @param b include ant run time yes or no
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
     * @param showOutput if true, send output to Ant's logging system too
     * @since Ant 1.5
     */
    public void setShowOutput(boolean showOutput) {
        this.showOutput = showOutput;
    }

    /**
     * If true, send any output generated by tests to the formatters.
     *
     * @param outputToFormatters if true, send output to formatters (Default
     *                           is true).
     * @since Ant 1.7.0
     */
    public void setOutputToFormatters(boolean outputToFormatters) {
        this.outputToFormatters = outputToFormatters;
    }

    /**
     * Assertions to enable in this program (if fork=true)
     * @since Ant 1.6
     * @param asserts assertion set
     */
    public void addAssertions(Assertions asserts) {
        if (getCommandline().getAssertions() != null) {
            throw new BuildException("Only one assertion declaration is allowed");
        }
        getCommandline().setAssertions(asserts);
    }

    /**
     * Sets the permissions for the application run inside the same JVM.
     * @since Ant 1.6
     * @return .
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
     * a bootclaspath.
     *
     * <p>Doesn't have any effect unless fork is true.</p>
     * @param cloneVm a <code>boolean</code> value.
     * @since Ant 1.7
     */
    public void setCloneVm(boolean cloneVm) {
        getCommandline().setCloneVm(cloneVm);
    }

    /**
     * Creates a new JUnitRunner and enables fork of a new Java VM.
     *
     * @throws Exception under ??? circumstances
     * @since Ant 1.2
     */
    public JUnitTask() throws Exception {
        getCommandline()
            .setClassname("org.apache.tools.ant.taskdefs.optional.junit.JUnitTestRunner");
    }

    /**
     * Where Ant should place temporary files.
     *
     * @param tmpDir location where temporary files should go to
     * @since Ant 1.6
     */
    public void setTempdir(File tmpDir) {
        if (tmpDir != null) {
            if (!tmpDir.exists() || !tmpDir.isDirectory()) {
                throw new BuildException(tmpDir.toString()
                                         + " is not a valid temp directory");
            }
        }
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
        splitJunit = !addClasspathEntry("/junit/framework/TestCase.class");
        addClasspathEntry("/org/apache/tools/ant/launch/AntMain.class");
        addClasspathEntry("/org/apache/tools/ant/Task.class");
        addClasspathEntry("/org/apache/tools/ant/taskdefs/optional/junit/JUnitTestRunner.class");
    }

    private static JUnitTaskMirror createMirror(JUnitTask task, ClassLoader loader) {
        try {
            loader.loadClass("junit.framework.Test"); // sanity check
        } catch (ClassNotFoundException e) {
            throw new BuildException(
                    "The <classpath> for <junit> must include junit.jar "
                    + "if not in Ant's own classpath",
                    e, task.getLocation());
        }
        try {
            Class c = loader.loadClass(JUnitTaskMirror.class.getName() + "Impl");
            if (c.getClassLoader() != loader) {
                throw new BuildException("Overdelegating loader", task.getLocation());
            }
            Constructor cons = c.getConstructor(new Class[] {JUnitTask.class});
            return (JUnitTaskMirror) cons.newInstance(new Object[] {task});
        } catch (Exception e) {
            throw new BuildException(e, task.getLocation());
        }
    }

    private final class SplitLoader extends AntClassLoader {

        public SplitLoader(ClassLoader parent, Path path) {
            super(parent, getProject(), path, true);
        }

        // forceLoadClass is not convenient here since it would not
        // properly deal with inner classes of these classes.
        protected synchronized Class loadClass(String classname, boolean resolve)
        throws ClassNotFoundException {
            Class theClass = findLoadedClass(classname);
            if (theClass != null) {
                return theClass;
            }
            if (isSplit(classname)) {
                theClass = findClass(classname);
                if (resolve) {
                    resolveClass(theClass);
                }
                return theClass;
            } else {
                return super.loadClass(classname, resolve);
            }
        }

        private final String[] splitClasses = {
            "BriefJUnitResultFormatter",
            "JUnitResultFormatter",
            "JUnitTaskMirrorImpl",
            "JUnitTestRunner",
            "JUnitVersionHelper",
            "OutErrSummaryJUnitResultFormatter",
            "PlainJUnitResultFormatter",
            "SummaryJUnitResultFormatter",
            "XMLJUnitResultFormatter",
        };

        private boolean isSplit(String classname) {
            String simplename = classname.substring(classname.lastIndexOf('.') + 1);
            for (int i = 0; i < splitClasses.length; i++) {
                if (simplename.equals(splitClasses[i])
                        || simplename.startsWith(splitClasses[i] + '$')) {
                    return true;
                }
            }
            return false;
        }

    }

    /**
     * Runs the testcase.
     *
     * @throws BuildException in case of test failures or errors
     * @since Ant 1.2
     */
    public void execute() throws BuildException {
        ClassLoader myLoader = JUnitTask.class.getClassLoader();
        ClassLoader mirrorLoader;
        if (splitJunit) {
            Path path = new Path(getProject());
            path.add(antRuntimeClasses);
            path.add(getCommandline().getClasspath());
            mirrorLoader = new SplitLoader(myLoader, path);
        } else {
            mirrorLoader = myLoader;
        }
        delegate = createMirror(this, mirrorLoader);

        List testLists = new ArrayList();

        boolean forkPerTest = forkMode.getValue().equals(ForkMode.PER_TEST);
        if (forkPerTest || forkMode.getValue().equals(ForkMode.ONCE)) {
            testLists.addAll(executeOrQueue(getIndividualTests(),
                                            forkPerTest));
        } else { /* forkMode.getValue().equals(ForkMode.PER_BATCH) */
            final int count = batchTests.size();
            for (int i = 0; i < count; i++) {
                BatchTest batchtest = (BatchTest) batchTests.elementAt(i);
                testLists.addAll(executeOrQueue(batchtest.elements(), false));
            }
            testLists.addAll(executeOrQueue(tests.elements(), forkPerTest));
        }

        try {
            Iterator iter = testLists.iterator();
            while (iter.hasNext()) {
                List l = (List) iter.next();
                if (l.size() == 1) {
                    execute((JUnitTest) l.get(0));
                } else {
                    execute(l);
                }
            }
        } finally {
            deleteClassLoader();
            if (mirrorLoader instanceof SplitLoader) {
                ((SplitLoader) mirrorLoader).cleanup();
            }
            delegate = null;
        }
    }

    /**
     * Run the tests.
     * @param arg one JunitTest
     * @throws BuildException in case of test failures or errors
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
        TestResultHolder result = null;
        if (!test.getFork()) {
            result = executeInVM(test);
        } else {
            ExecuteWatchdog watchdog = createWatchdog();
            result = executeAsForked(test, watchdog, null);
            // null watchdog means no timeout, you'd better not check with null
        }
        actOnTestResult(result, test, "Test " + test.getName());
    }

    /**
     * Execute a list of tests in a single forked Java VM.
     * @param tests the list of tests to execute.
     * @throws BuildException on error.
     */
    protected void execute(List tests) throws BuildException {
        JUnitTest test = null;
        // Create a temporary file to pass the test cases to run to
        // the runner (one test case per line)
        File casesFile = createTempPropertiesFile("junittestcases");
        PrintWriter writer = null;
        try {
            writer =
                new PrintWriter(new BufferedWriter(new FileWriter(casesFile)));
            Iterator iter = tests.iterator();
            while (iter.hasNext()) {
                test = (JUnitTest) iter.next();
                writer.print(test.getName());
                if (test.getTodir() == null) {
                    writer.print("," + getProject().resolveFile("."));
                } else {
                    writer.print("," + test.getTodir());
                }

                if (test.getOutfile() == null) {
                    writer.println("," + "TEST-" + test.getName());
                } else {
                    writer.println("," + test.getOutfile());
                }
            }
            writer.flush();
            writer.close();
            writer = null;

            // execute the test and get the return code
            ExecuteWatchdog watchdog = createWatchdog();
            TestResultHolder result =
                executeAsForked(test, watchdog, casesFile);
            actOnTestResult(result, test, "Tests");
        } catch (IOException e) {
            log(e.toString(), Project.MSG_ERR);
            throw new BuildException(e);
        } finally {
            if (writer != null) {
                writer.close();
            }

            try {
                casesFile.delete();
            } catch (Exception e) {
                log(e.toString(), Project.MSG_ERR);
            }
        }
    }

    /**
     * Execute a testcase by forking a new JVM. The command will block
     * until it finishes. To know if the process was destroyed or not
     * or whether the forked Java VM exited abnormally, use the
     * attributes of the returned holder object.
     * @param  test       the testcase to execute.
     * @param  watchdog   the watchdog in charge of cancelling the test if it
     * exceeds a certain amount of time. Can be <tt>null</tt>, in this case
     * the test could probably hang forever.
     * @param casesFile list of test cases to execute. Can be <tt>null</tt>,
     * in this case only one test is executed.
     * @throws BuildException in case of error creating a temporary property file,
     * or if the junit process can not be forked
     */
    private TestResultHolder executeAsForked(JUnitTest test,
                                             ExecuteWatchdog watchdog,
                                             File casesFile)
        throws BuildException {

        if (perm != null) {
            log("Permissions ignored when running in forked mode!",
                Project.MSG_WARN);
        }

        CommandlineJava cmd = null;
        try {
            cmd = (CommandlineJava) (getCommandline().clone());
        } catch (CloneNotSupportedException e) {
            throw new BuildException("This shouldn't happen", e, getLocation());
        }
        cmd.setClassname("org.apache.tools.ant.taskdefs.optional.junit.JUnitTestRunner");
        if (casesFile == null) {
            cmd.createArgument().setValue(test.getName());
        } else {
            log("Running multiple tests in the same VM", Project.MSG_VERBOSE);
            cmd.createArgument().setValue(Constants.TESTSFILE + casesFile);
        }

        cmd.createArgument().setValue(Constants.FILTERTRACE + test.getFiltertrace());
        cmd.createArgument().setValue(Constants.HALT_ON_ERROR + test.getHaltonerror());
        cmd.createArgument().setValue(Constants.HALT_ON_FAILURE
                                      + test.getHaltonfailure());
        if (includeAntRuntime) {
            Vector v = Execute.getProcEnvironment();
            Enumeration e = v.elements();
            while (e.hasMoreElements()) {
                String s = (String) e.nextElement();
                if (s.startsWith(CLASSPATH)) {
                    cmd.createClasspath(getProject()).createPath()
                        .append(new Path(getProject(),
                                         s.substring(CLASSPATH.length()
                                                     )));
                }
            }
            log("Implicitly adding " + antRuntimeClasses + " to CLASSPATH",
                Project.MSG_VERBOSE);
            cmd.createClasspath(getProject()).createPath()
                .append(antRuntimeClasses);
        }

        if (summary) {
            String prefix = "";
            if ("withoutanderr".equalsIgnoreCase(summaryValue)) {
                prefix = "OutErr";
            }
            cmd.createArgument()
                .setValue(Constants.FORMATTER
                          + "org.apache.tools.ant.taskdefs.optional.junit."
                          + prefix + "SummaryJUnitResultFormatter");
        }

        cmd.createArgument().setValue(Constants.SHOWOUTPUT
                                      + String.valueOf(showOutput));
        cmd.createArgument().setValue(Constants.OUTPUT_TO_FORMATTERS
                                      + String.valueOf(outputToFormatters));

        cmd.createArgument().setValue(
            Constants.LOGTESTLISTENEREVENTS + "true"); // #31885

        StringBuffer formatterArg = new StringBuffer(STRING_BUFFER_SIZE);
        final FormatterElement[] feArray = mergeFormatters(test);
        for (int i = 0; i < feArray.length; i++) {
            FormatterElement fe = feArray[i];
            if (fe.shouldUse(this)) {
                formatterArg.append(Constants.FORMATTER);
                formatterArg.append(fe.getClassname());
                File outFile = getOutput(fe, test);
                if (outFile != null) {
                    formatterArg.append(",");
                    formatterArg.append(outFile);
                }
                cmd.createArgument().setValue(formatterArg.toString());
                formatterArg = new StringBuffer();
            }
        }

        File vmWatcher = createTempPropertiesFile("junitvmwatcher");
        cmd.createArgument().setValue(Constants.CRASHFILE
                                      + vmWatcher.getAbsolutePath());
        File propsFile = createTempPropertiesFile("junit");
        cmd.createArgument().setValue(Constants.PROPSFILE
                                      + propsFile.getAbsolutePath());
        Hashtable p = getProject().getProperties();
        Properties props = new Properties();
        for (Enumeration e = p.keys(); e.hasMoreElements();) {
            Object key = e.nextElement();
            props.put(key, p.get(key));
        }
        try {
            FileOutputStream outstream = new FileOutputStream(propsFile);
            props.store(outstream, "Ant JUnitTask generated properties file");
            outstream.close();
        } catch (java.io.IOException e) {
            propsFile.delete();
            throw new BuildException("Error creating temporary properties "
                                     + "file.", e, getLocation());
        }

        Execute execute = new Execute(
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
        TestResultHolder result = new TestResultHolder();
        try {
            result.exitCode = execute.execute();
        } catch (IOException e) {
            throw new BuildException("Process fork failed.", e, getLocation());
        } finally {
            String vmCrashString = "unknown";
            BufferedReader br = null;
            try {
                br = new BufferedReader(new FileReader(vmWatcher));
                vmCrashString = br.readLine();
            } catch (Exception e) {
                e.printStackTrace();
                // ignored.
            } finally {
                FileUtils.close(br);
            }
            if (watchdog != null && watchdog.killedProcess()) {
                result.timedOut = true;
                logTimeout(feArray, test, vmCrashString);
            } else if (!Constants.TERMINATED_SUCCESSFULLY.equals(vmCrashString)) {
                result.crashed = true;
                logVmCrash(feArray, test, vmCrashString);
            }
            vmWatcher.delete();

            if (!propsFile.delete()) {
                throw new BuildException("Could not delete temporary "
                                         + "properties file.");
            }
        }

        return result;
    }

    /**
     * Create a temporary file to pass the properties to a new process.
     * Will auto-delete on (graceful) exit.
     * The file will be in the project basedir unless tmpDir declares
     * something else.
     * @param prefix
     * @return created file
     */
    private File createTempPropertiesFile(String prefix) {
        File propsFile =
            FILE_UTILS.createTempFile(prefix, ".properties",
                tmpDir != null ? tmpDir : getProject().getBaseDir(), true);
        return propsFile;
    }


    /**
     * Pass output sent to System.out to the TestRunner so it can
     * collect ot for the formatters.
     *
     * @param output output coming from System.out
     * @since Ant 1.5
     */
    protected void handleOutput(String output) {
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
     * @param output output coming from System.out
     * @since Ant 1.5.2
     */
    protected void handleFlush(String output) {
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
    public void handleErrorOutput(String output) {
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
    public void handleErrorFlush(String output) {
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
     */
    private TestResultHolder executeInVM(JUnitTest arg) throws BuildException {
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

        if (getCommandline().getBootclasspath() != null) {
            log("bootclasspath is ignored if running in the same VM.",
                Project.MSG_WARN);
        }

        CommandlineJava.SysProperties sysProperties =
                getCommandline().getSystemProperties();
        if (sysProperties != null) {
            sysProperties.setSystem();
        }

        try {
            log("Using System properties " + System.getProperties(),
                Project.MSG_VERBOSE);
            if (splitJunit) {
                classLoader = (AntClassLoader) delegate.getClass().getClassLoader();
            } else {
                createClassLoader();
            }
            if (classLoader != null) {
                classLoader.setThreadContextLoader();
            }
            runner = delegate.newJUnitTestRunner(test, test.getHaltonerror(),
                                         test.getFiltertrace(),
                                         test.getHaltonfailure(), false,
                                         true, classLoader);
            if (summary) {

                JUnitTaskMirror.SummaryJUnitResultFormatterMirror f =
                    delegate.newSummaryJUnitResultFormatter();
                f.setWithOutAndErr("withoutanderr"
                                   .equalsIgnoreCase(summaryValue));
                f.setOutput(getDefaultOutput());
                runner.addFormatter(f);
            }

            runner.setPermissions(perm);

            final FormatterElement[] feArray = mergeFormatters(test);
            for (int i = 0; i < feArray.length; i++) {
                FormatterElement fe = feArray[i];
                if (fe.shouldUse(this)) {
                    File outFile = getOutput(fe, test);
                    if (outFile != null) {
                        fe.setOutfile(outFile);
                    } else {
                        fe.setOutput(getDefaultOutput());
                    }
                    runner.addFormatter(fe.createFormatter(classLoader));
                }
            }

            runner.run();
            TestResultHolder result = new TestResultHolder();
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
     * @return <tt>null</tt> if there is a timeout value, otherwise the
     * watchdog instance.
     *
     * @throws BuildException under unspecified circumstances
     * @since Ant 1.2
     */
    protected ExecuteWatchdog createWatchdog() throws BuildException {
        if (timeout == null) {
            return null;
        }
        return new ExecuteWatchdog((long) timeout.intValue());
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
     * and return an enumeration over all <tt>JUnitTest</tt>.
     *
     * @return enumeration over individual tests
     * @since Ant 1.3
     */
    protected Enumeration getIndividualTests() {
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
     * return an enumeration listing each test, then each batchtest
     * @return enumeration
     * @since Ant 1.3
     */
    protected Enumeration allTests() {
        Enumeration[] enums = {tests.elements(), batchTests.elements()};
        return Enumerations.fromCompound(enums);
    }

    /**
     * @param test junit test
     * @return array of FormatterElement
     * @since Ant 1.3
     */
    private FormatterElement[] mergeFormatters(JUnitTest test) {
        Vector feVector = (Vector) formatters.clone();
        test.addFormattersTo(feVector);
        FormatterElement[] feArray = new FormatterElement[feVector.size()];
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
    protected File getOutput(FormatterElement fe, JUnitTest test) {
        if (fe.getUseFile()) {
            String base = test.getOutfile();
            if (base == null) {
                base = JUnitTaskMirror.JUnitTestRunnerMirror.IGNORED_FILE_NAME;
            }
            String filename = base + fe.getExtension();
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
     * @param resource resource that one wants to lookup
     * @return true if something was in fact added
     * @since Ant 1.4
     */
    protected boolean addClasspathEntry(String resource) {
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
            return true;
        } else {
            log("Couldn\'t find " + resource, Project.MSG_DEBUG);
            return false;
        }
    }

    /**
     * Take care that some output is produced in report files if the
     * watchdog kills the test.
     *
     * @since Ant 1.5.2
     */

    private void logTimeout(FormatterElement[] feArray, JUnitTest test, String testCase) {
        logVmExit(
            feArray, test,
            "Timeout occurred. Please note the time in the report does"
            + " not reflect the time until the timeout.",
            testCase);
    }

    /**
     * Take care that some output is produced in report files if the
     * forked machine exited before the test suite finished but the
     * reason is not a timeout.
     *
     * @since Ant 1.7
     */
    private void logVmCrash(FormatterElement[] feArray, JUnitTest test, String testCase) {
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
    private void logVmExit(FormatterElement[] feArray, JUnitTest test,
                           String message, String testCase) {
        try {
            log("Using System properties " + System.getProperties(),
                Project.MSG_VERBOSE);
            if (splitJunit) {
                classLoader = (AntClassLoader) delegate.getClass().getClassLoader();
            } else {
                createClassLoader();
            }
            if (classLoader != null) {
                classLoader.setThreadContextLoader();
            }

            test.setCounts(1, 0, 1);
            test.setProperties(getProject().getProperties());
            for (int i = 0; i < feArray.length; i++) {
                FormatterElement fe = feArray[i];
                File outFile = getOutput(fe, test);
                JUnitTaskMirror.JUnitResultFormatterMirror formatter =
                    fe.createFormatter(classLoader);
                if (outFile != null && formatter != null) {
                    try {
                        OutputStream out = new FileOutputStream(outFile);
                        delegate.addVmExit(test, formatter, out, message, testCase);
                    } catch (IOException e) {
                        // ignore
                    }
                }
            }
            if (summary) {
                JUnitTaskMirror.SummaryJUnitResultFormatterMirror f =
                    delegate.newSummaryJUnitResultFormatter();
                f.setWithOutAndErr("withoutanderr".equalsIgnoreCase(summaryValue));
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
        Path userClasspath = getCommandline().getClasspath();
        if (userClasspath != null) {
            if (reloading || classLoader == null) {
                deleteClassLoader();
                Path classpath = (Path) userClasspath.clone();
                if (includeAntRuntime) {
                    log("Implicitly adding " + antRuntimeClasses
                        + " to CLASSPATH", Project.MSG_VERBOSE);
                    classpath.append(antRuntimeClasses);
                }
                classLoader = getProject().createClassLoader(classpath);
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
                classLoader.addSystemPackageRoot("org.apache.tools.ant");
            }
        }
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
    }

    /**
     * Get the command line used to run the tests.
     * @return the command line.
     * @since Ant 1.6.2
     */
    protected CommandlineJava getCommandline() {
        if (commandline == null) {
            commandline = new CommandlineJava();
        }
        return commandline;
    }

    /**
     * Forked test support
     * @since Ant 1.6.2
     */
    private static final class ForkedTestConfiguration {
        private boolean filterTrace;
        private boolean haltOnError;
        private boolean haltOnFailure;
        private String errorProperty;
        private String failureProperty;

        /**
         * constructor for forked test configuration
         * @param filterTrace
         * @param haltOnError
         * @param haltOnFailure
         * @param errorProperty
         * @param failureProperty
         */
        ForkedTestConfiguration(boolean filterTrace, boolean haltOnError,
                                boolean haltOnFailure, String errorProperty,
                                String failureProperty) {
            this.filterTrace = filterTrace;
            this.haltOnError = haltOnError;
            this.haltOnFailure = haltOnFailure;
            this.errorProperty = errorProperty;
            this.failureProperty = failureProperty;
        }

        /**
         * configure from a test; sets member variables to attributes of the test
         * @param test
         */
        ForkedTestConfiguration(JUnitTest test) {
            this(test.getFiltertrace(),
                    test.getHaltonerror(),
                    test.getHaltonfailure(),
                    test.getErrorProperty(),
                    test.getFailureProperty());
        }

        /**
         * equality test checks all the member variables
         * @param other
         * @return true if everything is equal
         */
        public boolean equals(Object other) {
            if (other == null
                || other.getClass() != ForkedTestConfiguration.class) {
                return false;
            }
            ForkedTestConfiguration o = (ForkedTestConfiguration) other;
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
        public int hashCode() {
            return (filterTrace ? 1 : 0)
                + (haltOnError ? 2 : 0)
                + (haltOnFailure ? 4 : 0);
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
        public ForkMode(String value) {
            super();
            setValue(value);
        }

        /** {@inheritDoc}. */
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
    protected Collection executeOrQueue(Enumeration testList,
                                        boolean runIndividual) {
        Map testConfigurations = new HashMap();
        while (testList.hasMoreElements()) {
            JUnitTest test = (JUnitTest) testList.nextElement();
            if (test.shouldRun(getProject())) {
                if (runIndividual || !test.getFork()) {
                    execute(test);
                } else {
                    ForkedTestConfiguration c =
                        new ForkedTestConfiguration(test);
                    List l = (List) testConfigurations.get(c);
                    if (l == null) {
                        l = new ArrayList();
                        testConfigurations.put(c, l);
                    }
                    l.add(test);
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
    protected void actOnTestResult(int exitValue, boolean wasKilled,
                                   JUnitTest test, String name) {
        TestResultHolder t = new TestResultHolder();
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
    protected void actOnTestResult(TestResultHolder result, JUnitTest test,
                                   String name) {
        // if there is an error/failure and that it should halt, stop
        // everything otherwise just log a statement
        boolean fatal = result.timedOut || result.crashed;
        boolean errorOccurredHere =
            result.exitCode == JUnitTaskMirror.JUnitTestRunnerMirror.ERRORS || fatal;
        boolean failureOccurredHere =
            result.exitCode != JUnitTaskMirror.JUnitTestRunnerMirror.SUCCESS || fatal;
        if (errorOccurredHere || failureOccurredHere) {
            if ((errorOccurredHere && test.getHaltonerror())
                || (failureOccurredHere && test.getHaltonfailure())) {
                throw new BuildException(name + " failed"
                    + (result.timedOut ? " (timeout)" : "")
                    + (result.crashed ? " (crashed)" : ""), getLocation());
            } else {
                log(name + " FAILED"
                    + (result.timedOut ? " (timeout)" : "")
                    + (result.crashed ? " (crashed)" : ""), Project.MSG_ERR);
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
     * A value class that contains thee result of a test.
     */
    protected class TestResultHolder {
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
        private Task task; // local copy since LogOutputStream.task is private

        /**
         * Constructor.
         * @param task the task being logged.
         * @param level the log level used to log data written to this stream.
         */
        public JUnitLogOutputStream(Task task, int level) {
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
        protected void processLine(String line, int level) {
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
        public JUnitLogStreamHandler(Task task, int outlevel, int errlevel) {
            super(new JUnitLogOutputStream(task, outlevel),
                  new LogOutputStream(task, errlevel));
        }
    }
}
