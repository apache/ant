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

package org.apache.tools.ant.taskdefs.optional.vss;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Commandline;

/**
 * Creates a new project in Microsoft Visual SourceSafe.
 * <p>
 * The following attributes are interpreted:
 * <table border="1">
 *   <tr>
 *     <th>Attribute</th>
 *     <th>Values</th>
 *     <th>Required</th>
 *   </tr>
 *   <tr>
 *      <td>login</td>
 *      <td>username,password</td>
 *      <td>No</td>
 *   </tr>
 *   <tr>
 *      <td>vsspath</td>
 *      <td>SourceSafe path of project to be created</td>
 *      <td>Yes</td>
 *   </tr>
 *   <tr>
 *      <td>ssdir</td>
 *      <td>directory where <code>ss.exe</code> resides. By default the task
 *      expects it to be in the PATH.</td>
 *      <td>No</td>
 *   </tr>
 *   <tr>
 *      <td>quiet</td>
 *      <td>suppress output (off by default)</td>
 *      <td>No</td>
 *   </tr>
 *   <tr>
 *      <td>failOnError</td>
 *      <td>fail if there is an error creating the project (true by default)</td>
 *      <td>No</td>
 *   </tr>   
 *   <tr>
 *      <td>autoresponse</td>
 *      <td>What to respond with (sets the -I option). By default, -I- is
 *      used; values of Y or N will be appended to this.</td>
 *      <td>No</td>
 *   </tr>
 *   <tr>
 *      <td>comment</td>
 *      <td>The comment to use for this label. Empty or '-' for no comment.</td>
 *      <td>No</td>
 *   </tr>
 *      
 * </table>
 *
 * @author Gary S. Weaver
 * @ant.task name="vsscreate" category="scm"
 */
public class MSVSSCREATE extends MSVSS {

    private String m_AutoResponse = null;
    private String m_Name = null;
    private String m_Comment = "-";
    private boolean m_Quiet = false;
  
    /**
     * True by default since most of the time we won't be trying to create a 
     * project with the same name more than once.
     */
    private boolean m_FailOnError = true;

    /**
     * Executes the task.
     * <p>
     * Builds a command line to execute ss and then calls Exec's run method
     * to execute the command line.
     * @throws BuildException if the task fails.
     */
    public void execute() throws BuildException {
        Commandline commandLine = new Commandline();
        int result = 0;

        // first off, make sure that we've got a command and a vssdir...
        if (getVsspath() == null) {
            String msg = "vsspath attribute must be set!";
            throw new BuildException(msg, location);
        }

        // now look for illegal combinations of things ...

        // build the command line from what we got the format is
        // ss Create VSS items [-C] [-H] [-I-] [-N] [-O] [-S] [-Y] [-?]
        // as specified in the SS.EXE help
        commandLine.setExecutable(getSSCommand());
        commandLine.createArgument().setValue(COMMAND_CREATE);

        // VSS items
        commandLine.createArgument().setValue(getVsspath());

        // -C
        commandLine.createArgument().setValue("-C" + getComment());

        // -I- or -I-Y or -I-N
        getAutoresponse(commandLine);

        // -O-
        getQuietCommand(commandLine);

        // -Y
        getLoginCommand(commandLine);

        result = run(commandLine);
        if (result != 0 && m_FailOnError) {
            String msg = "Failed executing: " + commandLine.toString();
            throw new BuildException(msg, location);
        }

    }

    /**
     * Sets the comment to apply in SourceSafe.
     * <p>
     * If this is null or empty, it will be replaced with "-" which
     * is what SourceSafe uses for an empty comment.
     * @param comment the comment to apply in SourceSafe
     */
    public void setComment(String comment) {
        if (comment.equals("") || comment.equals("null")) {
            m_Comment = "-";
        } else {
            m_Comment = comment;
        }
    }

    /**
     * Gets the comment to be applied.
     * @return the comment to be applied.
     */
    public String getComment() {
        return m_Comment;
    }

    /**
     * Sets/clears quiet mode; optional, default false.
     * @param quiet whether or not command should be run in "quiet mode".
     */
    public final void setQuiet (boolean quiet) {
        this.m_Quiet = quiet;
    }

    /** 
     * Modify the commandline to add the quiet argument.
     * @param cmd the commandline to modify.
     */
    public void getQuietCommand (Commandline cmd) {
        if (m_Quiet) {
            cmd.createArgument().setValue (FLAG_QUIET);
        }
    }

    /**
     * Sets whether task should fail if there is an error creating the project;
     * optional, default true.
     * @param failOnError true if task should fail if there is an error creating 
     * the project.
     */
    public final void setFailOnError (boolean failOnError) {
        this.m_FailOnError = failOnError;
    }

    /**
     * What to respond with (sets the -I option). By default, -I- is
     * used; values of Y or N will be appended to this.
     * @param response the response.
     */
    public void setAutoresponse(String response) {
        if (response.equals("") || response.equals("null")) {
            m_AutoResponse = null;
        } else {
            m_AutoResponse = response;
        }
    }

    /**
     * Checks the value set for the autoResponse.
     * if it equals "Y" then we return -I-Y
     * if it equals "N" then we return -I-N
     * otherwise we return -I
     * @param cmd the commandline to modify with the autoresponse.
     */
    public void getAutoresponse(Commandline cmd) {
        if (m_AutoResponse == null) {
            cmd.createArgument().setValue(FLAG_AUTORESPONSE_DEF);
        } else if (m_AutoResponse.equalsIgnoreCase("Y")) {
            cmd.createArgument().setValue(FLAG_AUTORESPONSE_YES);

        } else if (m_AutoResponse.equalsIgnoreCase("N")) {
            cmd.createArgument().setValue(FLAG_AUTORESPONSE_NO);
        } else {
            cmd.createArgument().setValue(FLAG_AUTORESPONSE_DEF);
        } // end of else
    }
}
