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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.CommandlineJava;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.Environment;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.PropertySet;

/**
 * Represents the {@code fork} element within test definitions of the
 * {@code junitlauncher} task
 */
public class ForkDefinition {

    private static final String STANDALONE_LAUNCHER_CLASS_NAME = "org.apache.tools.ant.taskdefs.optional.junitlauncher.StandaloneLauncher";

    private boolean includeAntRuntimeLibraries = true;
    private boolean includeJUnitPlatformLibraries = true;

    private final CommandlineJava commandLineJava;
    private final Environment env = new Environment();

    private String dir;
    private long timeout = -1;
    private Mode mode = new Mode("once");

    ForkDefinition() {
        this.commandLineJava = new CommandlineJava();
    }

    public void setDir(final String dir) {
        this.dir = dir;
    }

    String getDir() {
        return this.dir;
    }

    public void setTimeout(final long timeout) {
        this.timeout = timeout;
    }

    long getTimeout() {
        return this.timeout;
    }

    /**
     * Possible values are "once" and "perTest". If set to "once" (the default),
     * only a single Java VM will be forked for all tests, with "perTest" each test
     * will be run in a fresh Java VM.
     * @param mode the mode to use.
     * @since Ant 1.10.13
     */
    public void setMode(final Mode mode) {
        this.mode = mode;
    }

    public Mode getMode() {
        return mode;
    }

    public void setIncludeJUnitPlatformLibraries(final boolean include) {
        this.includeJUnitPlatformLibraries = include;
    }

    public boolean isIncludeJUnitPlatformLibraries() {
        return includeJUnitPlatformLibraries;
    }

    public void setIncludeAntRuntimeLibraries(final boolean include) {
        this.includeAntRuntimeLibraries = include;
    }

    public boolean isIncludeAntRuntimeLibraries() {
        return includeAntRuntimeLibraries;
    }

    public Commandline.Argument createJvmArg() {
        return this.commandLineJava.createVmArgument();
    }

    public void addConfiguredSysProperty(final Environment.Variable sysProp) {
        // validate that key/value are present
        sysProp.validate();
        this.commandLineJava.addSysproperty(sysProp);
    }

    public void addConfiguredSysPropertySet(final PropertySet propertySet) {
        this.commandLineJava.addSyspropertyset(propertySet);
    }

    public void addConfiguredEnv(final Environment.Variable var) {
        this.env.addVariable(var);
    }

    public void addConfiguredModulePath(final Path modulePath) {
        this.commandLineJava.createModulepath(modulePath.getProject()).add(modulePath);
    }

    public void addConfiguredUpgradeModulePath(final Path upgradeModulePath) {
        this.commandLineJava.createUpgrademodulepath(upgradeModulePath.getProject()).add(upgradeModulePath);
    }

    Environment getEnv() {
        return this.env;
    }

    /**
     * The command used to launch {@code java}. This can be a path to the {@code java}
     * binary that will be used to launch the forked {@code java} process.
     *
     * @param java Path to the java command
     * @since Ant 1.10.14
     */
    public void setJava(String java) {
        this.commandLineJava.setVm(java);
    }

    /**
     * Generates a new {@link CommandlineJava} constructed out of the configurations set on this
     * {@link ForkDefinition}
     *
     * @param task The junitlaunchertask for which this is a fork definition
     * @return
     */
    CommandlineJava generateCommandLine(final JUnitLauncherTask task) {
        final CommandlineJava cmdLine;
        try {
            cmdLine = (CommandlineJava) this.commandLineJava.clone();
        } catch (CloneNotSupportedException e) {
            throw new BuildException(e);
        }
        cmdLine.setClassname(STANDALONE_LAUNCHER_CLASS_NAME);
        // VM arguments
        final Project project = task.getProject();
        final ClassLoader taskClassLoader = task.getClass().getClassLoader();
        // Ant runtime classes
        if (this.includeAntRuntimeLibraries) {
            final Path antRuntimeResources = new Path(project);
            JUnitLauncherClassPathUtil.addAntRuntimeResourceLocations(antRuntimeResources, taskClassLoader);
            final Path classPath = cmdLine.createClasspath(project);
            classPath.createPath().append(antRuntimeResources);
        } else {
            task.log("Excluding Ant runtime libraries from forked JVM classpath", Project.MSG_DEBUG);
        }
        // JUnit platform classes
        if (this.includeJUnitPlatformLibraries) {
            final Path junitPlatformResources = new Path(project);
            JUnitLauncherClassPathUtil.addJUnitPlatformResourceLocations(junitPlatformResources, taskClassLoader);
            final Path classPath = cmdLine.createClasspath(project);
            classPath.createPath().append(junitPlatformResources);
        } else {
            task.log("Excluding JUnit platform libraries from forked JVM classpath", Project.MSG_DEBUG);
        }
        return cmdLine;
    }

    /**
     * Forking option. There are two available: "once" and "perTest".
     * @since Ant 1.10.13
     */
    public static final class Mode extends EnumeratedAttribute {

        /**
         * fork once only
         */
        public static final String ONCE = "once";
        /**
         * fork once per test class
         */
        public static final String PER_TEST = "perTest";

        /** No arg constructor. */
        public Mode() {
            super();
        }

        /**
         * Constructor using a value.
         * @param value the value to use - once or perTest.
         */
        public Mode(final String value) {
            super();
            setValue(value);
        }

        /** {@inheritDoc}. */
        @Override
        public String[] getValues() {
            return new String[] {ONCE, PER_TEST};
        }
    }
}
