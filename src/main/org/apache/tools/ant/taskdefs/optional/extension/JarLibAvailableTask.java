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
import java.util.List;
import java.util.Vector;
import java.util.stream.Stream;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

/**
 * Checks whether an extension is present in a fileset or an extensionSet.
 *
 * @ant.task name="jarlib-available"
 */
public class JarLibAvailableTask extends Task {
    /**
     * The library to display information about.
     */
    private File libraryFile;

    /**
     * Filesets specifying all the libraries
     * to display information about.
     */
    private final List<ExtensionSet> extensionFileSets = new Vector<>();

    /**
     * The name of the property to set if extension is available.
     */
    private String propertyName;

    /**
     * The extension that is required.
     */
    private ExtensionAdapter requiredExtension;

    /**
     * The name of property to set if extensions are available.
     *
     * @param property The name of property to set if extensions is available.
     */
    public void setProperty(final String property) {
        this.propertyName = property;
    }

    /**
     * The JAR library to check.
     *
     * @param file The jar library to check.
     */
    public void setFile(final File file) {
        this.libraryFile = file;
    }

    /**
     * Set the Extension looking for.
     *
     * @param extension Set the Extension looking for.
     */
    public void addConfiguredExtension(final ExtensionAdapter extension) {
        if (null != requiredExtension) {
            throw new BuildException(
                "Can not specify extension to search for multiple times.");
        }
        requiredExtension = extension;
    }

    /**
     * Adds a set of extensions to search in.
     *
     * @param extensionSet a set of extensions to search in.
     */
    public void addConfiguredExtensionSet(final ExtensionSet extensionSet) {
        extensionFileSets.add(extensionSet);
    }

    /**
     * Execute the task.
     *
     * @throws BuildException if something goes wrong.
     */
    @Override
    public void execute() throws BuildException {
        validate();

        final Project prj = getProject();
        final Stream<Extension> extensions;

        // Check if list of files to check has been specified
        if (!extensionFileSets.isEmpty()) {
            extensions = extensionFileSets.stream()
                .map(xset -> xset.toExtensions(prj)).flatMap(Stream::of);
        } else {
            extensions = Stream.of(
                Extension.getAvailable(ExtensionUtil.getManifest(libraryFile)));
        }
        final Extension test = requiredExtension.toExtension();
        if (extensions.anyMatch(x -> x.isCompatibleWith(test))) {
            prj.setNewProperty(propertyName, "true");
        }
    }

    /**
     * Validate the tasks parameters.
     *
     * @throws BuildException if invalid parameters found
     */
    private void validate() throws BuildException {
        if (null == requiredExtension) {
            throw new BuildException("Extension element must be specified.");
        }
        if (null == libraryFile) {
            if (extensionFileSets.isEmpty()) {
                throw new BuildException("File attribute not specified.");
            }
        } else if (!libraryFile.exists()) {
            throw new BuildException("File '%s' does not exist.", libraryFile);
        } else if (!libraryFile.isFile()) {
            throw new BuildException("'%s' is not a file.", libraryFile);
        }
    }
}
