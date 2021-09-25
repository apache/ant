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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.Vector;
import java.util.stream.Stream;
import java.util.zip.CRC32;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.FileScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.ArchiveFileSet;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.ZipFileSet;
import org.apache.tools.ant.types.ZipScanner;
import org.apache.tools.ant.types.resources.ArchiveResource;
import org.apache.tools.ant.types.resources.FileProvider;
import org.apache.tools.ant.types.resources.FileResource;
import org.apache.tools.ant.types.resources.Union;
import org.apache.tools.ant.types.resources.ZipResource;
import org.apache.tools.ant.types.resources.selectors.ResourceSelector;
import org.apache.tools.ant.util.DateUtils;
import org.apache.tools.ant.util.FileNameMapper;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.GlobPatternMapper;
import org.apache.tools.ant.util.IdentityMapper;
import org.apache.tools.ant.util.MergingMapper;
import org.apache.tools.ant.util.ResourceUtils;
import org.apache.tools.zip.UnixStat;
import org.apache.tools.zip.Zip64Mode;
import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipExtraField;
import org.apache.tools.zip.ZipFile;
import org.apache.tools.zip.ZipOutputStream;
import org.apache.tools.zip.ZipOutputStream.UnicodeExtraFieldPolicy;

/**
 * Create a Zip file.
 *
 * @since Ant 1.1
 *
 * @ant.task category="packaging"
 */
public class Zip extends MatchingTask {
    private static final int BUFFER_SIZE = 8 * 1024;
    /**
     * The granularity of timestamps inside a ZIP archive.
     */
    private static final int ZIP_FILE_TIMESTAMP_GRANULARITY = 2000;
    private static final int ROUNDUP_MILLIS = ZIP_FILE_TIMESTAMP_GRANULARITY - 1;
    // CheckStyle:VisibilityModifier OFF - bc

    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    // For directories:
    private static final long EMPTY_CRC = new CRC32().getValue();

    private static final ResourceSelector MISSING_SELECTOR =
            target -> !target.isExists();

    private static final ResourceUtils.ResourceSelectorProvider
        MISSING_DIR_PROVIDER = sr -> MISSING_SELECTOR;

    protected File zipFile;
    // use to scan own archive
    private ZipScanner zs;
    private File baseDir;
    protected Hashtable<String, String> entries = new Hashtable<>();
    private final List<FileSet> groupfilesets = new Vector<>();
    private final List<ZipFileSet> filesetsFromGroupfilesets = new Vector<>();
    protected String duplicate = "add";
    private boolean doCompress = true;
    private boolean doUpdate = false;
    // shadow of the above if the value is altered in execute
    private boolean savedDoUpdate = false;
    private boolean doFilesonly = false;
    protected String archiveType = "zip";

    protected String emptyBehavior = "skip";
    private final List<ResourceCollection> resources = new Vector<>();
    protected Hashtable<String, String> addedDirs = new Hashtable<>();
    private final List<String> addedFiles = new Vector<>();

    private String fixedModTime = null; // User-provided.
    private long modTimeMillis = 0; // Calculated.

    /**
     * If this flag is true, execute() will run most operations twice,
     * the first time with {@link #skipWriting skipWriting} set to
     * true and the second time with setting it to false.
     *
     * <p>The only situation in Ant's current code base where this is
     * ever going to be true is if the jar task has been configured
     * with a filesetmanifest other than "skip".</p>
     */
    protected boolean doubleFilePass = false;
    /**
     * whether the methods should just perform some sort of dry-run.
     *
     * <p>Will only ever be true in the first pass if the task
     * performs two passes because {@link #doubleFilePass
     * doubleFilePass} is true.</p>
     */
    protected boolean skipWriting = false;

    /**
     * Whether this is the first time the archive building methods are invoked.
     *
     * @return true if either {@link #doubleFilePass doubleFilePass}
     * is false or {@link #skipWriting skipWriting} is true.
     *
     * @since Ant 1.8.0
     */
    protected final boolean isFirstPass() {
        return !doubleFilePass || skipWriting;
    }

    // CheckStyle:VisibilityModifier ON

    // This boolean is set if the task detects that the
    // target is outofdate and has written to the target file.
    private boolean updatedFile = false;

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
     * Assume 0 Unix mode is intentional.
     * @since Ant 1.8.0
     */
    private boolean preserve0Permissions = false;

    /**
     * Whether to set the language encoding flag when creating the archive.
     *
     * @since Ant 1.8.0
     */
    private boolean useLanguageEncodingFlag = true;

    /**
     * Whether to add unicode extra fields.
     *
     * @since Ant 1.8.0
     */
    private UnicodeExtraField createUnicodeExtraFields =
        UnicodeExtraField.NEVER;

    /**
     * Whether to fall back to UTF-8 if a name cannot be encoded using
     * the specified encoding.
     *
     * @since Ant 1.8.0
     */
    private boolean fallBackToUTF8 = false;

    /**
     * Whether to enable Zip64 extensions.
     *
     * @since Ant 1.9.1
     */
    private Zip64ModeAttribute zip64Mode = Zip64ModeAttribute.AS_NEEDED;

    /**
     * This is the name/location of where to
     * create the .zip file.
     * @param zipFile the path of the zipFile
     * @deprecated since 1.5.x.
     *             Use setDestFile(File) instead.
     * @ant.attribute ignore="true"
     */
    @Deprecated
    public void setZipfile(final File zipFile) {
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
    @Deprecated
    public void setFile(final File file) {
        setDestFile(file);
    }


    /**
     * The file to create; required.
     * @since Ant 1.5
     * @param destFile The new destination File
     */
    public void setDestFile(final File destFile) {
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
    public void setBasedir(final File baseDir) {
        this.baseDir = baseDir;
    }

    /**
     * Whether we want to compress the files or only store them;
     * optional, default=true;
     * @param c if true, compress the files
     */
    public void setCompress(final boolean c) {
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
    public void setFilesonly(final boolean f) {
        doFilesonly = f;
    }

    /**
     * If true, updates an existing file, otherwise overwrite
     * any existing one; optional defaults to false.
     * @param c if true, updates an existing zip file
     */
    public void setUpdate(final boolean c) {
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
    public void addFileset(final FileSet set) {
        add(set);
    }

    /**
     * Adds a set of files that can be
     * read from an archive and be given a prefix/fullpath.
     * @param set the zipfileset to add
     */
    public void addZipfileset(final ZipFileSet set) {
        add(set);
    }

    /**
     * Add a collection of resources to be archived.
     * @param a the resources to archive
     * @since Ant 1.7
     */
    public void add(final ResourceCollection a) {
        resources.add(a);
    }

    /**
     * Adds a group of zip files.
     * @param set the group (a fileset) to add
     */
    public void addZipGroupFileset(final FileSet set) {
        groupfilesets.add(set);
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
    public void setDuplicate(final Duplicate df) {
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
        @Override
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
    public void setWhenempty(final WhenEmpty we) {
        emptyBehavior = we.getValue();
    }

    /**
     * Encoding to use for filenames, defaults to the platform's
     * default encoding.
     *
     * <p>For a list of possible values see <a
     * href="https://docs.oracle.com/javase/8/docs/technotes/guides/intl/encoding.doc.html">https://docs.oracle.com/javase/8/docs/technotes/guides/intl/encoding.doc.html</a>.</p>
     * @param encoding the encoding name
     */
    public void setEncoding(final String encoding) {
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
    public void setKeepCompression(final boolean keep) {
        keepCompression = keep;
    }

    /**
     * Comment to use for archive.
     *
     * @param comment The content of the comment.
     * @since Ant 1.6.3
     */
    public void setComment(final String comment) {
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
    public void setLevel(final int level) {
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
    public void setRoundUp(final boolean r) {
        roundUp = r;
    }

    /**
     * Assume 0 Unix mode is intentional.
     * @param b boolean
     * @since Ant 1.8.0
     */
    public void setPreserve0Permissions(final boolean b) {
        preserve0Permissions = b;
    }

    /**
     * Assume 0 Unix mode is intentional.
     * @return boolean
     * @since Ant 1.8.0
     */
    public boolean getPreserve0Permissions() {
        return preserve0Permissions;
    }

    /**
     * Whether to set the language encoding flag.
     * @param b boolean
     * @since Ant 1.8.0
     */
    public void setUseLanguageEncodingFlag(final boolean b) {
        useLanguageEncodingFlag = b;
    }

    /**
     * Whether the language encoding flag will be used.
     * @return boolean
     * @since Ant 1.8.0
     */
    public boolean getUseLanguageEnodingFlag() {
        return useLanguageEncodingFlag;
    }

    /**
     * Whether Unicode extra fields will be created.
     * @param b boolean
     * @since Ant 1.8.0
     */
    public void setCreateUnicodeExtraFields(final UnicodeExtraField b) {
        createUnicodeExtraFields = b;
    }

    /**
     * Whether Unicode extra fields will be created.
     * @return boolean
     * @since Ant 1.8.0
     */
    public UnicodeExtraField getCreateUnicodeExtraFields() {
        return createUnicodeExtraFields;
    }

    /**
     * Whether to fall back to UTF-8 if a name cannot be encoded using
     * the specified encoding.
     *
     * <p>Defaults to false.</p>
     *
     * @param b boolean
     * @since Ant 1.8.0
     */
    public void setFallBackToUTF8(final boolean b) {
        fallBackToUTF8 = b;
    }

    /**
     * Whether to fall back to UTF-8 if a name cannot be encoded using
     * the specified encoding.
     *
     * @return boolean
     * @since Ant 1.8.0
     */
    public boolean getFallBackToUTF8() {
        return fallBackToUTF8;
    }

    /**
     * Whether Zip64 extensions should be used.
     * @param b boolean
     * @since Ant 1.9.1
     */
    public void setZip64Mode(final Zip64ModeAttribute b) {
        zip64Mode = b;
    }

    /**
     * Whether Zip64 extensions will be used.
     * @return boolean
     * @since Ant 1.9.1
     */
    public Zip64ModeAttribute getZip64Mode() {
        return zip64Mode;
    }

    /**
     * Set all stored file modification times to {@code time}.
     * @param time Milliseconds since 1970-01-01 00:00, or
     *        <code>YYYY-MM-DD{T/ }HH:MM[:SS[.SSS]][ ][&plusmn;ZZ[[:]ZZ]]</code>, or
     *        <code>MM/DD/YYYY HH:MM[:SS] {AM/PM}</code>, where {a/b} indicates
     *        that you must choose one of a or b, and [c] indicates that you
     *        may use or omit c. &plusmn;ZZZZ is the timezone offset, and may be
     *        literally "Z" to mean GMT.
     * @since Ant 1.10.2
     */
    public void setModificationtime(String time) {
        fixedModTime = time;
    }

    /**
     * The file modification time previously provided to
     * {@link #setModificationtime(String)} or {@code null} if unset.
     * @return String
     * @since Ant 1.10.2
     */
    public String getModificationtime() {
        return fixedModTime;
    }

    /**
     * validate and build
     * @throws BuildException on error
     */
    @Override
    public void execute() throws BuildException {

        if (doubleFilePass) {
            skipWriting = true;
            executeMain();
            skipWriting = false;
        }
        executeMain();
    }

    /**
     * Get the value of the updatedFile attribute.
     * This should only be called after executeMain has been
     * called.
     * @return true if executeMain has written to the zip file.
     */
    protected boolean hasUpdatedFile() {
        return updatedFile;
    }

    /**
     * Build the zip file.
     * This is called twice if doubleFilePass is true.
     * @throws BuildException on error
     */
    public void executeMain() throws BuildException {

        checkAttributesAndElements();

        // Renamed version of original file, if it exists
        File renamedFile = null;
        addingNewFiles = true;

        processDoUpdate();
        processGroupFilesets();

        // collect filesets to pass them to getResourcesToAdd
        final List<ResourceCollection> vfss = new ArrayList<>();
        if (baseDir != null) {
            final FileSet fs = (FileSet) getImplicitFileSet().clone();
            fs.setDir(baseDir);
            vfss.add(fs);
        }
        vfss.addAll(resources);

        final ResourceCollection[] fss =
            vfss.toArray(new ResourceCollection[0]);

        boolean success = false;
        try {
            // can also handle empty archives
            final ArchiveState state = getResourcesToAdd(fss, zipFile, false);

            // quick exit if the target is up to date
            if (!state.isOutOfDate()) {
                return;
            }

            final File parent = zipFile.getParentFile();
            if (parent != null && !parent.isDirectory()
                && !(parent.mkdirs() || parent.isDirectory())) {
                throw new BuildException(
                    "Failed to create missing parent directory for %s",
                    zipFile);
            }

            updatedFile = true;
            if (!zipFile.exists() && state.isWithoutAnyResources()) {
                createEmptyZip(zipFile);
                return;
            }
            final Resource[][] addThem = state.getResourcesToAdd();

            if (doUpdate) {
                renamedFile = renameFile();
            }

            final String action = doUpdate ? "Updating " : "Building ";

            if (!skipWriting) {
                log(action + archiveType + ": " + zipFile.getAbsolutePath());
            }

            ZipOutputStream zOut = null;
            try {
                if (!skipWriting) {
                    zOut = new ZipOutputStream(zipFile);

                    zOut.setEncoding(encoding);
                    zOut.setUseLanguageEncodingFlag(useLanguageEncodingFlag);
                    zOut.setCreateUnicodeExtraFields(createUnicodeExtraFields.
                                                     getPolicy());
                    zOut.setFallbackToUTF8(fallBackToUTF8);
                    zOut.setMethod(doCompress
                        ? ZipOutputStream.DEFLATED : ZipOutputStream.STORED);
                    zOut.setLevel(level);
                    zOut.setUseZip64(zip64Mode.getMode());
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
                    final ZipFileSet oldFiles = new ZipFileSet();
                    oldFiles.setProject(getProject());
                    oldFiles.setSrc(renamedFile);
                    oldFiles.setDefaultexcludes(false);

                    for (String addedFile : addedFiles) {
                        oldFiles.createExclude().setName(addedFile);
                    }
                    final DirectoryScanner ds =
                        oldFiles.getDirectoryScanner(getProject());
                    ((ZipScanner) ds).setEncoding(encoding);

                    Stream<String> includedResourceNames =
                        Stream.of(ds.getIncludedFiles());

                    if (!doFilesonly) {
                        includedResourceNames =
                            Stream.concat(includedResourceNames,
                                Stream.of(ds.getIncludedDirectories()));
                    }

                    Resource[] r = includedResourceNames.map(ds::getResource)
                        .toArray(Resource[]::new);

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
                        log("Warning: unable to delete temporary file "
                            + renamedFile.getName(), Project.MSG_WARN);
                    }
                }
                success = true;
            } finally {
                // Close the output stream.
                closeZout(zOut, success);
            }
        } catch (final IOException ioe) {
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
                } catch (final IOException e) {
                    msg += " (and I couldn't rename the temporary file "
                            + renamedFile.getName() + " back)";
                }
            }

            throw new BuildException(msg, ioe, getLocation());
        } finally {
            cleanUp();
        }
    }

    /** rename the zip file. */
    private File renameFile() {
        final File renamedFile = FILE_UTILS.createTempFile(
            getProject(), "zip", ".tmp", zipFile.getParentFile(), true, false);
        try {
            FILE_UTILS.rename(zipFile, renamedFile);
        } catch (final SecurityException | IOException e) {
            throw new BuildException(
                "Unable to rename old file (%s) to temporary file",
                zipFile.getAbsolutePath());
        }
        return renamedFile;
    }

    /** Close zout */
    private void closeZout(final ZipOutputStream zOut, final boolean success)
        throws IOException {
        if (zOut == null) {
            return;
        }
        try {
            zOut.close();
        } catch (final IOException ex) {
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

    /** Check the attributes and elements */
    private void checkAttributesAndElements() {
        if (baseDir == null && resources.isEmpty() && groupfilesets.isEmpty()
            && "zip".equals(archiveType)) {
            throw new BuildException(
                "basedir attribute must be set, or at least one resource collection must be given!");
        }

        if (zipFile == null) {
            throw new BuildException("You must specify the %s file to create!",
                archiveType);
        }

        if (fixedModTime != null) {
            try {
                modTimeMillis = DateUtils.parseLenientDateTime(fixedModTime).getTime();
            } catch (ParseException pe) {
                throw new BuildException("Failed to parse date string %s.", fixedModTime);
            }
            if (roundUp) {
                modTimeMillis += ROUNDUP_MILLIS;
            }
        }

        if (zipFile.exists() && !zipFile.isFile()) {
            throw new BuildException("%s is not a file.", zipFile);
        }

        if (zipFile.exists() && !zipFile.canWrite()) {
            throw new BuildException("%s is read-only.", zipFile);
        }
    }

    /** Process doupdate */
    private void processDoUpdate() {
        // Whether or not an actual update is required -
        // we don't need to update if the original file doesn't exist
        if (doUpdate && !zipFile.exists()) {
            doUpdate = false;
            logWhenWriting("ignoring update attribute as " + archiveType
                           + " doesn't exist.", Project.MSG_DEBUG);
        }
    }

    /** Process groupfilesets */
    private void processGroupFilesets() {
        // Add the files found in groupfileset to fileset
        for (FileSet fs : groupfilesets) {
            logWhenWriting("Processing groupfileset ", Project.MSG_VERBOSE);
            final FileScanner scanner = fs.getDirectoryScanner(getProject());
            final File basedir = scanner.getBasedir();
            for (String file : scanner.getIncludedFiles()) {
                logWhenWriting("Adding file " + file + " to fileset",
                               Project.MSG_VERBOSE);
                final ZipFileSet zf = new ZipFileSet();
                zf.setProject(getProject());
                zf.setSrc(new File(basedir, file));
                add(zf);
                filesetsFromGroupfilesets.add(zf);
            }
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
    protected final void addResources(final FileSet fileset, final Resource[] resources,
                                      final ZipOutputStream zOut)
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

        if (!prefix.isEmpty() && !fullpath.isEmpty()) {
            throw new BuildException(
                "Both prefix and fullpath attributes must not be set on the same fileset.");
        }

        if (resources.length != 1 && !fullpath.isEmpty()) {
            throw new BuildException(
                "fullpath attribute may only be specified for filesets that specify a single file.");
        }

        if (!prefix.isEmpty()) {
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

            for (Resource resource : resources) {
                String name;
                if (fullpath.isEmpty()) {
                    name = resource.getName();
                } else {
                    name = fullpath;
                }
                name = name.replace(File.separatorChar, '/');

                if (name.isEmpty()) {
                    continue;
                }

                if (resource.isDirectory()) {
                    if (doFilesonly) {
                        continue;
                    }
                    final int thisDirMode = zfs != null && zfs.hasDirModeBeenSet()
                        ? dirMode : getUnixMode(resource, zf, dirMode);
                    addDirectoryResource(resource, name, prefix,
                                         base, zOut,
                                         dirMode, thisDirMode);

                } else { // !isDirectory

                    addParentDirs(base, name, zOut, prefix, dirMode);

                    if (dealingWithFiles) {
                        final File f = FILE_UTILS.resolveFile(base,
                                                        resource.getName());
                        zipFile(f, zOut, prefix + name, fileMode);
                    } else {
                        final int thisFileMode =
                            zfs != null && zfs.hasFileModeBeenSet()
                            ? fileMode : getUnixMode(resource, zf,
                                                     fileMode);
                        addResource(resource, name, prefix,
                                    zOut, thisFileMode, zf,
                                    zfs == null
                                    ? null : zfs.getSrc(getProject()));
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
     * Add a directory entry to the archive using a specified
     * Unix-mode and the default mode for its parent directories (if
     * necessary).
     */
    private void addDirectoryResource(final Resource r, String name, final String prefix,
                                      final File base, final ZipOutputStream zOut,
                                      final int defaultDirMode, final int thisDirMode)
        throws IOException {

        if (!name.endsWith("/")) {
            name += "/";
        }

        final int nextToLastSlash = name.lastIndexOf('/', name.length() - 2);
        if (nextToLastSlash != -1) {
            addParentDirs(base, name.substring(0, nextToLastSlash + 1),
                          zOut, prefix, defaultDirMode);
        }
        zipDir(r, zOut, prefix + name, thisDirMode,
               r instanceof ZipResource
               ? ((ZipResource) r).getExtraFields() : null);
    }

    /**
     * Determine a Resource's Unix mode or return the given default
     * value if not available.
     */
    private int getUnixMode(final Resource r, final ZipFile zf, final int defaultMode) {

        int unixMode = defaultMode;
        if (zf != null) {
            final ZipEntry ze = zf.getEntry(r.getName());
            unixMode = ze.getUnixMode();
            if ((unixMode == 0 || unixMode == UnixStat.DIR_FLAG)
                && !preserve0Permissions) {
                unixMode = defaultMode;
            }
        } else if (r instanceof ArchiveResource) {
            unixMode = ((ArchiveResource) r).getMode();
        }
        return unixMode;
    }

    /**
     * Add a file entry.
     */
    private void addResource(final Resource r, final String name, final String prefix,
                             final ZipOutputStream zOut, final int mode,
                             final ZipFile zf, final File fromArchive)
        throws IOException {

        if (zf != null) {
            final ZipEntry ze = zf.getEntry(r.getName());

            if (ze != null) {
                final boolean oldCompress = doCompress;
                if (keepCompression) {
                    doCompress = (ze.getMethod() == ZipEntry.DEFLATED);
                }
                try (final BufferedInputStream is = new BufferedInputStream(zf.getInputStream(ze))) {
                    zipFile(is, zOut, prefix + name, ze.getTime(),
                            fromArchive, mode, ze.getExtraFields(true));
                } finally {
                    doCompress = oldCompress;
                }
            }
        } else {
            try (final BufferedInputStream is = new BufferedInputStream(r.getInputStream())) {
                zipFile(is, zOut, prefix + name, r.getLastModified(),
                        fromArchive, mode, r instanceof ZipResource
                        ? ((ZipResource) r).getExtraFields() : null);
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
    protected final void addResources(final ResourceCollection rc,
                                      final Resource[] resources,
                                      final ZipOutputStream zOut)
        throws IOException {
        if (rc instanceof FileSet) {
            addResources((FileSet) rc, resources, zOut);
            return;
        }
        for (final Resource resource : resources) {
            String name = resource.getName();
            if (name == null) {
                continue;
            }
            name = name.replace(File.separatorChar, '/');

            if (name.isEmpty()) {
                continue;
            }
            if (resource.isDirectory() && doFilesonly) {
                continue;
            }
            File base = null;
            final FileProvider fp = resource.as(FileProvider.class);
            if (fp != null) {
                base = ResourceUtils.asFileResource(fp).getBaseDir();
            }

            if (resource.isDirectory()) {
                addDirectoryResource(resource, name, "", base, zOut,
                                     ArchiveFileSet.DEFAULT_DIR_MODE,
                                     ArchiveFileSet.DEFAULT_DIR_MODE);

            } else {
                addParentDirs(base, name, zOut, "",
                              ArchiveFileSet.DEFAULT_DIR_MODE);

                if (fp != null) {
                    final File f = (fp).getFile();
                    zipFile(f, zOut, name, ArchiveFileSet.DEFAULT_FILE_MODE);
                } else {
                    addResource(resource, name, "", zOut,
                                ArchiveFileSet.DEFAULT_FILE_MODE,
                                null, null);
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
    protected void initZipOutputStream(final ZipOutputStream zOut)
        throws IOException, BuildException {
    }

    /**
     * method for subclasses to override
     * @param zOut the zip output stream
     * @throws IOException on output error
     * @throws BuildException on other errors
     */
    protected void finalizeZipOutputStream(final ZipOutputStream zOut)
        throws IOException, BuildException {
    }

    /**
     * Create an empty zip file
     * @param zipFile the zip file
     * @return true for historic reasons
     * @throws BuildException on error
     */
    protected boolean createEmptyZip(final File zipFile) throws BuildException {
        // In this case using java.util.zip will not work
        // because it does not permit a zero-entry archive.
        // Must create it manually.
        if (!skipWriting) {
            log("Note: creating empty " + archiveType + " archive " + zipFile,
                Project.MSG_INFO);
        }
        try (OutputStream os = Files.newOutputStream(zipFile.toPath())) {
            // CheckStyle:MagicNumber OFF
            // Cf. PKZIP specification.
            final byte[] empty = new byte[22];
            empty[0] = 80; // P
            empty[1] = 75; // K
            empty[2] = 5;
            empty[3] = 6;
            // remainder zeros
            // CheckStyle:MagicNumber ON
            os.write(empty);
        } catch (final IOException ioe) {
            throw new BuildException("Could not create empty ZIP archive "
                                     + "(" + ioe.getMessage() + ")", ioe,
                                     getLocation());
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
     * <p>This method first delegates to getNonFileSetResourcesToAdd
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
    protected ArchiveState getResourcesToAdd(final ResourceCollection[] rcs,
                                             final File zipFile,
                                             final boolean needsUpdate)
        throws BuildException {
        final List<FileSet> filesets = new ArrayList<>();
        final List<ResourceCollection> rest = new ArrayList<>();
        for (ResourceCollection rc : rcs) {
            if (rc instanceof FileSet) {
                filesets.add((FileSet) rc);
            } else {
                rest.add(rc);
            }
        }
        final ResourceCollection[] rc =
            rest.toArray(new ResourceCollection[0]);
        ArchiveState as = getNonFileSetResourcesToAdd(rc, zipFile,
                                                      needsUpdate);

        final FileSet[] fs = filesets.toArray(new FileSet[0]);
        final ArchiveState as2 = getResourcesToAdd(fs, zipFile, as.isOutOfDate());
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

        final Resource[][] toAdd = new Resource[rcs.length][];
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

    /*
     * This is yet another hacky construct to extend the FileSet[]
     * getResourcesToAdd method so we can pass the information whether
     * non-fileset resources have been available to it without having
     * to move the withEmpty behavior checks (since either would break
     * subclasses in several ways).
     */
    private static final ThreadLocal<Boolean> HAVE_NON_FILE_SET_RESOURCES_TO_ADD = ThreadLocal.withInitial(() -> Boolean.FALSE);

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
    protected ArchiveState getResourcesToAdd(final FileSet[] filesets,
                                             final File zipFile,
                                             boolean needsUpdate)
        throws BuildException {

        final Resource[][] initialResources = grabResources(filesets);
        if (isEmpty(initialResources)) {
            if (Boolean.FALSE.equals(HAVE_NON_FILE_SET_RESOURCES_TO_ADD.get())) {
                if (needsUpdate && doUpdate) {
                    /*
                     * This is a rather hairy case.
                     *
                     * One of our subclasses knows that we need to
                     * update the archive, but at the same time, there
                     * are no resources known to us that would need to
                     * be added.  Only the subclass seems to know
                     * what's going on.
                     *
                     * This happens if <jar> detects that the manifest
                     * has changed, for example.  The manifest is not
                     * part of any resources because of our support
                     * for inline <manifest>s.
                     *
                     * If we invoke createEmptyZip like Ant 1.5.2 did,
                     * we'll loose all stuff that has been in the
                     * original archive (bugzilla report 17780).
                     */
                    return new ArchiveState(true, initialResources);
                }

                if ("skip".equals(emptyBehavior)) {
                    if (doUpdate) {
                        logWhenWriting(archiveType + " archive " + zipFile
                                       + " not updated because no new files were"
                                       + " included.", Project.MSG_VERBOSE);
                    } else {
                        logWhenWriting("Warning: skipping " + archiveType
                                       + " archive " + zipFile
                                       + " because no files were included.",
                                       Project.MSG_WARN);
                    }
                } else if ("fail".equals(emptyBehavior)) {
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
            }

            // either there are non-fileset resources or we
            // (re-)create the archive anyway
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

        final Resource[][] newerResources = new Resource[filesets.length][];

        for (int i = 0; i < filesets.length; i++) {
            if (!(fileset instanceof ZipFileSet)
                || ((ZipFileSet) fileset).getSrc(getProject()) == null) {
                final File base = filesets[i].getDir(getProject());

                for (int j = 0; j < initialResources[i].length; j++) {
                    final File resourceAsFile =
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
                final ZipFileSet zfs = (ZipFileSet) filesets[i];
                if (zfs.getFullpath(getProject()) != null
                    && !zfs.getFullpath(getProject()).isEmpty()) {
                    // in this case all files from origin map to
                    // the fullPath attribute of the zipfileset at
                    // destination
                    final MergingMapper fm = new MergingMapper();
                    fm.setTo(zfs.getFullpath(getProject()));
                    myMapper = fm;

                } else if (zfs.getPrefix(getProject()) != null
                           && !zfs.getPrefix(getProject()).isEmpty()) {
                    final GlobPatternMapper gm = new GlobPatternMapper();
                    gm.setFrom("*");
                    String prefix = zfs.getPrefix(getProject());
                    if (!prefix.endsWith("/") && !prefix.endsWith("\\")) {
                        prefix += "/";
                    }
                    gm.setTo(prefix + "*");
                    myMapper = gm;
                }
            }

            newerResources[i] = selectOutOfDateResources(initialResources[i],
                                                         myMapper);
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
    protected ArchiveState getNonFileSetResourcesToAdd(final ResourceCollection[] rcs,
                                                       final File zipFile,
                                                       boolean needsUpdate)
        throws BuildException {
        /*
         * Backwards compatibility forces us to repeat the logic of
         * getResourcesToAdd(FileSet[], ...) here once again.
         */

        final Resource[][] initialResources = grabNonFileSetResources(rcs);
        final boolean empty = isEmpty(initialResources);
        HAVE_NON_FILE_SET_RESOURCES_TO_ADD.set(!empty);
        if (empty) {
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

        final Resource[][] newerResources = new Resource[rcs.length][];

        for (int i = 0; i < rcs.length; i++) {
            if (initialResources[i].length == 0) {
                newerResources[i] = new Resource[] {};
                continue;
            }

            for (int j = 0; j < initialResources[i].length; j++) {
                final FileProvider fp =
                    initialResources[i][j].as(FileProvider.class);
                if (fp != null && zipFile.equals(fp.getFile())) {
                    throw new BuildException("A zip file cannot include itself",
                        getLocation());
                }
            }

            newerResources[i] = selectOutOfDateResources(initialResources[i],
                                                         new IdentityMapper());
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

    private Resource[] selectOutOfDateResources(final Resource[] initial,
                                                final FileNameMapper mapper) {
        final Resource[] rs = selectFileResources(initial);
        Resource[] result =
            ResourceUtils.selectOutOfDateSources(this, rs, mapper,
                                                 getZipScanner(),
                                                 ZIP_FILE_TIMESTAMP_GRANULARITY);
        if (!doFilesonly) {
            final Union u = new Union();
            u.addAll(Arrays.asList(selectDirectoryResources(initial)));
            final ResourceCollection rc =
                ResourceUtils.selectSources(this, u, mapper,
                                            getZipScanner(),
                                            MISSING_DIR_PROVIDER);
            if (!rc.isEmpty()) {
                final List<Resource> newer = new ArrayList<>();
                newer.addAll(Arrays.asList(((Union) rc).listResources()));
                newer.addAll(Arrays.asList(result));
                result = newer.toArray(result);
            }
        }
        return result;
    }

    /**
     * Fetch all included and not excluded resources from the sets.
     *
     * <p>Included directories will precede included files.</p>
     * @param filesets an array of filesets
     * @return the resources included
     * @since Ant 1.5.2
     */
    protected Resource[][] grabResources(final FileSet[] filesets) {
        final Resource[][] result = new Resource[filesets.length][];
        for (int i = 0; i < filesets.length; i++) {
            boolean skipEmptyNames = true;
            if (filesets[i] instanceof ZipFileSet) {
                final ZipFileSet zfs = (ZipFileSet) filesets[i];
                skipEmptyNames = zfs.getPrefix(getProject()).isEmpty()
                    && zfs.getFullpath(getProject()).isEmpty();
            }
            final DirectoryScanner rs =
                filesets[i].getDirectoryScanner(getProject());
            if (rs instanceof ZipScanner) {
                ((ZipScanner) rs).setEncoding(encoding);
            }
            final List<Resource> resources = new Vector<>();
            if (!doFilesonly) {
                for (String d : rs.getIncludedDirectories()) {
                    if (!d.isEmpty() || !skipEmptyNames) {
                        resources.add(rs.getResource(d));
                    }
                }
            }
            for (String f : rs.getIncludedFiles()) {
                if (!f.isEmpty() || !skipEmptyNames) {
                    resources.add(rs.getResource(f));
                }
            }
            result[i] = resources.toArray(new Resource[0]);
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
    protected Resource[][] grabNonFileSetResources(final ResourceCollection[] rcs) {
        final Resource[][] result = new Resource[rcs.length][];
        for (int i = 0; i < rcs.length; i++) {
            final List<Resource> dirs = new ArrayList<>();
            final List<Resource> files = new ArrayList<>();
            for (final Resource r : rcs[i]) {
                if (r.isDirectory()) {
                    dirs.add(r);
                } else if (r.isExists()) {
                    files.add(r);
                }
            }
            // make sure directories are in alpha-order - this also
            // ensures parents come before their children
            dirs.sort(Comparator.comparing(Resource::getName));
            final List<Resource> rs = new ArrayList<>(dirs);
            rs.addAll(files);
            result[i] = rs.toArray(new Resource[0]);
        }
        return result;
    }

    /**
     * Add a directory to the zip stream.
     * @param dir  the directory to add to the archive
     * @param zOut the stream to write to
     * @param vPath the name this entry shall have in the archive
     * @param mode the Unix permissions to set.
     * @throws IOException on error
     * @since Ant 1.5.2
     */
    protected void zipDir(final File dir, final ZipOutputStream zOut, final String vPath,
                          final int mode)
        throws IOException {
        zipDir(dir, zOut, vPath, mode, null);
    }

    /**
     * Add a directory to the zip stream.
     * @param dir  the directory to add to the archive
     * @param zOut the stream to write to
     * @param vPath the name this entry shall have in the archive
     * @param mode the Unix permissions to set.
     * @param extra ZipExtraFields to add
     * @throws IOException on error
     * @since Ant 1.6.3
     */
    protected void zipDir(final File dir, final ZipOutputStream zOut, final String vPath,
                          final int mode, final ZipExtraField[] extra)
        throws IOException {
        zipDir(dir == null ? null : new FileResource(dir), zOut, vPath, mode,
            extra);
    }

    /**
     * Add a directory to the zip stream.
     * @param dir  the directory to add to the archive
     * @param zOut the stream to write to
     * @param vPath the name this entry shall have in the archive
     * @param mode the Unix permissions to set.
     * @param extra ZipExtraFields to add
     * @throws IOException on error
     * @since Ant 1.8.0
     */
    protected void zipDir(final Resource dir, final ZipOutputStream zOut, final String vPath,
                          final int mode, final ZipExtraField[] extra)
        throws IOException {
        if (doFilesonly) {
            logWhenWriting("skipping directory " + vPath
                           + " for file-only archive",
                           Project.MSG_VERBOSE);
            return;
        }
        if (addedDirs.get(vPath) != null) {
            // don't add directories we've already added.
            // no warning if we try, it is harmless in and of itself
            return;
        }

        logWhenWriting("adding directory " + vPath, Project.MSG_VERBOSE);
        addedDirs.put(vPath, vPath);

        if (!skipWriting) {
            final ZipEntry ze = new ZipEntry(vPath);

            // ZIPs store time with a granularity of 2 seconds, round up
            final int millisToAdd = roundUp ? ROUNDUP_MILLIS : 0;

            if (fixedModTime != null) {
                ze.setTime(modTimeMillis);
            } else if (dir != null && dir.isExists()) {
                ze.setTime(dir.getLastModified() + millisToAdd);
            } else {
                ze.setTime(System.currentTimeMillis() + millisToAdd);
            }
            ze.setSize(0);
            ze.setMethod(ZipEntry.STORED);
            // This is faintly ridiculous:
            ze.setCrc(EMPTY_CRC);
            ze.setUnixMode(mode);

            if (extra != null) {
                ze.setExtraFields(extra);
            }

            zOut.putNextEntry(ze);
        }
    }

    /*
     * This is a hacky construct to extend the zipFile method to
     * support a new parameter (extra fields to preserve) without
     * breaking subclasses that override the old method signature.
     */
    private static final ThreadLocal<ZipExtraField[]> CURRENT_ZIP_EXTRA = new ThreadLocal<>();

    /**
     * Provides the extra fields for the zip entry currently being
     * added to the archive - if any.
     * @return ZipExtraField[]
     * @since Ant 1.8.0
     */
    protected final ZipExtraField[] getCurrentExtraFields() {
        return CURRENT_ZIP_EXTRA.get();
    }

    /**
     * Sets the extra fields for the zip entry currently being
     * added to the archive - if any.
     * @param extra ZipExtraField[]
     * @since Ant 1.8.0
     */
    protected final void setCurrentExtraFields(final ZipExtraField[] extra) {
        CURRENT_ZIP_EXTRA.set(extra);
    }

    /**
     * Adds a new entry to the archive, takes care of duplicates as well.
     *
     * @param in the stream to read data for the entry from.  The
     * caller of the method is responsible for closing the stream.
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
    protected void zipFile(final InputStream in, final ZipOutputStream zOut, final String vPath,
                           final long lastModified, final File fromArchive, final int mode)
        throws IOException {
        // fromArchive is used in subclasses overriding this method

        if (entries.containsKey(vPath)) {

            if ("preserve".equals(duplicate)) {
                logWhenWriting(vPath + " already added, skipping",
                               Project.MSG_INFO);
                return;
            }
            if ("fail".equals(duplicate)) {
                throw new BuildException(
                    "Duplicate file %s was found and the duplicate attribute is 'fail'.",
                    vPath);
            }
            // duplicate equal to add, so we continue
            logWhenWriting("duplicate file " + vPath
                           + " found, adding.", Project.MSG_VERBOSE);
        } else {
            logWhenWriting("adding entry " + vPath, Project.MSG_VERBOSE);
        }

        entries.put(vPath, vPath);

        if (!skipWriting) {
            final ZipEntry ze = new ZipEntry(vPath);
            ze.setTime(fixedModTime != null ? modTimeMillis : lastModified);
            ze.setMethod(doCompress ? ZipEntry.DEFLATED : ZipEntry.STORED);
            // if the input stream doesn't support mark/reset ability, we wrap it in a
            // stream that adds that support.
            // Note: We do *not* close this newly created wrapping input stream, since
            // we don't "own" the underlying input stream that's passed to us and closing
            // that is the responsibility of the caller.
            final InputStream markableInputStream = in.markSupported() ? in : new BufferedInputStream(in);
            /*
             * ZipOutputStream.putNextEntry expects the ZipEntry to
             * know its size and the CRC sum before you start writing
             * the data when using STORED mode - unless it is seekable.
             *
             * This forces us to process the data twice.
             */
            if (!zOut.isSeekable() && !doCompress) {
                long size = 0;
                final CRC32 cal = new CRC32();
                markableInputStream.mark(Integer.MAX_VALUE);
                final byte[] buffer = new byte[BUFFER_SIZE];
                int count = 0;
                do {
                    size += count;
                    cal.update(buffer, 0, count);
                    count = markableInputStream.read(buffer, 0, buffer.length);
                } while (count != -1);
                markableInputStream.reset();
                ze.setSize(size);
                ze.setCrc(cal.getValue());
            }

            ze.setUnixMode(mode);
            final ZipExtraField[] extra = getCurrentExtraFields();
            if (extra != null) {
                ze.setExtraFields(extra);
            }

            zOut.putNextEntry(ze);

            final byte[] buffer = new byte[BUFFER_SIZE];
            int count = 0;
            do {
                if (count != 0) {
                    zOut.write(buffer, 0, count);
                }
                count = markableInputStream.read(buffer, 0, buffer.length);
            } while (count != -1);
        }
        addedFiles.add(vPath);
    }

    /**
     * Adds a new entry to the archive, takes care of duplicates as well.
     *
     * @param in the stream to read data for the entry from.  The
     * caller of the method is responsible for closing the stream.
     * @param zOut the stream to write to.
     * @param vPath the name this entry shall have in the archive.
     * @param lastModified last modification time for the entry.
     * @param fromArchive the original archive we are copying this
     * entry from, will be null if we are not copying from an archive.
     * @param mode the Unix permissions to set.
     * @param extra ZipExtraFields to add
     *
     * @since Ant 1.8.0
     * @throws IOException on error
     */
    protected final void zipFile(final InputStream in, final ZipOutputStream zOut,
                                 final String vPath, final long lastModified,
                                 final File fromArchive, final int mode,
                                 final ZipExtraField[] extra)
        throws IOException {
        try {
            setCurrentExtraFields(extra);
            zipFile(in, zOut, vPath, lastModified, fromArchive, mode);
        } finally {
            setCurrentExtraFields(null);
        }
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
    protected void zipFile(final File file, final ZipOutputStream zOut, final String vPath,
                           final int mode)
        throws IOException {
        if (file.equals(zipFile)) {
            throw new BuildException("A zip file cannot include itself",
                                     getLocation());
        }

        try (final BufferedInputStream bIn = new BufferedInputStream(Files.newInputStream(file.toPath()))) {
            // ZIPs store time with a granularity of 2 seconds, round up
            zipFile(bIn, zOut, vPath,
                    file.lastModified() + (roundUp ? ROUNDUP_MILLIS : 0),
                    null, mode);
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
    protected final void addParentDirs(final File baseDir, final String entry,
                                       final ZipOutputStream zOut, final String prefix,
                                       final int dirMode)
        throws IOException {
        if (!doFilesonly) {
            final Stack<String> directories = new Stack<>();
            int slashPos = entry.length();

            while ((slashPos = entry.lastIndexOf('/', slashPos - 1)) != -1) {
                final String dir = entry.substring(0, slashPos + 1);
                if (addedDirs.get(prefix + dir) != null) {
                    break;
                }
                directories.push(dir);
            }

            while (!directories.isEmpty()) {
                final String dir = directories.pop();
                File f;
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
        addedFiles.clear();
        entries.clear();
        addingNewFiles = false;
        doUpdate = savedDoUpdate;
        resources.removeAll(filesetsFromGroupfilesets);
        filesetsFromGroupfilesets.clear();
        HAVE_NON_FILE_SET_RESOURCES_TO_ADD.set(Boolean.FALSE);
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
        resources.clear();
        zipFile = null;
        baseDir = null;
        groupfilesets.clear();
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
    protected static final boolean isEmpty(final Resource[][] r) {
        for (Resource[] element : r) {
            if (element.length > 0) {
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
    protected Resource[] selectFileResources(final Resource[] orig) {
        return selectResources(orig,
                               r -> {
                                   if (!r.isDirectory()) {
                                       return true;
                                   }
                                   if (doFilesonly) {
                                       logWhenWriting("Ignoring directory "
                                                      + r.getName()
                                                      + " as only files will"
                                                      + " be added.",
                                                      Project.MSG_VERBOSE);
                                   }
                                   return false;
                               });
    }

    /**
     * Drops all non-directory resources from the given array.
     * @param orig the resources to filter
     * @return the filters resources
     * @since Ant 1.8.0
     */
    protected Resource[] selectDirectoryResources(final Resource[] orig) {
        return selectResources(orig, Resource::isDirectory);
    }

    /**
     * Drops all resources from the given array that are not selected
     * @param orig the resources to filter
     * @param selector ResourceSelector
     * @return the filters resources
     * @since Ant 1.8.0
     */
    protected Resource[] selectResources(final Resource[] orig,
                                         final ResourceSelector selector) {
        if (orig.length == 0) {
            return orig;
        }
        Resource[] result = Stream.of(orig).filter(selector::isSelected)
            .toArray(Resource[]::new);
        return result.length == orig.length ? orig : result;
    }

    /**
     * Logs a message at the given output level, but only if this is
     * the pass that will actually create the archive.
     *
     * @param msg String
     * @param level int
     * @since Ant 1.8.0
     */
    protected void logWhenWriting(final String msg, final int level) {
        if (!skipWriting) {
            log(msg, level);
        }
    }

    /**
     * Possible behaviors when a duplicate file is added:
     * "add", "preserve" or "fail"
     */
    public static class Duplicate extends EnumeratedAttribute {
        /**
         * @see EnumeratedAttribute#getValues()
         * {@inheritDoc}
         */
        @Override
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
        private final boolean outOfDate;
        private final Resource[][] resourcesToAdd;

        ArchiveState(final boolean state, final Resource[][] r) {
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
            for (Resource[] element : resourcesToAdd) {
                if (element != null && element.length > 0) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Policy for creation of Unicode extra fields: never, always or
     * not-encodeable.
     *
     * @since Ant 1.8.0
     */
    public static final class UnicodeExtraField extends EnumeratedAttribute {
        private static final Map<String, UnicodeExtraFieldPolicy> POLICIES = new HashMap<>();
        private static final String NEVER_KEY = "never";
        private static final String ALWAYS_KEY = "always";
        private static final String N_E_KEY = "not-encodeable";
        static {
            POLICIES.put(NEVER_KEY,
                         ZipOutputStream.UnicodeExtraFieldPolicy.NEVER);
            POLICIES.put(ALWAYS_KEY,
                         ZipOutputStream.UnicodeExtraFieldPolicy.ALWAYS);
            POLICIES.put(N_E_KEY,
                         ZipOutputStream.UnicodeExtraFieldPolicy
                         .NOT_ENCODEABLE);
        }

        @Override
        public String[] getValues() {
            return new String[] {NEVER_KEY, ALWAYS_KEY, N_E_KEY};
        }

        public static final UnicodeExtraField NEVER =
            new UnicodeExtraField(NEVER_KEY);

        private UnicodeExtraField(final String name) {
            setValue(name);
        }

        public UnicodeExtraField() {
        }

        public ZipOutputStream.UnicodeExtraFieldPolicy getPolicy() {
            return POLICIES.get(getValue());
        }
    }

    /**
     * The choices for Zip64 extensions.
     *
     * <p><b>never</b>: never add any Zip64 extensions.  This will
     * cause the task to fail if you try to add entries bigger than
     * 4GB or create an archive bigger than 4GB or holding more that
     * 65535 entries.</p>
     *
     * <p><b>as-needed</b>: create Zip64 extensions only when the
     * entry's size is bigger than 4GB or one of the archive limits is
     * hit.  This mode also adds partial Zip64 extensions for all
     * deflated entries written by Ant.</p>
     *
     * <p><b>always</b>: create Zip64 extensions for all entries.</p>
     *
     * <p><b>Note</b> some ZIP implementations don't handle Zip64
     * extensions well and others may fail if the Zip64 extra field
     * data is only present inside the local file header but not the
     * central directory - which is what <em>as-needed</em> may result
     * in.  Java5 and Microsoft Visual Studio's Extension loader are
     * known to fconsider the archive broken in such cases.  If you
     * are targeting such an archiver use the value <em>never</em>
     * unless you know you need Zip64 extensions.</p>
     *
     * @since Ant 1.9.1
     */
    public static final class Zip64ModeAttribute extends EnumeratedAttribute {
        private static final Map<String, Zip64Mode> MODES = new HashMap<>();

        private static final String NEVER_KEY = "never";
        private static final String ALWAYS_KEY = "always";
        private static final String A_N_KEY = "as-needed";
        static {
            MODES.put(NEVER_KEY, Zip64Mode.Never);
            MODES.put(ALWAYS_KEY, Zip64Mode.Always);
            MODES.put(A_N_KEY, Zip64Mode.AsNeeded);
        }

        @Override
        public String[] getValues() {
            return new String[] {NEVER_KEY, ALWAYS_KEY, A_N_KEY};
        }

        public static final Zip64ModeAttribute NEVER =
            new Zip64ModeAttribute(NEVER_KEY);
        public static final Zip64ModeAttribute AS_NEEDED =
            new Zip64ModeAttribute(A_N_KEY);

        private Zip64ModeAttribute(final String name) {
            setValue(name);
        }

        public Zip64ModeAttribute() {
        }

        public Zip64Mode getMode() {
            return MODES.get(getValue());
        }

    }
 }
