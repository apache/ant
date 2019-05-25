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
package org.apache.tools.ant.taskdefs.optional.net;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.Locale;
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.util.LoaderUtils;
import org.apache.tools.ant.util.Retryable;
import org.apache.tools.ant.util.SplitClassLoader;

/**
 * Basic FTP client. Performs the following actions:
 * <ul>
 *   <li> <strong>send</strong> - send files to a remote server. This is the
 *   default action.</li>
 *   <li> <strong>get</strong> - retrieve files from a remote server.</li>
 *   <li> <strong>del</strong> - delete files from a remote server.</li>
 *   <li> <strong>list</strong> - create a file listing.</li>
 *   <li> <strong>chmod</strong> - change unix file permissions.</li>
 *   <li> <strong>rmdir</strong> - remove directories, if empty, from a
 *   remote server.</li>
 * </ul>
 * <strong>Note:</strong> Some FTP servers - notably the Solaris server - seem
 * to hold data ports open after a "retr" operation, allowing them to timeout
 * instead of shutting them down cleanly. This happens in active or passive
 * mode, and the ports will remain open even after ending the FTP session. FTP
 * "send" operations seem to close ports immediately. This behavior may cause
 * problems on some systems when downloading large sets of files.
 *
 * @since Ant 1.3
 */
public class FTPTask extends Task implements FTPTaskConfig {
    public static final int SEND_FILES = 0;
    public static final int GET_FILES = 1;
    public static final int DEL_FILES = 2;
    public static final int LIST_FILES = 3;
    public static final int MK_DIR = 4;
    public static final int CHMOD = 5;
    public static final int RM_DIR = 6;
    public static final int SITE_CMD = 7;

    /** adjust uptodate calculations where server timestamps are HH:mm and client's
     * are HH:mm:ss */
    private static final long GRANULARITY_MINUTE = 60000L;

    /** Default port for FTP */
    public static final int DEFAULT_FTP_PORT = 21;

    private String remotedir;
    private String server;
    private String userid;
    private String password;
    private String account;
    private File listing;
    private boolean binary = true;
    private boolean passive = false;
    private boolean verbose = false;
    private boolean newerOnly = false;
    private long timeDiffMillis = 0;
    private long granularityMillis = 0L;
    private boolean timeDiffAuto = false;
    private int action = SEND_FILES;
    private Vector<FileSet> filesets = new Vector<>();
    private String remoteFileSep = "/";
    private int port = DEFAULT_FTP_PORT;
    private boolean skipFailedTransfers = false;
    private boolean ignoreNoncriticalErrors = false;
    private boolean preserveLastModified = false;
    private String chmod = null;
    private String umask = null;
    private FTPSystemType systemTypeKey = FTPSystemType.getDefault();
    private String defaultDateFormatConfig = null;
    private String recentDateFormatConfig = null;
    private String serverLanguageCodeConfig = null;
    private String serverTimeZoneConfig = null;
    private String shortMonthNamesConfig = null;
    private Granularity timestampGranularity = Granularity.getDefault();
    private boolean isConfigurationSet = false;
    private int retriesAllowed = 0;
    private String siteCommand = null;
    private String initialSiteCommand = null;
    private boolean enableRemoteVerification = true;

    private Path classpath;
    private ClassLoader mirrorLoader;
    private FTPTaskMirror delegate = null;

    public static final String[] ACTION_STRS = { //NOSONAR
        "sending",
        "getting",
        "deleting",
        "listing",
        "making directory",
        "chmod",
        "removing",
        "site"
    };

    public static final String[] COMPLETED_ACTION_STRS = { //NOSONAR
        "sent",
        "retrieved",
        "deleted",
        "listed",
        "created directory",
        "mode changed",
        "removed",
        "site command executed"
    };

    public static final String[] ACTION_TARGET_STRS = { //NOSONAR
        "files",
        "files",
        "files",
        "files",
        "directory",
        "files",
        "directories",
        "site command"
    };

    /**
     * Sets the remote directory where files will be placed. This may be a
     * relative or absolute path, and must be in the path syntax expected by
     * the remote server. No correction of path syntax will be performed.
     *
     * @param dir the remote directory name.
     */
    public void setRemotedir(String dir) {
        this.remotedir = dir;
    }

    public String getRemotedir() {
        return remotedir;
    }

    /**
     * Sets the FTP server to send files to.
     *
     * @param server the remote server name.
     */
    public void setServer(String server) {
        this.server = server;
    }

    public String getServer() {
        return server;
    }

    /**
     * Sets the FTP port used by the remote server.
     *
     * @param port the port on which the remote server is listening.
     */
    public void setPort(int port) {
        this.port = port;
    }

    public int getPort() {
        return port;
    }

    /**
     * Sets the login user id to use on the specified server.
     *
     * @param userid remote system userid.
     */
    public void setUserid(String userid) {
        this.userid = userid;
    }

    public String getUserid() {
        return userid;
    }

    /**
     * Sets the login password for the given user id.
     *
     * @param password the password on the remote system.
     */
    public void setPassword(String password) {
        this.password = password;
    }

    public String getPassword() {
        return password;
    }

    /**
     * Sets the login account to use on the specified server.
     *
     * @param pAccount the account name on remote system
     * @since Ant 1.7
     */
    public void setAccount(String pAccount) {
        this.account = pAccount;
    }

    public String getAccount() {
        return account;
    }

    /**
     * If true, uses binary mode, otherwise text mode (default is binary).
     *
     * @param binary if true use binary mode in transfers.
     */
    public void setBinary(boolean binary) {
        this.binary = binary;
    }

    public boolean isBinary() {
        return binary;
    }

    /**
     * Specifies whether to use passive mode. Set to true if you are behind a
     * firewall and cannot connect without it. Passive mode is disabled by
     * default.
     *
     * @param passive true is passive mode should be used.
     */
    public void setPassive(boolean passive) {
        this.passive = passive;
    }

    public boolean isPassive() {
        return passive;
    }

    /**
     * Set to true to receive notification about each file as it is
     * transferred.
     *
     * @param verbose true if verbose notifications are required.
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public boolean isVerbose() {
        return verbose;
    }

    /**
     * A synonym for <code>depends</code>. Set to true to transmit only new
     * or changed files.
     *
     * See the related attributes timediffmillis and timediffauto.
     *
     * @param newer if true only transfer newer files.
     */
    public void setNewer(boolean newer) {
        this.newerOnly = newer;
    }

    public boolean isNewer() {
        return newerOnly;
    }

    /**
     * number of milliseconds to add to the time on the remote machine
     * to get the time on the local machine.
     *
     * use in conjunction with <code>newer</code>
     *
     * @param timeDiffMillis number of milliseconds
     *
     * @since ant 1.6
     */
    public void setTimeDiffMillis(long timeDiffMillis) {
        this.timeDiffMillis = timeDiffMillis;
    }

    public long getTimeDiffMillis() {
        return timeDiffMillis;
    }

    /**
     * &quot;true&quot; to find out automatically the time difference
     * between local and remote machine.
     *
     * This requires right to create
     * and delete a temporary file in the remote directory.
     *
     * @param timeDiffAuto true = find automatically the time diff
     *
     * @since ant 1.6
     */
    public void setTimeDiffAuto(boolean timeDiffAuto) {
        this.timeDiffAuto = timeDiffAuto;
    }

    public boolean isTimeDiffAuto() {
        return timeDiffAuto;
    }

    /**
     * Set to true to preserve modification times for "gotten" files.
     *
     * @param preserveLastModified if true preserver modification times.
     */
    public void setPreserveLastModified(boolean preserveLastModified) {
        this.preserveLastModified = preserveLastModified;
    }

    public boolean isPreserveLastModified() {
        return preserveLastModified;
    }

    /**
     * Set to true to transmit only files that are new or changed from their
     * remote counterparts. The default is to transmit all files.
     *
     * @param depends if true only transfer newer files.
     */
    public void setDepends(boolean depends) {
        this.newerOnly = depends;
    }


    /**
     * Sets the remote file separator character. This normally defaults to the
     * Unix standard forward slash, but can be manually overridden using this
     * call if the remote server requires some other separator. Only the first
     * character of the string is used.
     *
     * @param separator the file separator on the remote system.
     */
    public void setSeparator(String separator) {
        remoteFileSep = separator;
    }


    public String getSeparator() {
        return remoteFileSep;
    }

    /**
     * Sets the file permission mode (Unix only) for files sent to the
     * server.
     *
     * @param theMode unix style file mode for the files sent to the remote
     *        system.
     */
    public void setChmod(String theMode) {
        this.chmod = theMode;
    }

    public String getChmod() {
        return chmod;
    }

    /**
     * Sets the default mask for file creation on a unix server.
     *
     * @param theUmask unix style umask for files created on the remote server.
     */
    public void setUmask(String theUmask) {
        this.umask = theUmask;
    }

    public String getUmask() {
        return umask;
    }

    /**
     *  A set of files to upload or download
     *
     * @param set the set of files to be added to the list of files to be
     *        transferred.
     */
    public void addFileset(FileSet set) {
        filesets.addElement(set);
    }

    public Vector<FileSet> getFilesets() {
        return filesets;
    }

    /**
     * Sets the FTP action to be taken. Currently accepts "put", "get", "del",
     * "mkdir", "chmod", "list", and "site".
     *
     * @deprecated since 1.5.x.
     *             setAction(String) is deprecated and is replaced with
     *      setAction(FTP.Action) to make Ant's Introspection mechanism do the
     *      work and also to encapsulate operations on the type in its own
     *      class.
     * @ant.attribute ignore="true"
     *
     * @param action the FTP action to be performed.
     *
     * @throws BuildException if the action is not a valid action.
     */
    @Deprecated
    public void setAction(String action) throws BuildException {
        log("DEPRECATED - The setAction(String) method has been deprecated."
            + " Use setAction(FTP.Action) instead.");

        Action a = new Action();

        a.setValue(action);
        this.action = a.getAction();
    }


    /**
     * Sets the FTP action to be taken. Currently accepts "put", "get", "del",
     * "mkdir", "chmod", "list", and "site".
     *
     * @param action the FTP action to be performed.
     *
     * @throws BuildException if the action is not a valid action.
     */
    public void setAction(Action action) throws BuildException {
        this.action = action.getAction();
    }

    public int getAction() {
        return this.action;
    }

    /**
     * The output file for the "list" action. This attribute is ignored for
     * any other actions.
     *
     * @param listing file in which to store the listing.
     */
    public void setListing(File listing) {
        this.listing = listing;
    }

    public File getListing() {
        return listing;
    }

    /**
     * If true, enables unsuccessful file put, delete and get
     * operations to be skipped with a warning and the remainder
     * of the files still transferred.
     *
     * @param skipFailedTransfers true if failures in transfers are ignored.
     */
    public void setSkipFailedTransfers(boolean skipFailedTransfers) {
        this.skipFailedTransfers = skipFailedTransfers;
    }

    public boolean isSkipFailedTransfers() {
        return skipFailedTransfers;
    }

    /**
     * set the flag to skip errors on directory creation.
     * (and maybe later other server specific errors)
     *
     * @param ignoreNoncriticalErrors true if non-critical errors should not
     *        cause a failure.
     */
    public void setIgnoreNoncriticalErrors(boolean ignoreNoncriticalErrors) {
        this.ignoreNoncriticalErrors = ignoreNoncriticalErrors;
    }

    public boolean isIgnoreNoncriticalErrors() {
        return ignoreNoncriticalErrors;
    }

    private void configurationHasBeenSet() {
        this.isConfigurationSet = true;
    }

    public boolean isConfigurationSet() {
        return this.isConfigurationSet;
    }

    /**
     * Sets the systemTypeKey attribute.
     * Method for setting <code>FTPClientConfig</code> remote system key.
     *
     * @param systemKey the key to be set - BUT if blank
     * the default value of null (which signifies "autodetect") will be kept.
     * @see org.apache.commons.net.ftp.FTPClientConfig
     */
    public void setSystemTypeKey(FTPSystemType systemKey) {
        if (systemKey != null && !systemKey.getValue().isEmpty()) {
            this.systemTypeKey = systemKey;
            configurationHasBeenSet();
        }
    }

    /**
     * Sets the defaultDateFormatConfig attribute.
     * @param defaultDateFormat configuration to be set, unless it is
     * null or empty string, in which case ignored.
     * @see org.apache.commons.net.ftp.FTPClientConfig
     */
    public void setDefaultDateFormatConfig(String defaultDateFormat) {
        if (defaultDateFormat != null && !defaultDateFormat.isEmpty()) {
            this.defaultDateFormatConfig = defaultDateFormat;
            configurationHasBeenSet();
        }
    }

    /**
     * Sets the recentDateFormatConfig attribute.
     * @param recentDateFormat configuration to be set, unless it is
     * null or empty string, in which case ignored.
     * @see org.apache.commons.net.ftp.FTPClientConfig
     */
    public void setRecentDateFormatConfig(String recentDateFormat) {
        if (recentDateFormat != null && !recentDateFormat.isEmpty()) {
            this.recentDateFormatConfig = recentDateFormat;
            configurationHasBeenSet();
        }
    }

    /**
     * Sets the serverLanguageCode attribute.
     * @param serverLanguageCode configuration to be set, unless it is
     * null or empty string, in which case ignored.
     * @see org.apache.commons.net.ftp.FTPClientConfig
     */
    public void setServerLanguageCodeConfig(String serverLanguageCode) {
        if (serverLanguageCode != null && !serverLanguageCode.isEmpty()) {
            this.serverLanguageCodeConfig = serverLanguageCode;
            configurationHasBeenSet();
        }
    }

    /**
     * Sets the serverTimeZoneConfig attribute.
     * @param serverTimeZoneId configuration to be set, unless it is
     * null or empty string, in which case ignored.
     * @see org.apache.commons.net.ftp.FTPClientConfig
     */
    public void setServerTimeZoneConfig(String serverTimeZoneId) {
        if (serverTimeZoneId != null && !serverTimeZoneId.isEmpty()) {
            this.serverTimeZoneConfig = serverTimeZoneId;
            configurationHasBeenSet();
        }
    }

    /**
     * Sets the shortMonthNamesConfig attribute
     *
     * @param shortMonthNames configuration to be set, unless it is
     * null or empty string, in which case ignored.
     * @see org.apache.commons.net.ftp.FTPClientConfig
     */
    public void setShortMonthNamesConfig(String shortMonthNames) {
        if (shortMonthNames != null && !shortMonthNames.isEmpty()) {
            this.shortMonthNamesConfig = shortMonthNames;
            configurationHasBeenSet();
        }
    }



    /**
     * Defines how many times to retry executing FTP command before giving up.
     * Default is 0 - try once and if failure then give up.
     *
     * @param retriesAllowed number of retries to allow.  -1 means
     * keep trying forever. "forever" may also be specified as a
     * synonym for -1.
     */
    public void setRetriesAllowed(String retriesAllowed) {
        if ("FOREVER".equalsIgnoreCase(retriesAllowed)) {
            this.retriesAllowed = Retryable.RETRY_FOREVER;
        } else {
            try {
                int retries = Integer.parseInt(retriesAllowed);
                if (retries < Retryable.RETRY_FOREVER) {
                    throw new BuildException(
                        "Invalid value for retriesAllowed attribute: %s",
                        retriesAllowed);
                }
                this.retriesAllowed = retries;
            } catch (NumberFormatException px) {
                throw new BuildException(
                    "Invalid value for retriesAllowed attribute: %s",
                    retriesAllowed);
            }

        }
    }

    public int getRetriesAllowed() {
        return retriesAllowed;
    }

    /**
     * @return Returns the systemTypeKey.
     */
    @Override
    public String getSystemTypeKey() {
        return systemTypeKey.getValue();
    }

    /**
     * @return Returns the defaultDateFormatConfig.
     */
    @Override
    public String getDefaultDateFormatConfig() {
        return defaultDateFormatConfig;
    }

    /**
     * @return Returns the recentDateFormatConfig.
     */
    @Override
    public String getRecentDateFormatConfig() {
        return recentDateFormatConfig;
    }

    /**
     * @return Returns the serverLanguageCodeConfig.
     */
    @Override
    public String getServerLanguageCodeConfig() {
        return serverLanguageCodeConfig;
    }

    /**
     * @return Returns the serverTimeZoneConfig.
     */
    @Override
    public String getServerTimeZoneConfig() {
        return serverTimeZoneConfig;
    }

    /**
     * @return Returns the shortMonthNamesConfig.
     */
    @Override
    public String getShortMonthNamesConfig() {
        return shortMonthNamesConfig;
    }

    /**
     * @return Returns the timestampGranularity.
     */
    public Granularity getTimestampGranularity() {
        return timestampGranularity;
    }

    /**
     * Sets the timestampGranularity attribute
     * @param timestampGranularity The timestampGranularity to set.
     */
    public void setTimestampGranularity(Granularity timestampGranularity) {
        if (null == timestampGranularity || timestampGranularity.getValue().isEmpty()) {
            return;
        }
        this.timestampGranularity = timestampGranularity;
    }

    /**
     * Sets the siteCommand attribute.  This attribute
     * names the command that will be executed if the action
     * is "site".
     * @param siteCommand The siteCommand to set.
     */
    public void setSiteCommand(String siteCommand) {
        this.siteCommand = siteCommand;
    }

    public String getSiteCommand() {
        return siteCommand;
    }

    /**
     * Sets the initialSiteCommand attribute.  This attribute
     * names a site command that will be executed immediately
     * after connection.
     * @param initialCommand The initialSiteCommand to set.
     */
    public void setInitialSiteCommand(String initialCommand) {
        this.initialSiteCommand = initialCommand;
    }

    public String getInitialSiteCommand() {
        return initialSiteCommand;
    }

    public long getGranularityMillis() {
        return this.granularityMillis;
    }

    public void setGranularityMillis(long granularity) {
        this.granularityMillis = granularity;
    }

    /**
     * Whether to verify that data and control connections are
     * connected to the same remote host.
     *
     * @param b boolean
     * @since Ant 1.8.0
     */
    public void setEnableRemoteVerification(boolean b) {
        enableRemoteVerification = b;
    }

    public boolean getEnableRemoteVerification() {
        return enableRemoteVerification;
    }

    /**
     * Checks to see that all required parameters are set.
     *
     * @throws BuildException if the configuration is not valid.
     */
    protected void checkAttributes() throws BuildException {
        if (server == null) {
            throw new BuildException("server attribute must be set!");
        }
        if (userid == null) {
            throw new BuildException("userid attribute must be set!");
        }
        if (password == null) {
            throw new BuildException("password attribute must be set!");
        }

        if (action == LIST_FILES && listing == null) {
            throw new BuildException(
                "listing attribute must be set for list action!");
        }

        if (action == MK_DIR && remotedir == null) {
            throw new BuildException(
                "remotedir attribute must be set for mkdir action!");
        }

        if (action == CHMOD && chmod == null) {
            throw new BuildException(
                "chmod attribute must be set for chmod action!");
        }
        if (action == SITE_CMD && siteCommand == null) {
            throw new BuildException(
                "sitecommand attribute must be set for site action!");
        }

        if (this.isConfigurationSet) {
            try {
                Class.forName("org.apache.commons.net.ftp.FTPClientConfig");
            } catch (ClassNotFoundException e) {
                throw new BuildException(
                    "commons-net.jar >= 1.4.0 is required for at least one of the attributes specified.");
            }
        }
    }

    /**
     * Runs the task.
     *
     * @throws BuildException if the task fails or is not configured
     *         correctly.
     */
    @Override
    public void execute() throws BuildException {
        checkAttributes();
        try {
            setupFTPDelegate();
            delegate.doFTP();
        } finally {
            if (mirrorLoader instanceof SplitClassLoader) {
                ((SplitClassLoader) mirrorLoader).cleanup();
            }
            mirrorLoader = null;
        }
    }

    public Path createClasspath() {
        if (classpath == null) {
            classpath = new Path(getProject());
        }
        return classpath;
    }

    protected void setupFTPDelegate() {
        ClassLoader myLoader = FTPTask.class.getClassLoader();
        if (mustSplit()) {
            mirrorLoader =
                new SplitClassLoader(myLoader, classpath, getProject(),
                                     new String[] {
                                         "FTPTaskMirrorImpl",
                                         "FTPConfigurator"
                                     });
        } else {
            mirrorLoader = myLoader;
        }
        delegate = createMirror(this, mirrorLoader);
    }

    private static boolean mustSplit() {
        return LoaderUtils.getResourceSource(FTPTask.class.getClassLoader(),
                                             "/org/apache/commons/net/"
                                             + "ftp/FTP.class")
            == null;
    }

    private static FTPTaskMirror createMirror(FTPTask task,
                                              ClassLoader loader) {
        try {
            loader.loadClass("org.apache.commons.net.ftp.FTP"); // sanity check
        } catch (ClassNotFoundException e) {
            throw new BuildException(
                "The <classpath> for <ftp> must include commons-net.jar if not in Ant's own classpath",
                e, task.getLocation());
        }
        try {
            Class<? extends FTPTaskMirror> c =
                loader.loadClass(FTPTaskMirror.class.getName() + "Impl")
                    .asSubclass(FTPTaskMirror.class);
            if (c.getClassLoader() != loader) {
                throw new BuildException("Overdelegating loader",
                    task.getLocation());
            }
            Constructor<? extends FTPTaskMirror> cons =
                c.getConstructor(FTPTask.class);
            return cons.newInstance(task);
        } catch (Exception e) {
            throw new BuildException(e, task.getLocation());
        }
    }

    /**
     * an action to perform, one of
     * "send", "put", "recv", "get", "del", "delete", "list", "mkdir", "chmod",
     * "rmdir"
     */
    public static class Action extends EnumeratedAttribute {

        private static final String[] VALID_ACTIONS = {
            "send", "put", "recv", "get", "del", "delete", "list", "mkdir",
            "chmod", "rmdir", "site"
        };

        /**
         * Get the valid values
         *
         * @return an array of the valid FTP actions.
         */
        @Override
        public String[] getValues() {
            return VALID_ACTIONS;
        }

        /**
         * Get the symbolic equivalent of the action value.
         *
         * @return the SYMBOL representing the given action.
         */
        public int getAction() {
            String actionL = getValue().toLowerCase(Locale.ENGLISH);
            switch (actionL) {
            case "send":
            case "put":
                return SEND_FILES;
            case "recv":
            case "get":
                return GET_FILES;
            case "del":
            case "delete":
                return DEL_FILES;
            case "list":
                return LIST_FILES;
            case "chmod":
                return CHMOD;
            case "mkdir":
                return MK_DIR;
            case "rmdir":
                return RM_DIR;
            case "site":
                return SITE_CMD;
            }
            return SEND_FILES;
        }
    }

    /**
     * represents one of the valid timestamp adjustment values
     * recognized by the <code>timestampGranularity</code> attribute.<p>

     * A timestamp adjustment may be used in file transfers for checking
     * uptodateness. MINUTE means to add one minute to the server
     * timestamp.  This is done because FTP servers typically list
     * timestamps HH:mm and client FileSystems typically use HH:mm:ss.
     *
     * The default is to use MINUTE for PUT actions and NONE for GET
     * actions, since GETs have the <code>preserveLastModified</code>
     * option, which takes care of the problem in most use cases where
     * this level of granularity is an issue.
     *
     */
    public static class Granularity extends EnumeratedAttribute {

        private static final String[] VALID_GRANULARITIES = {
            "", "MINUTE", "NONE"
        };

        /**
         * Get the valid values.
         * @return the list of valid Granularity values
         */
        @Override
        public String[] getValues() {
            return VALID_GRANULARITIES;
        }

        /**
         * returns the number of milliseconds associated with
         * the attribute, which can vary in some cases depending
         * on the value of the action parameter.
         * @param action SEND_FILES or GET_FILES
         * @return the number of milliseconds associated with
         * the attribute, in the context of the supplied action
         */
        public long getMilliseconds(int action) {
            String granularityU = getValue().toUpperCase(Locale.ENGLISH);
            if (granularityU.isEmpty()) {
                if (action == SEND_FILES) {
                    return GRANULARITY_MINUTE;
                }
            } else if ("MINUTE".equals(granularityU)) {
                return GRANULARITY_MINUTE;
            }
            return 0L;
        }

        static final Granularity getDefault() {
            Granularity g = new Granularity();
            g.setValue("");
            return g;
        }
    }

    /**
     * one of the valid system type keys recognized by the systemTypeKey
     * attribute.
     */
    public static class FTPSystemType extends EnumeratedAttribute {

        private static final String[] VALID_SYSTEM_TYPES = {
            "", "UNIX", "VMS", "WINDOWS", "OS/2", "OS/400",
            "MVS"
        };

        /**
         * Get the valid values.
         * @return the list of valid system types.
         */
        @Override
        public String[] getValues() {
            return VALID_SYSTEM_TYPES;
        }

        static final FTPSystemType getDefault() {
            FTPSystemType ftpst = new FTPSystemType();
            ftpst.setValue("");
            return ftpst;
        }
    }

}
