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

package org.apache.tools.ant.taskdefs;

import java.io.File;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.BufferedOutputStream;
import java.util.Vector;
import java.util.Enumeration;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.util.SourceFileScanner;
import org.apache.tools.ant.util.MergingMapper;
import org.apache.tools.tar.TarOutputStream;
import org.apache.tools.tar.TarConstants;
import org.apache.tools.tar.TarEntry;
import java.util.zip.GZIPOutputStream;
import org.apache.tools.bzip2.CBZip2OutputStream;



/**
 * Creates a tar archive.
 *
 * @author Stefano Mazzocchi
 *         <a href="mailto:stefano@apache.org">stefano@apache.org</a>
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @author Magesh Umasankar
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
     */
    public TarFileSet createTarFileSet() {
        TarFileSet fileset = new TarFileSet();
        filesets.addElement(fileset);
        return fileset;
    }


    /**
     * Set is the name/location of where to create the tar file.
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
     * <li>  fail - paths greater than the maximim cause a build exception
     * <li>  warn - paths greater than the maximum cause a warning and GNU is used
     * <li>  gnu - GNU extensions are used for any paths greater than the maximum.
     * <li>  omit - paths greater than the maximum are omitted from the archive
     * </ul>
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
     * <li>  fail - paths greater than the maximim cause a build exception
     * <li>  warn - paths greater than the maximum cause a warning and GNU is used
     * <li>  gnu - GNU extensions are used for any paths greater than the maximum.
     * <li>  omit - paths greater than the maximum are omitted from the archive
     * </ul>
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
     */
    public void setCompression(TarCompressionMethod mode) {
        this.compression = mode;
    }
    
    /**
     * do the business
     */
    public void execute() throws BuildException {
        if (tarFile == null) {
            throw new BuildException("tarfile attribute must be set!",
                                     location);
        }

        if (tarFile.exists() && tarFile.isDirectory()) {
            throw new BuildException("tarfile is a directory!",
                                     location);
        }

        if (tarFile.exists() && !tarFile.canWrite()) {
            throw new BuildException("Can not write to the specified tarfile!",
                                     location);
        }

        Vector savedFileSets = (Vector) filesets.clone();
        try {
            if (baseDir != null) {
                if (!baseDir.exists()) {
                    throw new BuildException("basedir does not exist!",
                                             location);
                }

                // add the main fileset to the list of filesets to process.
                TarFileSet mainFileSet = new TarFileSet(fileset);
                mainFileSet.setDir(baseDir);
                filesets.addElement(mainFileSet);
            }

            if (filesets.size() == 0) {
                throw new BuildException("You must supply either a basedir "
                                         + "attribute or some nested filesets.",
                                         location);
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
                                                 + "itself", location);
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
                } else if (longFileMode.isFailMode() ||
                         longFileMode.isOmitMode()) {
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
                throw new BuildException(msg, ioe, location);
            } finally {
                if (tOut != null) {
                    try {
                        // close up
                        tOut.close();
                    } catch (IOException e) {}
                }
            }
        } finally {
            filesets = savedFileSets;
        }
    }
    
    /**
     * tar a file
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
                    log("Entry: " + vPath + " longer than " +
                        TarConstants.NAMELEN + " characters.",
                        Project.MSG_WARN);
                    if (!longWarningGiven) {
                        log("Resulting tar file can only be processed "
                            + "successfully by GNU compatible tar commands",
                            Project.MSG_WARN);
                        longWarningGiven = true;
                    }
                } else if (longFileMode.isFailMode()) {
                    throw new BuildException(
                        "Entry: " + vPath + " longer than " +
                        TarConstants.NAMELEN + "characters.", location);
                }
            }

            TarEntry te = new TarEntry(vPath);
            te.setModTime(file.lastModified());
            if (!file.isDirectory()) {
                te.setSize(file.length());
                te.setMode(tarFileSet.getMode());
            }
            te.setUserName(tarFileSet.getUserName());
            te.setGroupName(tarFileSet.getGroup());

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
     * @deprecated use the two-arg version instead.
     */
    protected boolean archiveIsUpToDate(String[] files) {
        return archiveIsUpToDate(files, baseDir);
    }

    /**
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
     */
    public static class TarFileSet extends FileSet {
        private String[] files = null;

        private int mode = 0100644;

        private String userName = "";
        private String groupName = "";
        private String prefix = "";
        private String fullpath = "";
        private boolean preserveLeadingSlashes = false;

        public TarFileSet(FileSet fileset) {
            super(fileset);
        }

        public TarFileSet() {
            super();
        }

        /**
         *  Get a list of files and directories specified in the fileset.
         *  @return a list of file and directory names, relative to
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
         */
        public void setMode(String octalString) {
            this.mode = 0100000 | Integer.parseInt(octalString, 8);
        }

        public int getMode() {
            return mode;
        }

        /**
         * The username for the tar entry 
         * This is not the same as the UID, which is
         * not currently set by the task.
         */
        public void setUserName(String userName) {
            this.userName = userName;
        }

        public String getUserName() {
            return userName;
        }

        /**
         * The groupname for the tar entry; optional, default=""
         * This is not the same as the GID, which is
         * not currently set by the task.
         */
        public void setGroup(String groupName) {
            this.groupName = groupName;
        }

        public String getGroup() {
            return groupName;
        }

        /**
         * If the prefix attribute is set, all files in the fileset
         * are prefixed with that path in the archive.
         * optional.
         */
        public void setPrefix(String prefix) {
            this.prefix = prefix;
        }

        public String getPrefix() {
            return prefix;
        }

        /**
         * If the fullpath attribute is set, the file in the fileset
         * is written with that path in the archive. The prefix attribute,
         * if specified, is ignored. It is an error to have more than one file specified in
         * such a fileset.
         */
        public void setFullpath(String fullpath) {
            this.fullpath = fullpath;
        }

        public String getFullpath() {
            return fullpath;
        }

        /**
         * Flag to indicates whether leading `/'s should
         * be preserved in the file names.
         * Optional, default is <code>false</code>.
         */
        public void setPreserveLeadingSlashes(boolean b) {
            this.preserveLeadingSlashes = b;
        }

        public boolean getPreserveLeadingSlashes() {
            return preserveLeadingSlashes;
        }
    }

    /**
     * Set of options for long file handling in the task. 
     *
     * @author Magesh Umasankar
     */
    public static class TarLongFileMode extends EnumeratedAttribute {

        // permissable values for longfile attribute
        public static final String WARN = "warn";
        public static final String FAIL = "fail";
        public static final String TRUNCATE = "truncate";
        public static final String GNU = "gnu";
        public static final String OMIT = "omit";

        private final String[] validModes = {WARN, FAIL, TRUNCATE, GNU, OMIT};

        public TarLongFileMode() {
            super();
            setValue(WARN);
        }

        public String[] getValues() {
            return validModes;
        }

        public boolean isTruncateMode() {
            return TRUNCATE.equalsIgnoreCase(getValue());
        }

        public boolean isWarnMode() {
            return WARN.equalsIgnoreCase(getValue());
        }

        public boolean isGnuMode() {
            return GNU.equalsIgnoreCase(getValue());
        }

        public boolean isFailMode() {
            return FAIL.equalsIgnoreCase(getValue());
        }

        public boolean isOmitMode() {
            return OMIT.equalsIgnoreCase(getValue());
        }
    }

    /**
     * Valid Modes for Compression attribute to Tar Task
     *
     */
    public static final class TarCompressionMethod extends EnumeratedAttribute {

        // permissable values for compression attribute
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
            return new String[] { NONE, GZIP, BZIP2 };
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
