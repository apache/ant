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

package org.apache.tools.ant.taskdefs.optional.ccm;


import java.io.File;
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.FileSet;

/**
 * Class common to all check commands (checkout, checkin,checkin default task);
 * @ant.task ignore="true"
 */
public class CCMCheck extends Continuus {
    /**
     * -comment flag -- comment to attach to the file
     */
    public static final String FLAG_COMMENT = "/comment";

    /**
     *  -task flag -- associate checkout task with task
     */
    public static final String FLAG_TASK = "/task";

    private File file = null;
    private String comment = null;
    private String task = null;

    // CheckStyle:VisibilityModifier OFF - bc

    protected Vector<FileSet> filesets = new Vector<>();

    // CheckStyle:VisibilityModifier ON

    /**
     * Get the value of file.
     * @return value of file.
     */
    public File getFile() {
        return file;
    }

    /**
     * Sets the path to the file that the command will operate on.
     * @param v  Value to assign to file.
     */
    public void setFile(File v) {
        log("working file " + v, Project.MSG_VERBOSE);
        this.file = v;
    }

    /**
     * Get the value of comment.
     * @return value of comment.
     */
    public String getComment() {
        return comment;
    }

    /**
     * Specifies a comment.
     * @param v  Value to assign to comment.
     */
    public void setComment(String v) {
        this.comment = v;
    }

    /**
     * Get the value of task.
     * @return value of task.
     */
    public String getTask() {
        return task;
    }

    /**
     * Specifies the task number used to check
     * in the file (may use 'default').
     * @param v  Value to assign to task.
     */
    public void setTask(String v) {
        this.task = v;
    }

    /**
     * Adds a set of files to copy.
     * @param set the set of files
     */
    public void addFileset(FileSet set) {
        filesets.addElement(set);
    }


    /**
     * Executes the task.
     * <p>
     * Builds a command line to execute ccm and then calls Exec's run method
     * to execute the command line.
     * </p>
     * @throws BuildException on error
     */
    @Override
    public void execute() throws BuildException {

        if (file == null && filesets.isEmpty()) {
            throw new BuildException(
                "Specify at least one source - a file or a fileset.");
        }

        if (file != null && file.exists() && file.isDirectory()) {
            throw new BuildException("CCMCheck cannot be generated for directories");
        }

        if (file != null && !filesets.isEmpty()) {
            throw new BuildException("Choose between file and fileset !");
        }

        if (getFile() != null) {
            doit();
            return;
        }

        for (FileSet fs : filesets) {
            final File basedir = fs.getDir(getProject());
            DirectoryScanner ds = fs.getDirectoryScanner(getProject());
            for (String srcFile : ds.getIncludedFiles()) {
                setFile(new File(basedir, srcFile));
                doit();
            }
        }
    }

    /**
     * check the file given by getFile().
     */
    private void doit() {
        Commandline commandLine = new Commandline();

        // build the command line from what we got the format is
        // ccm co /t .. files
        // as specified in the CCM.EXE help

        commandLine.setExecutable(getCcmCommand());
        commandLine.createArgument().setValue(getCcmAction());

        checkOptions(commandLine);

        int result = run(commandLine);
        if (Execute.isFailure(result)) {
            throw new BuildException("Failed executing: " + commandLine,
                getLocation());
        }
    }

    /**
     * Check the command line options.
     */
    private void checkOptions(Commandline cmd) {
        if (getComment() != null) {
            cmd.createArgument().setValue(FLAG_COMMENT);
            cmd.createArgument().setValue(getComment());
        }

        if (getTask() != null) {
            cmd.createArgument().setValue(FLAG_TASK);
            cmd.createArgument().setValue(getTask());
        }

        if (getFile() != null) {
            cmd.createArgument().setValue(file.getAbsolutePath());
        }
    }

}
