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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Stack;
import java.util.Vector;
import java.util.zip.CRC32;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.FileScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.ArchiveFileSet;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.PatternSet;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.ZipFileSet;
import org.apache.tools.ant.types.ZipScanner;
import org.apache.tools.ant.types.resources.ArchiveResource;
import org.apache.tools.ant.types.resources.FileResource;
import org.apache.tools.ant.util.FileNameMapper;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.GlobPatternMapper;
import org.apache.tools.ant.util.IdentityMapper;
import org.apache.tools.ant.util.MergingMapper;
import org.apache.tools.ant.util.ResourceUtils;
import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipExtraField;
import org.apache.tools.zip.ZipFile;
import org.apache.tools.zip.ZipOutputStream;

/**
 * Create a Zip file.
 *
 * @since Ant 1.1
 *
 * @ant.task category="packaging"
 */
public class Zip extends MatchingTask {
    // CheckStyle:VisibilityModifier OFF - bc

    protected File zipFile;
    // use to scan own archive
    private ZipScanner zs;
    private File baseDir;
    protected Hashtable entries = new Hashtable();
    private Vector groupfilesets = new Vector();
    private Vector filesetsFromGroupfilesets = new Vector();
    protected String duplicate = "add";
    private boolean doCompress = true;
    private boolean doUpdate = false;
    // shadow of the above if the value is altered in execute
    private boolean savedDoUpdate = false;
    private boolean doFilesonly = false;
    protected String archiveType = "zip";

    // For directories:
    private static final long EMPTY_CRC = new CRC32 ().getValue ();
    protected String emptyBehavior = "skip";
    private Vector resources = new Vector();
    protected Hashtable addedDirs = new Hashtable();
    private Vector addedFiles = new Vector();

    protected boolean doubleFilePass = false;
    protected boolean skipWriting = false;

    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    // CheckStyle:VisibilityModifier ON

    /**
     * true when we are adding new files into the Zip file, as opposed
     * to adding back the unchanged files
     */
    private boolean addingNewFiles = false;

    /**
     * Encoding to use for filenames, defaults to the platform's
     * default encoding.
     */
    private String encoding;

    /**
     * Whether the original compression of entries coming from a ZIP
     * archive should be kept (for example when updating an archive).
     *
     * @since Ant 1.6
     */
    private boolean keepCompression = false;

    /**
     * Whether the file modification times will be rounded up to the
     * next even number of seconds.
     *
     * @since Ant 1.6.2
     */
    private boolean roundUp = true;

    /**
     * Comment for the archive.
     * @since Ant 1.6.3
     */
    private String comment = "";

    private int level = ZipOutputStream.DEFAULT_COMPRESSION;

    /**
     * This is the name/location of where to
     * create the .zip file.
     * @param zipFile the path of the zipFile
     * @deprecated since 1.5.x.
     *             Use setDestFile(File) instead.
     * @ant.attribute ignore="true"
     */
    public void setZipfile(File zipFile) {
        setDestFile(zipFile);
    }

    /**
     * This is the name/location of where to
     * create the file.
     * @param file the path of the zipFile
     * @since Ant 1.5
     * @deprecated since 1.5.x.
     *             Use setDestFile(File) instead.
     * @ant.attribute ignore="true"
     */
    public void setFile(File file) {
        setDestFile(file);
    }


    /**
     * The file to create; required.
     * @since Ant 1.5
     * @param destFile The new destination File
     */
    public void setDestFile(File destFile) {
       this.zipFile = destFile;
    }

    /**
     * The file to create.
     * @return the destination file
     * @since Ant 1.5.2
     */
    public File getDestFile() {
        return zipFile;
    }


    /**
     * Directory from which to archive files; optional.
     * @param baseDir the base directory
     */
    public void setBasedir(File baseDir) {
        this.baseDir = baseDir;
    }

    /**
     * Whether we want to compress the files or only store them;
     * optional, default=true;
     * @param c if true, compress the files
     */
    public void setCompress(boolean c) {
        doCompress = c;
    }

    /**
     * Whether we want to compress the files or only store them;
     * @return true if the files are to be compressed
     * @since Ant 1.5.2
     */
    public boolean isCompress() {
        return doCompress;
    }

    /**
     * If true, emulate Sun's jar utility by not adding parent directories;
     * optional, defaults to false.
     * @param f if true, emulate sun's jar by not adding parent directories
     */
    public void setFilesonly(boolean f) {
        doFilesonly = f;
    }

    /**
     * If true, updates an existing file, otherwise overwrite
     * any existing one; optional defaults to false.
     * @param c if true, updates an existing zip file
     */
    public void setUpdate(boolean c) {
        doUpdate = c;
        savedDoUpdate = c;
    }

    /**
     * Are we updating an existing archive?
     * @return true if updating an existing archive
     */
    public boolean isInUpdateMode() {
        return doUpdate;
    }

    /**
     * Adds a set of files.
     * @param set the fileset to add
     */
    public void addFileset(FileSet set) {
        add(set);
    }

    /**
     * Adds a set of files that can be
     * read from an archive and be given a prefix/fullpath.
     * @param set the zipfileset to add
     */
    public void addZipfileset(ZipFileSet set) {
        add(set);
    }

    /**
     * Add a collection of resources to be archived.
     * @param a the resources to archive
     * @since Ant 1.7
     */
    public void add(ResourceCollection a) {
        resources.add(a);
    }

    /**
     * Adds a group of zip files.
     * @param set the group (a fileset) to add
     */
    public void addZipGroupFileset(FileSet set) {
        groupfilesets.addElement(set);
    }

    /**
     * Sets behavior for when a duplicate file is about to be added -
     * one of <code>add</code>, <code>preserve</code> or <code>fail</code>.
     * Possible values are: <code>add</code> (keep both
     * of the files); <code>preserve</code> (keep the first version
     * of the file found); <code>fail</code> halt a problem
     * Default for zip tasks is <code>add</code>
     * @param df a <code>Duplicate</code> enumerated value
     */
    public void setDuplicate(Duplicate df) {
        duplicate = df.getValue();
    }

    /**
     * Possible behaviors when there are no matching files for the task:
     * "fail", "skip", or "create".
     */
    public static class WhenEmpty extends EnumeratedAttribute {
        /**
         * The string values for the enumerated value
         * @return the values
         */
        public String[] getValues() {
            return new String[] {"fail", "skip", "create"};
        }
    }

    /**
     * Sets behavior of the task when no files match.
     * Possible values are: <code>fail</code> (throw an exception
     * and halt the build); <code>skip</code> (do not create
     * any archive, but issue a warning); <code>create</code>
     * (make an archive with no entries).
     * Default for zip tasks is <code>skip</code>;
     * for jar tasks, <code>create</code>.
     * @param we a <code>WhenEmpty</code> enumerated value
     */
    public void setWhenempty(WhenEmpty we) {
        emptyBehavior = we.getValue();
    }

    /**
     * Encoding to use for filenames, defaults to the platform's
     * default encoding.
     *
     * <p>For a list of possible values see <a
     * href="http://java.sun.com/j2se/1.5.0/docs/guide/intl/encoding.doc.html">http://java.sun.com/j2se/1.5.0/docs/guide/intl/encoding.doc.html</a>.</p>
     * @param encoding the encoding name
     */
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    /**
     * Encoding to use for filenames.
     * @return the name of the encoding to use
     * @since Ant 1.5.2
     */
    public String getEncoding() {
        return encoding;
    }

    /**
     * Whether the original compression of entries coming from a ZIP
     * archive should be kept (for example when updating an archive).
     * Default is false.
     * @param keep if true, keep the original compression
     * @since Ant 1.6
     */
    public void setKeepCompression(boolean keep) {
        keepCompression = keep;
    }

    /**
     * Comment to use for archive.
     *
     * @param comment The content of the comment.
     * @since Ant 1.6.3
     */
    public void setComment(String comment) {
        this.comment = comment;
    }

    /**
     * Comment of the archive
     *
     * @return Comment of the archive.
     * @since Ant 1.6.3
     */
    public String getComment() {
        return comment;
    }

    /**
     * Set the compression level to use.  Default is
     * ZipOutputStream.DEFAULT_COMPRESSION.
     * @param level compression level.
     * @since Ant 1.7
     */
    public void setLevel(int level) {
        this.level = level;
    }

    /**
     * Get the compression level.
     * @return compression level.
     * @since Ant 1.7
     */
    public int getLevel() {
        return level;
    }

    /**
     * Whether the file modification times will be rounded up to the
     * next even number of seconds.
     *
     * <p>Zip archives store file modification times with a
     * granularity of two seconds, so the times will either be rounded
     * up or down.  If you round down, the archive will always seem
     * out-of-date when you rerun the task, so the default is to round
     * up.  Rounding up may lead to a different type of problems like
     * JSPs inside a web archive that seem to be slightly more recent
     * than precompiled pages, rendering precompilation useless.</p>
     * @param r a <code>boolean</code> value
     * @since Ant 1.6.2
     */
    public void setRoundUp(boolean r) {
        roundUp = r;
    }

    /**
     * validate and build
     * @throws BuildException on error
     */
    public void execute() throws BuildException {

        if (doubleFilePass) {
            skipWriting = true;
            executeMain();
            skipWriting = false;
            executeMain();
        } else {
            executeMain();
        }
    }

    /**
     * Build the zip file.
     * This is called twice if doubleFilePass is true.
     * @throws BuildException on error
     */
    public void executeMain() throws BuildException {

        if (baseDir == null && resources.size() == 0
            && groupfilesets.size() == 0 && "zip".equals(archiveType)) {
            throw new BuildException("basedir attribute must be set, "
                                     + "or at least one "
                                     + "resource collection must be given!");
        }

        if (zipFile == null) {
            throw new BuildException("You must specify the "
                                     + archiveType + " file to create!");
        }

        if (zipFile.exists() && !zipFile.isFile()) {
            throw new BuildException(zipFile + " is not a file.");
        }

        if (zipFile.exists() && !zipFile.canWrite()) {
            throw new BuildException(zipFile + " is read-only.");
        }

        // Renamed version of original file, if it exists
        File renamedFile = null;
        addingNewFiles = true;

        // Whether or not an actual update is required -
        // we don't need to update if the original file doesn't exist
        if (doUpdate && !zipFile.exists()) {
            doUpdate = false;
            log("ignoring update attribute as " + archiveType
                + " doesn't exist.", Project.MSG_DEBUG);
        }

        // Add the files found in groupfileset to fileset
        for (int i = 0; i < groupfilesets.size(); i++) {

            log("Processing groupfileset ", Project.MSG_VERBOSE);
            FileSet fs = (FileSet) groupfilesets.elementAt(i);
            FileScanner scanner = fs.getDirectoryScanner(getProject());
            String[] files = scanner.getIncludedFiles();
            File basedir = scanner.getBasedir();
            for (int j = 0; j < files.length; j++) {

                log("Adding file " + files[j] + " to fileset",
                    Project.MSG_VERBOSE);
                ZipFileSet zf = new ZipFileSet();
                zf.setProject(getProject());
                zf.setSrc(new File(basedir, files[j]));
                add(zf);
                filesetsFromGroupfilesets.addElement(zf);
            }
        }

        // collect filesets to pass them to getResourcesToAdd
        Vector vfss = new Vector();
        if (baseDir != null) {
            FileSet fs = (FileSet) getImplicitFileSet().clone();
            fs.setDir(baseDir);
            vfss.addElement(fs);
        }
        for (int i = 0; i < resources.size(); i++) {
            ResourceCollection rc = (ResourceCollection) resources.elementAt(i);
            vfss.addElement(rc);
        }

        ResourceCollection[] fss = new ResourceCollection[vfss.size()];
        vfss.copyInto(fss);
        boolean success = false;
        try {
            // can also handle empty archives
            ArchiveState state = getResourcesToAdd(fss, zipFile, false);

            // quick exit if the target is up to date
            if (!state.isOutOfDate()) {
                return;
            }

            if (!zipFile.exists() && state.isWithoutAnyResources()) {
                createEmptyZip(zipFile);
                return;
            }
            Resource[][] addThem = state.getResourcesToAdd();

            if (doUpdate) {
                renamedFile =
                    FILE_UTILS.createTempFile("zip", ".tmp",
                                              zipFile.getParentFile());
                renamedFile.deleteOnExit();

                try {
                    FILE_UTILS.rename(zipFile, renamedFile);
                } catch (SecurityException e) {
                    throw new BuildException(
                        "Not allowed to rename old file ("
                        + zipFile.getAbsolutePath()
                        + ") to temporary file");
                } catch (IOException e) {
                    throw new BuildException(
                        "Unable to rename old file ("
                        + zipFile.getAbsolutePath()
                        + ") to temporary file");
                }
            }

            String action = doUpdate ? "Updating " : "Building ";

            log(action + archiveType + ": " + zipFile.getAbsolutePath());

            ZipOutputStream zOut = null;
            try {
                if (!skipWriting) {
                    zOut = new ZipOutputStream(zipFile);

                    zOut.setEncoding(encoding);
                    zOut.setMethod(doCompress
                        ? ZipOutputStream.DEFLATED : ZipOutputStream.STORED);
                    zOut.setLevel(level);
                }
                initZipOutputStream(zOut);

                // Add the explicit resource collections to the archive.
                for (int i = 0; i < fss.length; i++) {
                    if (addThem[i].length != 0) {
                        addResources(fss[i], addThem[i], zOut);
                    }
                }

                if (doUpdate) {
                    addingNewFiles = false;
                    ZipFileSet oldFiles = new ZipFileSet();
                    oldFiles.setProject(getProject());
                    oldFiles.setSrc(renamedFile);
                    oldFiles.setDefaultexcludes(false);

                    for (int i = 0; i < addedFiles.size(); i++) {
                        PatternSet.NameEntry ne = oldFiles.createExclude();
                        ne.setName((String) addedFiles.elementAt(i));
                    }
                    DirectoryScanner ds =
                        oldFiles.getDirectoryScanner(getProject());
                    ((ZipScanner) ds).setEncoding(encoding);

                    String[] f = ds.getIncludedFiles();
                    Resource[] r = new Resource[f.length];
                    for (int i = 0; i < f.length; i++) {
                        r[i] = ds.getResource(f[i]);
                    }

                    if (!doFilesonly) {
                        String[] d = ds.getIncludedDirectories();
                        Resource[] dr = new Resource[d.length];
                        for (int i = 0; i < d.length; i++) {
                            dr[i] = ds.getResource(d[i]);
                        }
                        Resource[] tmp = r;
                        r = new Resource[tmp.length + dr.length];
                        System.arraycopy(dr, 0, r, 0, dr.length);
                        System.arraycopy(tmp, 0, r, dr.length, tmp.length);
                    }
                    addResources(oldFiles, r, zOut);
                }
                if (zOut != null) {
                    zOut.setComment(comment);
                }
                finalizeZipOutputStream(zOut);

                // If we've been successful on an update, delete the
                // temporary file
                if (doUpdate) {
                    if (!renamedFile.delete()) {
                        log ("Warning: unable to delete temporary file "
                            + renamedFile.getName(), Project.MSG_WARN);
                    }
                }
                success = true;
            } finally {
                // Close the output stream.
                try {
                    if (zOut != null) {
                        zOut.close();
                    }
                } catch (IOException ex) {
                    // If we're in this finally clause because of an
                    // exception, we don't really care if there's an
                    // exception when closing the stream. E.g. if it
                    // throws "ZIP file must have at least one entry",
                    // because an exception happened before we added
                    // any files, then we must swallow this
                    // exception. Otherwise, the error that's reported
                    // will be the close() error, which is not the
                    // real cause of the problem.
                    if (success) {
                        throw ex;
                    }
                }
            }
        } catch (IOException ioe) {
            String msg = "Problem creating " + archiveType + ": "
                + ioe.getMessage();

            // delete a bogus ZIP file (but only if it's not the original one)
            if ((!doUpdate || renamedFile != null) && !zipFile.delete()) {
                msg += " (and the archive is probably corrupt but I could not "
                    + "delete it)";
            }

            if (doUpdate && renamedFile != null) {
                try {
                    FILE_UTILS.rename(renamedFile, zipFile);
                } catch (IOException e) {
                    msg += " (and I couldn't rename the temporary file "
                            + renamedFile.getName() + " back)";
                }
            }

            throw new BuildException(msg, ioe, getLocation());
        } finally {
            cleanUp();
        }
    }

    /**
     * Indicates if the task is adding new files into the archive as opposed to
     * copying back unchanged files from the backup copy
     * @return true if adding new files
     */
    protected final boolean isAddingNewFiles() {
        return addingNewFiles;
    }

    /**
     * Add the given resources.
     *
     * @param fileset may give additional information like fullpath or
     * permissions.
     * @param resources the resources to add
     * @param zOut the stream to write to
     * @throws IOException on error
     *
     * @since Ant 1.5.2
     */
    protected final void addResources(FileSet fileset, Resource[] resources,
                                      ZipOutputStream zOut)
        throws IOException {

        String prefix = "";
        String fullpath = "";
        int dirMode = ArchiveFileSet.DEFAULT_DIR_MODE;
        int fileMode = ArchiveFileSet.DEFAULT_FILE_MODE;

        ArchiveFileSet zfs = null;
        if (fileset instanceof ArchiveFileSet) {
            zfs = (ArchiveFileSet) fileset;
            prefix = zfs.getPrefix(getProject());
            fullpath = zfs.getFullpath(getProject());
            dirMode = zfs.getDirMode(getProject());
            fileMode = zfs.getFileMode(getProject());
        }

        if (prefix.length() > 0 && fullpath.length() > 0) {
            throw new BuildException("Both prefix and fullpath attributes must"
                                     + " not be set on the same fileset.");
        }

        if (resources.length != 1 && fullpath.length() > 0) {
            throw new BuildException("fullpath attribute may only be specified"
                                     + " for filesets that specify a single"
                                     + " file.");
        }

        if (prefix.length() > 0) {
            if (!prefix.endsWith("/") && !prefix.endsWith("\\")) {
                prefix += "/";
            }
            addParentDirs(null, prefix, zOut, "", dirMode);
        }

        ZipFile zf = null;
        try {
            boolean dealingWithFiles = false;
            File base = null;

            if (zfs == null || zfs.getSrc(getProject()) == null) {
                dealingWithFiles = true;
                base = fileset.getDir(getProject());
            } else if (zfs instanceof ZipFileSet) {
                zf = new ZipFile(zfs.getSrc(getProject()), encoding);
            }

            for (int i = 0; i < resources.length; i++) {
                String name = null;
                if (fullpath.length() > 0) {
                    name = fullpath;
                } else {
                    name = resources[i].getName();
                }
                name = name.replace(File.separatorChar, '/');

                if ("".equals(name)) {
                    continue;
                }
                if (resources[i].isDirectory() && !name.endsWith("/")) {
                    name = name + "/";
                }

                if (!doFilesonly && !dealingWithFiles
                    && resources[i].isDirectory()
                    && !zfs.hasDirModeBeenSet()) {
                    int nextToLastSlash = name.lastIndexOf("/",
                                                           name.length() - 2);
                    if (nextToLastSlash != -1) {
                        addParentDirs(base, name.substring(0,
                                                           nextToLastSlash + 1),
                                      zOut, prefix, dirMode);
                    }
                    if (zf != null) {
                        ZipEntry ze = zf.getEntry(resources[i].getName());
                        addParentDirs(base, name, zOut, prefix,
                                      ze.getUnixMode());
                    } else {
                        ArchiveResource tr = (ArchiveResource) resources[i];
                        addParentDirs(base, name, zOut, prefix,
                                      tr.getMode());
                    }

                } else {
                    addParentDirs(base, name, zOut, prefix, dirMode);
                }

                if (!resources[i].isDirectory() && dealingWithFiles) {
                    File f = FILE_UTILS.resolveFile(base,
                                                   resources[i].getName());
                    zipFile(f, zOut, prefix + name, fileMode);
                } else if (!resources[i].isDirectory()) {
                    if (zf != null) {
                    ZipEntry ze = zf.getEntry(resources[i].getName());

                    if (ze != null) {
                        boolean oldCompress = doCompress;
                        if (keepCompression) {
                            doCompress = (ze.getMethod() == ZipEntry.DEFLATED);
                        }
                        try {
                            zipFile(zf.getInputStream(ze), zOut, prefix + name,
                                    ze.getTime(), zfs.getSrc(getProject()),
                                    zfs.hasFileModeBeenSet() ? fileMode
                                    : ze.getUnixMode());
                        } finally {
                            doCompress = oldCompress;
                        }
                    }
                    } else {
                        ArchiveResource tr = (ArchiveResource) resources[i];
                        InputStream is = null;
                        try {
                            is = tr.getInputStream();
                            zipFile(is, zOut, prefix + name,
                                    resources[i].getLastModified(),
                                    zfs.getSrc(getProject()),
                                    zfs.hasFileModeBeenSet() ? fileMode
                                    : tr.getMode());
                        } finally {
                            FileUtils.close(is);
                        }
                    }
                }
            }
        } finally {
            if (zf != null) {
                zf.close();
            }
        }
    }

    /**
     * Add the given resources.
     *
     * @param rc may give additional information like fullpath or
     * permissions.
     * @param resources the resources to add
     * @param zOut the stream to write to
     * @throws IOException on error
     *
     * @since Ant 1.7
     */
    protected final void addResources(ResourceCollection rc,
                                      Resource[] resources,
                                      ZipOutputStream zOut)
        throws IOException {
        if (rc instanceof FileSet) {
            addResources((FileSet) rc, resources, zOut);
            return;
        }
        for (int i = 0; i < resources.length; i++) {
            String name = resources[i].getName().replace(File.separatorChar,
                                                         '/');
            if ("".equals(name)) {
                continue;
            }
            if (resources[i].isDirectory() && doFilesonly) {
                continue;
            }
            File base = null;
            if (resources[i] instanceof FileResource) {
                base = ((FileResource) resources[i]).getBaseDir();
            }
            if (resources[i].isDirectory()) {
                if (!name.endsWith("/")) {
                    name = name + "/";
                }
            }

            addParentDirs(base, name, zOut, "",
                          ArchiveFileSet.DEFAULT_DIR_MODE);

            if (!resources[i].isDirectory()) {
                if (resources[i] instanceof FileResource) {
                    File f = ((FileResource) resources[i]).getFile();
                    zipFile(f, zOut, name, ArchiveFileSet.DEFAULT_FILE_MODE);
                } else {
                    InputStream is = null;
                    try {
                        is = resources[i].getInputStream();
                        zipFile(is, zOut, name,
                                resources[i].getLastModified(),
                                null, ArchiveFileSet.DEFAULT_FILE_MODE);
                    } finally {
                        FileUtils.close(is);
                    }
                }
            }
        }
    }

    /**
     * method for subclasses to override
     * @param zOut the zip output stream
     * @throws IOException on output error
     * @throws BuildException on other errors
     */
    protected void initZipOutputStream(ZipOutputStream zOut)
        throws IOException, BuildException {
    }

    /**
     * method for subclasses to override
     * @param zOut the zip output stream
     * @throws IOException on output error
     * @throws BuildException on other errors
     */
    protected void finalizeZipOutputStream(ZipOutputStream zOut)
        throws IOException, BuildException {
    }

    /**
     * Create an empty zip file
     * @param zipFile the zip file
     * @return true for historic reasons
     * @throws BuildException on error
     */
    protected boolean createEmptyZip(File zipFile) throws BuildException {
        // In this case using java.util.zip will not work
        // because it does not permit a zero-entry archive.
        // Must create it manually.
        log("Note: creating empty " + archiveType + " archive " + zipFile,
            Project.MSG_INFO);
        OutputStream os = null;
        try {
            os = new FileOutputStream(zipFile);
            // Cf. PKZIP specification.
            byte[] empty = new byte[22];
            empty[0] = 80; // P
            empty[1] = 75; // K
            empty[2] = 5;
            empty[3] = 6;
            // remainder zeros
            os.write(empty);
        } catch (IOException ioe) {
            throw new BuildException("Could not create empty ZIP archive "
                                     + "(" + ioe.getMessage() + ")", ioe,
                                     getLocation());
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    //ignore
                }
            }
        }
        return true;
    }

    /**
     * @since Ant 1.5.2
     */
    private synchronized ZipScanner getZipScanner() {
        if (zs == null) {
            zs = new ZipScanner();
            zs.setEncoding(encoding);
            zs.setSrc(zipFile);
        }
        return zs;
    }

    /**
     * Collect the resources that are newer than the corresponding
     * entries (or missing) in the original archive.
     *
     * <p>If we are going to recreate the archive instead of updating
     * it, all resources should be considered as new, if a single one
     * is.  Because of this, subclasses overriding this method must
     * call <code>super.getResourcesToAdd</code> and indicate with the
     * third arg if they already know that the archive is
     * out-of-date.</p>
     *
     * <p>This method first delegates to getNonFileSetResourceToAdd
     * and then invokes the FileSet-arg version.  All this to keep
     * backwards compatibility for subclasses that don't know how to
     * deal with non-FileSet ResourceCollections.</p>
     *
     * @param rcs The resource collections to grab resources from
     * @param zipFile intended archive file (may or may not exist)
     * @param needsUpdate whether we already know that the archive is
     * out-of-date.  Subclasses overriding this method are supposed to
     * set this value correctly in their call to
     * <code>super.getResourcesToAdd</code>.
     * @return an array of resources to add for each fileset passed in as well
     *         as a flag that indicates whether the archive is uptodate.
     *
     * @exception BuildException if it likes
     * @since Ant 1.7
     */
    protected ArchiveState getResourcesToAdd(ResourceCollection[] rcs,
                                             File zipFile,
                                             boolean needsUpdate)
        throws BuildException {
        ArrayList filesets = new ArrayList();
        ArrayList rest = new ArrayList();
        for (int i = 0; i < rcs.length; i++) {
            if (rcs[i] instanceof FileSet) {
                filesets.add(rcs[i]);
            } else {
                rest.add(rcs[i]);
            }
        }
        ResourceCollection[] rc = (ResourceCollection[])
            rest.toArray(new ResourceCollection[rest.size()]);
        ArchiveState as = getNonFileSetResourcesToAdd(rc, zipFile,
                                                      needsUpdate);

        FileSet[] fs = (FileSet[]) filesets.toArray(new FileSet[filesets
                                                                .size()]);
        ArchiveState as2 = getResourcesToAdd(fs, zipFile, as.isOutOfDate());
        if (!as.isOutOfDate() && as2.isOutOfDate()) {
            /*
             * Bad luck.
             *
             * There are resources in the filesets that make the
             * archive out of date, but not in the non-fileset
             * resources. We need to rescan the non-FileSets to grab
             * all of them now.
             */
            as = getNonFileSetResourcesToAdd(rc, zipFile, true);
        }

        Resource[][] toAdd = new Resource[rcs.length][];
        int fsIndex = 0;
        int restIndex = 0;
        for (int i = 0; i < rcs.length; i++) {
            if (rcs[i] instanceof FileSet) {
                toAdd[i] = as2.getResourcesToAdd()[fsIndex++];
            } else {
                toAdd[i] = as.getResourcesToAdd()[restIndex++];
            }
        }
        return new ArchiveState(as2.isOutOfDate(), toAdd);
    }

    /**
     * Collect the resources that are newer than the corresponding
     * entries (or missing) in the original archive.
     *
     * <p>If we are going to recreate the archive instead of updating
     * it, all resources should be considered as new, if a single one
     * is.  Because of this, subclasses overriding this method must
     * call <code>super.getResourcesToAdd</code> and indicate with the
     * third arg if they already know that the archive is
     * out-of-date.</p>
     *
     * @param filesets The filesets to grab resources from
     * @param zipFile intended archive file (may or may not exist)
     * @param needsUpdate whether we already know that the archive is
     * out-of-date.  Subclasses overriding this method are supposed to
     * set this value correctly in their call to
     * <code>super.getResourcesToAdd</code>.
     * @return an array of resources to add for each fileset passed in as well
     *         as a flag that indicates whether the archive is uptodate.
     *
     * @exception BuildException if it likes
     */
    protected ArchiveState getResourcesToAdd(FileSet[] filesets,
                                             File zipFile,
                                             boolean needsUpdate)
        throws BuildException {

        Resource[][] initialResources = grabResources(filesets);
        if (isEmpty(initialResources)) {
            if (needsUpdate && doUpdate) {
                /*
                 * This is a rather hairy case.
                 *
                 * One of our subclasses knows that we need to update the
                 * archive, but at the same time, there are no resources
                 * known to us that would need to be added.  Only the
                 * subclass seems to know what's going on.
                 *
                 * This happens if <jar> detects that the manifest has changed,
                 * for example.  The manifest is not part of any resources
                 * because of our support for inline <manifest>s.
                 *
                 * If we invoke createEmptyZip like Ant 1.5.2 did,
                 * we'll loose all stuff that has been in the original
                 * archive (bugzilla report 17780).
                 */
                return new ArchiveState(true, initialResources);
            }

            if (emptyBehavior.equals("skip")) {
                if (doUpdate) {
                    log(archiveType + " archive " + zipFile
                        + " not updated because no new files were included.",
                        Project.MSG_VERBOSE);
                } else {
                    log("Warning: skipping " + archiveType + " archive "
                        + zipFile + " because no files were included.",
                        Project.MSG_WARN);
                }
            } else if (emptyBehavior.equals("fail")) {
                throw new BuildException("Cannot create " + archiveType
                                         + " archive " + zipFile
                                         + ": no files were included.",
                                         getLocation());
            } else {
                // Create.
                if (!zipFile.exists())  {
                    needsUpdate = true;
                }
            }
            return new ArchiveState(needsUpdate, initialResources);
        }

        // initialResources is not empty

        if (!zipFile.exists()) {
            return new ArchiveState(true, initialResources);
        }

        if (needsUpdate && !doUpdate) {
            // we are recreating the archive, need all resources
            return new ArchiveState(true, initialResources);
        }

        Resource[][] newerResources = new Resource[filesets.length][];

        for (int i = 0; i < filesets.length; i++) {
            if (!(fileset instanceof ZipFileSet)
                || ((ZipFileSet) fileset).getSrc(getProject()) == null) {
                File base = filesets[i].getDir(getProject());

                for (int j = 0; j < initialResources[i].length; j++) {
                    File resourceAsFile =
                        FILE_UTILS.resolveFile(base,
                                              initialResources[i][j].getName());
                    if (resourceAsFile.equals(zipFile)) {
                        throw new BuildException("A zip file cannot include "
                                                 + "itself", getLocation());
                    }
                }
            }
        }

        for (int i = 0; i < filesets.length; i++) {
            if (initialResources[i].length == 0) {
                newerResources[i] = new Resource[] {};
                continue;
            }

            FileNameMapper myMapper = new IdentityMapper();
            if (filesets[i] instanceof ZipFileSet) {
                ZipFileSet zfs = (ZipFileSet) filesets[i];
                if (zfs.getFullpath(getProject()) != null
                    && !zfs.getFullpath(getProject()).equals("")) {
                    // in this case all files from origin map to
                    // the fullPath attribute of the zipfileset at
                    // destination
                    MergingMapper fm = new MergingMapper();
                    fm.setTo(zfs.getFullpath(getProject()));
                    myMapper = fm;

                } else if (zfs.getPrefix(getProject()) != null
                           && !zfs.getPrefix(getProject()).equals("")) {
                    GlobPatternMapper gm = new GlobPatternMapper();
                    gm.setFrom("*");
                    String prefix = zfs.getPrefix(getProject());
                    if (!prefix.endsWith("/") && !prefix.endsWith("\\")) {
                        prefix += "/";
                    }
                    gm.setTo(prefix + "*");
                    myMapper = gm;
                }
            }

            Resource[] resources = initialResources[i];
            if (doFilesonly) {
                resources = selectFileResources(resources);
            }

            newerResources[i] =
                ResourceUtils.selectOutOfDateSources(this,
                                                     resources,
                                                     myMapper,
                                                     getZipScanner());
            needsUpdate = needsUpdate || (newerResources[i].length > 0);

            if (needsUpdate && !doUpdate) {
                // we will return initialResources anyway, no reason
                // to scan further.
                break;
            }
        }

        if (needsUpdate && !doUpdate) {
            // we are recreating the archive, need all resources
            return new ArchiveState(true, initialResources);
        }

        return new ArchiveState(needsUpdate, newerResources);
    }

    /**
     * Collect the resources that are newer than the corresponding
     * entries (or missing) in the original archive.
     *
     * <p>If we are going to recreate the archive instead of updating
     * it, all resources should be considered as new, if a single one
     * is.  Because of this, subclasses overriding this method must
     * call <code>super.getResourcesToAdd</code> and indicate with the
     * third arg if they already know that the archive is
     * out-of-date.</p>
     *
     * @param rcs The filesets to grab resources from
     * @param zipFile intended archive file (may or may not exist)
     * @param needsUpdate whether we already know that the archive is
     * out-of-date.  Subclasses overriding this method are supposed to
     * set this value correctly in their call to
     * <code>super.getResourcesToAdd</code>.
     * @return an array of resources to add for each fileset passed in as well
     *         as a flag that indicates whether the archive is uptodate.
     *
     * @exception BuildException if it likes
     */
    protected ArchiveState getNonFileSetResourcesToAdd(ResourceCollection[] rcs,
                                                       File zipFile,
                                                       boolean needsUpdate)
        throws BuildException {
        /*
         * Backwards compatibility forces us to repeat the logic of
         * getResourcesToAdd(FileSet[], ...) here once again.
         */

        Resource[][] initialResources = grabNonFileSetResources(rcs);
        if (isEmpty(initialResources)) {
            // no emptyBehavior handling since the FileSet version
            // will take care of it.
            return new ArchiveState(needsUpdate, initialResources);
        }

        // initialResources is not empty

        if (!zipFile.exists()) {
            return new ArchiveState(true, initialResources);
        }

        if (needsUpdate && !doUpdate) {
            // we are recreating the archive, need all resources
            return new ArchiveState(true, initialResources);
        }

        Resource[][] newerResources = new Resource[rcs.length][];

        for (int i = 0; i < rcs.length; i++) {
            if (initialResources[i].length == 0) {
                newerResources[i] = new Resource[] {};
                continue;
            }

            for (int j = 0; j < initialResources[i].length; j++) {
                if (initialResources[i][j] instanceof FileResource
                    && zipFile.equals(((FileResource)
                                       initialResources[i][j]).getFile())) {
                    throw new BuildException("A zip file cannot include "
                                             + "itself", getLocation());
                }
            }

            Resource[] rs = initialResources[i];
            if (doFilesonly) {
                rs = selectFileResources(rs);
            }

            newerResources[i] =
                ResourceUtils.selectOutOfDateSources(this,
                                                     rs,
                                                     new IdentityMapper(),
                                                     getZipScanner());
            needsUpdate = needsUpdate || (newerResources[i].length > 0);

            if (needsUpdate && !doUpdate) {
                // we will return initialResources anyway, no reason
                // to scan further.
                break;
            }
        }

        if (needsUpdate && !doUpdate) {
            // we are recreating the archive, need all resources
            return new ArchiveState(true, initialResources);
        }

        return new ArchiveState(needsUpdate, newerResources);
    }

    /**
     * Fetch all included and not excluded resources from the sets.
     *
     * <p>Included directories will precede included files.</p>
     * @param filesets an array of filesets
     * @return the resources included
     * @since Ant 1.5.2
     */
    protected Resource[][] grabResources(FileSet[] filesets) {
        Resource[][] result = new Resource[filesets.length][];
        for (int i = 0; i < filesets.length; i++) {
            boolean skipEmptyNames = true;
            if (filesets[i] instanceof ZipFileSet) {
                ZipFileSet zfs = (ZipFileSet) filesets[i];
                skipEmptyNames = zfs.getPrefix(getProject()).equals("")
                    && zfs.getFullpath(getProject()).equals("");
            }
            DirectoryScanner rs =
                filesets[i].getDirectoryScanner(getProject());
            if (rs instanceof ZipScanner) {
                ((ZipScanner) rs).setEncoding(encoding);
            }
            Vector resources = new Vector();
            if (!doFilesonly) {
                String[] directories = rs.getIncludedDirectories();
                for (int j = 0; j < directories.length; j++) {
                    if (!"".equals(directories[j]) || !skipEmptyNames) {
                        resources.addElement(rs.getResource(directories[j]));
                    }
                }
            }
            String[] files = rs.getIncludedFiles();
            for (int j = 0; j < files.length; j++) {
                if (!"".equals(files[j]) || !skipEmptyNames) {
                    resources.addElement(rs.getResource(files[j]));
                }
            }

            result[i] = new Resource[resources.size()];
            resources.copyInto(result[i]);
        }
        return result;
    }

    /**
     * Fetch all included and not excluded resources from the collections.
     *
     * <p>Included directories will precede included files.</p>
     * @param rcs an array of resource collections
     * @return the resources included
     * @since Ant 1.7
     */
    protected Resource[][] grabNonFileSetResources(ResourceCollection[] rcs) {
        Resource[][] result = new Resource[rcs.length][];
        for (int i = 0; i < rcs.length; i++) {
            Iterator iter = rcs[i].iterator();
            ArrayList rs = new ArrayList();
            int lastDir = 0;
            while (iter.hasNext()) {
                Resource r = (Resource) iter.next();
                if (r.isExists()) {
                    if (r.isDirectory()) {
                        rs.add(lastDir++, r);
                    } else {
                        rs.add(r);
                    }
                }
            }
            result[i] = (Resource[]) rs.toArray(new Resource[rs.size()]);
        }
        return result;
    }

    /**
     * Add a directory to the zip stream.
     * @param dir  the directort to add to the archive
     * @param zOut the stream to write to
     * @param vPath the name this entry shall have in the archive
     * @param mode the Unix permissions to set.
     * @throws IOException on error
     * @since Ant 1.5.2
     */
    protected void zipDir(File dir, ZipOutputStream zOut, String vPath,
                          int mode)
        throws IOException {
        zipDir(dir, zOut, vPath, mode, null);
    }

    /**
     * Add a directory to the zip stream.
     * @param dir  the directort to add to the archive
     * @param zOut the stream to write to
     * @param vPath the name this entry shall have in the archive
     * @param mode the Unix permissions to set.
     * @param extra ZipExtraFields to add
     * @throws IOException on error
     * @since Ant 1.6.3
     */
    protected void zipDir(File dir, ZipOutputStream zOut, String vPath,
                          int mode, ZipExtraField[] extra)
        throws IOException {
        if (doFilesonly) {
            log("skipping directory " + vPath + " for file-only archive",
                    Project.MSG_VERBOSE);
            return;
        }
        if (addedDirs.get(vPath) != null) {
            // don't add directories we've already added.
            // no warning if we try, it is harmless in and of itself
            return;
        }

        log("adding directory " + vPath, Project.MSG_VERBOSE);
        addedDirs.put(vPath, vPath);

        if (!skipWriting) {
            ZipEntry ze = new ZipEntry (vPath);
            if (dir != null && dir.exists()) {
                // ZIPs store time with a granularity of 2 seconds, round up
                ze.setTime(dir.lastModified() + (roundUp ? 1999 : 0));
            } else {
                // ZIPs store time with a granularity of 2 seconds, round up
                ze.setTime(System.currentTimeMillis() + (roundUp ? 1999 : 0));
            }
            ze.setSize (0);
            ze.setMethod (ZipEntry.STORED);
            // This is faintly ridiculous:
            ze.setCrc (EMPTY_CRC);
            ze.setUnixMode(mode);

            if (extra != null) {
                ze.setExtraFields(extra);
            }

            zOut.putNextEntry(ze);
        }
    }

    /**
     * Adds a new entry to the archive, takes care of duplicates as well.
     *
     * @param in the stream to read data for the entry from.
     * @param zOut the stream to write to.
     * @param vPath the name this entry shall have in the archive.
     * @param lastModified last modification time for the entry.
     * @param fromArchive the original archive we are copying this
     * entry from, will be null if we are not copying from an archive.
     * @param mode the Unix permissions to set.
     *
     * @since Ant 1.5.2
     * @throws IOException on error
     */
    protected void zipFile(InputStream in, ZipOutputStream zOut, String vPath,
                           long lastModified, File fromArchive, int mode)
        throws IOException {
        if (entries.contains(vPath)) {

            if (duplicate.equals("preserve")) {
                log(vPath + " already added, skipping", Project.MSG_INFO);
                return;
            } else if (duplicate.equals("fail")) {
                throw new BuildException("Duplicate file " + vPath
                                         + " was found and the duplicate "
                                         + "attribute is 'fail'.");
            } else {
                // duplicate equal to add, so we continue
                log("duplicate file " + vPath
                    + " found, adding.", Project.MSG_VERBOSE);
            }
        } else {
            log("adding entry " + vPath, Project.MSG_VERBOSE);
        }

        entries.put(vPath, vPath);

        if (!skipWriting) {
            ZipEntry ze = new ZipEntry(vPath);
            ze.setTime(lastModified);
            ze.setMethod(doCompress ? ZipEntry.DEFLATED : ZipEntry.STORED);

            /*
             * ZipOutputStream.putNextEntry expects the ZipEntry to
             * know its size and the CRC sum before you start writing
             * the data when using STORED mode - unless it is seekable.
             *
             * This forces us to process the data twice.
             */
            if (!zOut.isSeekable() && !doCompress) {
                long size = 0;
                CRC32 cal = new CRC32();
                if (!in.markSupported()) {
                    // Store data into a byte[]
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();

                    byte[] buffer = new byte[8 * 1024];
                    int count = 0;
                    do {
                        size += count;
                        cal.update(buffer, 0, count);
                        bos.write(buffer, 0, count);
                        count = in.read(buffer, 0, buffer.length);
                    } while (count != -1);
                    in = new ByteArrayInputStream(bos.toByteArray());

                } else {
                    in.mark(Integer.MAX_VALUE);
                    byte[] buffer = new byte[8 * 1024];
                    int count = 0;
                    do {
                        size += count;
                        cal.update(buffer, 0, count);
                        count = in.read(buffer, 0, buffer.length);
                    } while (count != -1);
                    in.reset();
                }
                ze.setSize(size);
                ze.setCrc(cal.getValue());
            }

            ze.setUnixMode(mode);
            zOut.putNextEntry(ze);

            byte[] buffer = new byte[8 * 1024];
            int count = 0;
            do {
                if (count != 0) {
                    zOut.write(buffer, 0, count);
                }
                count = in.read(buffer, 0, buffer.length);
            } while (count != -1);
        }
        addedFiles.addElement(vPath);
    }

    /**
     * Method that gets called when adding from <code>java.io.File</code> instances.
     *
     * <p>This implementation delegates to the six-arg version.</p>
     *
     * @param file the file to add to the archive
     * @param zOut the stream to write to
     * @param vPath the name this entry shall have in the archive
     * @param mode the Unix permissions to set.
     * @throws IOException on error
     *
     * @since Ant 1.5.2
     */
    protected void zipFile(File file, ZipOutputStream zOut, String vPath,
                           int mode)
        throws IOException {
        if (file.equals(zipFile)) {
            throw new BuildException("A zip file cannot include itself",
                                     getLocation());
        }

        FileInputStream fIn = new FileInputStream(file);
        try {
            // ZIPs store time with a granularity of 2 seconds, round up
            zipFile(fIn, zOut, vPath,
                    file.lastModified() + (roundUp ? 1999 : 0),
                    null, mode);
        } finally {
            fIn.close();
        }
    }

    /**
     * Ensure all parent dirs of a given entry have been added.
     * @param baseDir the base directory to use (may be null)
     * @param entry   the entry name to create directories from
     * @param zOut    the stream to write to
     * @param prefix  a prefix to place on the created entries
     * @param dirMode the directory mode
     * @throws IOException on error
     * @since Ant 1.5.2
     */
    protected final void addParentDirs(File baseDir, String entry,
                                       ZipOutputStream zOut, String prefix,
                                       int dirMode)
        throws IOException {
        if (!doFilesonly) {
            Stack directories = new Stack();
            int slashPos = entry.length();

            while ((slashPos = entry.lastIndexOf('/', slashPos - 1)) != -1) {
                String dir = entry.substring(0, slashPos + 1);
                if (addedDirs.get(prefix + dir) != null) {
                    break;
                }
                directories.push(dir);
            }

            while (!directories.isEmpty()) {
                String dir = (String) directories.pop();
                File f = null;
                if (baseDir != null) {
                    f = new File(baseDir, dir);
                } else {
                    f = new File(dir);
                }
                zipDir(f, zOut, prefix + dir, dirMode);
            }
        }
    }

    /**
     * Do any clean up necessary to allow this instance to be used again.
     *
     * <p>When we get here, the Zip file has been closed and all we
     * need to do is to reset some globals.</p>
     *
     * <p>This method will only reset globals that have been changed
     * during execute(), it will not alter the attributes or nested
     * child elements.  If you want to reset the instance so that you
     * can later zip a completely different set of files, you must use
     * the reset method.</p>
     *
     * @see #reset
     */
    protected void cleanUp() {
        addedDirs.clear();
        addedFiles.removeAllElements();
        entries.clear();
        addingNewFiles = false;
        doUpdate = savedDoUpdate;
        Enumeration e = filesetsFromGroupfilesets.elements();
        while (e.hasMoreElements()) {
            ZipFileSet zf = (ZipFileSet) e.nextElement();
            resources.removeElement(zf);
        }
        filesetsFromGroupfilesets.removeAllElements();
    }

    /**
     * Makes this instance reset all attributes to their default
     * values and forget all children.
     *
     * @since Ant 1.5
     *
     * @see #cleanUp
     */
    public void reset() {
        resources.removeAllElements();
        zipFile = null;
        baseDir = null;
        groupfilesets.removeAllElements();
        duplicate = "add";
        archiveType = "zip";
        doCompress = true;
        emptyBehavior = "skip";
        doUpdate = false;
        doFilesonly = false;
        encoding = null;
    }

    /**
     * Check is the resource arrays are empty.
     * @param r the arrays to check
     * @return true if all individual arrays are empty
     *
     * @since Ant 1.5.2
     */
    protected static final boolean isEmpty(Resource[][] r) {
        for (int i = 0; i < r.length; i++) {
            if (r[i].length > 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Drops all non-file resources from the given array.
     * @param orig the resources to filter
     * @return the filters resources
     * @since Ant 1.6
     */
    protected Resource[] selectFileResources(Resource[] orig) {
        if (orig.length == 0) {
            return orig;
        }

        Vector v = new Vector(orig.length);
        for (int i = 0; i < orig.length; i++) {
            if (!orig[i].isDirectory()) {
                v.addElement(orig[i]);
            } else {
                log("Ignoring directory " + orig[i].getName()
                    + " as only files will be added.", Project.MSG_VERBOSE);
            }
        }

        if (v.size() != orig.length) {
            Resource[] r = new Resource[v.size()];
            v.copyInto(r);
            return r;
        }
        return orig;
    }

    /**
     * Possible behaviors when a duplicate file is added:
     * "add", "preserve" or "fail"
     */
    public static class Duplicate extends EnumeratedAttribute {
        /**
         * @see EnumeratedAttribute#getValues()
         */
        /** {@inheritDoc} */
        public String[] getValues() {
            return new String[] {"add", "preserve", "fail"};
        }
    }

    /**
     * Holds the up-to-date status and the out-of-date resources of
     * the original archive.
     *
     * @since Ant 1.5.3
     */
    public static class ArchiveState {
        private boolean outOfDate;
        private Resource[][] resourcesToAdd;

        ArchiveState(boolean state, Resource[][] r) {
            outOfDate = state;
            resourcesToAdd = r;
        }

        /**
         * Return the outofdate status.
         * @return the outofdate status
         */
        public boolean isOutOfDate() {
            return outOfDate;
        }

        /**
         * Get the resources to add.
         * @return the resources to add
         */
        public Resource[][] getResourcesToAdd() {
            return resourcesToAdd;
        }
        /**
         * find out if there are absolutely no resources to add
         * @since Ant 1.6.3
         * @return true if there are no resources to add
         */
        public boolean isWithoutAnyResources() {
            if (resourcesToAdd == null)  {
                return true;
            }
            for (int counter = 0; counter < resourcesToAdd.length; counter++) {
                if (resourcesToAdd[counter] != null) {
                    if (resourcesToAdd[counter].length > 0) {
                        return false;
                    }
                }
            }
            return true;
        }
    }
}
