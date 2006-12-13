/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;
import java.util.zip.GZIPOutputStream;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.ArchiveFileSet;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.ArchiveResource;
import org.apache.tools.ant.types.resources.FileResource;
import org.apache.tools.ant.types.selectors.SelectorUtils;
import org.apache.tools.ant.types.resources.TarResource;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.MergingMapper;
import org.apache.tools.ant.util.SourceFileScanner;
import org.apache.tools.bzip2.CBZip2OutputStream;
import org.apache.tools.tar.TarConstants;
import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarOutputStream;

/**
 * Creates a tar archive.
 *
 * @since Ant 1.1
 *
 * @ant.task category="packaging"
 */
public class Tar extends MatchingTask {

    /**
     * @deprecated since 1.5.x.
     *             Tar.WARN is deprecated and is replaced with
     *             Tar.TarLongFileMode.WARN
     */
    public static final String WARN = "warn";
    /**
     * @deprecated since 1.5.x.
     *             Tar.FAIL is deprecated and is replaced with
     *             Tar.TarLongFileMode.FAIL
     */
    public static final String FAIL = "fail";
    /**
     * @deprecated since 1.5.x.
     *             Tar.TRUNCATE is deprecated and is replaced with
     *             Tar.TarLongFileMode.TRUNCATE
     */
    public static final String TRUNCATE = "truncate";
    /**
     * @deprecated since 1.5.x.
     *             Tar.GNU is deprecated and is replaced with
     *             Tar.TarLongFileMode.GNU
     */
    public static final String GNU = "gnu";
    /**
     * @deprecated since 1.5.x.
     *             Tar.OMIT is deprecated and is replaced with
     *             Tar.TarLongFileMode.OMIT
     */
    public static final String OMIT = "omit";

    // CheckStyle:VisibilityModifier OFF - bc
    File tarFile;
    File baseDir;

    private TarLongFileMode longFileMode = new TarLongFileMode();

    // need to keep the package private version for backwards compatibility
    Vector filesets = new Vector();
    // we must keep two lists since other classes may modify the
    // filesets Vector (it is package private) without us noticing
    private Vector resourceCollections = new Vector();

    Vector fileSetFiles = new Vector();

    // CheckStyle:VisibilityModifier ON

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
        TarFileSet fs = new TarFileSet();
        fs.setProject(getProject());
        filesets.addElement(fs);
        return fs;
    }

    /**
     * Add a collection of resources to archive.
     * @param res a resource collection to archive.
     * @since Ant 1.7
     */
    public void add(ResourceCollection res) {
        resourceCollections.add(res);
    }

    /**
     * Set is the name/location of where to create the tar file.
     * @param tarFile the location of the tar file.
     * @deprecated since 1.5.x.
     *             For consistency with other tasks, please use setDestFile().
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
     * @deprecated since 1.5.x.
     *             setLongFile(String) is deprecated and is replaced with
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

            if (filesets.size() == 0 && resourceCollections.size() == 0) {
                throw new BuildException("You must supply either a basedir "
                                         + "attribute or some nested resource"
                                         + " collections.",
                                         getLocation());
            }

            // check if tar is out of date with respect to each
            // fileset
            boolean upToDate = true;
            for (Enumeration e = filesets.elements(); e.hasMoreElements();) {
                upToDate &= check((TarFileSet) e.nextElement());
            }
            for (Enumeration e = resourceCollections.elements();
                 e.hasMoreElements();) {
                upToDate &= check((ResourceCollection) e.nextElement());
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
                    tar((TarFileSet) e.nextElement(), tOut);
                }
                for (Enumeration e = resourceCollections.elements();
                     e.hasMoreElements();) {
                    tar((ResourceCollection) e.nextElement(), tOut);
                }
            } catch (IOException ioe) {
                String msg = "Problem creating TAR: " + ioe.getMessage();
                throw new BuildException(msg, ioe, getLocation());
            } finally {
                FileUtils.close(tOut);
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
        if (file.equals(tarFile)) {
            // If the archive is built for the first time and it is
            // matched by a resource collection, then it hasn't been
            // found in check (it hasn't been there) but will be
            // included now.
            //
            // for some strange reason the old code would simply skip
            // the entry and not fail, do the same now for backwards
            // compatibility reasons.  Without this, the which4j build
            // fails in Gump
            return;
        }
        tarResource(new FileResource(file), tOut, vPath, tarFileSet);
    }

    /**
     * tar a resource
     * @param r the resource to tar
     * @param tOut the output stream
     * @param vPath the path name of the file to tar
     * @param tarFileSet the fileset that the file came from, may be null.
     * @throws IOException on error
     * @since Ant 1.7
     */
    protected void tarResource(Resource r, TarOutputStream tOut, String vPath,
                               TarFileSet tarFileSet)
        throws IOException {

        if (!r.isExists()) {
            return;
        }

        if (tarFileSet != null) {
            String fullpath = tarFileSet.getFullpath(this.getProject());
            if (fullpath.length() > 0) {
                vPath = fullpath;
            } else {
                // don't add "" to the archive
                if (vPath.length() <= 0) {
                    return;
                }

                String prefix = tarFileSet.getPrefix(this.getProject());
                // '/' is appended for compatibility with the zip task.
                if (prefix.length() > 0 && !prefix.endsWith("/")) {
                    prefix = prefix + "/";
                }
                vPath = prefix + vPath;
            }

            if (vPath.startsWith("/")
                && !tarFileSet.getPreserveLeadingSlashes()) {
                int l = vPath.length();
                if (l <= 1) {
                    // we would end up adding "" to the archive
                    return;
                }
                vPath = vPath.substring(1, l);
            }
        }

        if (r.isDirectory() && !vPath.endsWith("/")) {
            vPath += "/";
        }

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
        te.setModTime(r.getLastModified());
        // preserve permissions
        if (r instanceof ArchiveResource) {
            ArchiveResource ar = (ArchiveResource) r;
            te.setMode(ar.getMode());
            if (r instanceof TarResource) {
                TarResource tr = (TarResource) r;
                te.setUserName(tr.getUserName());
                te.setUserId(tr.getUid());
                te.setGroupName(tr.getGroup());
                te.setGroupId(tr.getGid());
            }
        }

        if (!r.isDirectory()) {
            if (r.size() > TarConstants.MAXSIZE) {
                throw new BuildException(
                    "Resource: " + r + " larger than "
                    + TarConstants.MAXSIZE + " bytes.");
            }
            te.setSize(r.getSize());
            // override permissions if set explicitly
            if (tarFileSet != null && tarFileSet.hasFileModeBeenSet()) {
                te.setMode(tarFileSet.getMode());
            }
        } else if (tarFileSet != null && tarFileSet.hasDirModeBeenSet()) {
            // override permissions if set explicitly
            te.setMode(tarFileSet.getDirMode(this.getProject()));
        }

        if (tarFileSet != null) {
            // only override permissions if set explicitly
            if (tarFileSet.hasUserNameBeenSet()) {
                te.setUserName(tarFileSet.getUserName());
            }
            if (tarFileSet.hasGroupBeenSet()) {
                te.setGroupName(tarFileSet.getGroup());
            }
            if (tarFileSet.hasUserIdBeenSet()) {
                te.setUserId(tarFileSet.getUid());
            }
            if (tarFileSet.hasGroupIdBeenSet()) {
                te.setGroupId(tarFileSet.getGid());
            }
        }

        InputStream in = null;
        try {
            tOut.putNextEntry(te);

            if (!r.isDirectory()) {
                in = r.getInputStream();

                byte[] buffer = new byte[8 * 1024];
                int count = 0;
                do {
                    tOut.write(buffer, 0, count);
                    count = in.read(buffer, 0, buffer.length);
                } while (count != -1);
            }

            tOut.closeEntry();
        } finally {
            FileUtils.close(in);
        }
    }

    /**
     * Is the archive up to date in relationship to a list of files.
     * @param files the files to check
     * @return true if the archive is up to date.
     * @deprecated since 1.5.x.
     *             use the two-arg version instead.
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
     * Is the archive up to date in relationship to a list of files.
     * @param r the files to check
     * @return true if the archive is up to date.
     * @since Ant 1.7
     */
    protected boolean archiveIsUpToDate(Resource r) {
        return SelectorUtils.isOutOfDate(new FileResource(tarFile), r,
                                         FileUtils.getFileUtils()
                                         .getFileTimestampGranularity());
    }

    /**
     * Whether this task can deal with non-file resources.
     *
     * <p>This implementation returns true only if this task is
     * &lt;tar&gt;.  Any subclass of this class that also wants to
     * support non-file resources needs to override this method.  We
     * need to do so for backwards compatibility reasons since we
     * can't expect subclasses to support resources.</p>
     * @return true for this task.
     * @since Ant 1.7
     */
    protected boolean supportsNonFileResources() {
        return getClass().equals(Tar.class);
    }

    /**
     * Checks whether the archive is out-of-date with respect to the resources
     * of the given collection.
     *
     * <p>Also checks that either all collections only contain file
     * resources or this class supports non-file collections.</p>
     *
     * <p>And - in case of file-collections - ensures that the archive won't
     * contain itself.</p>
     *
     * @param rc the resource collection to check
     * @return whether the archive is up-to-date
     * @since Ant 1.7
     */
    protected boolean check(ResourceCollection rc) {
        boolean upToDate = true;
        if (isFileFileSet(rc)) {
            FileSet fs = (FileSet) rc;
            upToDate = check(fs.getDir(getProject()), getFileNames(fs));
        } else if (!rc.isFilesystemOnly() && !supportsNonFileResources()) {
            throw new BuildException("only filesystem resources are supported");
        } else if (rc.isFilesystemOnly()) {
            HashSet basedirs = new HashSet();
            HashMap basedirToFilesMap = new HashMap();
            Iterator iter = rc.iterator();
            while (iter.hasNext()) {
                FileResource r = (FileResource) iter.next();
                File base = r.getBaseDir();
                if (base == null) {
                    base = Copy.NULL_FILE_PLACEHOLDER;
                }
                basedirs.add(base);
                Vector files = (Vector) basedirToFilesMap.get(base);
                if (files == null) {
                    files = new Vector();
                    basedirToFilesMap.put(base, new Vector());
                }
                files.add(r.getName());
            }
            iter = basedirs.iterator();
            while (iter.hasNext()) {
                File base = (File) iter.next();
                Vector f = (Vector) basedirToFilesMap.get(base);
                String[] files = (String[]) f.toArray(new String[f.size()]);
                upToDate &=
                    check(base == Copy.NULL_FILE_PLACEHOLDER ? null : base,
                          files);
            }
        } else { // non-file resources
            Iterator iter = rc.iterator();
            while (upToDate && iter.hasNext()) {
                Resource r = (Resource) iter.next();
                upToDate &= archiveIsUpToDate(r);
            }
        }

        return upToDate;
    }

    /**
     * Checks whether the archive is out-of-date with respect to the
     * given files, ensures that the archive won't contain itself.</p>
     *
     * @param basedir base directory for file names
     * @param files array of relative file names
     * @return whether the archive is up-to-date
     * @since Ant 1.7
     */
    protected boolean check(File basedir, String[] files) {
        boolean upToDate = true;
        if (!archiveIsUpToDate(files, basedir)) {
            upToDate = false;
        }

        for (int i = 0; i < files.length; ++i) {
            if (tarFile.equals(new File(basedir, files[i]))) {
                throw new BuildException("A tar file cannot include "
                                         + "itself", getLocation());
            }
        }
        return upToDate;
    }

    /**
     * Adds the resources contained in this collection to the archive.
     *
     * <p>Uses the file based methods for file resources for backwards
     * compatibility.</p>
     *
     * @param rc the collection containing resources to add
     * @param tOut stream writing to the archive.
     * @throws IOException on error.
     * @since Ant 1.7
     */
    protected void tar(ResourceCollection rc, TarOutputStream tOut)
        throws IOException {
        ArchiveFileSet afs = null;
        if (rc instanceof ArchiveFileSet) {
            afs = (ArchiveFileSet) rc;
        }
        if (afs != null && afs.size() > 1
            && afs.getFullpath(this.getProject()).length() > 0) {
            throw new BuildException("fullpath attribute may only "
                                     + "be specified for "
                                     + "filesets that specify a "
                                     + "single file.");
        }
        TarFileSet tfs = asTarFileSet(afs);

        if (isFileFileSet(rc)) {
            FileSet fs = (FileSet) rc;
            String[] files = getFileNames(fs);
            for (int i = 0; i < files.length; i++) {
                File f = new File(fs.getDir(getProject()), files[i]);
                String name = files[i].replace(File.separatorChar, '/');
                tarFile(f, tOut, name, tfs);
            }
        } else if (rc.isFilesystemOnly()) {
            Iterator iter = rc.iterator();
            while (iter.hasNext()) {
                FileResource r = (FileResource) iter.next();
                File f = r.getFile();
                if (f == null) {
                    f = new File(r.getBaseDir(), r.getName());
                }
                tarFile(f, tOut, f.getName(), tfs);
            }
        } else { // non-file resources
            Iterator iter = rc.iterator();
            while (iter.hasNext()) {
                Resource r = (Resource) iter.next();
                tarResource(r, tOut, r.getName(), tfs);
            }
        }
    }

    /**
     * whether the given resource collection is a (subclass of)
     * FileSet that only contains file system resources.
     * @param rc the resource collection to check.
     * @return true if the collection is a fileset.
     * @since Ant 1.7
     */
    protected static final boolean isFileFileSet(ResourceCollection rc) {
        return rc instanceof FileSet && rc.isFilesystemOnly();
    }

    /**
     * Grabs all included files and directors from the FileSet and
     * returns them as an array of (relative) file names.
     * @param fs the fileset to operate on.
     * @return a list of the filenames.
     * @since Ant 1.7
     */
    protected static final String[] getFileNames(FileSet fs) {
        DirectoryScanner ds = fs.getDirectoryScanner(fs.getProject());
        String[] directories = ds.getIncludedDirectories();
        String[] filesPerSe = ds.getIncludedFiles();
        String[] files = new String [directories.length + filesPerSe.length];
        System.arraycopy(directories, 0, files, 0, directories.length);
        System.arraycopy(filesPerSe, 0, files, directories.length,
                         filesPerSe.length);
        return files;
    }

    /**
     * Copies fullpath, prefix and permission attributes from the
     * ArchiveFileSet to a new TarFileSet (or returns it unchanged if
     * it already is a TarFileSet).
     *
     * @param archiveFileSet fileset to copy attributes from, may be null
     * @return a new TarFileSet.
     * @since Ant 1.7
     */
    protected TarFileSet asTarFileSet(ArchiveFileSet archiveFileSet) {
        TarFileSet tfs = null;
        if (archiveFileSet != null && archiveFileSet instanceof TarFileSet) {
            tfs = (TarFileSet) archiveFileSet;
        } else {
            tfs = new TarFileSet();
            tfs.setProject(getProject());
            if (archiveFileSet != null) {
                tfs.setPrefix(archiveFileSet.getPrefix(getProject()));
                tfs.setFullpath(archiveFileSet.getFullpath(getProject()));
                if (archiveFileSet.hasFileModeBeenSet()) {
                    tfs.integerSetFileMode(archiveFileSet
                                           .getFileMode(getProject()));
                }
                if (archiveFileSet.hasDirModeBeenSet()) {
                    tfs.integerSetDirMode(archiveFileSet
                                          .getDirMode(getProject()));
                }

                if (archiveFileSet
                    instanceof org.apache.tools.ant.types.TarFileSet) {
                    org.apache.tools.ant.types.TarFileSet t =
                        (org.apache.tools.ant.types.TarFileSet) archiveFileSet;
                    if (t.hasUserNameBeenSet()) {
                        tfs.setUserName(t.getUserName());
                    }
                    if (t.hasGroupBeenSet()) {
                        tfs.setGroup(t.getGroup());
                    }
                    if (t.hasUserIdBeenSet()) {
                        tfs.setUid(t.getUid());
                    }
                    if (t.hasGroupIdBeenSet()) {
                        tfs.setGid(t.getGid());
                    }
                }
            }
        }
        return tfs;
    }

    /**
     * This is a FileSet with the option to specify permissions
     * and other attributes.
     */
    public static class TarFileSet
        extends org.apache.tools.ant.types.TarFileSet {
        private String[] files = null;

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
                files = getFileNames(this);
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
            setFileMode(octalString);
        }

        /**
         * @return the current mode.
         */
        public int getMode() {
            return getFileMode(this.getProject());
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
            final String v = getValue();
            if (GZIP.equals(v)) {
                return new GZIPOutputStream(ostream);
            } else {
                if (BZIP2.equals(v)) {
                    ostream.write('B');
                    ostream.write('Z');
                    return new CBZip2OutputStream(ostream);
                }
            }
            return ostream;
        }
    }
}
