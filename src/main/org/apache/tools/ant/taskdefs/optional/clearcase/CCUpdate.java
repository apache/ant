/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000,2002 The Apache Software Foundation.  All rights
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

package org.apache.tools.ant.taskdefs.optional.clearcase;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Commandline;





/**
 * Performs a ClearCase Update command.
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
 *      <td>viewpath</td>
 *      <td>Path to the ClearCase view file or directory that the command will operate on</td>
 *      <td>No</td>
 *   <tr>
 *   <tr>
 *      <td>graphical</td>
 *      <td>Displays a graphical dialog during the update</td>
 *      <td>No</td>
 *   <tr>
 *   <tr>
 *      <td>log</td>
 *      <td>Specifies a log file for ClearCase to write to</td>
 *      <td>No</td>
 *   <tr>
 *   <tr>
 *      <td>overwrite</td>
 *      <td>Specifies whether to overwrite hijacked files or not</td>
 *      <td>No</td>
 *   <tr>
 *   <tr>
 *      <td>rename</td>
 *      <td>Specifies that hijacked files should be renamed with a .keep extension</td>
 *      <td>No</td>
 *   <tr>
 *   <tr>
 *      <td>currenttime</td>
 *      <td>Specifies that modification time should be written as the current time. Either currenttime or preservetime can be specified.</td>
 *      <td>No</td>
 *   <tr>
 *   <tr>
 *      <td>preservetime</td>
 *      <td>Specifies that modification time should preserved from the VOB time. Either currenttime or preservetime can be specified.</td>
 *      <td>No</td>
 *   <tr>
 * </table>
 *
 * @author Curtis White
 */
public class CCUpdate extends ClearCase {
    private boolean m_Graphical = false;
    private boolean m_Overwrite = false;
    private boolean m_Rename = false;
    private boolean m_Ctime = false;
    private boolean m_Ptime = false;
    private String m_Log = null;

    /**
     * Executes the task.
     * <p>
     * Builds a command line to execute cleartool and then calls Exec's run method
     * to execute the command line.
     */
    public void execute() throws BuildException {
        Commandline commandLine = new Commandline();
        Project aProj = getProject();
        int result = 0;

        // Default the viewpath to basedir if it is not specified
        if (getViewPath() == null) {
            setViewPath(aProj.getBaseDir().getPath());
        }

        // build the command line from what we got the format is
        // cleartool update [options...] [viewpath ...]
        // as specified in the CLEARTOOL.EXE help
        commandLine.setExecutable(getClearToolCommand());
        commandLine.createArgument().setValue(COMMAND_UPDATE);

        // Check the command line options
        checkOptions(commandLine);

        // For debugging
        System.out.println(commandLine.toString());

        result = run(commandLine);
        if (result != 0) {
            String msg = "Failed executing: " + commandLine.toString();
            throw new BuildException(msg, location);
        }
    }

    /**
     * Check the command line options.
     */
    private void checkOptions(Commandline cmd) {
        // ClearCase items
        if (getGraphical()) {
            // -graphical
            cmd.createArgument().setValue(FLAG_GRAPHICAL);
        } else {
            if (getOverwrite()) {
                // -overwrite
                cmd.createArgument().setValue(FLAG_OVERWRITE);
            } else {
                if (getRename()) {
                    // -rename
                    cmd.createArgument().setValue(FLAG_RENAME);
                } else {
                    // -noverwrite
                    cmd.createArgument().setValue(FLAG_NOVERWRITE);
                }
            }

            if (getCurrentTime()) {
                // -ctime
                cmd.createArgument().setValue(FLAG_CURRENTTIME);
            } else {
                if (getPreserveTime()) {
                    // -ptime
                    cmd.createArgument().setValue(FLAG_PRESERVETIME);
                }
            }

            // -log logname
            getLogCommand(cmd);
        }

        // viewpath
        cmd.createArgument().setValue(getViewPath());
    }

    /**
     * If true, displays a graphical dialog during the update.
     *
     * @param graphical the status to set the flag to
     */
    public void setGraphical(boolean graphical) {
        m_Graphical = graphical;
    }

    /**
     * Get graphical flag status
     *
     * @return boolean containing status of graphical flag
     */
    public boolean getGraphical() {
        return m_Graphical;
    }

    /**
     * If true, overwrite hijacked files.
     *
     * @param ow the status to set the flag to
     */
    public void setOverwrite(boolean ow) {
        m_Overwrite = ow;
    }

    /**
     * Get overwrite hijacked files status
     *
     * @return boolean containing status of overwrite flag
     */
    public boolean getOverwrite() {
        return m_Overwrite;
    }

    /**
     * If true, hijacked files are renamed with a .keep extension.
     *
     * @param ren the status to set the flag to
     */
    public void setRename(boolean ren) {
        m_Rename = ren;
    }

    /**
     * Get rename hijacked files status
     *
     * @return boolean containing status of rename flag
     */
    public boolean getRename() {
        return m_Rename;
    }

    /**
     * If true, modification time should be written as the current time.
     * Either currenttime or preservetime can be specified.
     *
     * @param ct the status to set the flag to
     */
    public void setCurrentTime(boolean ct) {
        m_Ctime = ct;
    }

    /**
     * Get current time status
     *
     * @return boolean containing status of current time flag
     */
    public boolean getCurrentTime() {
        return m_Ctime;
    }

    /**
     * If true, modification time should be preserved from the VOB time.
     * Either currenttime or preservetime can be specified.
     *
     * @param pt the status to set the flag to
     */
    public void setPreserveTime(boolean pt) {
        m_Ptime = pt;
    }

    /**
     * Get preserve time status
     *
     * @return boolean containing status of preserve time flag
     */
    public boolean getPreserveTime() {
        return m_Ptime;
    }

    /**
     * Sets the log file where cleartool records
     * the status of the command.
     *
     * @param log the path to the log file
     */
    public void setLog(String log) {
        m_Log = log;
    }

    /**
     * Get log file
     *
     * @return String containing the path to the log file
     */
    public String getLog() {
        return m_Log;
    }

    /**
     * Get the 'log' command
     *
     * @param cmd containing the command line string with or without the log flag and path appended
     */
    private void getLogCommand(Commandline cmd) {
        if (getLog() == null) {
            return;
        } else {
            /* Had to make two separate commands here because if a space is
               inserted between the flag and the value, it is treated as a
               Windows filename with a space and it is enclosed in double
               quotes ("). This breaks clearcase.
            */
            cmd.createArgument().setValue(FLAG_LOG);
            cmd.createArgument().setValue(getLog());
        }
    }

    /**
     *  -graphical flag -- display graphical dialog during update operation
     */
    public static final String FLAG_GRAPHICAL = "-graphical";
    /**
     * -log flag -- file to log status to
     */
    public static final String FLAG_LOG = "-log";
    /**
     * -overwrite flag -- overwrite hijacked files
     */
    public static final String FLAG_OVERWRITE = "-overwrite";
    /**
     * -noverwrite flag -- do not overwrite hijacked files
     */
    public static final String FLAG_NOVERWRITE = "-noverwrite";
    /**
     * -rename flag -- rename hijacked files with .keep extension
     */
    public static final String FLAG_RENAME = "-rename";
    /**
     * -ctime flag -- modified time is written as the current time
     */
    public static final String FLAG_CURRENTTIME = "-ctime";
    /**
     * -ptime flag -- modified time is written as the VOB time
     */
    public static final String FLAG_PRESERVETIME = "-ptime";

}

