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
package org.apache.tools.ant.taskdefs.optional.extension;

import java.io.File;
import java.util.Iterator;
import java.util.Vector;
import java.util.jar.Manifest;
import org.apache.tools.ant.BuildException;
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
     * Filesets specifying all the librarys
     * to display information about.
     */
    private final Vector extensionFileSets = new Vector();

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
            final String message = "Can not specify extension to "
                + "search for multiple times.";
            throw new BuildException(message);
        }
        requiredExtension = extension;
    }

    /**
     * Adds a set of extensions to search in.
     *
     * @param extensionSet a set of extensions to search in.
     */
    public void addConfiguredExtensionSet(final ExtensionSet extensionSet) {
        extensionFileSets.addElement(extensionSet);
    }

    /**
     * Execute the task.
     *
     * @throws BuildException if somethign goes wrong.
     */
    public void execute() throws BuildException {
        validate();

        final Extension test = requiredExtension.toExtension();

        // Check if list of files to check has been specified
        if (!extensionFileSets.isEmpty()) {
            final Iterator iterator = extensionFileSets.iterator();
            while (iterator.hasNext()) {
                final ExtensionSet extensionSet
                    = (ExtensionSet) iterator.next();
                final Extension[] extensions =
                    extensionSet.toExtensions(getProject());
                for (int i = 0; i < extensions.length; i++) {
                    final Extension extension = extensions[ i ];
                    if (extension.isCompatibleWith(test)) {
                        getProject().setNewProperty(propertyName, "true");
                    }
                }
            }
        } else {
            final Manifest manifest = ExtensionUtil.getManifest(libraryFile);
            final Extension[] extensions = Extension.getAvailable(manifest);
            for (int i = 0; i < extensions.length; i++) {
                final Extension extension = extensions[ i ];
                if (extension.isCompatibleWith(test)) {
                    getProject().setNewProperty(propertyName, "true");
                }
            }
        }
    }

    /**
     * Validate the tasks parameters.
     *
     * @throws BuildException if invalid parameters found
     */
    private void validate() throws BuildException {
        if (null == requiredExtension) {
            final String message = "Extension element must be specified.";
            throw new BuildException(message);
        }

        if (null == libraryFile && extensionFileSets.isEmpty()) {
            final String message = "File attribute not specified.";
            throw new BuildException(message);
        }
        if (null != libraryFile && !libraryFile.exists()) {
            final String message = "File '" + libraryFile + "' does not exist.";
            throw new BuildException(message);
        }
        if (null != libraryFile && !libraryFile.isFile()) {
            final String message = "\'" + libraryFile + "\' is not a file.";
            throw new BuildException(message);
        }
    }
}
