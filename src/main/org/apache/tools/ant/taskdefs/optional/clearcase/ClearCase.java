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

package org.apache.tools.ant.taskdefs.optional.clearcase;

import java.io.File;
import java.io.IOException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.ExecTask;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.LogStreamHandler;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.util.FileUtils;

/**
 * A base class for creating tasks for executing commands on ClearCase.
 * <p>
 * By default the task expects the cleartool executable to be in the
 * path, you can override this be specifying the cleartooldir
 * attribute.
 * </p>
 * <p>
 * This class provides set and get methods for the 'viewpath' and 'objselect'
 * attribute. It also contains constants for the flags that can be passed to
 * cleartool.
 * </p>
 *
 */
public abstract class ClearCase extends Task {
    /**
     * Constant for the thing to execute
     */
    private static final String CLEARTOOL_EXE = "cleartool";
    /**
     * The 'Update' command
     */
    public static final String COMMAND_UPDATE = "update";
    /**
     * The 'Checkout' command
     */
    public static final String COMMAND_CHECKOUT = "checkout";
    /**
     * The 'Checkin' command
     */
    public static final String COMMAND_CHECKIN = "checkin";
    /**
     * The 'UndoCheckout' command
     */
    public static final String COMMAND_UNCHECKOUT = "uncheckout";
    /**
     * The 'Lock' command
     */
    public static final String COMMAND_LOCK = "lock";
    /**
     * The 'Unlock' command
     */
    public static final String COMMAND_UNLOCK = "unlock";
    /**
     * The 'Mkbl' command
     */
    public static final String COMMAND_MKBL = "mkbl";
    /**
     * The 'Mklabel' command
     */
    public static final String COMMAND_MKLABEL = "mklabel";
    /**
     * The 'Mklbtype' command
     */
    public static final String COMMAND_MKLBTYPE = "mklbtype";
    /**
     * The 'Rmtype' command
     */
    public static final String COMMAND_RMTYPE = "rmtype";
    /**
     * The 'LsCheckout' command
     */
    public static final String COMMAND_LSCO = "lsco";
    /**
     * The 'Mkelem' command
     */
    public static final String COMMAND_MKELEM = "mkelem";
    /**
     * The 'Mkattr' command
     */
    public static final String COMMAND_MKATTR = "mkattr";
    /**
     * The 'Mkdir' command
     */
    public static final String COMMAND_MKDIR = "mkdir";

    private String mClearToolDir = "";
    private String mviewPath = null;
    private String mobjSelect = null;
    private int pcnt = 0;
    private boolean mFailonerr = true;

    /**
     * Set the directory where the cleartool executable is located.
     *
     * @param dir the directory containing the cleartool executable
     */
    public final void setClearToolDir(String dir) {
        mClearToolDir = FileUtils.translatePath(dir);
    }

    /**
     * Builds and returns the command string to execute cleartool
     *
     * @return String containing path to the executable
     */
    protected final String getClearToolCommand() {
        String toReturn = mClearToolDir;
        if (!toReturn.isEmpty() && !toReturn.endsWith("/")) {
            toReturn += "/";
        }

        toReturn += CLEARTOOL_EXE;

        return toReturn;
    }

    /**
     * Set the path to the item in a ClearCase view to operate on.
     *
     * @param viewPath Path to the view directory or file
     */
    public final void setViewPath(String viewPath) {
        mviewPath = viewPath;
    }

    /**
     * Get the path to the item in a clearcase view
     *
     * @return mviewPath
     */
    public String getViewPath() {
        return mviewPath;
    }

    /**
     * Get the basename path of the item in a clearcase view
     *
     * @return basename
     */
    public String getViewPathBasename() {
        return (new File(mviewPath)).getName();
    }

    /**
     * Set the object to operate on.
     *
     * @param objSelect object to operate on
     */
    public final void setObjSelect(String objSelect) {
        mobjSelect = objSelect;
    }

    /**
     * Get the object to operate on
     *
     * @return mobjSelect
     */
    public String getObjSelect() {
        return mobjSelect;
    }

    /**
     * Execute the given command are return success or failure
     * @param cmd command line to execute
     * @return the exit status of the subprocess or <code>INVALID</code>
     */
    protected int run(Commandline cmd) {
        try {
            Project aProj = getProject();
            Execute exe = new Execute(
                new LogStreamHandler(this, Project.MSG_INFO, Project.MSG_WARN));
            exe.setAntRun(aProj);
            exe.setWorkingDirectory(aProj.getBaseDir());
            exe.setCommandline(cmd.getCommandline());
            return exe.execute();
        } catch (IOException e) {
            throw new BuildException(e, getLocation());
        }
    }

    /**
     * Execute the given command, and return it's output
     * @param cmdline command line to execute
     * @return output of the command line
     * @deprecated use the two arg version instead
     */
    @Deprecated
    protected String runS(Commandline cmdline) {
        return runS(cmdline, false);
    }

    /**
     * Execute the given command, and return it's output
     * @param cmdline command line to execute
     * @param failOnError whether to fail the build if the command fails
     * @return output of the command line
     * @since Ant 1.10.6
     */
    protected String runS(Commandline cmdline, boolean failOnError) {
        String   outV  = "opts.cc.runS.output" + pcnt++;
        ExecTask exe   = new ExecTask(this);
        Commandline.Argument arg = exe.createArg();

        exe.setExecutable(cmdline.getExecutable());
        arg.setLine(Commandline.toString(cmdline.getArguments()));
        exe.setOutputproperty(outV);
        exe.setFailonerror(failOnError);
        exe.execute();

        return getProject().getProperty(outV);
    }

    /**
     * If true, command will throw an exception on failure.
     *
     * @param failonerr the status to set the flag to
     * @since ant 1.6.1
     */
    public void setFailOnErr(boolean failonerr) {
        mFailonerr = failonerr;
    }

    /**
     * Get failonerr flag status
     *
     * @return boolean containing status of failonerr flag
     * @since ant 1.6.1
     */
    public boolean getFailOnErr() {
        return mFailonerr;
    }

}
