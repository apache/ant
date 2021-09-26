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

package org.apache.tools.ant.taskdefs;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.apache.tools.ant.types.resources.FileProvider;
import org.apache.tools.ant.types.resources.FileResource;
import org.apache.tools.ant.types.resources.TarResource;
import org.apache.tools.ant.types.selectors.SelectorUtils;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.MergingMapper;
import org.apache.tools.ant.util.ResourceUtils;
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
    private static final int BUFFER_SIZE = 8 * 1024;

    /**
     * @deprecated since 1.5.x.
     *             Tar.WARN is deprecated and is replaced with
     *             Tar.TarLongFileMode.WARN
     */
    @Deprecated
    public static final String WARN = "warn";
    /**
     * @deprecated since 1.5.x.
     *             Tar.FAIL is deprecated and is replaced with
     *             Tar.TarLongFileMode.FAIL
     */
    @Deprecated
    public static final String FAIL = "fail";
    /**
     * @deprecated since 1.5.x.
     *             Tar.TRUNCATE is deprecated and is replaced with
     *             Tar.TarLongFileMode.TRUNCATE
     */
    @Deprecated
    public static final String TRUNCATE = "truncate";
    /**
     * @deprecated since 1.5.x.
     *             Tar.GNU is deprecated and is replaced with
     *             Tar.TarLongFileMode.GNU
     */
    @Deprecated
    public static final String GNU = "gnu";
    /**
     * @deprecated since 1.5.x.
     *             Tar.OMIT is deprecated and is replaced with
     *             Tar.TarLongFileMode.OMIT
     */
    @Deprecated
    public static final String OMIT = "omit";

    // CheckStyle:VisibilityModifier OFF - bc
    File tarFile;
    File baseDir;

    private TarLongFileMode longFileMode = new TarLongFileMode();

    // need to keep the package private version for backwards compatibility
    Vector<TarFileSet> filesets = new Vector<>();
    // we must keep two lists since other classes may modify the
    // filesets Vector (it is package private) without us noticing
    private final List<ResourceCollection> resourceCollections = new Vector<>();

    // CheckStyle:VisibilityModifier ON

    /**
     * Indicates whether the user has been warned about long files already.
     */
    private boolean longWarningGiven = false;

    private TarCompressionMethod compression = new TarCompressionMethod();

    /**
     * Encoding to use for filenames, defaults to the platform's
     * default encoding.
     */
    private String encoding;

    /**
     * Add a new fileset with the option to specify permissions
     * @return the tar fileset to be used as the nested element.
     */
    public TarFileSet createTarFileSet() {
        final TarFileSet fs = new TarFileSet();
        fs.setProject(getProject());
        filesets.addElement(fs);
        return fs;
    }

    /**
     * Add a collection of resources to archive.
     * @param res a resource collection to archive.
     * @since Ant 1.7
     */
    public void add(final ResourceCollection res) {
        resourceCollections.add(res);
    }

    /**
     * Set is the name/location of where to create the tar file.
     * @param tarFile the location of the tar file.
     * @deprecated since 1.5.x.
     *             For consistency with other tasks, please use setDestFile().
     */
    @Deprecated
    public void setTarfile(final File tarFile) {
        this.tarFile = tarFile;
    }

    /**
     * Set is the name/location of where to create the tar file.
     * @since Ant 1.5
     * @param destFile The output of the tar
     */
    public void setDestFile(final File destFile) {
        this.tarFile = destFile;
    }

    /**
     * This is the base directory to look in for things to tar.
     * @param baseDir the base directory.
     */
    public void setBasedir(final File baseDir) {
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
    @Deprecated
    public void setLongfile(final String mode) {
        log("DEPRECATED - The setLongfile(String) method has been deprecated. Use setLongfile(Tar.TarLongFileMode) instead.");
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
     * <li>  gnu - extensions used by older versions of GNU tar are used for any paths greater than the maximum.
     * <li>  posix - use POSIX PAX extension headers for any paths greater than the maximum.  Supported by all modern tar implementations.
     * <li>  omit - paths greater than the maximum are omitted from the archive
     * </ul>
     * @param mode the mode to handle long file names.
     */
    public void setLongfile(final TarLongFileMode mode) {
        this.longFileMode = mode;
    }

    /**
     * Set compression method.
     * Allowable values are
     * <ul>
     * <li>  none - no compression
     * <li>  gzip - Gzip compression
     * <li>  bzip2 - Bzip2 compression
     * <li>xz - XZ compression, requires XZ for Java
     * </ul>
     * @param mode the compression method.
     */
    public void setCompression(final TarCompressionMethod mode) {
        this.compression = mode;
    }

    /**
     * Encoding to use for filenames, defaults to the platform's
     * default encoding.
     *
     * <p>For a list of possible values see <a
     * href="https://docs.oracle.com/javase/8/docs/technotes/guides/intl/encoding.doc.html">https://docs.oracle.com/javase/8/docs/technotes/guides/intl/encoding.doc.html</a>.</p>
     * @param encoding the encoding name
     *
     * @since Ant 1.9.5
     */
    public void setEncoding(final String encoding) {
        this.encoding = encoding;
    }

    /**
     * do the business
     * @throws BuildException on error
     */
    @Override
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

        final Vector<TarFileSet> savedFileSets = new Vector<>(filesets);
        try {
            if (baseDir != null) {
                if (!baseDir.exists()) {
                    throw new BuildException("basedir does not exist!",
                                             getLocation());
                }

                // add the main fileset to the list of filesets to process.
                final TarFileSet mainFileSet = new TarFileSet(fileset);
                mainFileSet.setDir(baseDir);
                filesets.addElement(mainFileSet);
            }

            if (filesets.isEmpty() && resourceCollections.isEmpty()) {
                throw new BuildException(
                    "You must supply either a basedir attribute or some nested resource collections.",
                    getLocation());
            }

            // check if tar is out of date with respect to each
            // fileset
            boolean upToDate = true;
            for (final TarFileSet tfs : filesets) {
                upToDate &= check(tfs);
            }
            for (final ResourceCollection rcol : resourceCollections) {
                upToDate &= check(rcol);
            }

            if (upToDate) {
                log("Nothing to do: " + tarFile.getAbsolutePath()
                    + " is up to date.", Project.MSG_INFO);
                return;
            }

            final File parent = tarFile.getParentFile();
            if (parent != null && !parent.isDirectory()
                && !(parent.mkdirs() || parent.isDirectory())) {
                throw new BuildException(
                    "Failed to create missing parent directory for %s",
                    tarFile);
            }

            log("Building tar: " + tarFile.getAbsolutePath(), Project.MSG_INFO);

            try (TarOutputStream tOut = new TarOutputStream(
                compression.compress(new BufferedOutputStream(
                    Files.newOutputStream(tarFile.toPath()))),
                encoding)) {
                tOut.setDebug(true);
                if (longFileMode.isTruncateMode()) {
                    tOut.setLongFileMode(TarOutputStream.LONGFILE_TRUNCATE);
                } else if (longFileMode.isFailMode()
                            || longFileMode.isOmitMode()) {
                    tOut.setLongFileMode(TarOutputStream.LONGFILE_ERROR);
                } else if (longFileMode.isPosixMode()) {
                    tOut.setLongFileMode(TarOutputStream.LONGFILE_POSIX);
                } else {
                    // warn or GNU
                    tOut.setLongFileMode(TarOutputStream.LONGFILE_GNU);
                }

                longWarningGiven = false;
                for (final TarFileSet tfs : filesets) {
                    tar(tfs, tOut);
                }
                for (final ResourceCollection rcol : resourceCollections) {
                    tar(rcol, tOut);
                }
            } catch (final IOException ioe) {
                final String msg = "Problem creating TAR: " + ioe.getMessage();
                throw new BuildException(msg, ioe, getLocation());
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
    protected void tarFile(final File file, final TarOutputStream tOut, final String vPath,
                           final TarFileSet tarFileSet)
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
    protected void tarResource(final Resource r, final TarOutputStream tOut, String vPath,
                               final TarFileSet tarFileSet)
        throws IOException {

        if (!r.isExists()) {
            return;
        }

        boolean preserveLeadingSlashes = false;

        if (tarFileSet != null) {
            final String fullpath = tarFileSet.getFullpath(this.getProject());
            if (fullpath.isEmpty()) {
                // don't add "" to the archive
                if (vPath.isEmpty()) {
                    return;
                }

                vPath = getCanonicalPrefix(tarFileSet, this.getProject()) + vPath;
            } else {
                vPath = fullpath;
            }

            preserveLeadingSlashes = tarFileSet.getPreserveLeadingSlashes();

            if (vPath.startsWith("/") && !preserveLeadingSlashes) {
                final int l = vPath.length();
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

        final TarEntry te = new TarEntry(vPath, preserveLeadingSlashes);
        te.setModTime(r.getLastModified());
        // preserve permissions
        if (r instanceof ArchiveResource) {
            final ArchiveResource ar = (ArchiveResource) r;
            te.setMode(ar.getMode());
            if (r instanceof TarResource) {
                final TarResource tr = (TarResource) r;
                te.setUserName(tr.getUserName());
                te.setUserId(tr.getLongUid());
                te.setGroupName(tr.getGroup());
                te.setGroupId(tr.getLongGid());
                String linkName = tr.getLinkName();
                byte linkFlag = tr.getLinkFlag();
                if (linkFlag == TarConstants.LF_LINK &&
                    linkName != null && linkName.length() > 0 && !linkName.startsWith("/")) {
                    linkName = getCanonicalPrefix(tarFileSet, this.getProject()) + linkName;
                }
                te.setLinkName(linkName);
                te.setLinkFlag(linkFlag);
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

                final byte[] buffer = new byte[BUFFER_SIZE];
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
    @Deprecated
    protected boolean archiveIsUpToDate(final String[] files) {
        return archiveIsUpToDate(files, baseDir);
    }

    /**
     * Is the archive up to date in relationship to a list of files.
     * @param files the files to check
     * @param dir   the base directory for the files.
     * @return true if the archive is up to date.
     * @since Ant 1.5.2
     */
    protected boolean archiveIsUpToDate(final String[] files, final File dir) {
        final SourceFileScanner sfs = new SourceFileScanner(this);
        final MergingMapper mm = new MergingMapper();
        mm.setTo(tarFile.getAbsolutePath());
        return sfs.restrict(files, dir, null, mm).length == 0;
    }

    /**
     * Is the archive up to date in relationship to a list of files.
     * @param r the files to check
     * @return true if the archive is up to date.
     * @since Ant 1.7
     */
    protected boolean archiveIsUpToDate(final Resource r) {
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
    protected boolean check(final ResourceCollection rc) {
        boolean upToDate = true;
        if (isFileFileSet(rc)) {
            final FileSet fs = (FileSet) rc;
            upToDate = check(fs.getDir(getProject()), getFileNames(fs));
        } else if (!rc.isFilesystemOnly() && !supportsNonFileResources()) {
            throw new BuildException("only filesystem resources are supported");
        } else if (rc.isFilesystemOnly()) {
            final Set<File> basedirs = new HashSet<>();
            final Map<File, List<String>> basedirToFilesMap = new HashMap<>();
            for (final Resource res : rc) {
                final FileResource r = ResourceUtils
                    .asFileResource(res.as(FileProvider.class));
                File base = r.getBaseDir();
                if (base == null) {
                    base = Copy.NULL_FILE_PLACEHOLDER;
                }
                basedirs.add(base);
                List<String> files = basedirToFilesMap.computeIfAbsent(base, k -> new Vector<>());
                if (base == Copy.NULL_FILE_PLACEHOLDER) {
                    files.add(r.getFile().getAbsolutePath());
                } else {
                    files.add(r.getName());
                }
            }
            for (final File base : basedirs) {
                final File tmpBase = base == Copy.NULL_FILE_PLACEHOLDER ? null : base;
                final List<String> files = basedirToFilesMap.get(base);
                upToDate &= check(tmpBase, files);
            }
        } else { // non-file resources
            for (Resource r : rc) {
                upToDate = archiveIsUpToDate(r);
            }
        }
        return upToDate;
    }

    /**
     * <p>Checks whether the archive is out-of-date with respect to the
     * given files, ensures that the archive won't contain itself.</p>
     *
     * @param basedir base directory for file names
     * @param files array of relative file names
     * @return whether the archive is up-to-date
     * @since Ant 1.7
     */
    protected boolean check(final File basedir, final String[] files) {
        boolean upToDate = archiveIsUpToDate(files, basedir);

        for (String file : files) {
            if (tarFile.equals(new File(basedir, file))) {
                throw new BuildException("A tar file cannot include itself",
                    getLocation());
            }
        }
        return upToDate;
    }

    /**
     * <p>Checks whether the archive is out-of-date with respect to the
     * given files, ensures that the archive won't contain itself.</p>
     *
     * @param basedir base directory for file names
     * @param files array of relative file names
     * @return whether the archive is up-to-date
     * @see #check(File, String[])
     * @since Ant 1.9.5
     */
    protected boolean check(final File basedir, final Collection<String> files) {
        return check(basedir, files.toArray(new String[0]));
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
    protected void tar(final ResourceCollection rc, final TarOutputStream tOut)
        throws IOException {
        ArchiveFileSet afs = null;
        if (rc instanceof ArchiveFileSet) {
            afs = (ArchiveFileSet) rc;
        }
        if (afs != null && afs.size() > 1
            && !afs.getFullpath(this.getProject()).isEmpty()) {
            throw new BuildException(
                "fullpath attribute may only be specified for filesets that specify a single file.");
        }
        final TarFileSet tfs = asTarFileSet(afs);

        if (isFileFileSet(rc)) {
            final FileSet fs = (FileSet) rc;
            for (String file : getFileNames(fs)) {
                final File f = new File(fs.getDir(getProject()), file);
                final String name = file.replace(File.separatorChar, '/');
                tarFile(f, tOut, name, tfs);
            }
        } else if (rc.isFilesystemOnly()) {
            for (final Resource r : rc) {
                final File f = r.as(FileProvider.class).getFile();
                tarFile(f, tOut, f.getName(), tfs);
            }
        } else { // non-file resources
            for (final Resource r : rc) {
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
    protected static boolean isFileFileSet(final ResourceCollection rc) {
        return rc instanceof FileSet && rc.isFilesystemOnly();
    }

    /**
     * Grabs all included files and directors from the FileSet and
     * returns them as an array of (relative) file names.
     * @param fs the fileset to operate on.
     * @return a list of the filenames.
     * @since Ant 1.7
     */
    protected static String[] getFileNames(final FileSet fs) {
        final DirectoryScanner ds = fs.getDirectoryScanner(fs.getProject());
        final String[] directories = ds.getIncludedDirectories();
        final String[] filesPerSe = ds.getIncludedFiles();
        final String[] files = new String[directories.length + filesPerSe.length];
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
    protected TarFileSet asTarFileSet(final ArchiveFileSet archiveFileSet) {
        TarFileSet tfs;
        if (archiveFileSet instanceof TarFileSet) {
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
                    final org.apache.tools.ant.types.TarFileSet t =
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

    private static String getCanonicalPrefix(TarFileSet tarFileSet, Project project) {
        String prefix = tarFileSet.getPrefix(project);
        // '/' is appended for compatibility with the zip task.
        if (prefix.isEmpty() || prefix.endsWith("/")) {
            return prefix;
        }
        return prefix += "/";
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
        public TarFileSet(final FileSet fileset) {
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
        public String[] getFiles(final Project p) {
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
        public void setMode(final String octalString) {
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
        public void setPreserveLeadingSlashes(final boolean b) {
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
            POSIX = "posix",
            OMIT = "omit";

        private static final String[] VALID_MODES = {
            WARN, FAIL, TRUNCATE, GNU, POSIX, OMIT
        };

        /** Constructor, defaults to "warn" */
        public TarLongFileMode() {
            super();
            setValue(WARN);
        }

        /**
         * @return the possible values for this enumerated type.
         */
        @Override
        public String[] getValues() {
            return VALID_MODES;
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

        /**
         * @return true if value is "posix".
         */
        public boolean isPosixMode() {
            return POSIX.equalsIgnoreCase(getValue());
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
         *  XZ compression
         * @since 1.10.1
         */
        private static final String XZ = "xz";

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
        @Override
        public String[] getValues() {
            return new String[] {NONE, GZIP, BZIP2, XZ};
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
            }
            if (XZ.equals(v)) {
                return newXZOutputStream(ostream);
            }
            if (BZIP2.equals(v)) {
                ostream.write('B');
                ostream.write('Z');
                return new CBZip2OutputStream(ostream);
            }
            return ostream;
        }

        private static OutputStream newXZOutputStream(OutputStream ostream)
            throws BuildException {
            try {
                Class<?> fClazz = Class.forName("org.tukaani.xz.FilterOptions");
                Class<?> oClazz = Class.forName("org.tukaani.xz.LZMA2Options");
                Class<? extends OutputStream> sClazz =
                    Class.forName("org.tukaani.xz.XZOutputStream")
                    .asSubclass(OutputStream.class);
                Constructor<? extends OutputStream> c =
                    sClazz.getConstructor(OutputStream.class, fClazz);
                return c.newInstance(ostream, oClazz.getDeclaredConstructor().newInstance());
            } catch (ClassNotFoundException ex) {
                throw new BuildException("xz compression requires the XZ for Java library",
                                         ex);
            } catch (NoSuchMethodException
                     | InstantiationException
                     | IllegalAccessException
                     | InvocationTargetException
                     ex) {
                throw new BuildException("failed to create XZOutputStream", ex);
            }
        }
    }
}
