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
import java.io.IOException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Commandline;

/**
 * Patches a file by applying a 'diff' file to it; requires "patch" to be
 * on the execution path.
 *
 * @since Ant 1.1
 *
 * @ant.task category="utility"
 */
public class Patch extends Task {

    private File originalFile;
    private File directory;
    private boolean havePatchfile = false;
    private Commandline cmd = new Commandline();

    /**
     * Halt on error return value from patch invocation.
     */
    private boolean failOnError = false;

    /**
     * The file to patch; optional if it can be inferred from
     * the diff file
     * @param file the file to patch
     */
    public void setOriginalfile(File file) {
        originalFile = file;
    }

    /**
     * The name of a file to send the output to, instead of patching
     * the file(s) in place; optional.
     * @param file the file to send the output to
     * @since Ant 1.6
     */
    public void setDestfile(File file) {
        if (file != null) {
            cmd.createArgument().setValue("-o");
            cmd.createArgument().setFile(file);
        }
    }

    /**
     * The file containing the diff output; required.
     * @param file the file containing the diff output
     */
    public void setPatchfile(File file) {
        if (!file.exists()) {
            throw new BuildException("patchfile " + file + " doesn't exist",
                                     getLocation());
        }
        cmd.createArgument().setValue("-i");
        cmd.createArgument().setFile(file);
        havePatchfile = true;
    }

    /**
     * flag to create backups; optional, default=false
     * @param backups if true create backups
     */
    public void setBackups(boolean backups) {
        if (backups) {
            cmd.createArgument().setValue("-b");
        }
    }

    /**
     * flag to ignore whitespace differences; default=false
     * @param ignore if true ignore whitespace differences
     */
    public void setIgnorewhitespace(boolean ignore) {
        if (ignore) {
            cmd.createArgument().setValue("-l");
        }
    }

    /**
     * Strip the smallest prefix containing <i>num</i> leading slashes
     * from filenames.
     *
     * <p>patch's <i>-p</i> option.
     * @param num number of lines to strip
     * @exception BuildException if num is &lt; 0, or other errors
     */
    public void setStrip(int num) throws BuildException {
        if (num < 0) {
            throw new BuildException("strip has to be >= 0", getLocation());
        }
        cmd.createArgument().setValue("-p" + num);
    }

    /**
     * Work silently unless an error occurs; optional, default=false
     * @param q if true suppress set the -s option on the patch command
     */
    public void setQuiet(boolean q) {
        if (q) {
            cmd.createArgument().setValue("-s");
        }
    }

    /**
     * Assume patch was created with old and new files swapped; optional,
     * default=false
     * @param r if true set the -R option on the patch command
     */
    public void setReverse(boolean r) {
        if (r) {
            cmd.createArgument().setValue("-R");
        }
    }

    /**
     * The directory to run the patch command in, defaults to the
     * project's base directory.
     * @param directory the directory to run the patch command in
     * @since Ant 1.5
     */
    public void setDir(File directory) {
        this.directory = directory;
    }

    /**
     * If <code>true</code>, stop the build process if the patch command
     * exits with an error status.
     * @param value <code>true</code> if it should halt, otherwise
     * <code>false</code>. The default is <code>false</code>.
     * @since Ant 1.8.0
     */
    public void setFailOnError(boolean value) {
        failOnError = value;
    }

    private static final String PATCH = "patch";

    /**
     * execute patch
     * @throws BuildException when it all goes a bit pear shaped
     */
    @Override
    public void execute() throws BuildException {
        if (!havePatchfile) {
            throw new BuildException("patchfile argument is required",
                                     getLocation());
        }
        Commandline toExecute =  (Commandline) cmd.clone();
        toExecute.setExecutable(PATCH);

        if (originalFile != null) {
            toExecute.createArgument().setFile(originalFile);
        }

        Execute exe = new Execute(new LogStreamHandler(this, Project.MSG_INFO,
                                                       Project.MSG_WARN),
                                  null);
        exe.setCommandline(toExecute.getCommandline());

        if (directory == null) {
            exe.setWorkingDirectory(getProject().getBaseDir());
        } else {
            if (!directory.isDirectory()) {
                throw new BuildException(directory + " is not a directory.",
                                         getLocation());
            }
            exe.setWorkingDirectory(directory);
        }

        log(toExecute.describeCommand(), Project.MSG_VERBOSE);
        try {
            int returncode = exe.execute();
            if (Execute.isFailure(returncode)) {
                String msg = "'" + PATCH + "' failed with exit code "
                    + returncode;
                if (failOnError) {
                    throw new BuildException(msg);
                }
                log(msg, Project.MSG_ERR);
            }
        } catch (IOException e) {
            throw new BuildException(e, getLocation());
        }
    }
}
