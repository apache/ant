/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
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
 * <p>
 * The class extends the 'exec' task as it operates by executing the soscmd(.exe) program
 * supplied with SOS. By default the task expects soscmd(.exe) to be in the path,
 * you can override this be specifying the sosdir attribute.
 * </p>
 * <p>
 * This class provides set and get methods for the following attributes
 * <br> 'vssserverpath'
 * <br> 'sosserverpath'
 * <br> 'vsspath'
 * <br> 'projectpath'
 * <br> 'username'
 * <br> 'password'
 * <br> 'soscmddir'
 * <br> 'file'
 * <br> 'soshome'
 * <br> 'localpath"
 * <br> 'comment'
 * <br> 'label'
 * <br> 'version'
 * <br> 'recursive'
 * <br> 'verbose'
 * <br> 'nocache'
 * <br> 'nocompression'
 * <br>
 *  It also contains constants for the flags that can be passed to SOS.
 * <p>
 *  These tasks were inspired by the VSS tasks
 *
 * @author    <a href="mailto:jesse@cryptocard.com">Jesse Stockall</a>
 */

public abstract class SOS extends Task {

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


    /**
     * flag to disable the cache when set;
     * optional needed if SOSHOME is set as an environment variable.
     *
     * @param  nocache  The new noCache value
     */
    public void setNoCache(boolean nocache) {
        noCache = nocache;
    }


    /**
     * Flag that disables compression when set; optional, default 
     *
     * @param  nocompress  true to disable compression
     */
    public void setNoCompress(boolean nocompress) {
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
     * Set the SourceSafe project path without the "$" prefix; required
     *
     * @param  projectpath  The new projectPath value
     */
    public final void setProjectPath(String projectpath) {
        projectPath = SOSCmd.PROJECT_PREFIX + projectpath;
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
     * Set the address and port of SourceOffSite Server,
     * eg. 192.168.0.1:8888 ; required. 
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
    public void setLocalPath(Path path) {
        localPath = path.toString();
    }


    /**
     * Set the Filename to act upon; optional.
     * If no file is specified then the tasks 
     * act upon the project
     *
     * @param  filename  The new file value
     */
    public final void setFile(String filename) {
        this.filename = filename;
    }


    /**
     * Enable verbose output; optional, default false
     *
     * @param  verbose  The new verbose value
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }


    /**
     * Flag to recursively apply the action (not valid
     * on all SOS tasks ); optional, default false
     *
     * @param  recursive  The new recursive value
     */
    public void setRecursive(boolean recursive) {
        this.recursive = recursive;
    }


    /**
     * Set the a version number to get - 
     * only works with the SOSGet on a file; optional.
     *
     * @param  version  The new version value
     */
    public void setVersion(String version) {
        this.version = version;
    }


    /**
     * Set the labeled version to operate on in SourceSafe
     *
     * @param  label  The new label value
     */
    public void setLabel(String label) {
        this.label = label;
    }


    /**
     * Set the comment to apply to all files being labelled;
     * optional, only valid in SOSLabel
     *
     * @param  comment  The new comment value
     */
    public void setComment(String comment) {
        this.comment = comment;
    }


    /**
     * Get the executable to run. Add the path if it was specifed in the build file
     *
     * @return    String the executable to run
     */
    public String getSosCommand() {
        if (sosCmdDir == null) {
            return SOSCmd.COMMAND_SOS_EXE;
        } else {
            return sosCmdDir + File.separator + SOSCmd.COMMAND_SOS_EXE;
        }
    }

    /**
     * Get the comment
     * @return String if it was set, null if not
     */
    public String getComment() {
        return comment;
    }


    /**
     * Get the version
     * @return String if it was set, null if not
     */
    public String getVersion() {
        return version;
    }


    /**
     * Get the label
     * @return String if it was set, null if not
     */
    public String getLabel() {
        return label;
    }


    /**
     * Get the username
     * @return String if it was set, null if not
     */
    public String getUsername() {
        return sosUsername;
    }


    /**
     * Get the password
     * @return String empty string if it wans't set
     */
    public String getPassword() {
        return sosPassword;
    }


    /**
     * Get the project path
     * @return String if it was set, null if not
     */
    public String getProjectPath() {
        return projectPath;
    }


    /**
     * Get the VSS server path
     * @return String if it was set, null if not
     */
    public String getVssServerPath() {
        return vssServerPath;
    }


    /**
     * Get the SOS home directory
     * @return String if it was set, null if not
     */
    public String getSosHome() {
        return sosHome;
    }


    /**
     * Get the SOS serve path
     * @return String if it was set, null if not
     */
    public String getSosServerPath() {
        return sosServerPath;
    }


    /**
     * Get the filename to be acted upon
     * @return String if it was set, null if not
     */
    public String getFilename() {
        return filename;
    }


    /**
     * Get the NoCompress flag
     *
     * @return    String the 'nocompress' Flag if the attribute was 'true', otherwise an empty string
     */
    public String getNoCompress() {
        if (!noCompress) {
            return "";
        } else {
            return SOSCmd.FLAG_NO_COMPRESSION;
        }
    }


    /**
     * Get the NoCache flag
     *
     * @return    String the 'nocache' Flag if the attribute was 'true', otherwise an empty string
     */
    public String getNoCache() {
        if (!noCache) {
            return "";
        } else {
            return SOSCmd.FLAG_NO_CACHE;
        }
    }


    /**
     * Get the 'verbose' Flag
     *
     * @return    String the 'verbose' Flag if the attribute was 'true', otherwise an empty string
     */
    public String getVerbose() {
        if (!verbose) {
            return "";
        } else {
            return SOSCmd.FLAG_VERBOSE;
        }
    }


    /**
     * Get the 'recursive' Flag
     *
     * @return    String the 'recursive' Flag if the attribute was 'true', otherwise an empty string
     */
    public String getRecursive() {
        if (!recursive) {
            return "";
        } else {
            return SOSCmd.FLAG_RECURSION;
        }
    }


    /**
     * Builds and returns the working directory.
     * <p>
     * The localpath is created if it didn't exist
     *
     * @return    String the absolute path of the working directory
     */
    public String getLocalPath() {
        if (localPath == null) {
            return project.getBaseDir().getAbsolutePath();
        } else {
            // make sure localDir exists, create it if it doesn't
            File dir = project.resolveFile(localPath);
            if (!dir.exists()) {
                boolean done = dir.mkdirs();
                if (!done) {
                    String msg = "Directory " + localPath + " creation was not " +
                            "successful for an unknown reason";
                    throw new BuildException(msg, location);
                }
                project.log("Created dir: " + dir.getAbsolutePath());
            }
            return dir.getAbsolutePath();
        }
    }


    /**
     * Execute the created command line
     *
     * @param  cmd              Description of Parameter
     * @return                  int the exit code
     * @throws  BuildException
     */
    protected int run(Commandline cmd) {
        try {
            Execute exe = new Execute(new LogStreamHandler(this,
                    Project.MSG_INFO,
                    Project.MSG_WARN));

            exe.setAntRun(project);
            exe.setWorkingDirectory(project.getBaseDir());
            exe.setCommandline(cmd.getCommandline());
            return exe.execute();
        } catch (java.io.IOException e) {
            throw new BuildException(e, location);
        }
    }
}

