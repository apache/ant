/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2003 The Apache Software Foundation.  All rights
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
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
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

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.LogStreamHandler;
import org.apache.tools.ant.types.Commandline;

/**
 * A base class for creating tasks for executing commands on Visual SourceSafe.
 * <p>
 * The class extends the 'exec' task as it operates by executing the ss.exe program
 * supplied with SourceSafe. By default the task expects ss.exe to be in the path,
 * you can override this be specifying the ssdir attribute.
 * </p>
 * <p>
 * This class provides set and get methods for 'login' and 'vsspath' attributes. It
 * also contains constants for the flags that can be passed to SS.
 * </p>
 *
 * @author Craig Cottingham
 * @author Andrew Everitt
 * @author Jesse Stockall
 */
public abstract class MSVSS extends Task implements MSVSSConstants {

    private String m_SSDir = null;
    private String m_vssLogin = null;
    private String m_vssPath = null;
    private String m_serverPath = null;

    /**  Version */
    private String m_Version = null;
    /**  Date */
    private String m_Date = null;
    /**  Label */
    private String m_Label = null;
    /**  Auto response */
    private String m_AutoResponse = null;
    /**  Local path */
    private String m_LocalPath = null;
    /**  Comment */
    private String m_Comment = null;
    /**  From label */
    private String m_FromLabel = null;
    /**  To label */
    private String m_ToLabel = null;
    /**  Output file name */
    private String m_OutputFileName = null;
    /**  User */
    private String m_User = null;
    /**  From date */
    private String m_FromDate = null;
    /**  To date */
    private String m_ToDate = null;
    /**  History style */
    private String m_Style = null;
    /**  Quiet defaults to false */
    private boolean m_Quiet = false;
    /**  Recursive defaults to false */
    private boolean m_Recursive = false;
    /**  Writable defaults to false */
    private boolean m_Writable = false;
    /**  Fail on error defaults to true */
    private boolean m_FailOnError = true;
    /**  Number of days offset for History */
    private int m_NumDays = Integer.MIN_VALUE;
    /**  Date format for History */
    private DateFormat m_DateFormat = DateFormat.getDateInstance(DateFormat.SHORT);

    /**
     * Each sub-class must implemnt this method and return the constructed
     * command line to be executed. It is up to the sub-task to determine the
     * required attrubutes and their order.
     *
     * @return    The Constructed command line.
     */
    abstract Commandline buildCmdLine();

    /**
     * Directory where <code>ss.exe</code> resides; optional.
     * By default the task expects it to be in the PATH.
     *
     * @param  dir  The directory containing ss.exe.
     */
    public final void setSsdir(String dir) {
        m_SSDir = Project.translatePath(dir);
    }

    /**
     * The login to use when accessing VSS, formatted as "username,password";
     * optional.
     * <p>
     * You can omit the password if your database is not password protected.
     *  if you have a password and omit it, Ant/VSS will hang.
     *
     * @param  login  The login string to use.
     */
    public final void setLogin(String login) {
        m_vssLogin = login;
    }

    /**
     * SourceSafe path which specifies the project/file(s) you wish to perform
     * the action on; required.<p>
     *
     * Also we strip off any 'vss://' prefix which is an XMS special and should
     * probably be removed!
     *
     * @param  vssPath  The VSS project path.
     */
    public final void setVsspath(String vssPath) {
        String projectPath;
        if (vssPath.startsWith("vss://")) {
            projectPath = vssPath.substring(5);
        } else {
            projectPath = vssPath;
        }

        if (projectPath.startsWith(PROJECT_PREFIX)) {
            m_vssPath = projectPath;
        } else {
            m_vssPath = PROJECT_PREFIX + projectPath;
        }
    }

    /**
     * Set the directory where <code>srssafe.ini</code> resides; optional.
     *
     * @param  serverPath  The path to the VSS server.
     */
    public final void setServerpath(String serverPath) {
        m_serverPath = serverPath;
    }

    /**
     * Executes the task. <br>
     * Builds a command line to execute ss.exe and then calls Exec's run method
     * to execute the command line.
     * @throws BuildException
     */
    public void execute()
        throws BuildException {
        int result = 0;
        Commandline commandLine = buildCmdLine();
        result = run(commandLine);
        if (result != 0 && getFailOnError()) {
            String msg = "Failed executing: " + commandLine.toString()
                     + " With a return code of " + result;
            throw new BuildException(msg, getLocation());
        }
    }

    // Special setters for the sub-classes

    protected void setInternalComment(String text) {
        m_Comment = text;
    }

    protected void setInternalAutoResponse(String text) {
        m_AutoResponse = text;
    }

    protected void setInternalDate(String text) {
        m_Date = text;
    }

    protected void setInternalDateFormat(DateFormat date) {
        m_DateFormat = date;
    }

    protected void setInternalFailOnError(boolean fail) {
        m_FailOnError = fail;
    }

    protected void setInternalFromDate(String text) {
        m_FromDate = text;
    }

    protected void setInternalFromLabel(String text) {
        m_FromLabel = text;
    }

    protected void setInternalLabel(String text) {
        m_Label = text;
    }

    protected void setInternalLocalPath(String text) {
        m_LocalPath = text;
    }

    protected void setInternalNumDays(int days) {
        m_NumDays = days;
    }

    protected void setInternalOutputFilename(String text) {
        m_OutputFileName = text;
    }

    protected void setInternalQuiet(boolean quiet) {
        m_Quiet = quiet;
    }

    protected void setInternalRecursive(boolean recursive) {
        m_Recursive = recursive;
    }

    protected void setInternalStyle(String style) {
        m_Style = style;
    }

    protected void setInternalToDate(String text) {
        m_ToDate = text;
    }

    protected void setInternalToLabel(String text) {
        m_ToLabel = text;
    }

    protected void setInternalUser(String user) {
        m_User = user;
    }

    protected void setInternalVersion(String text) {
        m_Version = text;
    }

    protected void setInternalWritable(boolean writable) {
        m_Writable = writable;
    }

    /**
     * Gets the sscommand string. "ss" or "c:\path\to\ss"
     *
     * @return    The path to ss.exe or just ss if sscommand is not set.
     */
    protected String getSSCommand() {
        if (m_SSDir == null) {
            return SS_EXE;
        }
        return m_SSDir.endsWith(File.separator) ? m_SSDir + SS_EXE : m_SSDir
                 + File.separator + SS_EXE;
    }

    /**
     * Gets the vssserverpath string.
     *
     * @return    null if vssserverpath is not set.
     */
    protected String getVsspath() {
        return m_vssPath;
    }

    /**
     *  Gets the quiet string. -O-
     *
     * @return    An empty string if quiet is not set or is false.
     */
    protected String getQuiet() {
        return m_Quiet ? FLAG_QUIET : "";
    }

    /**
     *  Gets the recursive string. "-R"
     *
     * @return    An empty string if recursive is not set or is false.
     */
    protected String getRecursive() {
        return m_Recursive ? FLAG_RECURSION : "";
    }

    /**
     *  Gets the writable string. "-W"
     *
     * @return    An empty string if writable is not set or is false.
     */
    protected String getWritable() {
        return m_Writable ? FLAG_WRITABLE : "";
    }

    /**
     *  Gets the label string. "-Lbuild1"
     *
     * @return    An empty string if label is not set.
     */
    protected String getLabel() {
        return m_Label != null ? FLAG_LABEL + m_Label : "";
    }

    /**
     *  Gets the style string. "-Lbuild1"
     *
     * @return    An empty string if label is not set.
     */
    protected String getStyle() {
        return m_Style != null ? m_Style : "";
    }

    /**
     *  Gets the version string. Returns the first specified of version "-V1.0",
     *  date "-Vd01.01.01", label "-Vlbuild1".
     *
     * @return    An empty string if a version is not set.
     */
    protected String getVersion() {
        if (m_Version != null) {
            return FLAG_VERSION + m_Version;
        } else if (m_Date != null) {
            return FLAG_VERSION_DATE + m_Date;
        } else if (m_Label != null) {
            return FLAG_VERSION_LABEL + m_Label;
        } else {
            return "";
        }
    }

    /**
     *  Gets the localpath string. "-GLc:\source" <p>
     *
     *  The localpath is created if it didn't exist.
     *
     * @return    An empty string if localpath is not set.
     */
    protected String getLocalpath() {
        if (m_LocalPath == null) {
            return "";
        } else {
            // make sure m_LocalDir exists, create it if it doesn't
            File dir = getProject().resolveFile(m_LocalPath);
            if (! dir.exists()) {
                boolean done = dir.mkdirs();
                if (! done) {
                    String msg = "Directory " + m_LocalPath + " creation was not "
                            + "successful for an unknown reason";
                    throw new BuildException(msg, getLocation());
                }
                getProject().log("Created dir: " + dir.getAbsolutePath());
            }
            return FLAG_OVERRIDE_WORKING_DIR + m_LocalPath;
        }
    }

    /**
     *  Gets the comment string. "-Ccomment text"
     *
     * @return    A comment of "-" if comment is not set.
     */
    protected String getComment() {
        return m_Comment != null ? FLAG_COMMENT + m_Comment : FLAG_COMMENT + "-";
    }

    /**
     *  Gets the auto response string. This can be Y "-I-Y" or N "-I-N".
     *
     * @return    The default value "-I-" if autoresponse is not set.
     */
    protected String getAutoresponse() {
        if (m_AutoResponse == null) {
            return FLAG_AUTORESPONSE_DEF;
        } else if (m_AutoResponse.equalsIgnoreCase("Y")) {
            return FLAG_AUTORESPONSE_YES;
        } else if (m_AutoResponse.equalsIgnoreCase("N")) {
            return FLAG_AUTORESPONSE_NO;
        } else {
            return FLAG_AUTORESPONSE_DEF;
        }
    }

    /**
     *  Gets the login string. This can be user and password, "-Yuser,password"
     *  or just user "-Yuser".
     *
     * @return    An empty string if login is not set.
     */
    protected String getLogin() {
        return m_vssLogin != null ? FLAG_LOGIN + m_vssLogin : "";
    }

    /**
     *  Gets the output file string. "-Ooutput.file"
     *
     * @return    An empty string if user is not set.
     */
    protected String getOutput() {
        return m_OutputFileName != null ? FLAG_OUTPUT + m_OutputFileName : "";
    }

    /**
     *  Gets the user string. "-Uusername"
     *
     * @return    An empty string if user is not set.
     */
    protected String getUser() {
        return m_User != null ? FLAG_USER + m_User : "";
    }

    /**
     *  Gets the version string. This can be to-from "-VLbuild2~Lbuild1", from
     *  "~Lbuild1" or to "-VLbuild2".
     *
     * @return    An empty string if neither tolabel or fromlabel are set.
     */
    protected String getVersionLabel() {
        if (m_FromLabel == null && m_ToLabel == null) {
            return "";
        }
        if (m_FromLabel != null && m_ToLabel != null) {
            return FLAG_VERSION_LABEL + m_ToLabel + VALUE_FROMLABEL + m_FromLabel;
        } else if (m_FromLabel != null) {
            return FLAG_VERSION + VALUE_FROMLABEL + m_FromLabel;
        } else {
            return FLAG_VERSION_LABEL + m_ToLabel;
        }
    }

    /**
     * Gets the Version date string.
     * @return An empty string is neither Todate or from date are set.
     * @throws BuildException
     */
    protected String getVersionDate() throws BuildException {
        if (m_FromDate == null && m_ToDate == null
            && m_NumDays == Integer.MIN_VALUE) {
            return "";
        }
        if (m_FromDate != null && m_ToDate != null) {
            return FLAG_VERSION_DATE + m_ToDate + VALUE_FROMDATE + m_FromDate;
        } else if (m_ToDate != null && m_NumDays != Integer.MIN_VALUE) {
            try {
                return FLAG_VERSION_DATE + m_ToDate + VALUE_FROMDATE
                        + calcDate(m_ToDate, m_NumDays);
            } catch (ParseException ex) {
                String msg = "Error parsing date: " + m_ToDate;
                throw new BuildException(msg, getLocation());
            }
        } else if (m_FromDate != null && m_NumDays != Integer.MIN_VALUE) {
            try {
                return FLAG_VERSION_DATE + calcDate(m_FromDate, m_NumDays)
                        + VALUE_FROMDATE + m_FromDate;
            } catch (ParseException ex) {
                String msg = "Error parsing date: " + m_FromDate;
                throw new BuildException(msg, getLocation());
            }
        } else {
            return m_FromDate != null ? FLAG_VERSION + VALUE_FROMDATE
                    + m_FromDate : FLAG_VERSION_DATE + m_ToDate;
        }
    }

    /**
     *  Gets the value of the fail on error flag. This is only used by execute
     *  when checking the return code.
     *
     * @return    True if the FailOnError flag has been set.
     */
    private boolean getFailOnError() {
        return m_FailOnError;
    }

    /**
     *  Sets up the required environment and executes the command line.
     *
     * @param  cmd  The command line to execute.
     * @return      The return code from the exec'd process.
     */
    private int run(Commandline cmd) {
        try {
            Execute exe = new Execute(new LogStreamHandler(this,
                    Project.MSG_INFO,
                    Project.MSG_WARN));

            // If location of ss.ini is specified we need to set the
            // environment-variable SSDIR to this value
            if (m_serverPath != null) {
                String[] env = exe.getEnvironment();
                if (env == null) {
                    env = new String[0];
                }
                String[] newEnv = new String[env.length + 1];
                for (int i = 0; i < env.length; i++) {
                    newEnv[i] = env[i];
                }
                newEnv[env.length] = "SSDIR=" + m_serverPath;

                exe.setEnvironment(newEnv);
            }

            exe.setAntRun(getProject());
            exe.setWorkingDirectory(getProject().getBaseDir());
            exe.setCommandline(cmd.getCommandline());
            return exe.execute();
        } catch (IOException e) {
            throw new BuildException(e, getLocation());
        }
    }

     /**
     * Calculates the start date for version comparison.
     * <p>
     * Calculates the date numDay days earlier than startdate.
     * @param   fromDate    The start date.
     * @param   numDays     The number of days to add.
     * @return The calculated date.
     * @throws ParseException
     */
    private String calcDate(String fromDate, int numDays) throws ParseException {
        String toDate = null;
        Date currdate = new Date();
        Calendar calend = new GregorianCalendar();
        currdate = m_DateFormat.parse(fromDate);
        calend.setTime(currdate);
        calend.add(Calendar.DATE, numDays);
        toDate = m_DateFormat.format(calend.getTime());
        return toDate;
    }
}
