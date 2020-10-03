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
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Delete;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.selectors.SelectorUtils;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.RetryHandler;
import org.apache.tools.ant.util.Retryable;
import org.apache.tools.ant.util.VectorSet;

public class FTPTaskMirrorImpl implements FTPTaskMirror {

    /** return code of ftp */
    private static final int CODE_521 = 521;
    private static final int CODE_550 = 550;
    private static final int CODE_553 = 553;

    /** Date formatter used in logging, note not thread safe! */
    private static final SimpleDateFormat TIMESTAMP_LOGGING_SDF =
        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    private final FTPTask task;
    private Set<File> dirCache = new HashSet<>();
    private int transferred = 0;
    private int skipped = 0;

    /**
     * Constructor.
     * @param task the FTPTask that uses this mirror.
     */
    public FTPTaskMirrorImpl(FTPTask task) {
        this.task = task;
    }

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
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < parts.length - 1; i++) {
                result.append(File.separatorChar).append(parts[i]);
            }
            return result.toString();
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

            Map<String, String> newroots = new Hashtable<>();
            // put in the newroots vector the include patterns without
            // wildcard tokens
            for (String include : includes) {
                String newpattern =
                        SelectorUtils.rtrimWildcardTokens(include);
                newroots.put(newpattern, include);
            }
            if (task.getRemotedir() == null) {
                try {
                    task.setRemotedir(ftp.printWorkingDirectory());
                } catch (IOException e) {
                    throw new BuildException("could not read current ftp directory",
                                             task.getLocation());
                }
            }
            AntFTPFile baseFTPFile = new AntFTPRootFile(ftp, task.getRemotedir());
            rootPath = baseFTPFile.getAbsolutePath();
            // construct it
            if (newroots.containsKey("")) {
                // we are going to scan everything anyway
                scandir(rootPath, "", true);
            } else {
                // only scan directories that can include matched files or
                // directories
                newroots.forEach(
                    (k, v) -> scanRoot(new AntFTPFile(baseFTPFile, k), v));
            }
        }

        private void scanRoot(AntFTPFile myfile, String originalpattern) {
            String currentelement;
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
                        throw new BuildException(be, task.getLocation());
                    } catch (BuildException be) {
                        isOK = false;
                    }
                }
            } else {
                isOK = false;
            }
            if (isOK) {
                currentelement = path.replace(task.getSeparator().charAt(0), File.separatorChar);
                if (!isFollowSymlinks()
                    && traversesSymlinks) {
                    return;
                }

                if (myfile.isDirectory()) {
                    if (isIncluded(currentelement) && !currentelement.isEmpty()) {
                        accountForIncludedDir(currentelement, myfile, true);
                    } else {
                        if (!currentelement.isEmpty() && currentelement.charAt(currentelement.length() - 1)
                                != File.separatorChar) {
                            currentelement += File.separatorChar;
                        }
                        scandir(myfile.getAbsolutePath(), currentelement, true);
                    }
                } else if (isCaseSensitive
                    && originalpattern.equals(currentelement)) {
                    accountForIncludedFile(currentelement);
                } else if (!isCaseSensitive
                           && originalpattern
                           .equalsIgnoreCase(currentelement)) {
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
                String completePath;
                if (vpath.isEmpty()) {
                    completePath = rootPath;
                } else {
                    completePath = rootPath + task.getSeparator()
                        + vpath.replace(File.separatorChar, task.getSeparator().charAt(0));
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
                            } else if (isFunctioningAsFile(ftp, dir, file)) {
                                accountForIncludedFile(name);
                            }
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
            if (!dirsIncluded.contains(name) && !dirsExcluded.contains(name)) {
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
            //task.log("listing files in directory " + directory, Project.MSG_DEBUG);
            String currentPath = directory;
            if (changedir) {
                try {
                    if (!ftp.changeWorkingDirectory(directory)) {
                        return null;
                    }
                    currentPath = ftp.printWorkingDirectory();
                } catch (IOException ioe) {
                    throw new BuildException(ioe, task.getLocation());
                }
            }
            if (fileListMap.containsKey(currentPath)) {
                task.log("filelist map used in listing files", Project.MSG_DEBUG);
                return fileListMap.get(currentPath);
            }
            FTPFile[] result;
            try {
                result = ftp.listFiles();
            } catch (IOException ioe) {
                throw new BuildException(ioe, task.getLocation());
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
                    throw new BuildException(ioe, task.getLocation());
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
                        task.log("will try to cd to "
                                         + target + " where a directory called " + array[icounter].getName()
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
                    task.log("testing case sensitivity, attempting to cd to "
                                     + target, Project.MSG_DEBUG);
                    remoteSystemCaseSensitive  = !ftp.changeWorkingDirectory(target);
                } catch (IOException ioe) {
                    remoteSystemCaseSensitive = true;
                } finally {
                    try {
                        ftp.changeWorkingDirectory(directory);
                    } catch (IOException ioe) {
                        throw new BuildException(ioe, task.getLocation()); //NOSONAR
                    }
                }
                task.log("remote system is case sensitive : "
                                      + remoteSystemCaseSensitive,
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
                    boolean result = this.client.changeWorkingDirectory(parent.getAbsolutePath());
                    //this should not happen, except if parent has been deleted by another process
                    if (!result) {
                        return;
                    }
                    this.curpwd = parent.getAbsolutePath();
                } catch (IOException ioe) {
                    throw new BuildException("could not change working dir to %s", parent.curpwd);
                }
                for (String currentPathElement : pathElements) {
                    try {
                        if (!this.client.changeWorkingDirectory(currentPathElement)) {
                            if (!isCaseSensitive() && (remoteSystemCaseSensitive
                                    || !remoteSensitivityChecked)) {
                                currentPathElement = findPathElementCaseUnsensitive(this.curpwd,
                                        currentPathElement);
                                if (currentPathElement == null) {
                                    return;
                                }
                            }
                            return;
                        }
                        this.curpwd = getCurpwdPlusFileSep() + currentPathElement;
                    } catch (IOException ioe) {
                        throw new BuildException("could not change working dir to %s from %s",
                                currentPathElement, this.curpwd);
                    }
                }
                String lastpathelement = pathElements.get(pathElements.size() - 1);
                this.ftpFile = getFile(listFiles(this.curpwd), lastpathelement);
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
                return ftpFile != null;
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
                if (absPath.startsWith(rootPath + task.getSeparator())) {
                    return absPath.substring(rootPath.length()
                                             + task.getSeparator().length());
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
                List<String> pathElements = SelectorUtils
                    .tokenizePath(getAbsolutePath(), task.getSeparator());
                List<String> pathElements2 = SelectorUtils
                    .tokenizePath(currentPath, task.getSeparator());
                StringBuilder relPath = new StringBuilder(currentRelativePath);
                final int size = pathElements.size();
                for (int pcount = pathElements2.size(); pcount < size; pcount++) {
                    String currentElement = pathElements.get(pcount);
                    FTPFile[] theFiles = listFiles(currentPath);
                    FTPFile theFile = null;
                    if (theFiles != null) {
                        theFile = getFile(theFiles, currentElement);
                    }
                    if (relPath.length() > 0) {
                        relPath.append(task.getSeparator());
                    }
                    if (theFile == null) {
                        // hit a hidden file assume not a symlink
                        relPath.append(currentElement);
                        currentPath += task.getSeparator() + currentElement;
                        task.log("Hidden file " + relPath
                                 + " assumed to not be a symlink.",
                                 Project.MSG_VERBOSE);
                    } else {
                        traversesSymlinks = traversesSymlinks || theFile.isSymbolicLink();
                        relPath.append(theFile.getName());
                        currentPath += task.getSeparator() + theFile.getName();
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
                String sep = task.getSeparator();
                return curpwd.endsWith(sep) ? curpwd : curpwd + sep;
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
                    throw new BuildException(ioe, task.getLocation());
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
    private boolean isFunctioningAsDirectory(FTPClient ftp, String dir, FTPFile file) {
        boolean result = false;
        String currentWorkingDir = null;
        if (file.isDirectory()) {
            return true;
        }
        if (file.isFile()) {
            return false;
        }
        try {
            currentWorkingDir = ftp.printWorkingDirectory();
        } catch (IOException ioe) {
            task.log("could not find current working directory " + dir
                                  + " while checking a symlink", Project.MSG_DEBUG);
        }
        if (currentWorkingDir != null) {
            try {
                result = ftp.changeWorkingDirectory(file.getLink());
            } catch (IOException ioe) {
                task.log("could not cd to " + file.getLink()
                                      + " while checking a symlink",
                                      Project.MSG_DEBUG);
            }
            if (result) {
                boolean comeback = false;
                try {
                    comeback = ftp.changeWorkingDirectory(currentWorkingDir);
                } catch (IOException ioe) {
                    task.log("could not cd back to " + dir + " while checking a symlink",
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
    private boolean isFunctioningAsFile(FTPClient ftp, String dir, FTPFile file) {
        return !file.isDirectory() && (file.isFile() || !isFunctioningAsDirectory(ftp, dir, file));
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
        if (task.getAction() == FTPTask.SEND_FILES) {
            ds = fs.getDirectoryScanner(task.getProject());
        } else {
            ds = new FTPDirectoryScanner(ftp);
            fs.setupDirectoryScanner(ds, task.getProject());
            ds.setFollowSymlinks(fs.isFollowSymlinks());
            ds.scan();
        }

        String[] dsfiles = null;
        if (task.getAction() == FTPTask.RM_DIR) {
            dsfiles = ds.getIncludedDirectories();
        } else {
            dsfiles = ds.getIncludedFiles();
        }

        if (ds.getBasedir() == null
            && (task.getAction() == FTPTask.SEND_FILES || task.getAction() == FTPTask.GET_FILES)) {
            throw new BuildException(
                "the dir attribute must be set for send and get actions");
        }
        String dir = null;
        if (task.getAction() == FTPTask.SEND_FILES || task.getAction() == FTPTask.GET_FILES) {
            dir = ds.getBasedir().getAbsolutePath();
        }

        // If we are doing a listing, we need the output stream created now.
        BufferedWriter bw = null;

        try {
            if (task.getAction() == FTPTask.LIST_FILES) {
                File pd = task.getListing().getParentFile();

                if (!pd.exists()) {
                    pd.mkdirs();
                }
                bw = new BufferedWriter(new FileWriter(task.getListing()));
            }
            RetryHandler h = new RetryHandler(task.getRetriesAllowed(), task);
            if (task.getAction() == FTPTask.RM_DIR) {
                // to remove directories, start by the end of the list
                // the trunk does not let itself be removed before the leaves
                for (int i = dsfiles.length - 1; i >= 0; i--) {
                    final String dsfile = dsfiles[i];
                    executeRetryable(h, () -> rmDir(ftp, dsfile), dsfile);
                }
            } else {
                final BufferedWriter fbw = bw;
                final String fdir = dir;
                if (task.isNewer()) {
                    task.setGranularityMillis(task.getTimestampGranularity()
                                              .getMilliseconds(task.getAction()));
                }
                for (final String dsfile : dsfiles) {
                    executeRetryable(h, () -> {
                        switch (task.getAction()) {
                            case FTPTask.SEND_FILES:
                                sendFile(ftp, fdir, dsfile);
                                break;
                            case FTPTask.GET_FILES:
                                getFile(ftp, fdir, dsfile);
                                break;
                            case FTPTask.DEL_FILES:
                                delFile(ftp, dsfile);
                                break;
                            case FTPTask.LIST_FILES:
                                listFile(ftp, fbw, dsfile);
                                break;
                            case FTPTask.CHMOD:
                                doSiteCommand(ftp, "chmod " + task.getChmod() + " "
                                        + resolveFile(dsfile));
                                transferred++;
                                break;
                            default:
                                throw new BuildException("unknown ftp action %s",
                                        task.getAction());
                        }
                    }, dsfile);
                }
            }
        } finally {
            if (bw != null) {
                bw.close();
            }
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

        if (task.getFilesets().isEmpty()) {
            throw new BuildException("at least one fileset must be specified.");
        }
        // get files from filesets
        for (FileSet fs : task.getFilesets()) {
            if (fs != null) {
                transferFiles(ftp, fs);
            }
        }
        task.log(
            transferred + " " + FTPTask.ACTION_TARGET_STRS[task.getAction()]
                + " " + FTPTask.COMPLETED_ACTION_STRS[task.getAction()]);
        if (skipped != 0) {
            task.log(
                skipped + " " + FTPTask.ACTION_TARGET_STRS[task.getAction()]
                    + " were not successfully "
                    + FTPTask.COMPLETED_ACTION_STRS[task.getAction()]);
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
        return file.replace(File.separator.charAt(0), task.getSeparator().charAt(0));
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

        Vector<File> parents = new Vector<>();
        String dirname;

        while ((dirname = dir.getParent()) != null) {
            File checkDir = new File(dirname);
            if (dirCache.contains(checkDir)) {
                break;
            }
            dir = checkDir;
            parents.addElement(dir);
        }

        // find first non cached dir
        int i = parents.size() - 1;

        if (i >= 0) {
            String cwd = ftp.printWorkingDirectory();
            String parent = dir.getParent();
            if (parent != null) {
                if (!ftp.changeWorkingDirectory(resolveFile(parent))) {
                    throw new BuildException(
                        "could not change to directory: %s",
                        ftp.getReplyString());
                }
            }

            while (i >= 0) {
                dir = parents.elementAt(i--);
                // check if dir exists by trying to change into it.
                if (!ftp.changeWorkingDirectory(dir.getName())) {
                    // could not change to it - try to create it
                    task.log("creating remote directory "
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
            BufferedInputStream instream = new BufferedInputStream(Files.newInputStream(tempFile.toPath()));
            ftp.storeFile(tempFile.getName(), instream);
            instream.close();
            if (FTPReply.isPositiveCompletion(ftp.getReplyCode())) {
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
            mydelete.bindToOwner(task);
            mydelete.setFile(tempFile.getCanonicalFile());
            mydelete.execute();
        } catch (Exception e) {
            throw new BuildException(e, task.getLocation());
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
            File localFile = FILE_UTILS.createTempFile(task.getProject(),
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
                throw new BuildException(ioe, task.getLocation());
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
        task.log("checking date for " + remoteFile, Project.MSG_VERBOSE);

        FTPFile[] files = ftp.listFiles(remoteFile);

        // For Microsoft's Ftp-Service an Array with length 0 is
        // returned if configured to return listings in "MS-DOS"-Format
        if (files == null || files.length == 0) {
            // If we are sending files, then assume out of date.
            // If we are getting files, then throw an error

            if (task.getAction() == FTPTask.SEND_FILES) {
                task.log("Could not date test remote file: " + remoteFile
                         + "assuming out of date.", Project.MSG_VERBOSE);
                return false;
            }
            throw new BuildException("could not date test remote file: %s",
                ftp.getReplyString());
        }

        long remoteTimestamp = files[0].getTimestamp().getTime().getTime();
        long localTimestamp = localFile.lastModified();
        long adjustedRemoteTimestamp = remoteTimestamp + task.getTimeDiffMillis()
            + task.getGranularityMillis();

        StringBuilder msg;
        synchronized (TIMESTAMP_LOGGING_SDF) {
            msg = new StringBuilder("   [")
                .append(TIMESTAMP_LOGGING_SDF.format(new Date(localTimestamp)))
                .append("] local");
        }
        task.log(msg.toString(), Project.MSG_VERBOSE);

        synchronized (TIMESTAMP_LOGGING_SDF) {
            msg = new StringBuilder("   [")
                .append(TIMESTAMP_LOGGING_SDF.format(new Date(adjustedRemoteTimestamp)))
                .append("] remote");
        }
        if (remoteTimestamp != adjustedRemoteTimestamp) {
            synchronized (TIMESTAMP_LOGGING_SDF) {
                msg.append(" - (raw: ")
                    .append(TIMESTAMP_LOGGING_SDF.format(new Date(remoteTimestamp)))
                    .append(")");
            }
        }
        task.log(msg.toString(), Project.MSG_VERBOSE);

        if (task.getAction() == FTPTask.SEND_FILES) {
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

        task.log("Doing Site Command: " + theCMD, Project.MSG_VERBOSE);

        if (!ftp.sendSiteCommand(theCMD)) {
            task.log("Failed to issue Site Command: " + theCMD,
                Project.MSG_WARN);
        } else {
            for (String reply : ftp.getReplyStrings()) {
                if (!reply.contains("200")) {
                    task.log(reply, Project.MSG_WARN);
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
            File file = task.getProject().resolveFile(new File(dir, filename).getPath());

            if (task.isNewer() && isUpToDate(ftp, file, resolveFile(filename))) {
                return;
            }

            if (task.isVerbose()) {
                task.log("transferring " + file.getAbsolutePath());
            }

            instream = new BufferedInputStream(Files.newInputStream(file.toPath()));

            createParents(ftp, filename);

            ftp.storeFile(resolveFile(filename), instream);

            if (!FTPReply.isPositiveCompletion(ftp.getReplyCode())) {
                String s = "could not put file: " + ftp.getReplyString();

                if (task.isSkipFailedTransfers()) {
                    task.log(s, Project.MSG_WARN);
                    skipped++;
                } else {
                    throw new BuildException(s);
                }

            } else {
                // see if we should issue a chmod command
                if (task.getChmod() != null) {
                    doSiteCommand(ftp, "chmod " + task.getChmod() + " "
                                  + resolveFile(filename));
                }
                task.log("File " + file.getAbsolutePath() + " copied to " + task.getServer(),
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
        if (task.isVerbose()) {
            task.log("deleting " + filename);
        }

        if (!ftp.deleteFile(resolveFile(filename))) {
            String s = "could not delete file: " + ftp.getReplyString();

            if (task.isSkipFailedTransfers()) {
                task.log(s, Project.MSG_WARN);
                skipped++;
            } else {
                throw new BuildException(s);
            }
        } else {
            task.log("File " + filename + " deleted from " + task.getServer(),
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
        if (task.isVerbose()) {
            task.log("removing " + dirname);
        }

        if (!ftp.removeDirectory(resolveFile(dirname))) {
            String s = "could not remove directory: " + ftp.getReplyString();

            if (task.isSkipFailedTransfers()) {
                task.log(s, Project.MSG_WARN);
                skipped++;
            } else {
                throw new BuildException(s);
            }
        } else {
            task.log("Directory " + dirname + " removed from " + task.getServer(),
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
        OutputStream outstream = null;
        try {
            File file = task.getProject().resolveFile(new File(dir, filename).getPath());

            if (task.isNewer() && isUpToDate(ftp, file, resolveFile(filename))) {
                return;
            }

            if (task.isVerbose()) {
                task.log("transferring " + filename + " to "
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

                if (task.isSkipFailedTransfers()) {
                    task.log(s, Project.MSG_WARN);
                    skipped++;
                } else {
                    throw new BuildException(s);
                }

            } else {
                task.log(
                         "File " + file.getAbsolutePath() + " copied from "
                         + task.getServer(), Project.MSG_VERBOSE);
                transferred++;
                if (task.isPreserveLastModified()) {
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
        if (task.isVerbose()) {
            task.log("listing " + filename);
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
        if (task.isVerbose()) {
            if (dir.startsWith("/") || workingDirectory == null) {
                task.log("Creating directory: " + dir + " in /");
            } else {
                task.log("Creating directory: " + dir + " in " + workingDirectory);
            }
        }
        if (dir.startsWith("/")) {
            ftp.changeWorkingDirectory("/");
        }
        StringTokenizer st = new StringTokenizer(dir, "/");
        while (st.hasMoreTokens()) {
            String subdir = st.nextToken();
            task.log("Checking " + subdir, Project.MSG_DEBUG);
            if (!ftp.changeWorkingDirectory(subdir)) {
                if (!ftp.makeDirectory(subdir)) {
                    // codes 521, 550 and 553 can be produced by FTP Servers
                    //  to indicate that an attempt to create a directory has
                    //  failed because the directory already exists.
                    int rc = ftp.getReplyCode();
                    if (!task.isIgnoreNoncriticalErrors()
                            || rc != CODE_550 && rc != CODE_553 && rc != CODE_521) {
                        throw new BuildException("could not create directory: "
                                                 + ftp.getReplyString());
                    }
                    if (task.isVerbose()) {
                        task.log("Directory already exists");
                    }
                } else {
                    if (task.isVerbose()) {
                        task.log("Directory created OK");
                    }
                    ftp.changeWorkingDirectory(subdir);
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
        if (!task.isIgnoreNoncriticalErrors()
                || rc != CODE_550 && rc != CODE_553 && rc != CODE_521) {
            throw new BuildException("could not create directory: "
                                     + ftp.getReplyString());
        }
    }

    @Override
    public void doFTP() throws BuildException {
        FTPClient ftp = null;

        try {
            task.log("Opening FTP connection to " + task.getServer(), Project.MSG_VERBOSE);

            ftp = new FTPClient();
            if (task.isConfigurationSet()) {
                ftp = FTPConfigurator.configure(ftp, task);
            }

            ftp.setRemoteVerificationEnabled(task.getEnableRemoteVerification());
            ftp.connect(task.getServer(), task.getPort());
            if (!FTPReply.isPositiveCompletion(ftp.getReplyCode())) {
                throw new BuildException("FTP connection failed: %s",
                    ftp.getReplyString());
            }

            task.log("connected", Project.MSG_VERBOSE);
            task.log("logging in to FTP server", Project.MSG_VERBOSE);

            if ((task.getAccount() != null && !ftp.login(task.getUserid(), task.getPassword(), task.getAccount()))
                || (task.getAccount() == null && !ftp.login(task.getUserid(), task.getPassword()))) {
                throw new BuildException("Could not login to FTP server");
            }

            task.log("login succeeded", Project.MSG_VERBOSE);

            if (task.isBinary()) {
                ftp.setFileType(org.apache.commons.net.ftp.FTP.BINARY_FILE_TYPE);
            } else {
                ftp.setFileType(org.apache.commons.net.ftp.FTP.ASCII_FILE_TYPE);
            }
            if (!FTPReply.isPositiveCompletion(ftp.getReplyCode())) {
                throw new BuildException("could not set transfer type: %s",
                    ftp.getReplyString());
            }

            if (task.isPassive()) {
                task.log("entering passive mode", Project.MSG_VERBOSE);
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
            if (task.getInitialSiteCommand() != null) {
                final FTPClient lftp = ftp;
                executeRetryable(new RetryHandler(task.getRetriesAllowed(), task),
                    () -> doSiteCommand(lftp, task.getInitialSiteCommand()),
                    "initial site command: " + task.getInitialSiteCommand());
            }

            // For a unix ftp server you can set the default mask for all files
            // created.

            if (task.getUmask() != null) {
                final FTPClient lftp = ftp;
                executeRetryable(
                    new RetryHandler(task.getRetriesAllowed(), task),
                    () -> doSiteCommand(lftp, "umask " + task.getUmask()),
                    "umask " + task.getUmask());
            }

            // If the action is MK_DIR, then the specified remote
            // directory is the directory to create.

            if (task.getAction() == FTPTask.MK_DIR) {
                final FTPClient lftp = ftp;
                executeRetryable(
                    new RetryHandler(task.getRetriesAllowed(), task),
                    () -> makeRemoteDir(lftp, task.getRemotedir()),
                    task.getRemotedir());
            } else if (task.getAction() == FTPTask.SITE_CMD) {
                final FTPClient lftp = ftp;
                executeRetryable(
                    new RetryHandler(task.getRetriesAllowed(), task),
                    () -> doSiteCommand(lftp, task.getSiteCommand()),
                    "Site Command: " + task.getSiteCommand());
            } else {
                if (task.getRemotedir() != null) {
                    task.log("changing the remote directory", Project.MSG_VERBOSE);
                    ftp.changeWorkingDirectory(task.getRemotedir());
                    if (!FTPReply.isPositiveCompletion(ftp.getReplyCode())) {
                        throw new BuildException(
                            "could not change remote directory: %s",
                            ftp.getReplyString());
                    }
                }
                if (task.isNewer() && task.isTimeDiffAuto()) {
                    // in this case we want to find how much time span there is between local
                    // and remote
                    task.setTimeDiffMillis(getTimeDiff(ftp));
                }
                task.log(FTPTask.ACTION_STRS[task.getAction()] + " " + FTPTask.ACTION_TARGET_STRS[task.getAction()]);
                transferFiles(ftp);
            }
        } catch (IOException ex) {
            throw new BuildException("error during FTP transfer: " + ex, ex);
        } finally {
            if (ftp != null && ftp.isConnected()) {
                try {
                    task.log("disconnecting", Project.MSG_VERBOSE);
                    ftp.logout();
                    ftp.disconnect();
                } catch (IOException ex) {
                    // ignore it
                }
            }
        }
    }
}
