/*
 * Copyright  2000-2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.tools.ant.taskdefs;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Vector;
import java.util.zip.GZIPOutputStream;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.util.MergingMapper;
import org.apache.tools.ant.util.SourceFileScanner;
import org.apache.tools.bzip2.CBZip2OutputStream;
import org.apache.tools.tar.TarConstants;
import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarOutputStream;
import org.apache.tools.zip.UnixStat;

/**
 * Creates a tar archive.
 *
 * @since Ant 1.1
 *
 * @ant.task category="packaging"
 */
public class Tar extends MatchingTask {

    /**
     * @deprecated Tar.WARN is deprecated and is replaced with
     *             Tar.TarLongFileMode.WARN
     */
    public static final String WARN = "warn";
    /**
     * @deprecated Tar.FAIL is deprecated and is replaced with
     *             Tar.TarLongFileMode.FAIL
     */
    public static final String FAIL = "fail";
    /**
     * @deprecated Tar.TRUNCATE is deprecated and is replaced with
     *             Tar.TarLongFileMode.TRUNCATE
     */
    public static final String TRUNCATE = "truncate";
    /**
     * @deprecated Tar.GNU is deprecated and is replaced with
     *             Tar.TarLongFileMode.GNU
     */
    public static final String GNU = "gnu";
    /**
     * @deprecated Tar.OMIT is deprecated and is replaced with
     *             Tar.TarLongFileMode.OMIT
     */
    public static final String OMIT = "omit";

    File tarFile;
    File baseDir;

    private TarLongFileMode longFileMode = new TarLongFileMode();

    Vector filesets = new Vector();
    Vector fileSetFiles = new Vector();

    /**
     * Indicates whether the user has been warned about long files already.
     */
    private boolean longWarningGiven = false;

    private TarCompressionMethod compression = new TarCompressionMethod();

    /**
     * Add a new fileset with the option to specify permissions
     * @return the tar fileset to be used as the nested element.
     */
    public TarFileSet createTarFileSet() {
        TarFileSet fileset = new TarFileSet();
        filesets.addElement(fileset);
        return fileset;
    }


    /**
     * Set is the name/location of where to create the tar file.
     * @param tarFile the location of the tar file.
     * @deprecated for consistency with other tasks, please use setDestFile()
     */
    public void setTarfile(File tarFile) {
        this.tarFile = tarFile;
    }

    /**
     * Set is the name/location of where to create the tar file.
     * @since Ant 1.5
     * @param destFile The output of the tar
     */
    public void setDestFile(File destFile) {
        this.tarFile = destFile;
    }

    /**
     * This is the base directory to look in for things to tar.
     * @param baseDir the base directory.
     */
    public void setBasedir(File baseDir) {
        this.baseDir = baseDir;
    }

    /**
     * Set how to handle long files, those with a path&gt;100 chars.
     * Optional, default=warn.
     * <p>
     * Allowable values are
     * <ul>
     * <li>  truncate - paths are truncated to the maximum length
     * <li>  fail - paths greater than the maximum cause a build exception
     * <li>  warn - paths greater than the maximum cause a warning and GNU is used
     * <li>  gnu - GNU extensions are used for any paths greater than the maximum.
     * <li>  omit - paths greater than the maximum are omitted from the archive
     * </ul>
     * @param mode the mode string to handle long files.
     * @deprecated setLongFile(String) is deprecated and is replaced with
     *             setLongFile(Tar.TarLongFileMode) to make Ant's Introspection
     *             mechanism do the work and also to encapsulate operations on
     *             the mode in its own class.
     */
    public void setLongfile(String mode) {
        log("DEPRECATED - The setLongfile(String) method has been deprecated."
            + " Use setLongfile(Tar.TarLongFileMode) instead.");
        this.longFileMode = new TarLongFileMode();
        longFileMode.setValue(mode);
    }

    /**
     * Set how to handle long files, those with a path&gt;100 chars.
     * Optional, default=warn.
     * <p>
     * Allowable values are
     * <ul>
     * <li>  truncate - paths are truncated to the maximum length
     * <li>  fail - paths greater than the maximum cause a build exception
     * <li>  warn - paths greater than the maximum cause a warning and GNU is used
     * <li>  gnu - GNU extensions are used for any paths greater than the maximum.
     * <li>  omit - paths greater than the maximum are omitted from the archive
     * </ul>
     * @param mode the mode to handle long file names.
     */
    public void setLongfile(TarLongFileMode mode) {
        this.longFileMode = mode;
    }

    /**
     * Set compression method.
     * Allowable values are
     * <ul>
     * <li>  none - no compression
     * <li>  gzip - Gzip compression
     * <li>  bzip2 - Bzip2 compression
     * </ul>
     * @param mode the compression method.
     */
    public void setCompression(TarCompressionMethod mode) {
        this.compression = mode;
    }

    /**
     * do the business
     * @throws BuildException on error
     */
    public void execute() throws BuildException {
        if (tarFile == null) {
            throw new BuildException("tarfile attribute must be set!",
                                     getLocation());
        }

        if (tarFile.exists() && tarFile.isDirectory()) {
            throw new BuildException("tarfile is a directory!",
                                     getLocation());
        }

        if (tarFile.exists() && !tarFile.canWrite()) {
            throw new BuildException("Can not write to the specified tarfile!",
                                     getLocation());
        }

        Vector savedFileSets = (Vector) filesets.clone();
        try {
            if (baseDir != null) {
                if (!baseDir.exists()) {
                    throw new BuildException("basedir does not exist!",
                                             getLocation());
                }

                // add the main fileset to the list of filesets to process.
                TarFileSet mainFileSet = new TarFileSet(fileset);
                mainFileSet.setDir(baseDir);
                filesets.addElement(mainFileSet);
            }

            if (filesets.size() == 0) {
                throw new BuildException("You must supply either a basedir "
                                         + "attribute or some nested filesets.",
                                         getLocation());
            }

            // check if tar is out of date with respect to each
            // fileset
            boolean upToDate = true;
            for (Enumeration e = filesets.elements(); e.hasMoreElements();) {
                TarFileSet fs = (TarFileSet) e.nextElement();
                String[] files = fs.getFiles(getProject());

                if (!archiveIsUpToDate(files, fs.getDir(getProject()))) {
                    upToDate = false;
                }

                for (int i = 0; i < files.length; ++i) {
                    if (tarFile.equals(new File(fs.getDir(getProject()),
                                                files[i]))) {
                        throw new BuildException("A tar file cannot include "
                                                 + "itself", getLocation());
                    }
                }
            }

            if (upToDate) {
                log("Nothing to do: " + tarFile.getAbsolutePath()
                    + " is up to date.", Project.MSG_INFO);
                return;
            }

            log("Building tar: " + tarFile.getAbsolutePath(), Project.MSG_INFO);

            TarOutputStream tOut = null;
            try {
                tOut = new TarOutputStream(
                    compression.compress(
                        new BufferedOutputStream(
                            new FileOutputStream(tarFile))));
                tOut.setDebug(true);
                if (longFileMode.isTruncateMode()) {
                    tOut.setLongFileMode(TarOutputStream.LONGFILE_TRUNCATE);
                } else if (longFileMode.isFailMode()
                            || longFileMode.isOmitMode()) {
                    tOut.setLongFileMode(TarOutputStream.LONGFILE_ERROR);
                } else {
                    // warn or GNU
                    tOut.setLongFileMode(TarOutputStream.LONGFILE_GNU);
                }

                longWarningGiven = false;
                for (Enumeration e = filesets.elements();
                     e.hasMoreElements();) {
                    TarFileSet fs = (TarFileSet) e.nextElement();
                    String[] files = fs.getFiles(getProject());
                    if (files.length > 1 && fs.getFullpath().length() > 0) {
                        throw new BuildException("fullpath attribute may only "
                                                 + "be specified for "
                                                 + "filesets that specify a "
                                                 + "single file.");
                    }
                    for (int i = 0; i < files.length; i++) {
                        File f = new File(fs.getDir(getProject()), files[i]);
                        String name = files[i].replace(File.separatorChar, '/');
                        tarFile(f, tOut, name, fs);
                    }
                }
            } catch (IOException ioe) {
                String msg = "Problem creating TAR: " + ioe.getMessage();
                throw new BuildException(msg, ioe, getLocation());
            } finally {
                if (tOut != null) {
                    try {
                        // close up
                        tOut.close();
                    } catch (IOException e) {
                        // ignore
                    }
                }
            }
        } finally {
            filesets = savedFileSets;
        }
    }

    /**
     * tar a file
     * @param file the file to tar
     * @param tOut the output stream
     * @param vPath the path name of the file to tar
     * @param tarFileSet the fileset that the file came from.
     * @throws IOException on error
     */
    protected void tarFile(File file, TarOutputStream tOut, String vPath,
                           TarFileSet tarFileSet)
        throws IOException {
        FileInputStream fIn = null;

        String fullpath = tarFileSet.getFullpath();
        if (fullpath.length() > 0) {
            vPath = fullpath;
        } else {
            // don't add "" to the archive
            if (vPath.length() <= 0) {
                return;
            }

            if (file.isDirectory() && !vPath.endsWith("/")) {
                vPath += "/";
            }

            String prefix = tarFileSet.getPrefix();
            // '/' is appended for compatibility with the zip task.
            if (prefix.length() > 0 && !prefix.endsWith("/")) {
                prefix = prefix + "/";
            }
            vPath = prefix + vPath;
        }

        if (vPath.startsWith("/") && !tarFileSet.getPreserveLeadingSlashes()) {
            int l = vPath.length();
            if (l <= 1) {
                // we would end up adding "" to the archive
                return;
            }
            vPath = vPath.substring(1, l);
        }

        try {
            if (vPath.length() >= TarConstants.NAMELEN) {
                if (longFileMode.isOmitMode()) {
                    log("Omitting: " + vPath, Project.MSG_INFO);
                    return;
                } else if (longFileMode.isWarnMode()) {
                    log("Entry: " + vPath + " longer than "
                        + TarConstants.NAMELEN + " characters.",
                        Project.MSG_WARN);
                    if (!longWarningGiven) {
                        log("Resulting tar file can only be processed "
                            + "successfully by GNU compatible tar commands",
                            Project.MSG_WARN);
                        longWarningGiven = true;
                    }
                } else if (longFileMode.isFailMode()) {
                    throw new BuildException("Entry: " + vPath
                        + " longer than " + TarConstants.NAMELEN
                        + "characters.", getLocation());
                }
            }

            TarEntry te = new TarEntry(vPath);
            te.setModTime(file.lastModified());
            if (!file.isDirectory()) {
                te.setSize(file.length());
                te.setMode(tarFileSet.getMode());
            } else {
                te.setMode(tarFileSet.getDirMode());
            }
            te.setUserName(tarFileSet.getUserName());
            te.setGroupName(tarFileSet.getGroup());
            te.setUserId(tarFileSet.getUid());
            te.setGroupId(tarFileSet.getGid());

            tOut.putNextEntry(te);

            if (!file.isDirectory()) {
                fIn = new FileInputStream(file);

                byte[] buffer = new byte[8 * 1024];
                int count = 0;
                do {
                    tOut.write(buffer, 0, count);
                    count = fIn.read(buffer, 0, buffer.length);
                } while (count != -1);
            }

            tOut.closeEntry();
        } finally {
            if (fIn != null) {
                fIn.close();
            }
        }
    }

    /**
     * Is the archive up to date in relationship to a list of files.
     * @param files the files to check
     * @return true if the archive is up to date.
     * @deprecated use the two-arg version instead.
     */
    protected boolean archiveIsUpToDate(String[] files) {
        return archiveIsUpToDate(files, baseDir);
    }

    /**
     * Is the archive up to date in relationship to a list of files.
     * @param files the files to check
     * @param dir   the base directory for the files.
     * @return true if the archive is up to date.
     * @since Ant 1.5.2
     */
    protected boolean archiveIsUpToDate(String[] files, File dir) {
        SourceFileScanner sfs = new SourceFileScanner(this);
        MergingMapper mm = new MergingMapper();
        mm.setTo(tarFile.getAbsolutePath());
        return sfs.restrict(files, dir, null, mm).length == 0;
    }

    /**
     * This is a FileSet with the option to specify permissions
     * and other attributes.
     */
    public static class TarFileSet extends FileSet {
        private String[] files = null;

        private int fileMode = UnixStat.FILE_FLAG | UnixStat.DEFAULT_FILE_PERM;
        private int dirMode = UnixStat.DIR_FLAG | UnixStat.DEFAULT_DIR_PERM;

        private String userName = "";
        private String groupName = "";
        private int    uid;
        private int    gid;
        private String prefix = "";
        private String fullpath = "";
        private boolean preserveLeadingSlashes = false;

        /**
         * Creates a new <code>TarFileSet</code> instance.
         * Using a fileset as a constructor argument.
         *
         * @param fileset a <code>FileSet</code> value
         */
        public TarFileSet(FileSet fileset) {
            super(fileset);
        }

        /**
         * Creates a new <code>TarFileSet</code> instance.
         *
         */
        public TarFileSet() {
            super();
        }

        /**
         *  Get a list of files and directories specified in the fileset.
         * @param p the current project.
         * @return a list of file and directory names, relative to
         *    the baseDir for the project.
         */
        public String[] getFiles(Project p) {
            if (files == null) {
                DirectoryScanner ds = getDirectoryScanner(p);
                String[] directories = ds.getIncludedDirectories();
                String[] filesPerSe = ds.getIncludedFiles();
                files = new String [directories.length + filesPerSe.length];
                System.arraycopy(directories, 0, files, 0, directories.length);
                System.arraycopy(filesPerSe, 0, files, directories.length,
                        filesPerSe.length);
            }

            return files;
        }

        /**
         * A 3 digit octal string, specify the user, group and
         * other modes in the standard Unix fashion;
         * optional, default=0644
         * @param octalString a 3 digit octal string.
         */
        public void setMode(String octalString) {
            this.fileMode =
                UnixStat.FILE_FLAG | Integer.parseInt(octalString, 8);
        }

        /**
         * @return the current mode.
         */
        public int getMode() {
            return fileMode;
        }

        /**
         * A 3 digit octal string, specify the user, group and
         * other modes in the standard Unix fashion;
         * optional, default=0755
         *
         * @param octalString a 3 digit octal string.
         * @since Ant 1.6
         */
        public void setDirMode(String octalString) {
            this.dirMode =
                UnixStat.DIR_FLAG | Integer.parseInt(octalString, 8);
        }

        /**
         * @return the current directory mode
         * @since Ant 1.6
         */
        public int getDirMode() {
            return dirMode;
        }

        /**
         * The username for the tar entry
         * This is not the same as the UID.
         * @param userName the user name for the tar entry.
         */
        public void setUserName(String userName) {
            this.userName = userName;
        }

        /**
         * @return the user name for the tar entry
         */
        public String getUserName() {
            return userName;
        }

        /**
         * The uid for the tar entry
         * This is not the same as the User name.
         * @param uid the id of the user for the tar entry.
         */
        public void setUid(int uid) {
            this.uid = uid;
        }

        /**
         * @return the uid for the tar entry
         */
        public int getUid() {
            return uid;
        }

        /**
         * The groupname for the tar entry; optional, default=""
         * This is not the same as the GID.
         * @param groupName the group name string.
         */
        public void setGroup(String groupName) {
            this.groupName = groupName;
        }

        /**
         * @return the group name string.
         */
        public String getGroup() {
            return groupName;
        }

        /**
         * The GID for the tar entry; optional, default="0"
         * This is not the same as the group name.
         * @param gid the group id.
         */
        public void setGid(int gid) {
            this.gid = gid;
        }

        /**
         * @return the group identifier.
         */
        public int getGid() {
            return gid;
        }

        /**
         * If the prefix attribute is set, all files in the fileset
         * are prefixed with that path in the archive.
         * optional.
         * @param prefix the path prefix.
         */
        public void setPrefix(String prefix) {
            this.prefix = prefix;
        }

        /**
         * @return the path prefix for the files in the fileset.
         */
        public String getPrefix() {
            return prefix;
        }

        /**
         * If the fullpath attribute is set, the file in the fileset
         * is written with that path in the archive. The prefix attribute,
         * if specified, is ignored. It is an error to have more than one file specified in
         * such a fileset.
         * @param fullpath the path to use for the file in a fileset.
         */
        public void setFullpath(String fullpath) {
            this.fullpath = fullpath;
        }

        /**
         * @return the path to use for a single file fileset.
         */
        public String getFullpath() {
            return fullpath;
        }

        /**
         * Flag to indicates whether leading `/'s should
         * be preserved in the file names.
         * Optional, default is <code>false</code>.
         * @param b the leading slashes flag.
         */
        public void setPreserveLeadingSlashes(boolean b) {
            this.preserveLeadingSlashes = b;
        }

        /**
         * @return the leading slashes flag.
         */
        public boolean getPreserveLeadingSlashes() {
            return preserveLeadingSlashes;
        }
    }

    /**
     * Set of options for long file handling in the task.
     *
     */
    public static class TarLongFileMode extends EnumeratedAttribute {

        /** permissible values for longfile attribute */
        public static final String
            WARN = "warn",
            FAIL = "fail",
            TRUNCATE = "truncate",
            GNU = "gnu",
            OMIT = "omit";

        private final String[] validModes = {WARN, FAIL, TRUNCATE, GNU, OMIT};

        /** Constructor, defaults to "warn" */
        public TarLongFileMode() {
            super();
            setValue(WARN);
        }

        /**
         * @return the possible values for this enumerated type.
         */
        public String[] getValues() {
            return validModes;
        }

        /**
         * @return true if value is "truncate".
         */
        public boolean isTruncateMode() {
            return TRUNCATE.equalsIgnoreCase(getValue());
        }

        /**
         * @return true if value is "warn".
         */
        public boolean isWarnMode() {
            return WARN.equalsIgnoreCase(getValue());
        }

        /**
         * @return true if value is "gnu".
         */
        public boolean isGnuMode() {
            return GNU.equalsIgnoreCase(getValue());
        }

        /**
         * @return true if value is "fail".
         */
        public boolean isFailMode() {
            return FAIL.equalsIgnoreCase(getValue());
        }

        /**
         * @return true if value is "omit".
         */
        public boolean isOmitMode() {
            return OMIT.equalsIgnoreCase(getValue());
        }
    }

    /**
     * Valid Modes for Compression attribute to Tar Task
     *
     */
    public static final class TarCompressionMethod extends EnumeratedAttribute {

        // permissible values for compression attribute
        /**
         *    No compression
         */
        private static final String NONE = "none";
        /**
         *    GZIP compression
         */
        private static final String GZIP = "gzip";
        /**
         *    BZIP2 compression
         */
        private static final String BZIP2 = "bzip2";


        /**
         * Default constructor
         */
        public TarCompressionMethod() {
            super();
            setValue(NONE);
        }

        /**
         *  Get valid enumeration values.
         *  @return valid enumeration values
         */
        public String[] getValues() {
            return new String[] {NONE, GZIP, BZIP2 };
        }

        /**
         *  This method wraps the output stream with the
         *     corresponding compression method
         *
         *  @param ostream output stream
         *  @return output stream with on-the-fly compression
         *  @exception IOException thrown if file is not writable
         */
        private OutputStream compress(final OutputStream ostream)
            throws IOException {
            final String value = getValue();
            if (GZIP.equals(value)) {
                return new GZIPOutputStream(ostream);
            } else {
                if (BZIP2.equals(value)) {
                    ostream.write('B');
                    ostream.write('Z');
                    return new CBZip2OutputStream(ostream);
                }
            }
            return ostream;
        }
    }
}
