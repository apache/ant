/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002-2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "Ant" and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */
package org.apache.tools.ant.taskdefs.optional.sos;

import java.io.File;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.LogStreamHandler;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.Path;

/**
 * A base class for creating tasks for executing commands on SourceOffSite.
 *
 *  These tasks were inspired by the VSS tasks
 *
 * @author Jesse Stockall
 */

public abstract class SOS extends Task implements SOSCmd {

    private String sosCmdDir = null;
    private String sosUsername = null;
    private String sosPassword = "";
    private String projectPath = null;
    private String vssServerPath = null;
    private String sosServerPath = null;
    private String sosHome = null;
    private String localPath = null;
    private String version = null;
    private String label = null;
    private String comment = null;
    private String filename = null;

    private boolean noCompress = false;
    private boolean noCache = false;
    private boolean recursive = false;
    private boolean verbose = false;

    /** Commandline to be executed */
    protected Commandline commandLine;

    /**
     * Flag to disable the cache when set;
     * optional needed if SOSHOME is set as an environment variable., default false
     *
     * @param  nocache  True to disable caching.
     */
    public final void setNoCache(boolean nocache) {
        noCache = nocache;
    }

    /**
     * Flag that disables compression when set; optional, default false
     *
     * @param  nocompress  True to disable compression.
     */
    public final void setNoCompress(boolean nocompress) {
        noCompress = nocompress;
    }

    /**
     * Set the directory where soscmd(.exe) is located;
     * optional, soscmd must be on the path if omitted.
     *
     * @param  dir  The new sosCmd value
     */
    public final void setSosCmd(String dir) {
        sosCmdDir = Project.translatePath(dir);
    }

    /**
     * Set the SourceSafe username; required.
     *
     * @param  username  The new username value
     */
    public final void setUsername(String username) {
        sosUsername = username;
    }

    /**
     * Set the SourceSafe password; optional.
     *
     * @param  password  The new password value
     */
    public final void setPassword(String password) {
        sosPassword = password;
    }

    /**
     * Set the SourceSafe project path; required.
     *
     * @param  projectpath  The new projectpath value
     */
    public final void setProjectPath(String projectpath) {
        if (projectpath.startsWith(SOSCmd.PROJECT_PREFIX)) {
            projectPath = projectpath;
        } else {
            projectPath = SOSCmd.PROJECT_PREFIX + projectpath;
        }
    }

    /**
     * Set the path to the location of the ss.ini file;
     * required.
     *
     * @param  vssServerPath  The new vssServerPath value
     */
    public final void setVssServerPath(String vssServerPath) {
        this.vssServerPath = vssServerPath;
    }

    /**
     * The path to the SourceOffSite home directory
     *
     * @param  sosHome  The new sosHome value
     */
    public final void setSosHome(String sosHome) {
        this.sosHome = sosHome;
    }

    /**
     * Sets the address and port of SourceOffSite Server,
     * for example 192.168.0.1:8888.; required.
     *
     * @param  sosServerPath  The new sosServerPath value
     */
    public final void setSosServerPath(String sosServerPath) {
        this.sosServerPath = sosServerPath;
    }

    /**
     * Override the working directory and get to the specified path; optional.
     *
     * @param  path  The new localPath value
     */
    public final void setLocalPath(Path path) {
        localPath = path.toString();
    }

    /**
     * Enable verbose output; optional, default false
     *
     * @param  verbose  True for verbose output.
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    // Special setters for the sub-classes

    protected void setInternalFilename(String file) {
        filename = file;
    }

    protected void setInternalRecursive(boolean recurse) {
        recursive = recurse;
    }

    protected void setInternalComment(String text) {
        comment = text;
    }

    protected void setInternalLabel(String text) {
        label = text;
    }

    protected void setInternalVersion(String text) {
        version = text;
    }

    /**
     * Get the executable to run. Add the path if it was specifed in the build file
     *
     * @return    String the executable to run
     */
    protected String getSosCommand() {
        if (sosCmdDir == null) {
            return COMMAND_SOS_EXE;
        } else {
            return sosCmdDir + File.separator + COMMAND_SOS_EXE;
        }
    }

    /**
     * Get the comment
     * @return String if it was set, null if not
     */
    protected String getComment() {
        return comment;
    }

    /**
     * Get the version
     * @return String if it was set, null if not
     */
    protected String getVersion() {
        return version;
    }

    /**
     * Get the label
     * @return String if it was set, null if not
     */
    protected String getLabel() {
        return label;
    }

    /**
     * Get the username
     * @return String if it was set, null if not
     */
    protected String getUsername() {
        return sosUsername;
    }

    /**
     * Get the password
     * @return String empty string if it wans't set
     */
    protected String getPassword() {
        return sosPassword;
    }

    /**
     * Get the project path
     * @return String if it was set, null if not
     */
    protected String getProjectPath() {
        return projectPath;
    }

    /**
     * Get the VSS server path
     * @return String if it was set, null if not
     */
    protected String getVssServerPath() {
        return vssServerPath;
    }

    /**
     * Get the SOS home directory
     * @return String if it was set, null if not
     */
    protected String getSosHome() {
        return sosHome;
    }

    /**
     * Get the SOS serve path
     * @return String if it was set, null if not
     */
    protected String getSosServerPath() {
        return sosServerPath;
    }

    /**
     * Get the filename to be acted upon
     * @return String if it was set, null if not
     */
    protected String getFilename() {
        return filename;
    }

    /**
     * Get the NoCompress flag
     *
     * @return    String the 'nocompress' Flag if the attribute was 'true', otherwise an empty string
     */
    protected String getNoCompress() {
        return noCompress ? FLAG_NO_COMPRESSION : "";
    }

    /**
     * Get the NoCache flag
     *
     * @return    String the 'nocache' Flag if the attribute was 'true', otherwise an empty string
     */
    protected String getNoCache() {
        return noCache ? FLAG_NO_CACHE : "";
    }

    /**
     * Get the 'verbose' Flag
     *
     * @return    String the 'verbose' Flag if the attribute was 'true', otherwise an empty string
     */
    protected String getVerbose() {
        return verbose ? FLAG_VERBOSE : "";
    }

    /**
     * Get the 'recursive' Flag
     *
     * @return    String the 'recursive' Flag if the attribute was 'true', otherwise an empty string
     */
    protected String getRecursive() {
        return recursive ? FLAG_RECURSION : "";
    }

    /**
     * Builds and returns the working directory.
     * <p>
     * The localpath is created if it didn't exist
     *
     * @return    String the absolute path of the working directory
     */
    protected String getLocalPath() {
        if (localPath == null) {
            return getProject().getBaseDir().getAbsolutePath();
        } else {
            // make sure localDir exists, create it if it doesn't
            File dir = getProject().resolveFile(localPath);
            if (!dir.exists()) {
                boolean done = dir.mkdirs();
                if (!done) {
                    String msg = "Directory " + localPath + " creation was not " +
                            "successful for an unknown reason";
                    throw new BuildException(msg, getLocation());
                }
                getProject().log("Created dir: " + dir.getAbsolutePath());
            }
            return dir.getAbsolutePath();
        }
    }

    /**
     * Subclasses implement the logic required to construct the command line.
     *
     * @return   The command line to execute.
     */
    abstract Commandline buildCmdLine();


    /**
     * Execute the created command line.
     *
     * @throws BuildException
     */
    public void execute()
        throws BuildException {
        int result = 0;
        buildCmdLine();
        result = run(commandLine);
        if (result == 255) {  // This is the exit status
            String msg = "Failed executing: " + commandLine.toString();
            throw new BuildException(msg, getLocation());
        }
    }

    /**
     * Execute the created command line.
     *
     * @param  cmd              The command line to run.
     * @return                  int the exit code.
     * @throws  BuildException
     */
    protected int run(Commandline cmd) {
        try {
            Execute exe = new Execute(new LogStreamHandler(this,
                    Project.MSG_INFO,
                    Project.MSG_WARN));

            exe.setAntRun(getProject());
            exe.setWorkingDirectory(getProject().getBaseDir());
            exe.setCommandline(cmd.getCommandline());
            exe.setVMLauncher(false);  // Use the OS VM launcher so we get environment variables
            return exe.execute();
        } catch (java.io.IOException e) {
            throw new BuildException(e, getLocation());
        }
    }

    /** Sets the executable and add the required attributes to the command line. */
    protected void getRequiredAttributes() {
        // Get the path to the soscmd(.exe)
        commandLine.setExecutable(getSosCommand());
        // SOS server address is required
        if (getSosServerPath() == null) {
            throw new BuildException("sosserverpath attribute must be set!", getLocation());
        }
        commandLine.createArgument().setValue(FLAG_SOS_SERVER);
        commandLine.createArgument().setValue(getSosServerPath());
        // Login info is required
        if (getUsername() == null) {
            throw new BuildException("username attribute must be set!", getLocation());
        }
        commandLine.createArgument().setValue(FLAG_USERNAME);
        commandLine.createArgument().setValue(getUsername());
        // The SOS class knows that the SOS server needs the password flag,
        // even if there is no password ,so we send a " "
        commandLine.createArgument().setValue(FLAG_PASSWORD);
        commandLine.createArgument().setValue(getPassword());
        // VSS Info is required
        if (getVssServerPath() == null) {
            throw new BuildException("vssserverpath attribute must be set!", getLocation());
        }
        commandLine.createArgument().setValue(FLAG_VSS_SERVER);
        commandLine.createArgument().setValue(getVssServerPath());
        // VSS project is required
        if (getProjectPath() == null) {
            throw new BuildException("projectpath attribute must be set!", getLocation());
        }
        commandLine.createArgument().setValue(FLAG_PROJECT);
        commandLine.createArgument().setValue(getProjectPath());
    }

    /** Adds the optional attributes to the command line. */
    protected void getOptionalAttributes() {
        // -verbose
        commandLine.createArgument().setValue(getVerbose());
        // Disable Compression
        commandLine.createArgument().setValue(getNoCompress());
        // Path to the SourceOffSite home directory /home/user/.sos
        if (getSosHome() == null) {
            // If -soshome was not specified then we can look for nocache
            commandLine.createArgument().setValue(getNoCache());
        } else {
            commandLine.createArgument().setValue(FLAG_SOS_HOME);
            commandLine.createArgument().setValue(getSosHome());
        }
        //If a working directory was specified then add it to the command line
        if (getLocalPath() != null) {
            commandLine.createArgument().setValue(FLAG_WORKING_DIR);
            commandLine.createArgument().setValue(getLocalPath());
        }
    }
}
