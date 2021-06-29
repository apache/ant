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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Delete;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.selectors.SelectorUtils;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.RetryHandler;
import org.apache.tools.ant.util.Retryable;
import org.apache.tools.ant.util.VectorSet;

/**
 * Basic FTP client. Performs the following actions:
 * <ul>
 *   <li><strong>send</strong> - send files to a remote server. This is the
 *   default action.</li>
 *   <li><strong>get</strong> - retrieve files from a remote server.</li>
 *   <li><strong>del</strong> - delete files from a remote server.</li>
 *   <li><strong>list</strong> - create a file listing.</li>
 *   <li><strong>chmod</strong> - change unix file permissions.</li>
 *   <li><strong>rmdir</strong> - remove directories, if empty, from a
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
public class FTP extends Task implements FTPTaskConfig {
    protected static final int SEND_FILES = 0;
    protected static final int GET_FILES = 1;
    protected static final int DEL_FILES = 2;
    protected static final int LIST_FILES = 3;
    protected static final int MK_DIR = 4;
    protected static final int CHMOD = 5;
    protected static final int RM_DIR = 6;
    protected static final int SITE_CMD = 7;
    /** return code of ftp */
    private static final int CODE_521 = 521;
    private static final int CODE_550 = 550;
    private static final int CODE_553 = 553;

    /** adjust uptodate calculations where server timestamps are HH:mm and client's
     * are HH:mm:ss */
    private static final long GRANULARITY_MINUTE = 60000L;

    /** Date formatter used in logging, note not thread safe! */
    private static final SimpleDateFormat TIMESTAMP_LOGGING_SDF =
        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /** Default port for FTP */
    public static final int DEFAULT_FTP_PORT = 21;

    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

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
    private Set<File> dirCache = new HashSet<>();
    private int transferred = 0;
    private String remoteFileSep = "/";
    private int port = DEFAULT_FTP_PORT;
    private boolean skipFailedTransfers = false;
    private int skipped = 0;
    private boolean ignoreNoncriticalErrors = false;
    private boolean preserveLastModified = false;
    private String chmod = null;
    private String umask = null;
    private FTPSystemType systemTypeKey = FTPSystemType.getDefault();
    private String defaultDateFormatConfig = null;
    private String recentDateFormatConfig = null;
    private LanguageCode serverLanguageCodeConfig = LanguageCode.getDefault();
    private String serverTimeZoneConfig = null;
    private String shortMonthNamesConfig = null;
    private Granularity timestampGranularity = Granularity.getDefault();
    private boolean isConfigurationSet = false;
    private int retriesAllowed = 0;
    private String siteCommand = null;
    private String initialSiteCommand = null;
    private boolean enableRemoteVerification = true;
    private int dataTimeout = -1;
    private int wakeUpTransferInterval = -1;
    private long lastWakeUpTime = 0;


    protected static final String[] ACTION_STRS = {//NOSONAR
        "sending",
        "getting",
        "deleting",
        "listing",
        "making directory",
        "chmod",
        "removing",
        "site"
    };

    protected static final String[] COMPLETED_ACTION_STRS = {//NOSONAR
        "sent",
        "retrieved",
        "deleted",
        "listed",
        "created directory",
        "mode changed",
        "removed",
        "site command executed"
    };

    protected static final String[] ACTION_TARGET_STRS = {//NOSONAR
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
     * internal class providing a File-like interface to some of the information
     * available from the FTP server
     *
     */
    protected static class FTPFileProxy extends File {
        private static final long serialVersionUID = 1L;

        private final FTPFile file;
        private final String[] parts;
        private final String name;

        /**
         * creates a proxy to a FTP file
         * @param file FTPFile
         */
        public FTPFileProxy(FTPFile file) {
            super(file.getName());
            name = file.getName();
            this.file = file;
            parts = FileUtils.getPathStack(name);
        }

        /**
         * creates a proxy to a FTP directory
         * @param completePath the remote directory.
         */
        public FTPFileProxy(String completePath) {
            super(completePath);
            file = null;
            name = completePath;
            parts = FileUtils.getPathStack(completePath);
        }


        /* (non-Javadoc)
         * @see java.io.File#exists()
         */
        @Override
        public boolean exists() {
            return true;
        }


        /* (non-Javadoc)
         * @see java.io.File#getAbsolutePath()
         */
        @Override
        public String getAbsolutePath() {
            return name;
        }


        /* (non-Javadoc)
         * @see java.io.File#getName()
         */
        @Override
        public String getName() {
            return parts.length > 0 ? parts[parts.length - 1] : name;
        }


        /* (non-Javadoc)
         * @see java.io.File#getParent()
         */
        @Override
        public String getParent() {
            return File.separator + String.join(File.separator, parts);
        }


        /* (non-Javadoc)
         * @see java.io.File#getPath()
         */
        @Override
        public String getPath() {
            return name;
        }


        /**
         * FTP files are stored as absolute paths
         * @return true
         */
        @Override
        public boolean isAbsolute() {
            return true;
        }


        /* (non-Javadoc)
         * @see java.io.File#isDirectory()
         */
        @Override
        public boolean isDirectory() {
            return file == null;
        }


        /* (non-Javadoc)
         * @see java.io.File#isFile()
         */
        @Override
        public boolean isFile() {
            return file != null;
        }


        /**
         * FTP files cannot be hidden
         *
         * @return  false
         */
        @Override
        public boolean isHidden() {
            return false;
        }


        /* (non-Javadoc)
         * @see java.io.File#lastModified()
         */
        @Override
        public long lastModified() {
            if (file != null) {
                return file.getTimestamp().getTimeInMillis();
            }
            return 0;
        }


        /* (non-Javadoc)
         * @see java.io.File#length()
         */
        @Override
        public long length() {
            if (file != null) {
                return file.getSize();
            }
            return 0;
        }
    }

    /**
     * internal class allowing to read the contents of a remote file system
     * using the FTP protocol
     * used in particular for ftp get operations
     * differences with DirectoryScanner
     * "" (the root of the fileset) is never included in the included directories
     * followSymlinks defaults to false
     */
    protected class FTPDirectoryScanner extends DirectoryScanner {
        // CheckStyle:VisibilityModifier OFF - bc
        protected FTPClient ftp = null;
        // CheckStyle:VisibilityModifier ON

        private String rootPath = null;

        /**
         * since ant 1.6
         * this flag should be set to true on UNIX and can save scanning time
         */
        private boolean remoteSystemCaseSensitive = false;
        private boolean remoteSensitivityChecked = false;

        /**
         * constructor
         * @param ftp  ftpclient object
         */
        public FTPDirectoryScanner(FTPClient ftp) {
            super();
            this.ftp = ftp;
            this.setFollowSymlinks(false);
        }


        /**
         * scans the remote directory,
         * storing internally the included files, directories, ...
         */
        @Override
        public void scan() {
            if (includes == null) {
                // No includes supplied, so set it to 'matches all'
                includes = new String[1];
                includes[0] = "**";
            }
            if (excludes == null) {
                excludes = new String[0];
            }

            filesIncluded = new VectorSet<>();
            filesNotIncluded = new Vector<>();
            filesExcluded = new VectorSet<>();
            dirsIncluded = new VectorSet<>();
            dirsNotIncluded = new Vector<>();
            dirsExcluded = new VectorSet<>();

            try {
                String cwd = ftp.printWorkingDirectory();
                // always start from the current ftp working dir
                forceRemoteSensitivityCheck();

                checkIncludePatterns();
                clearCaches();
                ftp.changeWorkingDirectory(cwd);
            } catch (IOException e) {
                throw new BuildException("Unable to scan FTP server: ", e);
            }
        }


        /**
         * this routine is actually checking all the include patterns in
         * order to avoid scanning everything under base dir
         * @since ant1.6
         */
        private void checkIncludePatterns() {

            Map<String, String> newroots = new HashMap<>();
            // put in the newroots vector the include patterns without
            // wildcard tokens
            for (String include : includes) {
                String newpattern
                        = SelectorUtils.rtrimWildcardTokens(include);
                newroots.put(newpattern, include);
            }
            if (remotedir == null) {
                try {
                    remotedir = ftp.printWorkingDirectory();
                } catch (IOException e) {
                    throw new BuildException("could not read current ftp directory",
                                             getLocation());
                }
            }
            AntFTPFile baseFTPFile = new AntFTPRootFile(ftp, remotedir);
            rootPath = baseFTPFile.getAbsolutePath();
            // construct it
            if (newroots.containsKey("")) {
                // we are going to scan everything anyway
                scandir(rootPath, "", true);
            } else {
                // only scan directories that can include matched files or
                // directories
                newroots.forEach((k, v) -> scanRoots(baseFTPFile, k, v));
            }
        }

        private void scanRoots(AntFTPFile baseFTPFile, String currentelement,
                               String originalpattern) {
            AntFTPFile myfile = new AntFTPFile(baseFTPFile, currentelement);
            boolean isOK = true;
            boolean traversesSymlinks = false;
            String path = null;

            if (myfile.exists()) {
                forceRemoteSensitivityCheck();
                if (remoteSensitivityChecked
                    && remoteSystemCaseSensitive && isFollowSymlinks()) {
                    // cool case,
                    //we do not need to scan all the subdirs in the relative path
                    path = myfile.getFastRelativePath();
                } else {
                    // may be on a case insensitive file system.  We want
                    // the results to show what's really on the disk, so
                    // we need to double check.
                    try {
                        path = myfile.getRelativePath();
                        traversesSymlinks = myfile.isTraverseSymlinks();
                    } catch (IOException be) {
                        throw new BuildException(be, getLocation());
                    } catch (BuildException be) {
                        isOK = false;
                    }
                }
            } else {
                isOK = false;
            }
            if (isOK) {
                currentelement = path.replace(remoteFileSep.charAt(0), File.separatorChar);
                if (!isFollowSymlinks() && traversesSymlinks) {
                    return;
                }

                if (myfile.isDirectory()) {
                    if (isIncluded(currentelement) && !currentelement.isEmpty()) {
                        accountForIncludedDir(currentelement, myfile, true);
                    } else {
                        if (!currentelement.isEmpty()
                                && currentelement.charAt(currentelement.length() - 1)
                                != File.separatorChar) {
                            currentelement += File.separatorChar;
                        }
                        scandir(myfile.getAbsolutePath(), currentelement, true);
                    }
                } else if (isCaseSensitive && originalpattern.equals(currentelement)) {
                    accountForIncludedFile(currentelement);
                } else if (!isCaseSensitive && originalpattern.equalsIgnoreCase(currentelement)) {
                    accountForIncludedFile(currentelement);
                }
            }
        }

        /**
         * scans a particular directory. populates the scannedDirs cache.
         *
         * @param dir directory to scan
         * @param vpath  relative path to the base directory of the remote fileset
         * always ended with a File.separator
         * @param fast seems to be always true in practice
         */
        protected void scandir(String dir, String vpath, boolean fast) {
            // avoid double scanning of directories, can only happen in fast mode
            if (fast && hasBeenScanned(vpath)) {
                return;
            }
            try {
                if (!ftp.changeWorkingDirectory(dir)) {
                    return;
                }
                String completePath = null;
                if (!vpath.isEmpty()) {
                    completePath = rootPath + remoteFileSep
                        + vpath.replace(File.separatorChar, remoteFileSep.charAt(0));
                } else {
                    completePath = rootPath;
                }
                FTPFile[] newfiles = listFiles(completePath, false);

                if (newfiles == null) {
                    ftp.changeToParentDirectory();
                    return;
                }
                for (FTPFile file : newfiles) {
                    if (file != null
                            && !".".equals(file.getName())
                            && !"..".equals(file.getName())) {
                        String name = vpath + file.getName();
                        scannedDirs.put(name, new FTPFileProxy(file));
                        if (isFunctioningAsDirectory(ftp, dir, file)) {
                            boolean slowScanAllowed = true;
                            if (!isFollowSymlinks() && file.isSymbolicLink()) {
                                dirsExcluded.addElement(name);
                                slowScanAllowed = false;
                            } else if (isIncluded(name)) {
                                accountForIncludedDir(name,
                                        new AntFTPFile(ftp, file, completePath), fast);
                            } else {
                                dirsNotIncluded.addElement(name);
                                if (fast && couldHoldIncluded(name)) {
                                    scandir(file.getName(),
                                            name + File.separator, fast);
                                }
                            }
                            if (!fast && slowScanAllowed) {
                                scandir(file.getName(),
                                        name + File.separator, fast);
                            }
                        } else {
                            if (!isFollowSymlinks() && file.isSymbolicLink()) {
                                filesExcluded.addElement(name);
                            } else {
                                // at this point, it's either a symbolic link or a file,
                                // but not a directory, so we include it
                                accountForIncludedFile(name);
                            }
                        }
                    }
                    if (wakeUpTransferInterval > 0) {
                        if (wakeUpTransferIntervalExpired()) {
                            getProject().log("wakeUpTransferInterval is reached,"
                                    + " trigger a data connection ", Project.MSG_DEBUG);
                            // send a minimalist command to trigger a data connection
                            ftp.listFiles(file.getName());
                        }
                    }

                }
                ftp.changeToParentDirectory();
            } catch (IOException e) {
                throw new BuildException("Error while communicating with FTP "
                                         + "server: ", e);
            }
        }
        /**
         * process included file
         * @param name  path of the file relative to the directory of the fileset
         */
        private void accountForIncludedFile(String name) {
            if (!filesIncluded.contains(name)
                && !filesExcluded.contains(name)) {

                if (isIncluded(name)) {
                    if (!isExcluded(name)
                        && isSelected(name, scannedDirs.get(name))) {
                        filesIncluded.addElement(name);
                    } else {
                        filesExcluded.addElement(name);
                    }
                } else {
                    filesNotIncluded.addElement(name);
                }
            }
        }

        /**
         *
         * @param name path of the directory relative to the directory of
         * the fileset
         * @param file directory as file
         * @param fast boolean
         */
        private void accountForIncludedDir(String name, AntFTPFile file, boolean fast) {
            if (!dirsIncluded.contains(name)
                && !dirsExcluded.contains(name)) {

                if (!isExcluded(name)) {
                    if (fast) {
                        if (file.isSymbolicLink()) {
                            try {
                                file.getClient().changeWorkingDirectory(file.curpwd);
                            } catch (IOException ioe) {
                                throw new BuildException("could not change directory to curpwd");
                            }
                            scandir(file.getLink(),
                                    name + File.separator, fast);
                        } else {
                            try {
                                file.getClient().changeWorkingDirectory(file.curpwd);
                            } catch (IOException ioe) {
                                throw new BuildException("could not change directory to curpwd");
                            }
                            scandir(file.getName(),
                                    name + File.separator, fast);
                        }
                    }
                    dirsIncluded.addElement(name);
                } else {
                    dirsExcluded.addElement(name);
                    if (fast && couldHoldIncluded(name)) {
                        try {
                            file.getClient().changeWorkingDirectory(file.curpwd);
                        } catch (IOException ioe) {
                            throw new BuildException("could not change directory to curpwd");
                        }
                        scandir(file.getName(),
                                name + File.separator, fast);
                    }
                }
            }
        }
        /**
         * temporary table to speed up the various scanning methods below
         *
         * @since Ant 1.6
         */
        private Map<String, FTPFile[]> fileListMap = new HashMap<>();
        /**
         * List of all scanned directories.
         *
         * @since Ant 1.6
         */

        private Map<String, FTPFileProxy> scannedDirs = new HashMap<>();

        /**
         * Has the directory with the given path relative to the base
         * directory already been scanned?
         *
         * @since Ant 1.6
         */
        private boolean hasBeenScanned(String vpath) {
            return scannedDirs.containsKey(vpath);
        }

        /**
         * Clear internal caches.
         *
         * @since Ant 1.6
         */
        private void clearCaches() {
            fileListMap.clear();
            scannedDirs.clear();
        }
        /**
         * list the files present in one directory.
         * @param directory full path on the remote side
         * @param changedir if true change to directory directory before listing
         * @return array of FTPFile
         */
        public FTPFile[] listFiles(String directory, boolean changedir) {
            String currentPath = directory;
            if (changedir) {
                try {
                    if (!ftp.changeWorkingDirectory(directory)) {
                        return null;
                    }
                    currentPath = ftp.printWorkingDirectory();
                } catch (IOException ioe) {
                    throw new BuildException(ioe, getLocation());
                }
            }
            if (fileListMap.containsKey(currentPath)) {
                getProject().log("filelist map used in listing files", Project.MSG_DEBUG);
                return fileListMap.get(currentPath);
            }
            FTPFile[] result;
            try {
                result = ftp.listFiles();
            } catch (IOException ioe) {
                throw new BuildException(ioe, getLocation());
            }
            fileListMap.put(currentPath, result);
            if (!remoteSensitivityChecked) {
                checkRemoteSensitivity(result, directory);
            }
            return result;
        }

        private void forceRemoteSensitivityCheck() {
            if (!remoteSensitivityChecked) {
                try {
                    checkRemoteSensitivity(ftp.listFiles(), ftp.printWorkingDirectory());
                } catch (IOException ioe) {
                    throw new BuildException(ioe, getLocation());
                }
            }
        }
        /**
         * cd into one directory and
         * list the files present in one directory.
         * @param directory full path on the remote side
         * @return array of FTPFile
         */
        public FTPFile[] listFiles(String directory) {
            return listFiles(directory, true);
        }

        private void checkRemoteSensitivity(FTPFile[] array, String directory) {
            if (array == null) {
                return;
            }
            boolean candidateFound = false;
            String target = null;
            for (int icounter = 0; icounter < array.length; icounter++) {
                if (array[icounter] != null && array[icounter].isDirectory()) {
                    if (!".".equals(array[icounter].getName())
                        && !"..".equals(array[icounter].getName())) {
                        candidateFound = true;
                        target = fiddleName(array[icounter].getName());
                        getProject().log("will try to cd to "
                                         + target + " where a directory called "
                                         + array[icounter].getName()
                                         + " exists", Project.MSG_DEBUG);
                        for (int pcounter = 0; pcounter < array.length; pcounter++) {
                            if (array[pcounter] != null
                                && pcounter != icounter
                                && target.equals(array[pcounter].getName())) {
                                candidateFound = false;
                                break;
                            }
                        }
                        if (candidateFound) {
                            break;
                        }
                    }
                }
            }
            if (candidateFound) {
                try {
                    getProject().log("testing case sensitivity, attempting to cd to "
                                     + target, Project.MSG_DEBUG);
                    remoteSystemCaseSensitive  = !ftp.changeWorkingDirectory(target);
                } catch (IOException ioe) {
                    remoteSystemCaseSensitive = true;
                } finally {
                    try {
                        ftp.changeWorkingDirectory(directory);
                    } catch (IOException ioe) {
                        throw new BuildException(ioe, getLocation()); //NOSONAR
                    }
                }
                getProject().log("remote system is case sensitive : " + remoteSystemCaseSensitive,
                                 Project.MSG_VERBOSE);
                remoteSensitivityChecked = true;
            }
        }

        private String fiddleName(String origin) {
            StringBuilder result = new StringBuilder();
            for (char ch : origin.toCharArray()) {
                if (Character.isLowerCase(ch)) {
                    result.append(Character.toUpperCase(ch));
                } else if (Character.isUpperCase(ch)) {
                    result.append(Character.toLowerCase(ch));
                } else {
                    result.append(ch);
                }
            }
            return result.toString();
        }

        /**
         * an AntFTPFile is a representation of a remote file
         * @since Ant 1.6
         */
        protected class AntFTPFile {
            /**
             * ftp client
             */
            private FTPClient client;
            /**
             * parent directory of the file
             */
            private String curpwd;
            /**
             * the file itself
             */
            private FTPFile ftpFile;
            /**
             *
             */
            private AntFTPFile parent = null;
            private boolean relativePathCalculated = false;
            private boolean traversesSymlinks = false;
            private String relativePath = "";
            /**
             * constructor
             * @param client ftp client variable
             * @param ftpFile the file
             * @param curpwd absolute remote path where the file is found
             */
            public AntFTPFile(FTPClient client, FTPFile ftpFile, String curpwd) {
                this.client = client;
                this.ftpFile = ftpFile;
                this.curpwd = curpwd;
            }
            /**
             * other constructor
             * @param parent the parent file
             * @param path  a relative path to the parent file
             */
            public AntFTPFile(AntFTPFile parent, String path) {
                this.parent = parent;
                this.client = parent.client;
                List<String> pathElements = SelectorUtils.tokenizePath(path);
                try {
                    //this should not happen, except if parent has been deleted by another process
                    if (!this.client.changeWorkingDirectory(parent.getAbsolutePath())) {
                        return;
                    }
                    this.curpwd = parent.getAbsolutePath();
                } catch (IOException ioe) {
                    throw new BuildException(
                        "could not change working dir to %s", parent.curpwd);
                }
                for (String currentPathElement : pathElements) {
                    try {
                        if (!this.client
                            .changeWorkingDirectory(currentPathElement)) {
                            if (!isCaseSensitive() && (remoteSystemCaseSensitive
                                || !remoteSensitivityChecked)) {
                                currentPathElement =
                                    findPathElementCaseUnsensitive(this.curpwd,
                                        currentPathElement);
                                if (currentPathElement == null) {
                                    return;
                                }
                            }
                            return;
                        }
                        this.curpwd =
                            getCurpwdPlusFileSep() + currentPathElement;
                    } catch (IOException ioe) {
                        throw new BuildException(
                            "could not change working dir to %s from %s",
                            currentPathElement, curpwd);
                    }
                }
                String lastpathelement = pathElements.get(pathElements.size() - 1);
                FTPFile[] theFiles = listFiles(this.curpwd);
                this.ftpFile = getFile(theFiles, lastpathelement);
            }

            /**
             * find a file in a directory in case insensitive way
             * @param parentPath        where we are
             * @param soughtPathElement what is being sought
             * @return                  the first file found or null if not found
             */
            private String findPathElementCaseUnsensitive(String parentPath,
                                                          String soughtPathElement) {
                // we are already in the right path, so the second parameter
                // is false
                FTPFile[] files = listFiles(parentPath, false);
                if (files == null) {
                    return null;
                }
                for (FTPFile file : files) {
                    if (file != null
                        && file.getName().equalsIgnoreCase(soughtPathElement)) {
                        return file.getName();
                    }
                }
                return null;
            }

            /**
             * find out if the file exists
             * @return  true if the file exists
             */
            public boolean exists() {
                return (ftpFile != null);
            }

            /**
             * if the file is a symbolic link, find out to what it is pointing
             * @return the target of the symbolic link
             */
            public String getLink() {
                return ftpFile.getLink();
            }

            /**
             * get the name of the file
             * @return the name of the file
             */
            public String getName() {
                return ftpFile.getName();
            }

            /**
             * find out the absolute path of the file
             * @return absolute path as string
             */
            public String getAbsolutePath() {
                return getCurpwdPlusFileSep() + ftpFile.getName();
            }

            /**
             * find out the relative path assuming that the path used to construct
             * this AntFTPFile was spelled properly with regards to case.
             * This is OK on a case sensitive system such as UNIX
             * @return relative path
             */
            public String getFastRelativePath() {
                String absPath = getAbsolutePath();
                if (absPath.startsWith(rootPath + remoteFileSep)) {
                    return absPath.substring(rootPath.length() + remoteFileSep.length());
                }
                return null;
            }

            /**
             * find out the relative path to the rootPath of the enclosing scanner.
             * this relative path is spelled exactly like on disk,
             * for instance if the AntFTPFile has been instantiated as ALPHA,
             * but the file is really called alpha, this method will return alpha.
             * If a symbolic link is encountered, it is followed, but the name of the link
             * rather than the name of the target is returned.
             * (ie does not behave like File.getCanonicalPath())
             * @return                relative path, separated by remoteFileSep
             * @throws IOException    if a change directory fails, ...
             * @throws BuildException if one of the components of the relative path cannot
             * be found.
             */
            public String getRelativePath() throws IOException, BuildException {
                if (!relativePathCalculated) {
                    if (parent != null) {
                        traversesSymlinks = parent.isTraverseSymlinks();
                        relativePath = getRelativePath(parent.getAbsolutePath(),
                                                       parent.getRelativePath());
                    } else {
                        relativePath = getRelativePath(rootPath, "");
                        relativePathCalculated = true;
                    }
                }
                return relativePath;
            }

            /**
             * get the relative path of this file
             * @param currentPath          base path
             * @param currentRelativePath  relative path of the base path with regards to remote dir
             * @return relative path
             */
            private String getRelativePath(String currentPath, String currentRelativePath) {
                List<String> pathElements = SelectorUtils.tokenizePath(getAbsolutePath(),
                        remoteFileSep);
                StringBuilder relPath = new StringBuilder(currentRelativePath == null
                        ? "" : currentRelativePath);
                for (String currentElement : pathElements.subList(
                                SelectorUtils.tokenizePath(currentPath, remoteFileSep).size(),
                                pathElements.size())) {
                    FTPFile[] theFiles = listFiles(currentPath);
                    FTPFile theFile = null;
                    if (theFiles != null) {
                        theFile = getFile(theFiles, currentElement);
                    }
                    if (relPath.length() > 0) {
                        relPath.append(remoteFileSep);
                    }
                    if (theFile == null) {
                        // hit a hidden file assume not a symlink
                        relPath.append(currentElement);
                        currentPath += remoteFileSep + currentElement;
                        log("Hidden file " + relPath + " assumed to not be a symlink.",
                            Project.MSG_VERBOSE);
                    } else {
                        traversesSymlinks = traversesSymlinks || theFile.isSymbolicLink();
                        relPath.append(theFile.getName());
                        currentPath += remoteFileSep + theFile.getName();
                    }
                }
                return relPath.toString();
            }

            /**
             * find a file matching a string in an array of FTPFile.
             * This method will find "alpha" when requested for "ALPHA"
             * if and only if the caseSensitive attribute is set to false.
             * When caseSensitive is set to true, only the exact match is returned.
             * @param theFiles  array of files
             * @param lastpathelement  the file name being sought
             * @return null if the file cannot be found, otherwise return the matching file.
             */
            public FTPFile getFile(FTPFile[] theFiles, String lastpathelement) {
                if (theFiles == null) {
                    return null;
                }
                Predicate<String> test =
                    isCaseSensitive() ? lastpathelement::equals
                        : lastpathelement::equalsIgnoreCase;
                return Stream.of(theFiles)
                    .filter(Objects::nonNull)
                    .filter(f -> test.test(f.getName()))
                    .findFirst().orElse(null);
            }

            /**
             * tell if a file is a directory.
             * note that it will return false for symbolic links pointing to directories.
             * @return <code>true</code> for directories
             */
            public boolean isDirectory() {
                return ftpFile.isDirectory();
            }

            /**
             * tell if a file is a symbolic link
             * @return <code>true</code> for symbolic links
             */
            public boolean isSymbolicLink() {
                return ftpFile.isSymbolicLink();
            }

            /**
             * return the attached FTP client object.
             * Warning : this instance is really shared with the enclosing class.
             * @return  FTP client
             */
            protected FTPClient getClient() {
                return client;
            }

            /**
             * sets the current path of an AntFTPFile
             * @param curpwd the current path one wants to set
             */
            protected void setCurpwd(String curpwd) {
                this.curpwd = curpwd;
            }

            /**
             * returns the path of the directory containing the AntFTPFile.
             * of the full path of the file itself in case of AntFTPRootFile
             * @return parent directory of the AntFTPFile
             */
            public String getCurpwd() {
                return curpwd;
            }

            /**
             * returns the path of the directory containing the AntFTPFile.
             * of the full path of the file itself in case of AntFTPRootFile
             * and appends the remote file separator if necessary.
             * @return parent directory of the AntFTPFile
             * @since Ant 1.8.2
             */
            public String getCurpwdPlusFileSep() {
                return curpwd.endsWith(remoteFileSep) ? curpwd
                    : curpwd + remoteFileSep;
            }

            /**
             * find out if a symbolic link is encountered in the relative path of this file
             * from rootPath.
             * @return <code>true</code> if a symbolic link is encountered in the relative path.
             * @throws IOException if one of the change directory or directory listing operations
             * fails
             * @throws BuildException if a path component in the relative path cannot be found.
             */
            public boolean isTraverseSymlinks() throws IOException, BuildException {
                if (!relativePathCalculated) {
                    // getRelativePath also finds about symlinks
                    getRelativePath();
                }
                return traversesSymlinks;
            }

            /**
             * Get a string rep of this object.
             * @return a string containing the pwd and the file.
             */
            @Override
            public String toString() {
                return "AntFtpFile: " + curpwd + "%" + ftpFile;
            }
        }

        /**
         * special class to represent the remote directory itself
         * @since Ant 1.6
         */
        protected class AntFTPRootFile extends AntFTPFile {
            private String remotedir;

            /**
             * constructor
             * @param aclient FTP client
             * @param remotedir remote directory
             */
            public AntFTPRootFile(FTPClient aclient, String remotedir) {
                super(aclient, null, remotedir);
                this.remotedir = remotedir;
                try {
                    this.getClient().changeWorkingDirectory(this.remotedir);
                    this.setCurpwd(this.getClient().printWorkingDirectory());
                } catch (IOException ioe) {
                    throw new BuildException(ioe, getLocation());
                }
            }

            /**
             * find the absolute path
             * @return absolute path
             */
            @Override
            public String getAbsolutePath() {
                return this.getCurpwd();
            }

            /**
             * find out the relative path to root
             * @return empty string
             * @throws BuildException actually never
             * @throws IOException  actually never
             */
            @Override
            public String getRelativePath() throws BuildException, IOException {
                return "";
            }
        }
    }

    /**
     * check FTPFiles to check whether they function as directories too
     * the FTPFile API seem to make directory and symbolic links incompatible
     * we want to find out if we can cd to a symbolic link
     * @param dir  the parent directory of the file to test
     * @param file the file to test
     * @return true if it is possible to cd to this directory
     * @since ant 1.6
     */
    private boolean isFunctioningAsDirectory(FTPClient ftp, String dir, FTPFile file)
            throws FTPConnectionClosedException {
        if (file.isDirectory()) {
            return true;
        }
        if (file.isFile()) {
            return false;
        }
        String currentWorkingDir = null;
        try {
            currentWorkingDir = ftp.printWorkingDirectory();
        } catch (FTPConnectionClosedException ftpcce) {
            getProject().log("could not find current working directory " + dir
                             + " while checking a symlink because connection was closed",
                             Project.MSG_DEBUG);
            throw(ftpcce);
        } catch (IOException ioe) {
            getProject().log("could not find current working directory " + dir
                             + " while checking a symlink",
                             Project.MSG_DEBUG);
        }
        boolean result = false;
        if (currentWorkingDir != null) {
            try {
                result = ftp.changeWorkingDirectory(file.getLink());
            } catch (FTPConnectionClosedException ftpcce) {
                getProject().log("could not find current working directory " + dir
                                + " while checking a symlink because connection was closed",
                                Project.MSG_DEBUG);
                throw(ftpcce);
            } catch (IOException ioe) {
                getProject().log("could not cd to " + file.getLink() + " while checking a symlink",
                                 Project.MSG_DEBUG);
            }
            if (result) {
                boolean comeback = false;
                try {
                    comeback = ftp.changeWorkingDirectory(currentWorkingDir);
                } catch (IOException ioe) {
                    getProject().log("could not cd back to " + dir + " while checking a symlink",
                                     Project.MSG_ERR);
                } finally {
                    if (!comeback) {
                        throw new BuildException(
                            "could not cd back to %s while checking a symlink",
                            dir);
                    }
                }
            }
        }
        return result;
    }

    /**
     * check FTPFiles to check whether they function as directories too
     * the FTPFile API seem to make directory and symbolic links incompatible
     * we want to find out if we can cd to a symbolic link
     * @param dir  the parent directory of the file to test
     * @param file the file to test
     * @return true if it is possible to cd to this directory
     * @since ant 1.6
     */
    private boolean isFunctioningAsFile(FTPClient ftp, String dir, FTPFile file)
            throws FTPConnectionClosedException {
        return !file.isDirectory() && (file.isFile() || !isFunctioningAsDirectory(ftp, dir, file));
    }

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

    /**
     * Sets the FTP server to send files to.
     *
     * @param server the remote server name.
     */
    public void setServer(String server) {
        this.server = server;
    }


    /**
     * Sets the FTP port used by the remote server.
     *
     * @param port the port on which the remote server is listening.
     */
    public void setPort(int port) {
        this.port = port;
    }


    /**
     * Sets the login user id to use on the specified server.
     *
     * @param userid remote system userid.
     */
    public void setUserid(String userid) {
        this.userid = userid;
    }


    /**
     * Sets the login password for the given user id.
     *
     * @param password the password on the remote system.
     */
    public void setPassword(String password) {
        this.password = password;
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


    /**
     * If true, uses binary mode, otherwise text mode (default is binary).
     *
     * @param binary if true use binary mode in transfers.
     */
    public void setBinary(boolean binary) {
        this.binary = binary;
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


    /**
     * Set to true to receive notification about each file as it is
     * transferred.
     *
     * @param verbose true if verbose notifications are required.
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
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

    /**
     * Set to true to preserve modification times for "gotten" files.
     *
     * @param preserveLastModified if true preserver modification times.
     */
    public void setPreserveLastModified(boolean preserveLastModified) {
        this.preserveLastModified = preserveLastModified;
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


    /**
     * Sets the default mask for file creation on a unix server.
     *
     * @param theUmask unix style umask for files created on the remote server.
     */
    public void setUmask(String theUmask) {
        this.umask = theUmask;
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


    /**
     * The output file for the "list" action. This attribute is ignored for
     * any other actions.
     *
     * @param listing file in which to store the listing.
     */
    public void setListing(File listing) {
        this.listing = listing;
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

    private void configurationHasBeenSet() {
        this.isConfigurationSet = true;
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
    public void setServerLanguageCodeConfig(LanguageCode serverLanguageCode) {
        if (serverLanguageCode != null && !serverLanguageCode.getValue().isEmpty()) {
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
        return serverLanguageCodeConfig.getValue();
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
    Granularity getTimestampGranularity() {
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

    /**
     * Sets the initialSiteCommand attribute.  This attribute
     * names a site command that will be executed immediately
     * after connection.
     * @param initialCommand The initialSiteCommand to set.
     */
    public void setInitialSiteCommand(String initialCommand) {
        this.initialSiteCommand = initialCommand;
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

    /**
     * Sets the timeout on the data connection in milliseconds.
     * Any negative value is discarded and leaves the default
     * A value of 0 means an infinite timeout
     *
     * @param dataTimeout int
     * @since Ant 1.10.7
     */
    public void setDataTimeout(int dataTimeout) {
        if (dataTimeout >= 0) {
            this.dataTimeout = dataTimeout;
        }
    }

    /**
     * Sets the time interval when we should automatically
     * call a command triggering a transfer
     * The parameter is in seconds
     *
     * @param wakeUpTransferInterval int
     * @since Ant 1.10.7
     */
    public void setWakeUpTransferInterval(int wakeUpTransferInterval) {
        if (wakeUpTransferInterval > 0) {
            this.wakeUpTransferInterval = wakeUpTransferInterval;
        }
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
                    "commons-net.jar >= 1.4.0 is required for the specified attributes.");
            }
        }
    }

    /**
     * Executable a retryable object.
     * @param h the retry handler.
     * @param r the object that should be retried until it succeeds
     *          or the number of retries is reached.
     * @param descr a description of the command that is being run.
     * @throws IOException if there is a problem.
     */
    protected void executeRetryable(RetryHandler h, Retryable r, String descr)
        throws IOException {
        h.execute(r, descr);
    }


    /**
     * For each file in the fileset, do the appropriate action: send, get,
     * delete, or list.
     *
     * @param ftp the FTPClient instance used to perform FTP actions
     * @param fs the fileset on which the actions are performed.
     *
     * @return the number of files to be transferred.
     *
     * @throws IOException if there is a problem reading a file
     * @throws BuildException if there is a problem in the configuration.
     */
    protected int transferFiles(final FTPClient ftp, FileSet fs)
        throws IOException, BuildException {
        DirectoryScanner ds;
        if (action == SEND_FILES) {
            ds = fs.getDirectoryScanner(getProject());
        } else {
            ds = new FTPDirectoryScanner(ftp);
            fs.setupDirectoryScanner(ds, getProject());
            ds.setFollowSymlinks(fs.isFollowSymlinks());
            ds.scan();
        }

        String[] dsfiles;
        if (action == RM_DIR) {
            dsfiles = ds.getIncludedDirectories();
        } else {
            dsfiles = ds.getIncludedFiles();
        }
        String dir = null;

        if (ds.getBasedir() == null
            && (action == SEND_FILES || action == GET_FILES)) {
            throw new BuildException(
                "the dir attribute must be set for send and get actions");
        }
        if (action == SEND_FILES || action == GET_FILES) {
            dir = ds.getBasedir().getAbsolutePath();
        }

        // If we are doing a listing, we need the output stream created now.
        BufferedWriter bw = null;

        try {
            if (action == LIST_FILES) {
                File pd = listing.getParentFile();

                if (!pd.exists()) {
                    pd.mkdirs();
                }
                bw = new BufferedWriter(new FileWriter(listing));
            }
            RetryHandler h = new RetryHandler(this.retriesAllowed, this);
            if (action == RM_DIR) {
                // to remove directories, start by the end of the list
                // the trunk does not let itself be removed before the leaves
                for (int i = dsfiles.length - 1; i >= 0; i--) {
                    final String dsfile = dsfiles[i];
                    executeRetryable(h, () -> rmDir(ftp, dsfile), dsfile);
                }
            } else {
                final BufferedWriter fbw = bw;
                final String fdir = dir;
                if (this.newerOnly) {
                    this.granularityMillis =
                        this.timestampGranularity.getMilliseconds(action);
                }
                for (final String dsfile : dsfiles) {
                    executeRetryable(h, () -> {
                        switch (action) {
                            case SEND_FILES:
                                sendFile(ftp, fdir, dsfile);
                                break;
                            case GET_FILES:
                                getFile(ftp, fdir, dsfile);
                                break;
                            case DEL_FILES:
                                delFile(ftp, dsfile);
                                break;
                            case LIST_FILES:
                                listFile(ftp, fbw, dsfile);
                                break;
                            case CHMOD:
                                doSiteCommand(ftp, "chmod " + chmod
                                        + " " + resolveFile(dsfile));
                                transferred++;
                                break;
                            default:
                                throw new BuildException("unknown ftp action " + action);
                        }
                    }, dsfile);
                }
            }
        } finally {
            FileUtils.close(bw);
        }

        return dsfiles.length;
    }

    /**
     * Sends all files specified by the configured filesets to the remote
     * server.
     *
     * @param ftp the FTPClient instance used to perform FTP actions
     *
     * @throws IOException if there is a problem reading a file
     * @throws BuildException if there is a problem in the configuration.
     */
    protected void transferFiles(FTPClient ftp)
        throws IOException, BuildException {
        transferred = 0;
        skipped = 0;

        if (filesets.isEmpty()) {
            throw new BuildException("at least one fileset must be specified.");
        }
        for (FileSet fs : filesets) {
            if (fs != null) {
                transferFiles(ftp, fs);
            }
        }

        log(transferred + " " + ACTION_TARGET_STRS[action] + " "
            + COMPLETED_ACTION_STRS[action]);
        if (skipped != 0) {
            log(skipped + " " + ACTION_TARGET_STRS[action]
                + " were not successfully " + COMPLETED_ACTION_STRS[action]);
        }
    }

    /**
     * Correct a file path to correspond to the remote host requirements. This
     * implementation currently assumes that the remote end can handle
     * Unix-style paths with forward-slash separators. This can be overridden
     * with the <code>separator</code> task parameter. No attempt is made to
     * determine what syntax is appropriate for the remote host.
     *
     * @param file the remote file name to be resolved
     *
     * @return the filename as it will appear on the server.
     */
    protected String resolveFile(String file) {
        return file.replace(File.separator.charAt(0), remoteFileSep.charAt(0));
    }

    /**
     * Creates all parent directories specified in a complete relative
     * pathname. Attempts to create existing directories will not cause
     * errors.
     *
     * @param ftp the FTP client instance to use to execute FTP actions on
     *        the remote server.
     * @param filename the name of the file whose parents should be created.
     * @throws IOException under non documented circumstances
     * @throws BuildException if it is impossible to cd to a remote directory
     *
     */
    protected void createParents(FTPClient ftp, String filename)
        throws IOException, BuildException {

        File dir = new File(filename);
        if (dirCache.contains(dir)) {
            return;
        }

        List<File> parents = new Vector<>();
        String dirname;

        while ((dirname = dir.getParent()) != null) {
            File checkDir = new File(dirname);
            if (dirCache.contains(checkDir)) {
                break;
            }
            dir = checkDir;
            parents.add(dir);
        }

        // find first non cached dir
        int i = parents.size() - 1;

        if (i >= 0) {
            String cwd = ftp.printWorkingDirectory();
            String parent = dir.getParent();
            if (parent != null
                && !ftp.changeWorkingDirectory(resolveFile(parent))) {
                throw new BuildException("could not change to directory: %s",
                    ftp.getReplyString());
            }

            while (i >= 0) {
                dir = parents.get(i--);
                // check if dir exists by trying to change into it.
                if (!ftp.changeWorkingDirectory(dir.getName())) {
                    // could not change to it - try to create it
                    log("creating remote directory "
                        + resolveFile(dir.getPath()), Project.MSG_VERBOSE);
                    if (!ftp.makeDirectory(dir.getName())) {
                        handleMkDirFailure(ftp);
                    }
                    if (!ftp.changeWorkingDirectory(dir.getName())) {
                        throw new BuildException(
                            "could not change to directory: %s",
                            ftp.getReplyString());
                    }
                }
                dirCache.add(dir);
            }
            ftp.changeWorkingDirectory(cwd);
        }
    }
    /**
     * auto find the time difference between local and remote
     * @param ftp handle to ftp client
     * @return number of millis to add to remote time to make it comparable to local time
     * @since ant 1.6
     */
    private long getTimeDiff(FTPClient ftp) {
        long returnValue = 0;
        File tempFile = findFileName(ftp);
        try {
            // create a local temporary file
            FILE_UTILS.createNewFile(tempFile);
            long localTimeStamp = tempFile.lastModified();
            BufferedInputStream instream = new BufferedInputStream(
                    Files.newInputStream(tempFile.toPath()));
            ftp.storeFile(tempFile.getName(), instream);
            instream.close();
            boolean success = FTPReply.isPositiveCompletion(ftp.getReplyCode());
            if (success) {
                FTPFile[] ftpFiles = ftp.listFiles(tempFile.getName());
                if (ftpFiles.length == 1) {
                    long remoteTimeStamp = ftpFiles[0].getTimestamp().getTime().getTime();
                    returnValue = localTimeStamp - remoteTimeStamp;
                }
                ftp.deleteFile(ftpFiles[0].getName());
            }
            // delegate the deletion of the local temp file to the delete task
            // because of race conditions occurring on Windows
            Delete mydelete = new Delete();
            mydelete.bindToOwner(this);
            mydelete.setFile(tempFile.getCanonicalFile());
            mydelete.execute();
        } catch (Exception e) {
            throw new BuildException(e, getLocation());
        }
        return returnValue;
    }

    /**
     *  find a suitable name for local and remote temporary file
     */
    private File findFileName(FTPClient ftp) {
        FTPFile[] files = null;
        final int maxIterations = 1000;
        for (int counter = 1; counter < maxIterations; counter++) {
            File localFile = FILE_UTILS.createTempFile(getProject(),
                                                       "ant" + counter, ".tmp",
                                                       null, false, false);
            String fileName = localFile.getName();
            boolean found = false;
            try {
                if (files == null) {
                    files = ftp.listFiles();
                }
                for (FTPFile file : files) {
                    if (file != null && file.getName().equals(fileName)) {
                        found = true;
                        break;
                    }
                }
            } catch (IOException ioe) {
                throw new BuildException(ioe, getLocation());
            }
            if (!found) {
                localFile.deleteOnExit();
                return localFile;
            }
        }
        return null;
    }

    /**
     * Checks to see if the remote file is current as compared with the local
     * file. Returns true if the target file is up to date.
     * @param ftp ftpclient
     * @param localFile local file
     * @param remoteFile remote file
     * @return true if the target file is up to date
     * @throws IOException  in unknown circumstances
     * @throws BuildException if the date of the remote files cannot be found and the action is
     * GET_FILES
     */
    protected boolean isUpToDate(FTPClient ftp, File localFile,
                                 String remoteFile)
        throws IOException, BuildException {
        log("checking date for " + remoteFile, Project.MSG_VERBOSE);

        FTPFile[] files = ftp.listFiles(remoteFile);

        // For Microsoft's Ftp-Service an Array with length 0 is
        // returned if configured to return listings in "MS-DOS"-Format
        if (files == null || files.length == 0) {
            // If we are sending files, then assume out of date.
            // If we are getting files, then throw an error

            if (action == SEND_FILES) {
                log("Could not date test remote file: " + remoteFile
                    + "assuming out of date.", Project.MSG_VERBOSE);
                return false;
            }
            throw new BuildException("could not date test remote file: %s",
                ftp.getReplyString());
        }

        long remoteTimestamp = files[0].getTimestamp().getTime().getTime();
        long localTimestamp = localFile.lastModified();
        long adjustedRemoteTimestamp =
            remoteTimestamp + this.timeDiffMillis + this.granularityMillis;

        StringBuilder msg;
        synchronized (TIMESTAMP_LOGGING_SDF) {
            msg = new StringBuilder("   [")
                .append(TIMESTAMP_LOGGING_SDF.format(new Date(localTimestamp)))
                .append("] local");
        }
        log(msg.toString(), Project.MSG_VERBOSE);

        synchronized (TIMESTAMP_LOGGING_SDF) {
            msg = new StringBuilder("   [")
                .append(TIMESTAMP_LOGGING_SDF
                    .format(new Date(adjustedRemoteTimestamp)))
                .append("] remote");
        }
        if (remoteTimestamp != adjustedRemoteTimestamp) {
            synchronized (TIMESTAMP_LOGGING_SDF) {
                msg.append(" - (raw: ")
                    .append(
                        TIMESTAMP_LOGGING_SDF.format(new Date(remoteTimestamp)))
                    .append(")");
            }
        }
        log(msg.toString(), Project.MSG_VERBOSE);

        if (this.action == SEND_FILES) {
            return adjustedRemoteTimestamp >= localTimestamp;
        }
        return localTimestamp >= adjustedRemoteTimestamp;
    }

    /**
     * Sends a site command to the ftp server
     * @param ftp ftp client
     * @param theCMD command to execute
     * @throws IOException  in unknown circumstances
     * @throws BuildException in unknown circumstances
     */
    protected void doSiteCommand(FTPClient ftp, String theCMD)
        throws IOException, BuildException {

        log("Doing Site Command: " + theCMD, Project.MSG_VERBOSE);

        if (!ftp.sendSiteCommand(theCMD)) {
            log("Failed to issue Site Command: " + theCMD, Project.MSG_WARN);
        } else {
            for (String reply : ftp.getReplyStrings()) {
                if (reply != null && !reply.contains("200")) {
                    log(reply, Project.MSG_WARN);
                }
            }
        }
    }

    /**
     * Sends a single file to the remote host. <code>filename</code> may
     * contain a relative path specification. When this is the case, <code>sendFile</code>
     * will attempt to create any necessary parent directories before sending
     * the file. The file will then be sent using the entire relative path
     * spec - no attempt is made to change directories. It is anticipated that
     * this may eventually cause problems with some FTP servers, but it
     * simplifies the coding.
     * @param ftp ftp client
     * @param dir base directory of the file to be sent (local)
     * @param filename relative path of the file to be send
     *        locally relative to dir
     *        remotely relative to the remotedir attribute
     * @throws IOException  in unknown circumstances
     * @throws BuildException in unknown circumstances
     */
    protected void sendFile(FTPClient ftp, String dir, String filename)
        throws IOException, BuildException {
        InputStream instream = null;

        try {
            // TODO - why not simply new File(dir, filename)?
            File file = getProject().resolveFile(new File(dir, filename).getPath());

            if (newerOnly && isUpToDate(ftp, file, resolveFile(filename))) {
                return;
            }

            if (verbose) {
                log("transferring " + file.getAbsolutePath());
            }

            instream = new BufferedInputStream(Files.newInputStream(file.toPath()));

            createParents(ftp, filename);

            ftp.storeFile(resolveFile(filename), instream);

            boolean success = FTPReply.isPositiveCompletion(ftp.getReplyCode());

            if (!success) {
                String s = "could not put file: " + ftp.getReplyString();

                if (skipFailedTransfers) {
                    log(s, Project.MSG_WARN);
                    skipped++;
                } else {
                    throw new BuildException(s);
                }

            } else {
                // see if we should issue a chmod command
                if (chmod != null) {
                    doSiteCommand(ftp, "chmod " + chmod + " " + resolveFile(filename));
                }
                log("File " + file.getAbsolutePath() + " copied to " + server,
                    Project.MSG_VERBOSE);
                transferred++;
            }
        } finally {
            FileUtils.close(instream);
        }
    }

    /**
     * Delete a file from the remote host.
     * @param ftp ftp client
     * @param filename file to delete
     * @throws IOException  in unknown circumstances
     * @throws BuildException if skipFailedTransfers is set to false
     * and the deletion could not be done
     */
    protected void delFile(FTPClient ftp, String filename)
        throws IOException, BuildException {
        if (verbose) {
            log("deleting " + filename);
        }

        if (!ftp.deleteFile(resolveFile(filename))) {
            String s = "could not delete file: " + ftp.getReplyString();

            if (skipFailedTransfers) {
                log(s, Project.MSG_WARN);
                skipped++;
            } else {
                throw new BuildException(s);
            }
        } else {
            log("File " + filename + " deleted from " + server,
                Project.MSG_VERBOSE);
            transferred++;
        }
    }

    /**
     * Delete a directory, if empty, from the remote host.
     * @param ftp ftp client
     * @param dirname directory to delete
     * @throws IOException  in unknown circumstances
     * @throws BuildException if skipFailedTransfers is set to false
     * and the deletion could not be done
     */
    protected void rmDir(FTPClient ftp, String dirname)
        throws IOException, BuildException {
        if (verbose) {
            log("removing " + dirname);
        }

        if (!ftp.removeDirectory(resolveFile(dirname))) {
            String s = "could not remove directory: " + ftp.getReplyString();

            if (skipFailedTransfers) {
                log(s, Project.MSG_WARN);
                skipped++;
            } else {
                throw new BuildException(s);
            }
        } else {
            log("Directory " + dirname + " removed from " + server,
                Project.MSG_VERBOSE);
            transferred++;
        }
    }


    /**
     * Retrieve a single file from the remote host. <code>filename</code> may
     * contain a relative path specification. <p>
     *
     * The file will then be retrieved using the entire relative path spec -
     * no attempt is made to change directories. It is anticipated that this
     * may eventually cause problems with some FTP servers, but it simplifies
     * the coding.</p>
     * @param ftp the ftp client
     * @param dir local base directory to which the file should go back
     * @param filename relative path of the file based upon the ftp remote directory
     *        and/or the local base directory (dir)
     * @throws IOException  in unknown circumstances
     * @throws BuildException if skipFailedTransfers is false
     * and the file cannot be retrieved.
     */
    protected void getFile(FTPClient ftp, String dir, String filename)
        throws IOException, BuildException {
        File file = getProject().resolveFile(new File(dir, filename).getPath());
        OutputStream outstream = null;
        try {
            if (newerOnly && isUpToDate(ftp, file, resolveFile(filename))) {
                return;
            }

            if (verbose) {
                log("transferring " + filename + " to "
                    + file.getAbsolutePath());
            }

            File pdir = file.getParentFile();

            if (!pdir.exists()) {
                pdir.mkdirs();
            }
            outstream = new BufferedOutputStream(Files.newOutputStream(file.toPath()));
            ftp.retrieveFile(resolveFile(filename), outstream);

            if (!FTPReply.isPositiveCompletion(ftp.getReplyCode())) {
                String s = "could not get file: " + ftp.getReplyString();

                if (skipFailedTransfers) {
                    log(s, Project.MSG_WARN);
                    skipped++;
                } else {
                    throw new BuildException(s);
                }

            } else {
                log("File " + file.getAbsolutePath() + " copied from "
                    + server, Project.MSG_VERBOSE);
                transferred++;
                if (preserveLastModified) {
                    outstream.close();
                    outstream = null;
                    FTPFile[] remote = ftp.listFiles(resolveFile(filename));
                    if (remote.length > 0) {
                        FILE_UTILS.setFileLastModified(file,
                                                       remote[0].getTimestamp()
                                                       .getTime().getTime());
                    }
                }
            }
        } finally {
            FileUtils.close(outstream);
        }
    }

    /**
     * List information about a single file from the remote host. <code>filename</code>
     * may contain a relative path specification. <p>
     *
     * The file listing will then be retrieved using the entire relative path
     * spec - no attempt is made to change directories. It is anticipated that
     * this may eventually cause problems with some FTP servers, but it
     * simplifies the coding.</p>
     * @param ftp ftp client
     * @param bw buffered writer
     * @param filename the directory one wants to list
     * @throws IOException  in unknown circumstances
     * @throws BuildException in unknown circumstances
     */
    protected void listFile(FTPClient ftp, BufferedWriter bw, String filename)
        throws IOException, BuildException {
        if (verbose) {
            log("listing " + filename);
        }
        FTPFile[] ftpfiles = ftp.listFiles(resolveFile(filename));

        if (ftpfiles != null && ftpfiles.length > 0) {
            bw.write(ftpfiles[0].toString());
            bw.newLine();
            transferred++;
        }
    }


    /**
     * Create the specified directory on the remote host.
     *
     * @param ftp The FTP client connection
     * @param dir The directory to create (format must be correct for host
     *      type)
     * @throws IOException  in unknown circumstances
     * @throws BuildException if ignoreNoncriticalErrors has not been set to true
     *         and a directory could not be created, for instance because it was
     *         already existing. Precisely, the codes 521, 550 and 553 will trigger
     *         a BuildException
     */
    protected void makeRemoteDir(FTPClient ftp, String dir)
        throws IOException, BuildException {
        String workingDirectory = ftp.printWorkingDirectory();
        boolean absolute = dir.startsWith("/");
        if (verbose) {
            if (absolute || workingDirectory == null) {
                log("Creating directory: " + dir + " in /");
            } else {
                log("Creating directory: " + dir + " in " + workingDirectory);
            }
        }
        if (absolute) {
            ftp.changeWorkingDirectory("/");
        }
        StringTokenizer st = new StringTokenizer(dir, "/");
        while (st.hasMoreTokens()) {
            String subdir = st.nextToken();
            log("Checking " + subdir, Project.MSG_DEBUG);
            if (!ftp.changeWorkingDirectory(subdir)) {
                if (ftp.makeDirectory(subdir)) {
                    if (verbose) {
                        log("Directory created OK");
                    }
                    ftp.changeWorkingDirectory(subdir);
                } else {
                    // codes 521, 550 and 553 can be produced by FTP Servers
                    //  to indicate that an attempt to create a directory has
                    //  failed because the directory already exists.
                    int rc = ftp.getReplyCode();
                    if (!ignoreNoncriticalErrors
                            || rc != CODE_550 && rc != CODE_553 && rc != CODE_521) {
                        throw new BuildException(
                            "could not create directory: %s",
                            ftp.getReplyString());
                    }
                    if (verbose) {
                        log("Directory already exists");
                    }
                }
            }
        }
        if (workingDirectory != null) {
            ftp.changeWorkingDirectory(workingDirectory);
        }
    }

    /**
     * look at the response for a failed mkdir action, decide whether
     * it matters or not. If it does, we throw an exception
     * @param ftp current ftp connection
     * @throws BuildException if this is an error to signal
     */
    private void handleMkDirFailure(FTPClient ftp)
        throws BuildException {
        int rc = ftp.getReplyCode();
        if (!ignoreNoncriticalErrors || rc != CODE_550 && rc != CODE_553 && rc != CODE_521) {
            throw new BuildException("could not create directory: %s",
                ftp.getReplyString());
        }
    }

    /**
     * checks if the wake up interval is expired
     */
    private boolean wakeUpTransferIntervalExpired() {
        boolean result = false;

        // on the first call, initialize the keep-alive mechanism
        // by storing the current date
        if (lastWakeUpTime == 0) {
            lastWakeUpTime = (new Date()).getTime();
        } else {
            long currentTime = (new Date()).getTime();
            if (currentTime > (lastWakeUpTime + wakeUpTransferInterval * 1000)) {
                lastWakeUpTime = currentTime;
                result = true;
            }
        }

        return result;
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

        FTPClient ftp = null;

        try {
            log("Opening FTP connection to " + server, Project.MSG_VERBOSE);

            ftp = new FTPClient();
            if (this.isConfigurationSet) {
                ftp = FTPConfigurator.configure(ftp, this);
            }

            ftp.setRemoteVerificationEnabled(enableRemoteVerification);
            ftp.connect(server, port);

            if (dataTimeout >= 0) {
                ftp.setDataTimeout(dataTimeout);
                log("Setting data timeout to " + dataTimeout, Project.MSG_VERBOSE);
            }
            if (!FTPReply.isPositiveCompletion(ftp.getReplyCode())) {
                throw new BuildException("FTP connection failed: %s",
                    ftp.getReplyString());
            }

            log("connected", Project.MSG_VERBOSE);
            log("logging in to FTP server", Project.MSG_VERBOSE);

            if ((this.account != null && !ftp.login(userid, password, account))
                || (this.account == null && !ftp.login(userid, password))) {
                throw new BuildException("Could not login to FTP server");
            }

            log("login succeeded", Project.MSG_VERBOSE);

            if (binary) {
                ftp.setFileType(org.apache.commons.net.ftp.FTP.BINARY_FILE_TYPE);
            } else {
                ftp.setFileType(org.apache.commons.net.ftp.FTP.ASCII_FILE_TYPE);
            }
            if (!FTPReply.isPositiveCompletion(ftp.getReplyCode())) {
                throw new BuildException("could not set transfer type: %s",
                    ftp.getReplyString());
            }

            if (passive) {
                log("entering passive mode", Project.MSG_VERBOSE);
                ftp.enterLocalPassiveMode();
                if (!FTPReply.isPositiveCompletion(ftp.getReplyCode())) {
                    throw new BuildException(
                        "could not enter into passive mode: %s",
                        ftp.getReplyString());
                }
            }

            // If an initial command was configured then send it.
            // Some FTP servers offer different modes of operation,
            // E.G. switching between a UNIX file system mode and
            // a legacy file system.
            if (this.initialSiteCommand != null) {
                final FTPClient lftp = ftp;
                executeRetryable(new RetryHandler(this.retriesAllowed, this),
                    () -> doSiteCommand(lftp, FTP.this.initialSiteCommand),
                    "initial site command: " + this.initialSiteCommand);
            }

            // For a unix ftp server you can set the default mask for all files
            // created.

            if (umask != null) {
                final FTPClient lftp = ftp;
                executeRetryable(new RetryHandler(this.retriesAllowed, this),
                    () -> doSiteCommand(lftp, "umask " + umask),
                    "umask " + umask);
            }

            // If the action is MK_DIR, then the specified remote
            // directory is the directory to create.

            if (action == MK_DIR) {
                final FTPClient lftp = ftp;
                executeRetryable(new RetryHandler(this.retriesAllowed, this),
                    () -> makeRemoteDir(lftp, remotedir), remotedir);
            } else if (action == SITE_CMD) {
                final FTPClient lftp = ftp;
                executeRetryable(new RetryHandler(this.retriesAllowed, this),
                    () -> doSiteCommand(lftp, FTP.this.siteCommand),
                    "Site Command: " + this.siteCommand);
            } else {
                if (remotedir != null) {
                    log("changing the remote directory to " + remotedir,
                        Project.MSG_VERBOSE);
                    ftp.changeWorkingDirectory(remotedir);
                    if (!FTPReply.isPositiveCompletion(ftp.getReplyCode())) {
                        throw new BuildException(
                            "could not change remote directory: %s",
                            ftp.getReplyString());
                    }
                }
                if (newerOnly && timeDiffAuto) {
                    // in this case we want to find how much time span there is between local
                    // and remote
                    timeDiffMillis = getTimeDiff(ftp);
                }
                log(ACTION_STRS[action] + " " + ACTION_TARGET_STRS[action]);
                transferFiles(ftp);
            }

        } catch (IOException ex) {
            final Throwable cause = ex.getCause();
            if (cause != null) {
                final String msg = cause.toString();
                if (msg != null && msg.contains("java.net.SocketTimeoutException")) {
                    // When a read timeout occurs, inform the server that it
                    // should abort.
                    // Note that the latest commons-net (3.6) still does not
                    // support sending urgent data, which is normally a
                    // prerequisite for ABORT command.
                    // As a consequence, it  might not be taken in account immediately
                    try {
                        ftp.abort();
                    } catch (IOException ioe) {
                        // ignore it
                    }
                }
            }
            throw new BuildException("error during FTP transfer: " + ex, ex);
        } finally {
            if (ftp != null && ftp.isConnected()) {
                try {
                    log("disconnecting", Project.MSG_VERBOSE);
                    ftp.logout();
                    ftp.disconnect();
                } catch (IOException ex) {
                    // ignore it
                }
            }
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
            switch (getValue().toLowerCase(Locale.ENGLISH)) {
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
            default:
                return SEND_FILES;
            }

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

    /**
     * Enumerated class for languages.
     */
    public static class LanguageCode extends EnumeratedAttribute {

        private static final String[] VALID_LANGUAGE_CODES =
            getValidLanguageCodes();

        private static String[] getValidLanguageCodes() {
            Collection<String> c = FTPClientConfig.getSupportedLanguageCodes();
            String[] ret = new String[c.size() + 1];
            int i = 0;
            ret[i++] = "";
            for (String element : c) {
                ret[i++] = element;
            }
            return ret;
        }

        /**
         * Return the value values.
         * @return the list of valid language types.
         */
        @Override
        public String[] getValues() {
            return VALID_LANGUAGE_CODES;
        }

        static final LanguageCode getDefault() {
            LanguageCode lc = new LanguageCode();
            lc.setValue("");
            return lc;
        }
    }

}
