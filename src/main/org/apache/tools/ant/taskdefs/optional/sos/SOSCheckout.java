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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Commandline;

/**
 * Retrieves and locks files in Visual SourceSafe via a SourceOffSite server.
 *
 * <p>
 * The following attributes are interpretted:
 * <table border="1">
 *   <tr>
 *     <th>Attribute</th>
 *     <th>Values</th>
 *     <th>Required</th>
 *   </tr>
 *   <tr>
 *     <td>soscmddir</td>
 *     <td>Directory which contains soscmd(.exe) <br>
 *     soscmd(.exe) must be in the path if this is not specified</td>
 *     <td>No</td>
 *   </tr>
 *   <tr>
 *      <td>vssserverpath</td>
 *      <td>path to the srcsafe.ini  - eg. \\server\vss\srcsafe.ini</td>
 *      <td>Yes</td>
 *   </tr>
 *   <tr>
 *      <td>sosserverpath</td>
 *      <td>address and port of the SOS server  - eg. 192.168.0.1:8888</td>
 *      <td>Yes</td>
 *   </tr>
 *   <tr>
 *      <td>projectpath</td>
 *      <td>SourceSafe project path without the "$"</td>
 *      <td>Yes</td>
 *   </tr>
 *   <tr>
 *      <td>file</td>
 *      <td>Filename to act upon<br> If no file is specified then act upon the project</td>
 *      <td>No</td>
 *   </tr>
 *   <tr>
 *      <td>username</td>
 *      <td>SourceSafe username</td>
 *      <td>Yes</td>
 *   </tr>
 *   <tr>
 *      <td>password</td>
 *      <td>SourceSafe password</td>
 *      <td>No</td>
 *   </tr>
 *   <tr>
 *      <td>localpath</td>
 *      <td>Override the working directory and get to the specified path</td>
 *      <td>No</td>
 *   </tr>
 *   <tr>
 *      <td>soshome</td>
 *      <td>The path to the SourceOffSite home directory</td>
 *      <td>No</td>
 *   </tr>
 *   <tr>
 *      <td>nocompression</td>
 *      <td>true or false - disable compression</td>
 *      <td>No</td>
 *   </tr>
 *   <tr>
 *      <td>recursive</td>
 *      <td>true or false - Only works with the CheckOutProject command</td>
 *      <td>No</td>
 *   </tr>
 *   <tr>
 *      <td>nocache</td>
 *      <td>true or false - Only needed if SOSHOME is set as an enviroment variable</td>
 *      <td>No</td>
 *   </tr>
 *   <tr>
 *      <td>verbose</td>
 *      <td>true or false - Status messages are displayed</td>
 *      <td>No</td>
 *   </tr>
 * </table>
 *
 * @author    <a href="mailto:jesse@cryptocard.com">Jesse Stockall</a>
 */

public class SOSCheckout extends SOS {
    Commandline commandLine;


    /**
     * Executes the task.
     * <br>
     * Builds a command line to execute soscmd and then calls Exec's run method
     * to execute the command line.
     *
     * @exception  BuildException  Description of Exception
     */
    public void execute() throws BuildException {
        int result = 0;
        buildCmdLine();
        result = run(commandLine);
        if (result == 255) {
            // This is the exit status
            String msg = "Failed executing: " + commandLine.toString();
            throw new BuildException(msg, location);
        }
    }


    /**
     * Build the command line <br>
     *
     * CheckOutFile required parameters: -server -name -password -database -project -file<br>
     * CheckOutFile optional parameters: -workdir -verbose -nocache -nocompression -soshome<br>
     *
     * CheckOutProject required parameters: -server -name -password -database -project<br>
     * CheckOutProject optional parameters:-workdir -recursive -verbose -nocache
     * -nocompression -soshome<br>
     *
     * @return    Commandline the generated command to be executed
     */
    protected Commandline buildCmdLine() {
        commandLine = new Commandline();
        // Get the path to the soscmd(.exe)
        commandLine.setExecutable(getSosCommand());
        // If we find a "file" attribute then act on a file otherwise act on a project
        if (getFilename() != null) {
            // add -command CheckOutFile to the commandline
            commandLine.createArgument().setValue(SOSCmd.FLAG_COMMAND);
            commandLine.createArgument().setValue(SOSCmd.COMMAND_CHECKOUT_FILE);
            // add -file xxxxx to the commandline
            commandLine.createArgument().setValue(SOSCmd.FLAG_FILE);
            commandLine.createArgument().setValue(getFilename());
        } else {
            // add -command CheckOutProject to the commandline
            commandLine.createArgument().setValue(SOSCmd.FLAG_COMMAND);
            commandLine.createArgument().setValue(SOSCmd.COMMAND_CHECKOUT_PROJECT);
            // look for a recursive option
            commandLine.createArgument().setValue(getRecursive());
        }
        // SOS server address is required
        if (getSosServerPath() == null) {
            throw new BuildException("sosserverpath attribute must be set!", location);
        }
        commandLine.createArgument().setValue(SOSCmd.FLAG_SOS_SERVER);
        commandLine.createArgument().setValue(getSosServerPath());
        // Login info is required
        if (getUsername() == null) {
            throw new BuildException("username attribute must be set!", location);
        }
        commandLine.createArgument().setValue(SOSCmd.FLAG_USERNAME);
        commandLine.createArgument().setValue(getUsername());
        // The SOS class knows that the SOS server needs the password flag,
        // even if there is no password ,so we send a " "
        commandLine.createArgument().setValue(SOSCmd.FLAG_PASSWORD);
        commandLine.createArgument().setValue(getPassword());
        // VSS Info is required
        if (getVssServerPath() == null) {
            throw new BuildException("vssserverpath attribute must be set!", location);
        }
        commandLine.createArgument().setValue(SOSCmd.FLAG_VSS_SERVER);
        commandLine.createArgument().setValue(getVssServerPath());
        // VSS project is required
        if (getProjectPath() == null) {
            throw new BuildException("projectpath attribute must be set!", location);
        }
        commandLine.createArgument().setValue(SOSCmd.FLAG_PROJECT);
        commandLine.createArgument().setValue(getProjectPath());

        // The following options are optional.

        // -verbose
        commandLine.createArgument().setValue(getVerbose());
        // Disable Compression
        commandLine.createArgument().setValue(getNoCompress());
        // Path to the SourceOffSite home directory /home/user/.sos
        if (getSosHome() == null) {
            // If -soshome was not specified then we can look for nocache
            commandLine.createArgument().setValue(getNoCache());
        } else {
            commandLine.createArgument().setValue(SOSCmd.FLAG_SOS_HOME);
            commandLine.createArgument().setValue(getSosHome());
        }
        // If a working directory was specified then add it to the command line
        if (getLocalPath() != null) {
            commandLine.createArgument().setValue(SOSCmd.FLAG_WORKING_DIR);
            commandLine.createArgument().setValue(getLocalPath());
        }
        return commandLine;
    }
}

