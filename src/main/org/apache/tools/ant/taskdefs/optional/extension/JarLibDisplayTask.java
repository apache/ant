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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;

/**
 * Displays the "Optional Package" and "Package Specification" information
 * contained within the specified JARs.
 *
 * <p>Prior to JDK1.3, an "Optional Package" was known as an Extension.
 * The specification for this mechanism is available in the JDK1.3
 * documentation in the directory
 * $JDK_HOME/docs/guide/extensions/versioning.html. Alternatively it is
 * available online at <a href="https://docs.oracle.com/javase/8/docs/technotes/guides/extensions/versioning.html">
 * https://docs.oracle.com/javase/8/docs/technotes/guides/extensions/versioning.html</a>.</p>
 *
 * @ant.task name="jarlib-display"
 */
public class JarLibDisplayTask extends Task {
    /**
     * The library to display information about.
     */
    private File libraryFile;

    /**
     * Filesets specifying all the libraries
     * to display information about.
     */
    private final List<FileSet> libraryFileSets = new Vector<>();

    /**
     * The JAR library to display information for.
     *
     * @param file The jar library to display information for.
     */
    public void setFile(final File file) {
        this.libraryFile = file;
    }

    /**
     * Adds a set of files about which library data will be displayed.
     *
     * @param fileSet a set of files about which library data will be displayed.
     */
    public void addFileset(final FileSet fileSet) {
        libraryFileSets.add(fileSet);
    }

    /**
     * Execute the task.
     *
     * @throws BuildException if the task fails.
     */
    @Override
    public void execute() throws BuildException {
        validate();

        final LibraryDisplayer displayer = new LibraryDisplayer();
        // Check if list of files to check has been specified
        if (libraryFileSets.isEmpty()) {
            displayer.displayLibrary(libraryFile);
        } else {
            for (FileSet fileSet : libraryFileSets) {
                final DirectoryScanner scanner =
                    fileSet.getDirectoryScanner(getProject());
                final File basedir = scanner.getBasedir();
                for (String filename : scanner.getIncludedFiles()) {
                    displayer.displayLibrary(new File(basedir, filename));
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
        if (null == libraryFile) {
            if (libraryFileSets.isEmpty()) {
                throw new BuildException("File attribute not specified.");
            }
        } else if (!libraryFile.exists()) {
            throw new BuildException("File '%s' does not exist.", libraryFile);
        } else if (!libraryFile.isFile()) {
            throw new BuildException("'%s' is not a file.", libraryFile);
        }
    }
}
