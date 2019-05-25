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
package org.apache.tools.ant.taskdefs.optional.extension;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Manifest;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.optional.extension.resolvers.AntResolver;
import org.apache.tools.ant.taskdefs.optional.extension.resolvers.LocationResolver;
import org.apache.tools.ant.taskdefs.optional.extension.resolvers.URLResolver;

/**
 * Tries to locate a JAR to satisfy an extension and place
 * location of JAR into property.
 *
 * @ant.task name="jarlib-resolve"
 */
public class JarLibResolveTask extends Task {
    /**
     * The name of the property in which the location of
     * library is stored.
     */
    private String propertyName;

    /**
     * The extension that is required.
     */
    private Extension requiredExtension;

    /**
     * The set of resolvers to use to attempt to locate library.
     */
    private final List<ExtensionResolver> resolvers = new ArrayList<>();

    /**
     * Flag to indicate that you should check that
     * the libraries resolved actually contain
     * extension and if they don't then raise
     * an exception.
     */
    private boolean checkExtension = true;

    /**
     * Flag indicating whether or not you should
     * throw a BuildException if you cannot resolve
     * library.
     */
    private boolean failOnError = true;

    /**
     * The name of the property in which the location of
     * library is stored.
     *
     * @param property The name of the property in which the location of
     *                 library is stored.
     */
    public void setProperty(final String property) {
        this.propertyName = property;
    }

    /**
     * Check nested libraries for extensions
     *
     * @param checkExtension if true, libraries returned by nested
     * resolvers should be checked to see if they supply extension.
     */
    public void setCheckExtension(final boolean checkExtension) {
        this.checkExtension = checkExtension;
    }

    /**
     * Set whether to fail if error.
     *
     * @param failOnError if true, failure to locate library should fail build.
     */
    public void setFailOnError(final boolean failOnError) {
        this.failOnError = failOnError;
    }

    /**
     * Adds location resolver to look for a library in a location
     * relative to project directory.
     *
     * @param loc the resolver location to search.
     */
    public void addConfiguredLocation(final LocationResolver loc) {
        resolvers.add(loc);
    }

    /**
     * Adds a URL resolver to download a library from a URL
     * to a local file.
     *
     * @param url the URL resolver from which to download the library
     */
    public void addConfiguredUrl(final URLResolver url) {
        resolvers.add(url);
    }

    /**
     * Adds Ant resolver to run an Ant build file to generate a library.
     *
     * @param ant the AntResolver to generate the library.
     */
    public void addConfiguredAnt(final AntResolver ant) {
        resolvers.add(ant);
    }

    /**
     * Set the Extension looking for.
     *
     * @param extension Set the Extension looking for.
     */
    public void addConfiguredExtension(final ExtensionAdapter extension) {
        if (null != requiredExtension) {
            throw new BuildException(
                "Can not specify extension to resolve multiple times.");
        }
        requiredExtension = extension.toExtension();
    }

    /**
     * Execute the task.
     *
     * @throws BuildException if the task fails.
     */
    @Override
    public void execute() throws BuildException {
        validate();

        getProject().log("Resolving extension: " + requiredExtension, Project.MSG_VERBOSE);

        String candidate = getProject().getProperty(propertyName);

        if (null != candidate) {
            final String message = "Property Already set to: " + candidate;
            if (failOnError) {
                throw new BuildException(message);
            }
            getProject().log(message, Project.MSG_ERR);
            return;
        }

        for (ExtensionResolver resolver : resolvers) {
            getProject().log("Searching for extension using Resolver:" + resolver,
                    Project.MSG_VERBOSE);
            try {
                final File file =
                    resolver.resolve(requiredExtension, getProject());
                try {
                    checkExtension(file);
                    return;
                } catch (final BuildException be) {
                    getProject().log("File " + file + " returned by "
                        + "resolver failed to satisfy extension due to: "
                        + be.getMessage(), Project.MSG_WARN);
                }
            } catch (final BuildException be) {
                getProject()
                    .log(
                        "Failed to resolve extension to file "
                            + "using resolver " + resolver + " due to: " + be,
                        Project.MSG_WARN);
            }
        }
        missingExtension();
    }

    /**
     * Utility method that will throw a {@link BuildException}
     * if {@link #failOnError} is true else it just displays
     * a warning.
     */
    private void missingExtension() {
        final String message = "Unable to resolve extension to a file";
        if (failOnError) {
            throw new BuildException(message);
        }
        getProject().log(message, Project.MSG_ERR);
    }

    /**
     * Check if specified file satisfies extension.
     * If it does then set the relevant property
     * else throw a BuildException.
     *
     * @param file the candidate library
     * @throws BuildException if library does not satisfy extension
     */
    private void checkExtension(final File file) {
        if (!file.exists()) {
            throw new BuildException("File %s does not exist", file);
        }
        if (!file.isFile()) {
            throw new BuildException("File %s is not a file", file);
        }
        if (!checkExtension) {
            getProject().log("Setting property to " + file
                    + " without verifying library satisfies extension", Project.MSG_VERBOSE);
            setLibraryProperty(file);
        } else {
            getProject().log("Checking file " + file + " to see if it satisfies extension",
                    Project.MSG_VERBOSE);
            final Manifest manifest = ExtensionUtil.getManifest(file);
            for (final Extension extension : Extension.getAvailable(manifest)) {
                if (extension.isCompatibleWith(requiredExtension)) {
                    setLibraryProperty(file);
                    return;
                }
            }
            final String message = "File " + file + " skipped as it "
                + "does not satisfy extension";
            getProject().log(message, Project.MSG_VERBOSE);
            throw new BuildException(message);
        }
    }

    /**
     * Utility method to set the appropriate property
     * to indicate that specified file satisfies library
     * requirements.
     *
     * @param file the library
     */
    private void setLibraryProperty(final File file) {
        getProject().setNewProperty(propertyName, file.getAbsolutePath());
    }

    /**
     * Validate the tasks parameters.
     *
     * @throws BuildException if invalid parameters found
     */
    private void validate() throws BuildException {
        if (null == propertyName) {
            throw new BuildException("Property attribute must be specified.");
        }
        if (null == requiredExtension) {
            throw new BuildException("Extension element must be specified.");
        }
    }
}
