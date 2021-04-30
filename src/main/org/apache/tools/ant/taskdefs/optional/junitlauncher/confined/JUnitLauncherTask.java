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
package org.apache.tools.ant.taskdefs.optional.junitlauncher.confined;

import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.ExecuteWatchdog;
import org.apache.tools.ant.taskdefs.LogOutputStream;
import org.apache.tools.ant.taskdefs.PumpStreamHandler;
import org.apache.tools.ant.types.CommandlineJava;
import org.apache.tools.ant.types.Environment;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.util.FileUtils;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static org.apache.tools.ant.taskdefs.optional.junitlauncher.confined.Constants.LD_XML_ATTR_EXCLUDE_TAGS;
import static org.apache.tools.ant.taskdefs.optional.junitlauncher.confined.Constants.LD_XML_ATTR_HALT_ON_FAILURE;
import static org.apache.tools.ant.taskdefs.optional.junitlauncher.confined.Constants.LD_XML_ATTR_INCLUDE_TAGS;
import static org.apache.tools.ant.taskdefs.optional.junitlauncher.confined.Constants.LD_XML_ATTR_PRINT_SUMMARY;
import static org.apache.tools.ant.taskdefs.optional.junitlauncher.confined.Constants.LD_XML_ELM_LAUNCH_DEF;


/**
 * An Ant {@link Task} responsible for launching the JUnit platform for running tests.
 * This requires a minimum of JUnit 5, since that's the version in which the JUnit platform launcher
 * APIs were introduced.
 * <p>
 * This task in itself doesn't run the JUnit tests, instead the sole responsibility of
 * this task is to setup the JUnit platform launcher, build requests, launch those requests and then parse the
 * result of the execution to present in a way that's been configured on this Ant task.
 * </p>
 * <p>
 * Furthermore, this task allows users control over which classes to select for passing on to the JUnit 5
 * platform for test execution. It however, is solely the JUnit 5 platform, backed by test engines that
 * decide and execute the tests.
 *
 * @see <a href="https://junit.org/junit5/">JUnit 5 documentation</a>
 */
public class JUnitLauncherTask extends Task {

    private static final String LAUNCHER_SUPPORT_CLASS_NAME = "org.apache.tools.ant.taskdefs.optional.junitlauncher.LauncherSupport";
    private static final String IN_VM_TEST_EXECUTION_CONTEXT_CLASS_NAME = "org.apache.tools.ant.taskdefs.optional.junitlauncher.InVMExecution";
    private static final String TEST_EXECUTION_CONTEXT_CLASS_NAME = "org.apache.tools.ant.taskdefs.optional.junitlauncher.TestExecutionContext";

    private Path classPath;
    private boolean haltOnFailure;
    private String failureProperty;
    private boolean printSummary;
    private final List<TestDefinition> tests = new ArrayList<>();
    private final List<ListenerDefinition> listeners = new ArrayList<>();
    private List<String> includeTags = new ArrayList<>();
    private List<String> excludeTags = new ArrayList<>();

    public JUnitLauncherTask() {
    }

    @Override
    public void execute() throws BuildException {
        if (this.tests.isEmpty()) {
            return;
        }
        final Project project = getProject();
        for (final TestDefinition test : this.tests) {
            if (!test.shouldRun(project)) {
                log("Excluding test " + test + " since it's considered not to run " +
                        "in context of project " + project, Project.MSG_DEBUG);
                continue;
            }
            if (test.getForkDefinition() != null) {
                forkTest(test);
            } else {
                launchViaReflection(new InVMLaunch(Collections.singletonList(test)));
            }
        }
    }

    /**
     * Adds the {@link Path} to the classpath which will be used for execution of the tests
     *
     * @param path The classpath
     */
    public void addConfiguredClassPath(final Path path) {
        if (this.classPath == null) {
            // create a "wrapper" path which can hold on to multiple
            // paths that get passed to this method (if at all the task in the build is
            // configured with multiple classpaht elements)
            this.classPath = new Path(getProject());
        }
        this.classPath.add(path);
    }

    /**
     * Adds a {@link SingleTestClass} that will be passed on to the underlying JUnit platform
     * for possible execution of the test
     *
     * @param test The test
     */
    public void addConfiguredTest(final SingleTestClass test) {
        this.preConfigure(test);
        this.tests.add(test);
    }

    /**
     * Adds {@link TestClasses} that will be passed on to the underlying JUnit platform for
     * possible execution of the tests
     *
     * @param testClasses The test classes
     */
    public void addConfiguredTestClasses(final TestClasses testClasses) {
        this.preConfigure(testClasses);
        this.tests.add(testClasses);
    }

    /**
     * Adds a {@link ListenerDefinition listener} which will be enrolled for listening to test
     * execution events
     *
     * @param listener The listener
     */
    public void addConfiguredListener(final ListenerDefinition listener) {
        this.listeners.add(listener);
    }

    public void setHaltonfailure(final boolean haltonfailure) {
        this.haltOnFailure = haltonfailure;
    }

    public void setFailureProperty(final String failureProperty) {
        this.failureProperty = failureProperty;
    }

    public void setPrintSummary(final boolean printSummary) {
        this.printSummary = printSummary;
    }

    /**
     * Tags to include. Will trim each tag.
     *
     * @param includes comma separated list of tags to include while running the tests.
     * @since Ant 1.10.7
     */
    public void setIncludeTags(final String includes) {
        final StringTokenizer tokens = new StringTokenizer(includes, ",");
        while (tokens.hasMoreTokens()) {
            includeTags.add(tokens.nextToken().trim());
        }
    }

    /**
     * Tags to exclude. Will trim each tag.
     *
     * @param excludes comma separated list of tags to exclude while running the tests.
     * @since Ant 1.10.7
     */
    public void setExcludeTags(final String excludes) {
        final StringTokenizer tokens = new StringTokenizer(excludes, ",");
        while (tokens.hasMoreTokens()) {
            excludeTags.add(tokens.nextToken().trim());
        }
    }

    private void preConfigure(final TestDefinition test) {
        if (test.getHaltOnFailure() == null) {
            test.setHaltOnFailure(this.haltOnFailure);
        }
        if (test.getFailureProperty() == null) {
            test.setFailureProperty(this.failureProperty);
        }
    }

    private void launchViaReflection(final InVMLaunch launchDefinition) {
        final ClassLoader cl = launchDefinition.getClassLoader();
        // instantiate a new TestExecutionContext instance using the launch definition's classloader
        final Class<?> testExecutionCtxClass;
        final Object testExecutionCtx;
        try {
            testExecutionCtxClass = Class.forName(TEST_EXECUTION_CONTEXT_CLASS_NAME, false, cl);
            final Class<?> klass = Class.forName(IN_VM_TEST_EXECUTION_CONTEXT_CLASS_NAME, false, cl);
            testExecutionCtx = klass.getConstructor(JUnitLauncherTask.class).newInstance(this);
        } catch (Exception e) {
            throw new BuildException("Failed to create a test execution context for in-vm tests", e);
        }
        // instantiate a new LauncherSupport instance using the launch definition's ClassLoader
        try {
            final Class<?> klass = Class.forName(LAUNCHER_SUPPORT_CLASS_NAME, false, cl);
            final Object launcherSupport = klass.getConstructor(LaunchDefinition.class, testExecutionCtxClass)
                    .newInstance(launchDefinition, testExecutionCtx);
            klass.getMethod("launch").invoke(launcherSupport);
        } catch (Exception e) {
            throw new BuildException("Failed to launch in-vm tests", e);
        }
    }

    private java.nio.file.Path dumpProjectProperties() throws IOException {
        final java.nio.file.Path propsPath = FileUtils.getFileUtils()
            .createTempFile(getProject(), null, "properties", null, true, true)
            .toPath();
        final Hashtable<String, Object> props = this.getProject().getProperties();
        final Properties projProperties = new Properties();
        projProperties.putAll(props);
        try (final OutputStream os = Files.newOutputStream(propsPath)) {
            // TODO: Is it always UTF-8?
            projProperties.store(os, StandardCharsets.UTF_8.name());
        }
        return propsPath;
    }

    private void forkTest(final TestDefinition test) {
        // create launch command
        final ForkDefinition forkDefinition = test.getForkDefinition();
        final CommandlineJava commandlineJava = forkDefinition.generateCommandLine(this);
        if (this.classPath != null) {
            commandlineJava.createClasspath(getProject()).createPath().append(this.classPath);
        }
        final java.nio.file.Path projectPropsPath;
        try {
            projectPropsPath = dumpProjectProperties();
        } catch (IOException e) {
            throw new BuildException("Could not create the necessary properties file while forking a process" +
                    " for a test", e);
        }
        // --properties <path-to-properties-file>
        commandlineJava.createArgument().setValue(Constants.ARG_PROPERTIES);
        commandlineJava.createArgument().setValue(projectPropsPath.toAbsolutePath().toString());

        final java.nio.file.Path launchDefXmlPath = newLaunchDefinitionXml();
        try (final OutputStream os = Files.newOutputStream(launchDefXmlPath)) {
            final XMLStreamWriter writer = XMLOutputFactory.newFactory().createXMLStreamWriter(os, "UTF-8");
            try {
                writer.writeStartDocument();
                writer.writeStartElement(LD_XML_ELM_LAUNCH_DEF);
                if (this.printSummary) {
                    writer.writeAttribute(LD_XML_ATTR_PRINT_SUMMARY, "true");
                }
                if (this.haltOnFailure) {
                    writer.writeAttribute(LD_XML_ATTR_HALT_ON_FAILURE, "true");
                }
                if (this.includeTags.size() > 0) {
                    writer.writeAttribute(LD_XML_ATTR_INCLUDE_TAGS, commaSeparatedListElements(includeTags));
                }
                if (this.excludeTags.size() > 0) {
                    writer.writeAttribute(LD_XML_ATTR_EXCLUDE_TAGS, commaSeparatedListElements(excludeTags));
                }
                // task level listeners
                for (final ListenerDefinition listenerDef : this.listeners) {
                    if (!listenerDef.shouldUse(getProject())) {
                        continue;
                    }
                    // construct the listener definition argument
                    listenerDef.toForkedRepresentation(writer);
                }
                // test definition as XML
                test.toForkedRepresentation(this, writer);
                writer.writeEndElement();
                writer.writeEndDocument();
            } finally {
                writer.close();
            }
        } catch (Exception e) {
            throw new BuildException("Failed to construct command line for test", e);
        }
        // --launch-definition <xml-file-path>
        commandlineJava.createArgument().setValue(Constants.ARG_LAUNCH_DEFINITION);
        commandlineJava.createArgument().setValue(launchDefXmlPath.toAbsolutePath().toString());

        // launch the process and wait for process to complete
        final int exitCode = executeForkedTest(forkDefinition, commandlineJava);
        switch (exitCode) {
            case Constants.FORK_EXIT_CODE_SUCCESS: {
                // success
                break;
            }
            case Constants.FORK_EXIT_CODE_EXCEPTION: {
                // process failed with some exception
                throw new BuildException("Forked test(s) failed with an exception");
            }
            case Constants.FORK_EXIT_CODE_TESTS_FAILED: {
                // test has failure(s)
                try {
                    if (test.getFailureProperty() != null) {
                        // if there are test failures and the test is configured to set a property in case
                        // of failure, then set the property to true
                        this.getProject().setNewProperty(test.getFailureProperty(), "true");
                    }
                } finally {
                    if (test.isHaltOnFailure()) {
                        // if the test is configured to halt on test failures, throw a build error
                        final String errorMessage;
                        if (test instanceof NamedTest) {
                            errorMessage = "Test " + ((NamedTest) test).getName() + " has failure(s)";
                        } else {
                            errorMessage = "Some test(s) have failure(s)";
                        }
                        throw new BuildException(errorMessage);
                    }
                }
                break;
            }
            case Constants.FORK_EXIT_CODE_TIMED_OUT: {
                throw new BuildException(new TimeoutException("Forked test(s) timed out"));
            }
        }
    }

    private static String commaSeparatedListElements(final List<String> stringList) {
        return stringList.stream()
                .map(Object::toString)
                .collect(Collectors.joining(", "));
    }

    private int executeForkedTest(final ForkDefinition forkDefinition, final CommandlineJava commandlineJava) {
        final LogOutputStream outStream = new LogOutputStream(this, Project.MSG_INFO);
        final LogOutputStream errStream = new LogOutputStream(this, Project.MSG_WARN);
        final ExecuteWatchdog watchdog = forkDefinition.getTimeout() > 0
                ? createExecuteWatchdog(forkDefinition.getTimeout()) : null;
        final Execute execute = new Execute(new PumpStreamHandler(outStream, errStream), watchdog);
        execute.setCommandline(commandlineJava.getCommandline());
        execute.setAntRun(getProject());
        if (forkDefinition.getDir() != null) {
            execute.setWorkingDirectory(Paths.get(forkDefinition.getDir()).toFile());
        }
        final Environment env = forkDefinition.getEnv();
        if (env != null && env.getVariables() != null) {
            execute.setEnvironment(env.getVariables());
        }
        log(commandlineJava.describeCommand(), Project.MSG_VERBOSE);
        int exitCode;
        try {
            exitCode = execute.execute();
        } catch (IOException e) {
            throw new BuildException("Process fork failed", e, getLocation());
        }
        return (watchdog != null && watchdog.killedProcess()) ? Constants.FORK_EXIT_CODE_TIMED_OUT : exitCode;
    }

    protected ExecuteWatchdog createExecuteWatchdog(long timeout) {
        return new ExecuteWatchdog(timeout);
    }

    private java.nio.file.Path newLaunchDefinitionXml() {
        return FileUtils.getFileUtils()
            .createTempFile(getProject(), null, ".xml", null, true, true)
            .toPath();
    }

    private final class InVMLaunch implements LaunchDefinition {

        private final List<TestDefinition> inVMTests;
        private final ClassLoader executionCL;

        private InVMLaunch(final List<TestDefinition> inVMTests) {
            this.inVMTests = inVMTests;
            this.executionCL = createInVMExecutionClassLoader();
        }

        @Override
        public List<TestDefinition> getTests() {
            return this.inVMTests;
        }

        @Override
        public List<ListenerDefinition> getListeners() {
            return listeners;
        }

        @Override
        public boolean isPrintSummary() {
            return printSummary;
        }

        @Override
        public boolean isHaltOnFailure() {
            return haltOnFailure;
        }

        @Override
        public List<String> getIncludeTags() {
            return includeTags;
        }

        @Override
        public List<String> getExcludeTags() {
            return excludeTags;
        }

        @Override
        public ClassLoader getClassLoader() {
            return this.executionCL;
        }

        private ClassLoader createInVMExecutionClassLoader() {
            final Path taskConfiguredClassPath = JUnitLauncherTask.this.classPath;
            if (taskConfiguredClassPath == null) {
                // no specific classpath configured for the task, so use the classloader
                // of this task
                return JUnitLauncherTask.class.getClassLoader();
            }
            // there's a classpath configured for the task.
            // we first check if the Ant runtime classpath has JUnit platform classes.
            // - if it does, then we use the Ant runtime classpath plus the task's configured classpath
            // with the traditional parent first loading.
            // - else (i.e. Ant runtime classpath doesn't have JUnit platform classes), then we
            // expect/assume the task's configured classpath to have the JUnit platform classes and we
            // then create a "overriding" classloader which prefers certain resources (specifically the classes
            // from org.apache.tools.ant.taskdefs.optional.junitlauncher package), from the task's
            // classpath, even if the Ant's runtime classpath has those resources.
            if (JUnitLauncherClassPathUtil.hasJUnitPlatformResources(JUnitLauncherTask.class.getClassLoader())) {
                return new AntClassLoader(JUnitLauncherTask.class.getClassLoader(), getProject(), taskConfiguredClassPath, true);
            }
            final Path cp = new Path(getProject());
            cp.add(taskConfiguredClassPath);
            // add the Ant runtime resources to this path
            JUnitLauncherClassPathUtil.addLauncherSupportResourceLocation(cp, JUnitLauncherTask.class.getClassLoader());
            return new TaskConfiguredPathClassLoader(JUnitLauncherTask.class.getClassLoader(), cp, getProject());
        }
    }

    /**
     * A {@link ClassLoader}, very similar to the {@link org.apache.tools.ant.util.SplitClassLoader},
     * which uses the {@link #TaskConfiguredPathClassLoader(ClassLoader, Path, Project) configured Path}
     * to load a class, if the class belongs to the {@code org.apache.tools.ant.taskdefs.optional.junitlauncher}
     * package.
     * <p>
     * While looking for classes belonging to the {@code org.apache.tools.ant.taskdefs.optional.junitlauncher}
     * package, this classloader completely ignores Ant runtime classpath, even if that classpath has
     * those classes. This allows the users of this classloader to use a custom location and thus more control over
     * where these classes reside, when running the {@code junitlauncher} task
     */
    private final class TaskConfiguredPathClassLoader extends AntClassLoader {

        /**
         * @param parent  ClassLoader
         * @param path    Path
         * @param project Project
         */
        private TaskConfiguredPathClassLoader(ClassLoader parent, Path path, Project project) {
            super(parent, project, path, true);
        }

        // forceLoadClass is not convenient here since it would not
        // properly deal with inner classes of these classes.
        @Override
        protected synchronized Class<?> loadClass(String classname, boolean resolve)
                throws ClassNotFoundException {
            Class<?> theClass = findLoadedClass(classname);
            if (theClass != null) {
                return theClass;
            }
            final String packageName = classname.contains(".") ? classname.substring(0, classname.lastIndexOf('.'))
                    : "";
            if (packageName.equals("org.apache.tools.ant.taskdefs.optional.junitlauncher")) {
                theClass = findClass(classname);
                if (resolve) {
                    resolveClass(theClass);
                }
                return theClass;
            }
            return super.loadClass(classname, resolve);
        }
    }
}
