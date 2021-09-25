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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.FilterChain;
import org.apache.tools.ant.types.FilterSet;
import org.apache.tools.ant.types.FilterSetCollection;
import org.apache.tools.ant.types.Mapper;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.FileProvider;
import org.apache.tools.ant.types.resources.FileResource;
import org.apache.tools.ant.util.FileNameMapper;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.FlatFileNameMapper;
import org.apache.tools.ant.util.IdentityMapper;
import org.apache.tools.ant.util.LinkedHashtable;
import org.apache.tools.ant.util.ResourceUtils;
import org.apache.tools.ant.util.SourceFileScanner;
import org.apache.tools.ant.util.StringUtils;

/**
 * <p>Copies a file or directory to a new file
 * or directory.  Files are only copied if the source file is newer
 * than the destination file, or when the destination file does not
 * exist.  It is possible to explicitly overwrite existing files.</p>
 *
 * <p>This implementation is based on Arnout Kuiper's initial design
 * document, the following mailing list discussions, and the
 * copyfile/copydir tasks.</p>
 *
 *
 * @since Ant 1.2
 *
 * @ant.task category="filesystem"
 */
public class Copy extends Task {
    private static final String MSG_WHEN_COPYING_EMPTY_RC_TO_FILE =
        "Cannot perform operation from directory to file.";

    @Deprecated
    static final String LINE_SEPARATOR = StringUtils.LINE_SEP;
    static final File NULL_FILE_PLACEHOLDER = new File("/NULL_FILE");
    // CheckStyle:VisibilityModifier OFF - bc
    protected File file = null;     // the source file
    protected File destFile = null; // the destination file
    protected File destDir = null;  // the destination directory
    protected Vector<ResourceCollection> rcs = new Vector<>();
    // here to provide API backwards compatibility
    protected Vector<ResourceCollection> filesets = rcs;

    private boolean enableMultipleMappings = false;
    protected boolean filtering = false;
    protected boolean preserveLastModified = false;
    protected boolean forceOverwrite = false;
    protected boolean flatten = false;
    protected int verbosity = Project.MSG_VERBOSE;
    protected boolean includeEmpty = true;
    protected boolean failonerror = true;

    protected Hashtable<String, String[]> fileCopyMap = new LinkedHashtable<>();
    protected Hashtable<String, String[]> dirCopyMap = new LinkedHashtable<>();
    protected Hashtable<File, File> completeDirMap = new LinkedHashtable<>();

    protected Mapper mapperElement = null;
    protected FileUtils fileUtils;
    //CheckStyle:VisibilityModifier ON
    private final Vector<FilterChain> filterChains = new Vector<>();
    private final Vector<FilterSet> filterSets = new Vector<>();
    private String inputEncoding = null;
    private String outputEncoding = null;
    private long granularity = 0;
    private boolean force = false;
    private boolean quiet = false;

    // used to store the single non-file resource to copy when the
    // tofile attribute has been used
    private Resource singleResource = null;

    /**
     * Copy task constructor.
     */
    public Copy() {
        fileUtils = FileUtils.getFileUtils();
        granularity = fileUtils.getFileTimestampGranularity();
    }

    /**
     * Get the FileUtils for this task.
     * @return the fileutils object.
     */
    protected FileUtils getFileUtils() {
        return fileUtils;
    }

    /**
     * Set a single source file to copy.
     * @param file the file to copy.
     */
    public void setFile(final File file) {
        this.file = file;
    }

    /**
     * Set the destination file.
     * @param destFile the file to copy to.
     */
    public void setTofile(final File destFile) {
        this.destFile = destFile;
    }

    /**
     * Set the destination directory.
     * @param destDir the destination directory.
     */
    public void setTodir(final File destDir) {
        this.destDir = destDir;
    }

    /**
     * Add a FilterChain.
     * @return a filter chain object.
     */
    public FilterChain createFilterChain() {
        final FilterChain filterChain = new FilterChain();
        filterChains.addElement(filterChain);
        return filterChain;
    }

    /**
     * Add a filterset.
     * @return a filter set object.
     */
    public FilterSet createFilterSet() {
        final FilterSet filterSet = new FilterSet();
        filterSets.addElement(filterSet);
        return filterSet;
    }

    /**
     * Give the copied files the same last modified time as the original files.
     * @param preserve a boolean string.
     * @deprecated since 1.5.x.
     *             setPreserveLastModified(String) has been deprecated and
     *             replaced with setPreserveLastModified(boolean) to
     *             consistently let the Introspection mechanism work.
     */
    @Deprecated
    public void setPreserveLastModified(final String preserve) {
        setPreserveLastModified(Project.toBoolean(preserve));
    }

    /**
     * Give the copied files the same last modified time as the original files.
     * @param preserve if true preserve the modified time; default is false.
     */
    public void setPreserveLastModified(final boolean preserve) {
        preserveLastModified = preserve;
    }

    /**
     * Get whether to give the copied files the same last modified time as
     * the original files.
     * @return the whether destination files will inherit the modification
     *         times of the corresponding source files.
     * @since 1.32, Ant 1.5
     */
    public boolean getPreserveLastModified() {
        return preserveLastModified;
    }

    /**
     * Get the filtersets being applied to this operation.
     *
     * @return a vector of FilterSet objects.
     */
    protected Vector<FilterSet> getFilterSets() {
        return filterSets;
    }

    /**
     * Get the filterchains being applied to this operation.
     *
     * @return a vector of FilterChain objects.
     */
    protected Vector<FilterChain> getFilterChains() {
        return filterChains;
    }

    /**
     * Set filtering mode.
     * @param filtering if true enable filtering; default is false.
     */
    public void setFiltering(final boolean filtering) {
        this.filtering = filtering;
    }

    /**
     * Set overwrite mode regarding existing destination file(s).
     * @param overwrite if true force overwriting of destination file(s)
     *                  even if the destination file(s) are younger than
     *                  the corresponding source file. Default is false.
     */
    public void setOverwrite(final boolean overwrite) {
        this.forceOverwrite = overwrite;
    }

    /**
     * Whether read-only destinations will be overwritten.
     *
     * <p>Defaults to false</p>
     *
     * @param f boolean
     * @since Ant 1.8.2
     */
    public void setForce(final boolean f) {
        force = f;
    }

    /**
     * Whether read-only destinations will be overwritten.
     *
     * @return boolean
     * @since Ant 1.8.2
     */
    public boolean getForce() {
        return force;
    }

    /**
     * Set whether files copied from directory trees will be "flattened"
     * into a single directory.  If there are multiple files with
     * the same name in the source directory tree, only the first
     * file will be copied into the "flattened" directory, unless
     * the forceoverwrite attribute is true.
     * @param flatten if true flatten the destination directory. Default
     *                is false.
     */
    public void setFlatten(final boolean flatten) {
        this.flatten = flatten;
    }

    /**
     * Set verbose mode. Used to force listing of all names of copied files.
     * @param verbose whether to output the names of copied files.
     *                Default is false.
     */
    public void setVerbose(final boolean verbose) {
        this.verbosity = verbose ? Project.MSG_INFO : Project.MSG_VERBOSE;
    }

    /**
     * Set whether to copy empty directories.
     * @param includeEmpty if true copy empty directories. Default is true.
     */
    public void setIncludeEmptyDirs(final boolean includeEmpty) {
        this.includeEmpty = includeEmpty;
    }

    /**
     * Set quiet mode. Used to hide messages when a file or directory to be
     * copied does not exist.
     *
     * @param quiet
     *            whether or not to display error messages when a file or
     *            directory does not exist. Default is false.
     */
    public void setQuiet(final boolean quiet) {
        this.quiet = quiet;
    }

    /**
     * Set method of handling mappers that return multiple
     * mappings for a given source path.
     * @param enableMultipleMappings If true the task will
     *        copy to all the mappings for a given source path, if
     *        false, only the first file or directory is
     *        processed.
     *        By default, this setting is false to provide backward
     *        compatibility with earlier releases.
     * @since Ant 1.6
     */
    public void setEnableMultipleMappings(final boolean enableMultipleMappings) {
        this.enableMultipleMappings = enableMultipleMappings;
    }

    /**
     * Get whether multiple mapping is enabled.
     * @return true if multiple mapping is enabled; false otherwise.
     */
    public boolean isEnableMultipleMapping() {
        return enableMultipleMappings;
    }

    /**
     * Set whether to fail when errors are encountered. If false, note errors
     * to the output but keep going. Default is true.
     * @param failonerror true or false.
     */
    public void setFailOnError(final boolean failonerror) {
        this.failonerror = failonerror;
    }

    /**
     * Add a set of files to copy.
     * @param set a set of files to copy.
     */
    public void addFileset(final FileSet set) {
        add(set);
    }

    /**
     * Add a collection of files to copy.
     * @param res a resource collection to copy.
     * @since Ant 1.7
     */
    public void add(final ResourceCollection res) {
        rcs.add(res);
    }

    /**
     * Define the mapper to map source to destination files.
     * @return a mapper to be configured.
     * @exception BuildException if more than one mapper is defined.
     */
    public Mapper createMapper() throws BuildException {
        if (mapperElement != null) {
            throw new BuildException("Cannot define more than one mapper",
                                     getLocation());
        }
        mapperElement = new Mapper(getProject());
        return mapperElement;
    }

    /**
     * Add a nested filenamemapper.
     * @param fileNameMapper the mapper to add.
     * @since Ant 1.6.3
     */
    public void add(final FileNameMapper fileNameMapper) {
        createMapper().add(fileNameMapper);
    }

    /**
     * Set the character encoding.
     * @param encoding the character encoding.
     * @since 1.32, Ant 1.5
     */
    public void setEncoding(final String encoding) {
        this.inputEncoding = encoding;
        if (outputEncoding == null) {
            outputEncoding = encoding;
        }
    }

    /**
     * Get the character encoding to be used.
     * @return the character encoding, <code>null</code> if not set.
     *
     * @since 1.32, Ant 1.5
     */
    public String getEncoding() {
        return inputEncoding;
    }

    /**
     * Set the character encoding for output files.
     * @param encoding the output character encoding.
     * @since Ant 1.6
     */
    public void setOutputEncoding(final String encoding) {
        this.outputEncoding = encoding;
    }

    /**
     * Get the character encoding for output files.
     * @return the character encoding for output files,
     * <code>null</code> if not set.
     *
     * @since Ant 1.6
     */
    public String getOutputEncoding() {
        return outputEncoding;
    }

    /**
     * Set the number of milliseconds leeway to give before deciding a
     * target is out of date.
     *
     * <p>Default is 1 second, or 2 seconds on DOS systems.</p>
     * @param granularity the granularity used to decide if a target is out of
     *                    date.
     * @since Ant 1.6.2
     */
    public void setGranularity(final long granularity) {
        this.granularity = granularity;
    }

    /**
     * Perform the copy operation.
     * @exception BuildException if an error occurs.
     */
    @Override
    public void execute() throws BuildException {
        final File savedFile = file; // may be altered in validateAttributes
        final File savedDestFile = destFile;
        final File savedDestDir = destDir;
        ResourceCollection savedRc = null;
        if (file == null && destFile != null && rcs.size() == 1) {
            // will be removed in validateAttributes
            savedRc = rcs.elementAt(0);
        }

        try {
            // make sure we don't have an illegal set of options
            try {
                validateAttributes();
            } catch (final BuildException e) {
                if (failonerror
                    || !getMessage(e)
                    .equals(MSG_WHEN_COPYING_EMPTY_RC_TO_FILE)) {
                    throw e;
                } else {
                    log("Warning: " + getMessage(e), Project.MSG_ERR);
                    return;
                }
            }

            // deal with the single file
            copySingleFile();

            // deal with the ResourceCollections

            /* for historical and performance reasons we have to do
               things in a rather complex way.

               (1) Move is optimized to move directories if a fileset
               has been included completely, therefore FileSets need a
               special treatment.  This is also required to support
               the failOnError semantic (skip filesets with broken
               basedir but handle the remaining collections).

               (2) We carry around a few protected methods that work
               on basedirs and arrays of names.  To optimize stuff, all
               resources with the same basedir get collected in
               separate lists and then each list is handled in one go.
            */

            final Map<File, List<String>> filesByBasedir = new HashMap<>();
            final Map<File, List<String>> dirsByBasedir = new HashMap<>();
            final Set<File> baseDirs = new HashSet<>();
            final List<Resource> nonFileResources = new ArrayList<>();

            for (ResourceCollection rc : rcs) {

                // Step (1) - beware of the ZipFileSet
                if (rc instanceof FileSet && rc.isFilesystemOnly()) {
                    final FileSet fs = (FileSet) rc;
                    DirectoryScanner ds;
                    try {
                        ds = fs.getDirectoryScanner(getProject());
                    } catch (final BuildException e) {
                        if (failonerror
                            || !getMessage(e).endsWith(DirectoryScanner
                                                       .DOES_NOT_EXIST_POSTFIX)) {
                            throw e;
                        }
                        if (!quiet) {
                            log("Warning: " + getMessage(e), Project.MSG_ERR);
                        }
                        continue;
                    }
                    final File fromDir = fs.getDir(getProject());

                    if (!flatten && mapperElement == null
                        && ds.isEverythingIncluded() && !fs.hasPatterns()) {
                        completeDirMap.put(fromDir, destDir);
                    }
                    add(fromDir, ds.getIncludedFiles(), filesByBasedir);
                    add(fromDir, ds.getIncludedDirectories(), dirsByBasedir);
                    baseDirs.add(fromDir);
                } else { // not a fileset or contains non-file resources

                    if (!rc.isFilesystemOnly() && !supportsNonFileResources()) {
                        throw new BuildException(
                                   "Only FileSystem resources are supported.");
                    }

                    for (final Resource r : rc) {
                        if (!r.isExists()) {
                            final String message = "Warning: Could not find resource "
                                + r.toLongString() + " to copy.";
                            if (!failonerror) {
                                if (!quiet) {
                                    log(message, Project.MSG_ERR);
                                }
                            } else {
                                throw new BuildException(message);
                            }
                            continue;
                        }

                        File baseDir = NULL_FILE_PLACEHOLDER;
                        String name = r.getName();
                        final FileProvider fp = r.as(FileProvider.class);
                        if (fp != null) {
                            final FileResource fr = ResourceUtils.asFileResource(fp);
                            baseDir = getKeyFile(fr.getBaseDir());
                            if (fr.getBaseDir() == null) {
                                name = fr.getFile().getAbsolutePath();
                            }
                        }

                        // copying of dirs is trivial and can be done
                        // for non-file resources as well as for real
                        // files.
                        if (r.isDirectory() || fp != null) {
                            add(baseDir, name,
                                r.isDirectory() ? dirsByBasedir
                                                : filesByBasedir);
                            baseDirs.add(baseDir);
                        } else { // a not-directory file resource
                            // needs special treatment
                            nonFileResources.add(r);
                        }
                    }
                }
            }

            iterateOverBaseDirs(baseDirs, dirsByBasedir, filesByBasedir);

            // do all the copy operations now...
            try {
                doFileOperations();
            } catch (final BuildException e) {
                if (!failonerror) {
                    if (!quiet) {
                        log("Warning: " + getMessage(e), Project.MSG_ERR);
                    }
                } else {
                    throw e;
                }
            }

            if (!nonFileResources.isEmpty() || singleResource != null) {
                final Resource[] nonFiles =
                    nonFileResources.toArray(new Resource[0]);
                // restrict to out-of-date resources
                final Map<Resource, String[]> map = scan(nonFiles, destDir);
                if (singleResource != null) {
                    map.put(singleResource,
                            new String[] {destFile.getAbsolutePath()});
                }
                try {
                    doResourceOperations(map);
                } catch (final BuildException e) {
                    if (!failonerror) {
                        if (!quiet) {
                            log("Warning: " + getMessage(e), Project.MSG_ERR);
                        }
                    } else {
                        throw e;
                    }
                }
            }
        } finally {
            // clean up again, so this instance can be used a second
            // time
            singleResource = null;
            file = savedFile;
            destFile = savedDestFile;
            destDir = savedDestDir;
            if (savedRc != null) {
                rcs.insertElementAt(savedRc, 0);
            }
            fileCopyMap.clear();
            dirCopyMap.clear();
            completeDirMap.clear();
        }
    }

    /************************************************************************
     **  protected and private methods
     ************************************************************************/

    private void copySingleFile() {
        // deal with the single file
        if (file != null) {
            if (file.exists()) {
                if (destFile == null) {
                    destFile = new File(destDir, file.getName());
                }
                if (forceOverwrite || !destFile.exists()
                    || (file.lastModified() - granularity
                        > destFile.lastModified())) {
                    fileCopyMap.put(file.getAbsolutePath(),
                                    new String[] {destFile.getAbsolutePath()});
                } else {
                    log(file + " omitted as " + destFile
                        + " is up to date.", Project.MSG_VERBOSE);
                }
            } else {
                final String message = "Warning: Could not find file "
                    + file.getAbsolutePath() + " to copy.";
                if (!failonerror) {
                    if (!quiet) {
                      log(message, Project.MSG_ERR);
                    }
                } else {
                    throw new BuildException(message);
                }
            }
        }
    }

    private void iterateOverBaseDirs(final Set<File> baseDirs,
        final Map<File, List<String>> dirsByBasedir,
        final Map<File, List<String>> filesByBasedir) {

        for (final File f : baseDirs) {
            final List<String> files = filesByBasedir.get(f);
            final List<String> dirs = dirsByBasedir.get(f);

            String[] srcFiles = new String[0];
            if (files != null) {
                srcFiles = files.toArray(srcFiles);
            }
            String[] srcDirs = new String[0];
            if (dirs != null) {
                srcDirs = dirs.toArray(srcDirs);
            }
            scan(f == NULL_FILE_PLACEHOLDER ? null : f, destDir, srcFiles,
                 srcDirs);
        }
    }

    /**
     * Ensure we have a consistent and legal set of attributes, and set
     * any internal flags necessary based on different combinations
     * of attributes.
     * @exception BuildException if an error occurs.
     */
    protected void validateAttributes() throws BuildException {
        if (file == null && rcs.isEmpty()) {
            throw new BuildException(
                "Specify at least one source--a file or a resource collection.");
        }
        if (destFile != null && destDir != null) {
            throw new BuildException(
                "Only one of tofile and todir may be set.");
        }
        if (destFile == null && destDir == null) {
            throw new BuildException("One of tofile or todir must be set.");
        }
        if (file != null && file.isDirectory()) {
            throw new BuildException("Use a resource collection to copy directories.");
        }
        if (destFile != null && !rcs.isEmpty()) {
            if (rcs.size() > 1) {
                throw new BuildException(
                    "Cannot concatenate multiple files into a single file.");
            }
            final ResourceCollection rc = rcs.elementAt(0);
            if (!rc.isFilesystemOnly() && !supportsNonFileResources()) {
                throw new BuildException(
                    "Only FileSystem resources are supported.");
            }
            if (rc.isEmpty()) {
                throw new BuildException(MSG_WHEN_COPYING_EMPTY_RC_TO_FILE);
            }
            if (rc.size() == 1) {
                final Resource res = rc.iterator().next();
                final FileProvider r = res.as(FileProvider.class);
                if (file == null) {
                    if (r != null) {
                        file = r.getFile();
                    } else {
                        singleResource = res;
                    }
                    rcs.removeElementAt(0);
                } else {
                    throw new BuildException(
                        "Cannot concatenate multiple files into a single file.");
                }
            } else {
                throw new BuildException(
                    "Cannot concatenate multiple files into a single file.");
            }
        }
        if (destFile != null) {
            destDir = destFile.getParentFile();
        }
    }

    /**
     * Compares source files to destination files to see if they should be
     * copied.
     *
     * @param fromDir  The source directory.
     * @param toDir    The destination directory.
     * @param files    A list of files to copy.
     * @param dirs     A list of directories to copy.
     */
    protected void scan(final File fromDir, final File toDir, final String[] files,
                        final String[] dirs) {
        final FileNameMapper mapper = getMapper();
        buildMap(fromDir, toDir, files, mapper, fileCopyMap);

        if (includeEmpty) {
            buildMap(fromDir, toDir, dirs, mapper, dirCopyMap);
        }
    }

    /**
     * Compares source resources to destination files to see if they
     * should be copied.
     *
     * @param fromResources  The source resources.
     * @param toDir          The destination directory.
     *
     * @return a Map with the out-of-date resources as keys and an
     * array of target file names as values.
     *
     * @since Ant 1.7
     */
    protected Map<Resource, String[]> scan(final Resource[] fromResources, final File toDir) {
        return buildMap(fromResources, toDir, getMapper());
    }

    /**
     * Add to a map of files/directories to copy.
     *
     * @param fromDir the source directory.
     * @param toDir   the destination directory.
     * @param names   a list of filenames.
     * @param mapper  a <code>FileNameMapper</code> value.
     * @param map     a map of source file to array of destination files.
     */
    protected void buildMap(final File fromDir, final File toDir, final String[] names,
                            final FileNameMapper mapper, final Hashtable<String, String[]> map) {
        String[] toCopy = null;
        if (forceOverwrite) {
            final List<String> v = new ArrayList<>();
            for (String name : names) {
                if (mapper.mapFileName(name) != null) {
                    v.add(name);
                }
            }
            toCopy = v.toArray(new String[0]);
        } else {
            final SourceFileScanner ds = new SourceFileScanner(this);
            toCopy = ds.restrict(names, fromDir, toDir, mapper, granularity);
        }
        for (String name : toCopy) {
            final File src = new File(fromDir, name);
            final String[] mappedFiles = mapper.mapFileName(name);
            if (mappedFiles == null || mappedFiles.length == 0) {
                continue;
            }

            if (!enableMultipleMappings) {
                map.put(src.getAbsolutePath(),
                        new String[]{new File(toDir, mappedFiles[0]).getAbsolutePath()});
            } else {
                // reuse the array created by the mapper
                for (int k = 0; k < mappedFiles.length; k++) {
                    mappedFiles[k] = new File(toDir, mappedFiles[k]).getAbsolutePath();
                }
                map.put(src.getAbsolutePath(), mappedFiles);
            }
        }
    }

    /**
     * Create a map of resources to copy.
     *
     * @param fromResources  The source resources.
     * @param toDir   the destination directory.
     * @param mapper  a <code>FileNameMapper</code> value.
     * @return a map of source resource to array of destination files.
     * @since Ant 1.7
     */
    protected Map<Resource, String[]> buildMap(final Resource[] fromResources, final File toDir,
                           final FileNameMapper mapper) {
        final Map<Resource, String[]> map = new HashMap<>();
        Resource[] toCopy;
        if (forceOverwrite) {
            final List<Resource> v = new ArrayList<>();
            for (Resource rc : fromResources) {
                if (mapper.mapFileName(rc.getName()) != null) {
                    v.add(rc);
                }
            }
            toCopy = v.toArray(new Resource[0]);
        } else {
            toCopy = ResourceUtils.selectOutOfDateSources(this, fromResources, mapper,
                    name -> new FileResource(toDir, name), granularity);
        }
        for (Resource rc : toCopy) {
            final String[] mappedFiles = mapper.mapFileName(rc.getName());
            if (mappedFiles == null || mappedFiles.length == 0) {
                throw new BuildException("Can't copy a resource without a"
                        + " name if the mapper doesn't"
                        + " provide one.");
            }
            if (!enableMultipleMappings) {
                map.put(rc, new String[]{new File(toDir, mappedFiles[0]).getAbsolutePath()});
            } else {
                // reuse the array created by the mapper
                for (int k = 0; k < mappedFiles.length; k++) {
                    mappedFiles[k] = new File(toDir, mappedFiles[k]).getAbsolutePath();
                }
                map.put(rc, mappedFiles);
            }
        }
        return map;
    }

    /**
     * Actually does the file (and possibly empty directory) copies.
     * This is a good method for subclasses to override.
     */
    protected void doFileOperations() {
        if (!fileCopyMap.isEmpty()) {
            log("Copying " + fileCopyMap.size()
                + " file" + (fileCopyMap.size() == 1 ? "" : "s")
                + " to " + destDir.getAbsolutePath());

            for (final Map.Entry<String, String[]> e : fileCopyMap.entrySet()) {
                final String fromFile = e.getKey();

                for (final String toFile : e.getValue()) {
                    if (fromFile.equals(toFile)) {
                        log("Skipping self-copy of " + fromFile, verbosity);
                        continue;
                    }
                    try {
                        log("Copying " + fromFile + " to " + toFile, verbosity);

                        final FilterSetCollection executionFilters =
                            new FilterSetCollection();
                        if (filtering) {
                            executionFilters
                                .addFilterSet(getProject().getGlobalFilterSet());
                        }
                        for (final FilterSet filterSet : filterSets) {
                            executionFilters.addFilterSet(filterSet);
                        }
                        fileUtils.copyFile(new File(fromFile), new File(toFile),
                                           executionFilters,
                                           filterChains, forceOverwrite,
                                           preserveLastModified,
                                           /* append: */ false, inputEncoding,
                                           outputEncoding, getProject(),
                                           getForce());
                    } catch (final IOException ioe) {
                        String msg = "Failed to copy " + fromFile + " to " + toFile
                            + " due to " + getDueTo(ioe);
                        final File targetFile = new File(toFile);
                        if (!(ioe instanceof
                              ResourceUtils.ReadOnlyTargetFileException)
                            && targetFile.exists() && !targetFile.delete()) {
                            msg += " and I couldn't delete the corrupt " + toFile;
                        }
                        if (failonerror) {
                            throw new BuildException(msg, ioe, getLocation());
                        }
                        log(msg, Project.MSG_ERR);
                    }
                }
            }
        }
        if (includeEmpty) {
            int createCount = 0;
            for (final String[] dirs : dirCopyMap.values()) {
                for (String dir : dirs) {
                    final File d = new File(dir);
                    if (!d.exists()) {
                        if (!d.mkdirs() && !d.isDirectory()) {
                            log("Unable to create directory "
                                + d.getAbsolutePath(), Project.MSG_ERR);
                        } else {
                            createCount++;
                        }
                    }
                }
            }
            if (createCount > 0) {
                log("Copied " + dirCopyMap.size()
                    + " empty director"
                    + (dirCopyMap.size() == 1 ? "y" : "ies")
                    + " to " + createCount
                    + " empty director"
                    + (createCount == 1 ? "y" : "ies") + " under "
                    + destDir.getAbsolutePath());
            }
        }
    }

    /**
     * Actually does the resource copies.
     * This is a good method for subclasses to override.
     * @param map a map of source resource to array of destination files.
     * @since Ant 1.7
     */
    protected void doResourceOperations(final Map<Resource, String[]> map) {
        if (!map.isEmpty()) {
            log("Copying " + map.size()
                + " resource" + (map.size() == 1 ? "" : "s")
                + " to " + destDir.getAbsolutePath());

            for (final Map.Entry<Resource, String[]> e : map.entrySet()) {
                final Resource fromResource = e.getKey();
                for (final String toFile : e.getValue()) {
                    try {
                        log("Copying " + fromResource + " to " + toFile,
                            verbosity);

                        final FilterSetCollection executionFilters = new FilterSetCollection();
                        if (filtering) {
                            executionFilters
                                .addFilterSet(getProject().getGlobalFilterSet());
                        }
                        for (final FilterSet filterSet : filterSets) {
                            executionFilters.addFilterSet(filterSet);
                        }
                        ResourceUtils.copyResource(fromResource,
                                                   new FileResource(destDir,
                                                                    toFile),
                                                   executionFilters,
                                                   filterChains,
                                                   forceOverwrite,
                                                   preserveLastModified,
                                                   /* append: */ false,
                                                   inputEncoding,
                                                   outputEncoding,
                                                   getProject(),
                                                   getForce());
                    } catch (final IOException ioe) {
                        String msg = "Failed to copy " + fromResource
                            + " to " + toFile
                            + " due to " + getDueTo(ioe);
                        final File targetFile = new File(toFile);
                        if (!(ioe instanceof
                              ResourceUtils.ReadOnlyTargetFileException)
                            && targetFile.exists() && !targetFile.delete()) {
                            msg += " and I couldn't delete the corrupt " + toFile;
                        }
                        if (failonerror) {
                            throw new BuildException(msg, ioe, getLocation());
                        }
                        log(msg, Project.MSG_ERR);
                    }
                }
            }
        }
    }

    /**
     * Whether this task can deal with non-file resources.
     *
     * <p>&lt;copy&gt; can while &lt;move&gt; can't since we don't
     * know how to remove non-file resources.</p>
     *
     * <p>This implementation returns true only if this task is
     * &lt;copy&gt;.  Any subclass of this class that also wants to
     * support non-file resources needs to override this method.  We
     * need to do so for backwards compatibility reasons since we
     * can't expect subclasses to support resources.</p>
     * @return true if this task supports non file resources.
     * @since Ant 1.7
     */
    protected boolean supportsNonFileResources() {
        return getClass().equals(Copy.class);
    }

    /**
     * Adds the given strings to a list contained in the given map.
     * The file is the key into the map.
     */
    private static void add(File baseDir, final String[] names, final Map<File, List<String>> m) {
        if (names != null) {
            baseDir = getKeyFile(baseDir);
            List<String> l = m.computeIfAbsent(baseDir, k -> new ArrayList<>(names.length));
            l.addAll(Arrays.asList(names));
        }
    }

    /**
     * Adds the given string to a list contained in the given map.
     * The file is the key into the map.
     */
    private static void add(final File baseDir, final String name, final Map<File, List<String>> m) {
        if (name != null) {
            add(baseDir, new String[] {name}, m);
        }
    }

    /**
     * Either returns its argument or a placeholder if the argument is null.
     */
    private static File getKeyFile(final File f) {
        return f == null ? NULL_FILE_PLACEHOLDER : f;
    }

    /**
     * returns the mapper to use based on nested elements or the
     * flatten attribute.
     */
    private FileNameMapper getMapper() {
        FileNameMapper mapper = null;
        if (mapperElement != null) {
            mapper = mapperElement.getImplementation();
        } else if (flatten) {
            mapper = new FlatFileNameMapper();
        } else {
            mapper = new IdentityMapper();
        }
        return mapper;
    }

    /**
     * Handle getMessage() for exceptions.
     * @param ex the exception to handle
     * @return ex.getMessage() if ex.getMessage() is not null
     *         otherwise return ex.toString()
     */
    private String getMessage(final Exception ex) {
        return ex.getMessage() == null ? ex.toString() : ex.getMessage();
    }

    /**
     * Returns a reason for failure based on
     * the exception thrown.
     * If the exception is not IOException output the class name,
     * output the message
     * if the exception is MalformedInput add a little note.
     */
    private String getDueTo(final Exception ex) {
        final boolean baseIOException = ex.getClass() == IOException.class;
        final StringBuilder message = new StringBuilder();
        if (!baseIOException || ex.getMessage() == null) {
            message.append(ex.getClass().getName());
        }
        if (ex.getMessage() != null) {
            if (!baseIOException) {
                message.append(" ");
            }
            message.append(ex.getMessage());
        }
        if (ex.getClass().getName().contains("MalformedInput")) {
            message.append(String.format(
                    "%nThis is normally due to the input file containing invalid"
                            + "%nbytes for the character encoding used : %s%n",
                    inputEncoding == null ? fileUtils.getDefaultEncoding() : inputEncoding));
        }
        return message.toString();
    }
}
