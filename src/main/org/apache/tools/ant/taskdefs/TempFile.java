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
package org.apache.tools.ant.taskdefs;

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.util.FileUtils;

/**
 *  This task sets a property to the name of a temporary file.
 *  Unlike {@link File#createTempFile}, this task does not (by default) actually create the
 *  temporary file, but it does guarantee that the file did not
 *  exist when the task was executed.
 * <p>
 * Examples
 * <pre>&lt;tempfile property="temp.file" /&gt;</pre>
 * create a temporary file
 * <pre>&lt;tempfile property="temp.file" suffix=".xml" /&gt;</pre>
 * create a temporary file with the .xml suffix.
 * <pre>&lt;tempfile property="temp.file" destDir="build"/&gt;</pre>
 * create a temp file in the build subdir
 * @since       Ant 1.5
 * @ant.task
 */

public class TempFile extends Task {

    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    /**
     * Name of property to set.
     */
    private String property;

    /**
     * Directory to create the file in. Can be null.
     */
    private File destDir = null;

    /**
     * Prefix for the file.
     */
    private String prefix;

    /**
     * Suffix for the file.
     */
    private String suffix = "";

    /** deleteOnExit flag */
    private boolean deleteOnExit;

    /** createFile flag */
    private boolean createFile;

    /**
     * Sets the property you wish to assign the temporary file to.
     *
     * @param  property  The property to set
     * @ant.attribute group="required"
     */
    public void setProperty(String property) {
        this.property = property;
    }

    /**
     * Sets the destination directory. If not set,
     * the basedir directory is used instead.
     *
     * @param  destDir  The new destDir value
     */
    public void setDestDir(File destDir) {
        this.destDir = destDir;
    }

    /**
     * Sets the optional prefix string for the temp file.
     *
     * @param  prefix  string to prepend to generated string
     */
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    /**
     * Sets the optional suffix string for the temp file.
     *
     * @param  suffix  suffix including any ".", e.g ".xml"
     */
    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    /**
     * Set whether the tempfile created by this task should be set
     * for deletion on normal VM exit.
     * @param deleteOnExit boolean flag.
     */
    public void setDeleteOnExit(boolean deleteOnExit) {
        this.deleteOnExit = deleteOnExit;
    }

    /**
     * Learn whether deleteOnExit is set for this tempfile task.
     * @return boolean deleteOnExit flag.
     */
    public boolean isDeleteOnExit() {
        return deleteOnExit;
    }

    /**
     * If set the file is actually created, if not just a name is created.
     * @param createFile boolean flag.
     */
    public void setCreateFile(boolean createFile) {
        this.createFile = createFile;
    }

    /**
     * Learn whether createFile flag is set for this tempfile task.
     * @return the createFile flag.
     */
    public boolean isCreateFile() {
        return createFile;
    }

    /**
     * Creates the temporary file.
     *
     * @exception  BuildException  if something goes wrong with the build
     */
    @Override
    public void execute() throws BuildException {
        if (property == null || property.isEmpty()) {
            throw new BuildException("no property specified");
        }
        if (destDir == null) {
            destDir = getProject().resolveFile(".");
        }
        File tfile = FILE_UTILS.createTempFile(getProject(), prefix, suffix, destDir,
                    deleteOnExit, createFile);
        getProject().setNewProperty(property, tfile.toString());
    }
}
