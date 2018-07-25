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

package org.apache.tools.ant.taskdefs.optional.junitlauncher;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.launch.AntMain;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.CommandlineJava;
import org.apache.tools.ant.types.Environment;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.PropertySet;
import org.apache.tools.ant.util.LoaderUtils;
import org.junit.platform.commons.annotation.Testable;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.launcher.core.LauncherFactory;

import java.io.File;

/**
 * Represents the {@code fork} element within test definitions of the
 * {@code junitlauncher} task
 */
public class ForkDefinition {

    private boolean includeAntRuntimeLibraries = true;
    private boolean includeJunitPlatformLibraries = true;

    private final CommandlineJava commandLineJava;
    private final Environment env = new Environment();

    private String dir;
    private long timeout = -1;

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
        cmdLine.setClassname(StandaloneLauncher.class.getName());
        // VM arguments
        final Project project = task.getProject();
        final Path antRuntimeResourceSources = new Path(project);
        if (this.includeAntRuntimeLibraries) {
            addAntRuntimeResourceSource(antRuntimeResourceSources, task, toResourceName(AntMain.class));
            addAntRuntimeResourceSource(antRuntimeResourceSources, task, toResourceName(Task.class));
            addAntRuntimeResourceSource(antRuntimeResourceSources, task, toResourceName(JUnitLauncherTask.class));
        }

        if (this.includeJunitPlatformLibraries) {
            // platform-engine
            addAntRuntimeResourceSource(antRuntimeResourceSources, task, toResourceName(TestEngine.class));
            // platform-launcher
            addAntRuntimeResourceSource(antRuntimeResourceSources, task, toResourceName(LauncherFactory.class));
            // platform-commons
            addAntRuntimeResourceSource(antRuntimeResourceSources, task, toResourceName(Testable.class));
        }
        final Path classPath = cmdLine.createClasspath(project);
        classPath.createPath().append(antRuntimeResourceSources);

        return cmdLine;
    }

    private static boolean addAntRuntimeResourceSource(final Path path, final JUnitLauncherTask task, final String resource) {
        final File f = LoaderUtils.getResourceSource(task.getClass().getClassLoader(), resource);
        if (f == null) {
            task.log("Could not locate source of resource " + resource);
            return false;
        }
        task.log("Found source " + f + " of resource " + resource);
        path.createPath().setLocation(f);
        return true;
    }

    private static String toResourceName(final Class klass) {
        final String name = klass.getName();
        return name.replaceAll("\\.", "/") + ".class";
    }

}
