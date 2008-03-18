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
 * available online at <a href="http://java.sun.com/j2se/1.3/docs/guide/extensions/versioning.html">
 * http://java.sun.com/j2se/1.3/docs/guide/extensions/versioning.html</a>.</p>
 *
 * @ant.task name="jarlib-display"
 */
public class JarLibDisplayTask extends Task {
    /**
     * The library to display information about.
     */
    private File libraryFile;

    /**
     * Filesets specifying all the librarys
     * to display information about.
     */
    private final Vector libraryFileSets = new Vector();

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
        libraryFileSets.addElement(fileSet);
    }

    /**
     * Execute the task.
     *
     * @throws BuildException if the task fails.
     */
    public void execute() throws BuildException {
        validate();

        final LibraryDisplayer displayer = new LibraryDisplayer();
        // Check if list of files to check has been specified
        if (!libraryFileSets.isEmpty()) {
            final Iterator iterator = libraryFileSets.iterator();
            while (iterator.hasNext()) {
                final FileSet fileSet = (FileSet) iterator.next();
                final DirectoryScanner scanner
                    = fileSet.getDirectoryScanner(getProject());
                final File basedir = scanner.getBasedir();
                final String[] files = scanner.getIncludedFiles();
                for (int i = 0; i < files.length; i++) {
                    final File file = new File(basedir, files[ i ]);
                    displayer.displayLibrary(file);
                }
            }
        } else {
            displayer.displayLibrary(libraryFile);
        }
    }

    /**
     * Validate the tasks parameters.
     *
     * @throws BuildException if invalid parameters found
     */
    private void validate() throws BuildException {
        if (null == libraryFile && libraryFileSets.isEmpty()) {
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
