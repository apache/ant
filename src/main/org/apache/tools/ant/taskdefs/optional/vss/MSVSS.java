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
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.util.FileUtils;

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
 */
public abstract class MSVSS extends Task implements MSVSSConstants {

    private String ssDir = null;
    private String vssLogin = null;
    private String vssPath = null;
    private String serverPath = null;

    /**  Version */
    private String version = null;
    /**  Date */
    private String date = null;
    /**  Label */
    private String label = null;
    /**  Auto response */
    private String autoResponse = null;
    /**  Local path */
    private String localPath = null;
    /**  Comment */
    private String comment = null;
    /**  From label */
    private String fromLabel = null;
    /**  To label */
    private String toLabel = null;
    /**  Output file name */
    private String outputFileName = null;
    /**  User */
    private String user = null;
    /**  From date */
    private String fromDate = null;
    /**  To date */
    private String toDate = null;
    /**  History style */
    private String style = null;
    /**  Quiet defaults to false */
    private boolean quiet = false;
    /**  Recursive defaults to false */
    private boolean recursive = false;
    /**  Writable defaults to false */
    private boolean writable = false;
    /**  Fail on error defaults to true */
    private boolean failOnError = true;
    /**  Get local copy for checkout defaults to true */
    private boolean getLocalCopy = true;
    /**  Number of days offset for History */
    private int numDays = Integer.MIN_VALUE;
    /**  Date format for History */
    private DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.SHORT);
    /**  Timestamp for retrieved files */
    private CurrentModUpdated timestamp = null;
    /**  Behaviour for writable files */
    private WritableFiles writableFiles = null;

    /**
     * Each sub-class must implement this method and return the constructed
     * command line to be executed. It is up to the sub-task to determine the
     * required attributes and their order.
     * @return    The Constructed command line.
     */
    abstract Commandline buildCmdLine();

    /**
     * Directory where <code>ss.exe</code> resides.
     * By default the task expects it to be in the PATH.
     * @param  dir  The directory containing ss.exe.
     */
    public final void setSsdir(String dir) {
        this.ssDir = FileUtils.translatePath(dir);
    }

    /**
     * Login to use when accessing VSS, formatted as "username,password".
     * <p>
     * You can omit the password if your database is not password protected.
     * If you have a password and omit it, Ant will hang.
     * @param  vssLogin  The login string to use.
     */
    public final void setLogin(final String vssLogin) {
        this.vssLogin = vssLogin;
    }

    /**
     * SourceSafe path which specifies the project/file(s) you wish to perform
     * the action on.
     * <p>
     * A prefix of 'vss://' will be removed if specified.
     * @param  vssPath  The VSS project path.
     * @ant.attribute group="required"
     */
    public final void setVsspath(final String vssPath) {
        String projectPath;
        // CheckStyle:MagicNumber OFF
        if (vssPath.startsWith("vss://")) { //$NON-NLS-1$
            projectPath = vssPath.substring(5);
        } else {
            projectPath = vssPath;
        }
        // CheckStyle:MagicNumber ON

        if (projectPath.startsWith(PROJECT_PREFIX)) {
            this.vssPath = projectPath;
        } else {
            this.vssPath = PROJECT_PREFIX + projectPath;
        }
    }

    /**
     * Directory where <code>srssafe.ini</code> resides.
     * @param  serverPath  The path to the VSS server.
     */
    public final void setServerpath(final String serverPath) {
        this.serverPath = serverPath;
    }

    /**
     * Indicates if the build should fail if the Sourcesafe command does. Defaults to true.
     * @param failOnError True if task should fail on any error.
     */
    public final void setFailOnError(final boolean failOnError) {
        this.failOnError = failOnError;
    }

    /**
     * Executes the task.
     * <p>
     * Builds a command line to execute ss.exe and then calls Exec's run method
     * to execute the command line.
     * </p>
     *
     * @throws BuildException if the command cannot execute.
     */
    public void execute() throws BuildException {
        Commandline commandLine = buildCmdLine();
        int result = run(commandLine);
        if (Execute.isFailure(result) && getFailOnError()) {
            String msg = "Failed executing: " + formatCommandLine(commandLine)
                     + " With a return code of " + result;
            throw new BuildException(msg, getLocation());
        }
    }

    // Special setters for the sub-classes

    /**
     * Set the internal comment attribute.
     * @param comment the value to use.
     */
    protected void setInternalComment(final String comment) {
        this.comment = comment;
    }

    /**
     * Set the auto response attribute.
     * @param autoResponse the value to use.
     */
    protected void setInternalAutoResponse(final String autoResponse) {
        this.autoResponse = autoResponse;
    }

    /**
     * Set the date attribute.
     * @param date the value to use.
     */
    protected void setInternalDate(final String date) {
        this.date = date;
    }

    /**
     * Set the date format attribute.
     * @param dateFormat the value to use.
     */
    protected void setInternalDateFormat(final DateFormat dateFormat) {
        this.dateFormat = dateFormat;
    }

    /**
     * Set the failOnError attribute.
     * @param failOnError the value to use.
     */
    protected void setInternalFailOnError(final boolean failOnError) {
        this.failOnError = failOnError;
    }

    /**
     * Set the from date attribute.
     * @param fromDate the value to use.
     */
    protected void setInternalFromDate(final String fromDate) {
        this.fromDate = fromDate;
    }

    /**
     * Set the from label attribute.
     * @param fromLabel the value to use.
     */
    protected void setInternalFromLabel(final String fromLabel) {
        this.fromLabel = fromLabel;
    }

    /**
     * Set the label attribute.
     * @param label the value to use.
     */
    protected void setInternalLabel(final String label) {
        this.label = label;
    }

    /**
     * Set the local path comment attribute.
     * @param localPath the value to use.
     */
    protected void setInternalLocalPath(final String localPath) {
        this.localPath = localPath;
    }

    /**
     * Set the num days attribute.
     * @param numDays the value to use.
     */
    protected void setInternalNumDays(final int numDays) {
        this.numDays = numDays;
    }

    /**
     * Set the outputFileName comment attribute.
     * @param outputFileName the value to use.
     */
    protected void setInternalOutputFilename(final String outputFileName) {
        this.outputFileName = outputFileName;
    }

    /**
     * Set the quiet attribute.
     * @param quiet the value to use.
     */
    protected void setInternalQuiet(final boolean quiet) {
        this.quiet = quiet;
    }

    /**
     * Set the recursive attribute.
     * @param recursive the value to use.
     */
    protected void setInternalRecursive(final boolean recursive) {
        this.recursive = recursive;
    }

    /**
     * Set the style attribute.
     * @param style the value to use.
     */
    protected void setInternalStyle(final String style) {
        this.style = style;
    }

    /**
     * Set the to date attribute.
     * @param toDate the value to use.
     */
    protected void setInternalToDate(final String toDate) {
        this.toDate = toDate;
    }

    /**
     * Set the to label attribute.
     * @param toLabel the value to use.
     */
    protected void setInternalToLabel(final String toLabel) {
        this.toLabel = toLabel;
    }

    /**
     * Set the user attribute.
     * @param user the value to use.
     */
    protected void setInternalUser(final String user) {
        this.user = user;
    }

    /**
     * Set the version attribute.
     * @param version the value to use.
     */
    protected void setInternalVersion(final String version) {
        this.version = version;
    }

    /**
     * Set the writable attribute.
     * @param writable the value to use.
     */
    protected void setInternalWritable(final boolean writable) {
        this.writable = writable;
    }

    /**
     * Set the timestamp attribute.
     * @param timestamp the value to use.
     */
    protected void setInternalFileTimeStamp(final CurrentModUpdated timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Set the writableFiles attribute.
     * @param writableFiles the value to use.
     */
    protected void setInternalWritableFiles(final WritableFiles writableFiles) {
        this.writableFiles = writableFiles;
    }

    /**
     * Set the getLocalCopy attribute.
     * @param getLocalCopy the value to use.
     */
    protected void setInternalGetLocalCopy(final boolean getLocalCopy) {
        this.getLocalCopy = getLocalCopy;
    }

    /**
     * Gets the sscommand string. "ss" or "c:\path\to\ss"
     * @return    The path to ss.exe or just ss if sscommand is not set.
     */
    protected String getSSCommand() {
        if (ssDir == null) {
            return SS_EXE;
        }
        return ssDir.endsWith(File.separator) ? ssDir + SS_EXE : ssDir
                 + File.separator + SS_EXE;
    }

    /**
     * Gets the vssserverpath string.
     * @return    null if vssserverpath is not set.
     */
    protected String getVsspath() {
        return vssPath;
    }

    /**
     * Gets the quiet string. -O-
     * @return An empty string if quiet is not set or is false.
     */
    protected String getQuiet() {
        return quiet ? FLAG_QUIET : "";
    }

    /**
     * Gets the recursive string. "-R"
     * @return An empty string if recursive is not set or is false.
     */
    protected String getRecursive() {
        return recursive ? FLAG_RECURSION : "";
    }

    /**
     * Gets the writable string. "-W"
     * @return An empty string if writable is not set or is false.
     */
    protected String getWritable() {
        return writable ? FLAG_WRITABLE : "";
    }

    /**
     * Gets the label string. "-Lbuild1"
     * Max label length is 32 chars
     * @return An empty string if label is not set.
     */
    protected String getLabel() {
        String shortLabel = "";
        if (label != null && !label.isEmpty()) {
                shortLabel = FLAG_LABEL + getShortLabel();
        }
        return shortLabel;
    }
    /**
     * Return at most the 30 first chars of the label,
     * logging a warning message about the truncation
     * @return at most the 30 first chars of the label
     */
    private String getShortLabel() {
        String shortLabel;
        // CheckStyle:MagicNumber OFF
        if (label !=  null && label.length() > 31) {
            shortLabel = this.label.substring(0, 30);
            log("Label is longer than 31 characters, truncated to: " + shortLabel,
                Project.MSG_WARN);
        } else {
            shortLabel = label;
        }
        // CheckStyle:MagicNumber ON
        return shortLabel;
    }
    /**
     * Gets the style string. "-Lbuild1"
     * @return An empty string if label is not set.
     */
    protected String getStyle() {
        return style != null ? style : "";
    }

    /**
     * Gets the version string. Returns the first specified of version "-V1.0",
     * date "-Vd01.01.01", label "-Vlbuild1".
     * @return An empty string if a version, date and label are not set.
     */
    protected String getVersionDateLabel() {
        String versionDateLabel = "";
        if (version != null) {
            versionDateLabel = FLAG_VERSION + version;
        } else if (date != null) {
            versionDateLabel = FLAG_VERSION_DATE + date;
        } else {
            // Use getShortLabel() so labels longer then 30 char are truncated
            // and the user is warned
            String shortLabel = getShortLabel();
            if (shortLabel != null && !shortLabel.isEmpty()) {
                versionDateLabel = FLAG_VERSION_LABEL + shortLabel;
            }
        }
        return versionDateLabel;
    }

    /**
     * Gets the version string.
     * @return An empty string if a version is not set.
     */
    protected String getVersion() {
        return version != null ? FLAG_VERSION + version : "";
    }

    /**
     * Gets the localpath string. "-GLc:\source" <p>
     * The localpath is created if it didn't exist.
     * @return An empty string if localpath is not set.
     */
    protected String getLocalpath() {
        String lclPath = ""; //set to empty str if no local path return
        if (localPath != null) {
            //make sure m_LocalDir exists, create it if it doesn't
            File dir = getProject().resolveFile(localPath);
            if (!dir.exists()) {
                boolean done = dir.mkdirs() || dir.exists();
                if (!done) {
                    String msg = "Directory " + localPath + " creation was not "
                            + "successful for an unknown reason";
                    throw new BuildException(msg, getLocation());
                }
                getProject().log("Created dir: " + dir.getAbsolutePath());
            }
            lclPath = FLAG_OVERRIDE_WORKING_DIR + localPath;
        }
        return lclPath;
    }

    /**
     * Gets the comment string. "-Ccomment text"
     * @return A comment of "-" if comment is not set.
     */
    protected String getComment() {
        return comment != null ? FLAG_COMMENT + comment : FLAG_COMMENT + "-";
    }

    /**
     * Gets the auto response string. This can be Y "-I-Y" or N "-I-N".
     * @return The default value "-I-" if autoresponse is not set.
     */
    protected String getAutoresponse() {
        if (autoResponse == null) {
            return FLAG_AUTORESPONSE_DEF;
        }
        if (autoResponse.equalsIgnoreCase("Y")) {
            return FLAG_AUTORESPONSE_YES;
        } else if (autoResponse.equalsIgnoreCase("N")) {
            return FLAG_AUTORESPONSE_NO;
        } else {
            return FLAG_AUTORESPONSE_DEF;
        }
    }

    /**
     * Gets the login string. This can be user and password, "-Yuser,password"
     * or just user "-Yuser".
     * @return An empty string if login is not set.
     */
    protected String getLogin() {
        return vssLogin != null ? FLAG_LOGIN + vssLogin : "";
    }

    /**
     * Gets the output file string. "-Ooutput.file"
     * @return An empty string if user is not set.
     */
    protected String getOutput() {
        return outputFileName != null ? FLAG_OUTPUT + outputFileName : "";
    }

    /**
     * Gets the user string. "-Uusername"
     * @return An empty string if user is not set.
     */
    protected String getUser() {
        return user != null ? FLAG_USER + user : "";
    }

    /**
     * Gets the version string. This can be to-from "-VLbuild2~Lbuild1", from
     * "~Lbuild1" or to "-VLbuild2".
     * @return An empty string if neither tolabel or fromlabel are set.
     */
    protected String getVersionLabel() {
        if (fromLabel == null && toLabel == null) {
            return "";
        }
        // CheckStyle:MagicNumber OFF
        if (fromLabel != null && toLabel != null) {
            if (fromLabel.length() > 31) {
                fromLabel = fromLabel.substring(0, 30);
                log("FromLabel is longer than 31 characters, truncated to: "
                    + fromLabel, Project.MSG_WARN);
            }
            if (toLabel.length() > 31) {
                toLabel = toLabel.substring(0, 30);
                log("ToLabel is longer than 31 characters, truncated to: "
                    + toLabel, Project.MSG_WARN);
            }
            return FLAG_VERSION_LABEL + toLabel + VALUE_FROMLABEL + fromLabel;
        } else if (fromLabel != null) {
            if (fromLabel.length() > 31) {
                fromLabel = fromLabel.substring(0, 30);
                log("FromLabel is longer than 31 characters, truncated to: "
                    + fromLabel, Project.MSG_WARN);
            }
            return FLAG_VERSION + VALUE_FROMLABEL + fromLabel;
        } else {
            if (toLabel.length() > 31) {
                toLabel = toLabel.substring(0, 30);
                log("ToLabel is longer than 31 characters, truncated to: "
                    + toLabel, Project.MSG_WARN);
            }
            return FLAG_VERSION_LABEL + toLabel;
        }
        // CheckStyle:MagicNumber ON
    }

    /**
     * Gets the Version date string.
     * @return An empty string if neither Todate or from date are set.
     * @throws BuildException if there is an error.
     */
    protected String getVersionDate() throws BuildException {
        if (fromDate == null && toDate == null
            && numDays == Integer.MIN_VALUE) {
            return "";
        }
        if (fromDate != null && toDate != null) {
            return FLAG_VERSION_DATE + toDate + VALUE_FROMDATE + fromDate;
        } else if (toDate != null && numDays != Integer.MIN_VALUE) {
            try {
                return FLAG_VERSION_DATE + toDate + VALUE_FROMDATE
                        + calcDate(toDate, numDays);
            } catch (ParseException ex) {
                String msg = "Error parsing date: " + toDate;
                throw new BuildException(msg, getLocation());
            }
        } else if (fromDate != null && numDays != Integer.MIN_VALUE) {
            try {
                return FLAG_VERSION_DATE + calcDate(fromDate, numDays)
                        + VALUE_FROMDATE + fromDate;
            } catch (ParseException ex) {
                String msg = "Error parsing date: " + fromDate;
                throw new BuildException(msg, getLocation());
            }
        } else {
            return fromDate != null ? FLAG_VERSION + VALUE_FROMDATE
                    + fromDate : FLAG_VERSION_DATE + toDate;
        }
    }

    /**
     * Builds and returns the -G- flag if required.
     * @return An empty string if get local copy is true.
     */
    protected String getGetLocalCopy() {
        return (!getLocalCopy) ? FLAG_NO_GET : "";
    }

    /**
     * Gets the value of the fail on error flag.
     * @return    True if the FailOnError flag has been set or if 'writablefiles=skip'.
     */
    private boolean getFailOnError() {
        return !getWritableFiles().equals(WRITABLE_SKIP) && failOnError;
    }


    /**
     * Gets the value set for the FileTimeStamp.
     * if it equals "current" then we return -GTC
     * if it equals "modified" then we return -GTM
     * if it equals "updated" then we return -GTU
     * otherwise we return -GTC
     *
     * @return The default file time flag, if not set.
     */
    public String getFileTimeStamp() {
        if (timestamp == null) {
            return "";
        } else if (timestamp.getValue().equals(TIME_MODIFIED)) {
            return FLAG_FILETIME_MODIFIED;
        } else if (timestamp.getValue().equals(TIME_UPDATED)) {
            return FLAG_FILETIME_UPDATED;
        } else {
            return FLAG_FILETIME_DEF;
        }
    }


    /**
     * Gets the value to determine the behaviour when encountering writable files.
     * @return An empty String, if not set.
     */
    public String getWritableFiles() {
        if (writableFiles == null) {
            return "";
        } else if (writableFiles.getValue().equals(WRITABLE_REPLACE)) {
            return FLAG_REPLACE_WRITABLE;
        } else if (writableFiles.getValue().equals(WRITABLE_SKIP)) {
            // ss.exe exits with '100', when files have been skipped
            // so we have to ignore the failure
            failOnError = false;
            return FLAG_SKIP_WRITABLE;
        } else {
            return "";
        }
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
            if (serverPath != null) {
                String[] env = exe.getEnvironment();
                if (env == null) {
                    env = new String[0];
                }
                String[] newEnv = new String[env.length + 1];
                System.arraycopy(env, 0, newEnv, 0, env.length);
                newEnv[env.length] = "SSDIR=" + serverPath;

                exe.setEnvironment(newEnv);
            }

            exe.setAntRun(getProject());
            exe.setWorkingDirectory(getProject().getBaseDir());
            exe.setCommandline(cmd.getCommandline());
            // Use the OS launcher so we get environment variables
            exe.setVMLauncher(false);
            return exe.execute();
        } catch (IOException e) {
            throw new BuildException(e, getLocation());
        }
    }

     /**
     * Calculates the start date for version comparison.
     * <p>
     * Calculates the date numDay days earlier than startdate.
     * @param   startDate    The start date.
     * @param   daysToAdd     The number of days to add.
     * @return The calculated date.
     * @throws ParseException
     */
    private String calcDate(String startDate, int daysToAdd) throws ParseException {
        Calendar calendar = new GregorianCalendar();
        Date currentDate = dateFormat.parse(startDate);
        calendar.setTime(currentDate);
        calendar.add(Calendar.DATE, daysToAdd);
        return dateFormat.format(calendar.getTime());
    }

    /**
     * Changes the password to '***' so it isn't displayed on screen if the build fails
     *
     * @param cmd   The command line to clean
     * @return The command line as a string with out the password
     */
    private String formatCommandLine(Commandline cmd) {
        final StringBuilder sBuff = new StringBuilder(cmd.toString());
        int indexUser = sBuff.substring(0).indexOf(FLAG_LOGIN);
        if (indexUser > 0) {
            int indexPass = sBuff.substring(0).indexOf(",", indexUser);
            int indexAfterPass = sBuff.substring(0).indexOf(" ", indexPass);

            for (int i = indexPass + 1; i < indexAfterPass; i++) {
                sBuff.setCharAt(i, '*');
            }
        }
        return sBuff.toString();
    }

    /**
     * Extension of EnumeratedAttribute to hold the values for file time stamp.
     */
    public static class CurrentModUpdated extends EnumeratedAttribute {
        /**
         * Gets the list of allowable values.
         * @return The values.
         */
        public String[] getValues() {
            return new String[] {TIME_CURRENT, TIME_MODIFIED, TIME_UPDATED};
        }
    }

    /**
     * Extension of EnumeratedAttribute to hold the values for writable filess.
     */
    public static class WritableFiles extends EnumeratedAttribute {
        /**
         * Gets the list of allowable values.
         * @return The values.
         */
        public String[] getValues() {
            return new String[] {WRITABLE_REPLACE, WRITABLE_SKIP, WRITABLE_FAIL};
        }
    }
}
