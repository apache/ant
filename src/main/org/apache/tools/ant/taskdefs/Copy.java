/*
 * Copyright  2000-2005 The Apache Software Foundation
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.Mapper;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.FilterSet;
import org.apache.tools.ant.types.FilterChain;
import org.apache.tools.ant.types.FilterSetCollection;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.FileResource;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.FileNameMapper;
import org.apache.tools.ant.util.IdentityMapper;
import org.apache.tools.ant.util.SourceFileScanner;
import org.apache.tools.ant.util.FlatFileNameMapper;

/**
 * Copies a file or directory to a new file
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
    private static final File NULL_FILE_PLACEHOLDER = new File("/NULL_FILE");

    protected File file = null;     // the source file
    protected File destFile = null; // the destination file
    protected File destDir = null;  // the destination directory
    protected Vector rcs = new Vector();

    private boolean enableMultipleMappings = false;
    protected boolean filtering = false;
    protected boolean preserveLastModified = false;
    protected boolean forceOverwrite = false;
    protected boolean flatten = false;
    protected int verbosity = Project.MSG_VERBOSE;
    protected boolean includeEmpty = true;
    protected boolean failonerror = true;

    protected Hashtable fileCopyMap = new Hashtable();
    protected Hashtable dirCopyMap = new Hashtable();
    protected Hashtable completeDirMap = new Hashtable();

    protected Mapper mapperElement = null;
    protected FileUtils fileUtils;
    private Vector filterChains = new Vector();
    private Vector filterSets = new Vector();
    private String inputEncoding = null;
    private String outputEncoding = null;
    private long granularity = 0;

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
    public void setFile(File file) {
        this.file = file;
    }

    /**
     * Set the destination file.
     * @param destFile the file to copy to.
     */
    public void setTofile(File destFile) {
        this.destFile = destFile;
    }

    /**
     * Set the destination directory.
     * @param destDir the destination directory.
     */
    public void setTodir(File destDir) {
        this.destDir = destDir;
    }

    /**
     * Add a FilterChain.
     * @return a filter chain object.
     */
    public FilterChain createFilterChain() {
        FilterChain filterChain = new FilterChain();
        filterChains.addElement(filterChain);
        return filterChain;
    }

    /**
     * Add a filterset.
     * @return a filter set object.
     */
    public FilterSet createFilterSet() {
        FilterSet filterSet = new FilterSet();
        filterSets.addElement(filterSet);
        return filterSet;
    }

    /**
     * Give the copied files the same last modified time as the original files.
     * @param preserve a boolean string.
     * @deprecated setPreserveLastModified(String) has been deprecated and
     *             replaced with setPreserveLastModified(boolean) to
     *             consistently let the Introspection mechanism work.
     */
    public void setPreserveLastModified(String preserve) {
        setPreserveLastModified(Project.toBoolean(preserve));
    }

    /**
     * Give the copied files the same last modified time as the original files.
     * @param preserve if true preserve the modified time; default is false.
     */
    public void setPreserveLastModified(boolean preserve) {
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
    protected Vector getFilterSets() {
        return filterSets;
    }

    /**
     * Get the filterchains being applied to this operation.
     *
     * @return a vector of FilterChain objects.
     */
    protected Vector getFilterChains() {
        return filterChains;
    }

    /**
     * Set filtering mode.
     * @param filtering if true enable filtering; default is false.
     */
    public void setFiltering(boolean filtering) {
        this.filtering = filtering;
    }

    /**
     * Set overwrite mode regarding existing destination file(s).
     * @param overwrite if true force overwriting of destination file(s)
     *                  even if the destination file(s) are younger than
     *                  the corresponding source file. Default is false.
     */
    public void setOverwrite(boolean overwrite) {
        this.forceOverwrite = overwrite;
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
    public void setFlatten(boolean flatten) {
        this.flatten = flatten;
    }

    /**
     * Set verbose mode. Used to force listing of all names of copied files.
     * @param verbose whether to output the names of copied files.
     *                Default is false.
     */
    public void setVerbose(boolean verbose) {
        this.verbosity = verbose ? Project.MSG_INFO : Project.MSG_VERBOSE;
    }

    /**
     * Set whether to copy empty directories.
     * @param includeEmpty if true copy empty directories. Default is true.
     */
    public void setIncludeEmptyDirs(boolean includeEmpty) {
        this.includeEmpty = includeEmpty;
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
    public void setEnableMultipleMappings(boolean enableMultipleMappings) {
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
    public void setFailOnError(boolean failonerror) {
        this.failonerror = failonerror;
    }

    /**
     * Add a set of files to copy.
     * @param set a set of files to copy.
     */
    public void addFileset(FileSet set) {
	add(set);
    }
    
    /**
     * Add a collection of files to copy.
     * @param res a resource collection to copy.
     * @since Ant 1.7
     */
    public void add(ResourceCollection res) {
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
    public void add(FileNameMapper fileNameMapper) {
        createMapper().add(fileNameMapper);
    }

    /**
     * Set the character encoding.
     * @param encoding the character encoding.
     * @since 1.32, Ant 1.5
     */
    public void setEncoding(String encoding) {
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
    public void setOutputEncoding(String encoding) {
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
    public void setGranularity(long granularity) {
        this.granularity = granularity;
    }

    /**
     * Perform the copy operation.
     * @exception BuildException if an error occurs.
     */
    public void execute() throws BuildException {
        File savedFile = file; // may be altered in validateAttributes
        File savedDestFile = destFile;
        File savedDestDir = destDir;
	ResourceCollection savedRc = null;
        if (file == null && destFile != null && rcs.size() == 1) {
            // will be removed in validateAttributes
	    savedRc = (ResourceCollection) rcs.elementAt(0);
        }
        // make sure we don't have an illegal set of options
        validateAttributes();

        try {
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
                    String message = "Warning: Could not find file "
                        + file.getAbsolutePath() + " to copy.";
                    if (!failonerror) {
                        log(message);
                    } else {
                        throw new BuildException(message);
                    }
                }
            }
            // deal with the ResourceCollections

	    /* for historical and performance reasons we have to do
	       things in a rather complex way.
	    
	       (1) Move is optimized to move directories if a fileset
	       has been included completely, therefore FileSets need a
	       special treatment.  This is also required to support
	       the failOnError semantice (skip filesets with broken
	       basedir but handle the remaining collections).

	       (2) We carry around a few protected methods that work
	       on basedirs and arrays of names.  To optimize stuff, all
	       resources with the same basedir get collected in
	       separate lists and then each list is handled in one go.
	    */

	    HashMap filesByBasedir = new HashMap();
	    HashMap dirsByBasedir = new HashMap();
	    HashSet baseDirs = new HashSet();
            for (int i = 0; i < rcs.size(); i++) {
		ResourceCollection rc = (ResourceCollection) rcs.elementAt(i);

		if (rc.isFilesystemOnly()) {

		    // Step (1)
		    if (rc instanceof FileSet) {
			FileSet fs = (FileSet) rc;
			DirectoryScanner ds = null;
			try {
			    ds = fs.getDirectoryScanner(getProject());
			} catch (BuildException e) {
			    if (failonerror
				|| !e.getMessage().endsWith(" not found.")) {
				throw e;
			    } else {
				log("Warning: " + e.getMessage());
				continue;
			    }
			}
			File fromDir = fs.getDir(getProject());

			String[] srcFiles = ds.getIncludedFiles();
			String[] srcDirs = ds.getIncludedDirectories();
			if (!flatten && mapperElement == null
			    && ds.isEverythingIncluded() && !fs.hasPatterns()) {
			    completeDirMap.put(fromDir, destDir);
			}
			add(fromDir, srcFiles, filesByBasedir);
			add(fromDir, srcDirs, dirsByBasedir);
			baseDirs.add(fromDir);
		    } else { // not a fileset

			Iterator resources = rc.iterator();
			while (resources.hasNext()) {
			    FileResource fr = (FileResource) resources.next();
			    if (!fr.isExists()) {
				continue;
			    }
			    File baseDir = getKeyFile(fr.getBaseDir());
			    add(baseDir, 
				baseDir == NULL_FILE_PLACEHOLDER
				? fr.getFile().getAbsolutePath() : fr.getName(),
				fr.isDirectory() ? dirsByBasedir 
				                 : filesByBasedir);
			    baseDirs.add(baseDir);
			}
		    }

		    Iterator iter = baseDirs.iterator();
		    while (iter.hasNext()) {
			File f = (File) iter.next();
			List files = (List) filesByBasedir.get(f);
			List dirs = (List) dirsByBasedir.get(f);

			String[] srcFiles = new String[0];
			if (files != null) {
			    srcFiles = (String[]) files.toArray(srcFiles);
			}
			String[] srcDirs = new String[0];
			if (dirs != null) {
			    srcDirs = (String[]) dirs.toArray(srcDirs);
			}
			scan(f == NULL_FILE_PLACEHOLDER ? null : f, destDir,
			     srcFiles, srcDirs);
		    }
		} else { // not a File resource collection
		    throw new BuildException("Only FileSystem resources are"
					     + " supported.");
		}
            }
            // do all the copy operations now...
            try {
                doFileOperations();
            } catch (BuildException e) {
                if (!failonerror) {
                    log("Warning: " + e.getMessage(), Project.MSG_ERR);
                } else {
                    throw e;
                }
            }
        } finally {
            // clean up again, so this instance can be used a second
            // time
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

    /**
     * Ensure we have a consistent and legal set of attributes, and set
     * any internal flags necessary based on different combinations
     * of attributes.
     * @exception BuildException if an error occurs.
     */
    protected void validateAttributes() throws BuildException {
        if (file == null && rcs.size() == 0) {
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
        if (destFile != null && rcs.size() > 0) {
            if (rcs.size() > 1) {
                throw new BuildException(
                    "Cannot concatenate multiple files into a single file.");
            } else {
		ResourceCollection rc = (ResourceCollection) rcs.elementAt(0);
		if (!rc.isFilesystemOnly()) {
		    throw new BuildException("Only FileSystem resources are"
					     + " supported.");
		}
		if (rc.size() == 0) {
                    throw new BuildException(
                        "Cannot perform operation from directory to file.");
                } else if (rc.size() == 1) {
		    FileResource r = (FileResource) rc.iterator().next();
                    if (file == null) {
                        file = r.getFile();
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
    protected void scan(File fromDir, File toDir, String[] files,
                        String[] dirs) {
        FileNameMapper mapper = null;
        if (mapperElement != null) {
            mapper = mapperElement.getImplementation();
        } else if (flatten) {
            mapper = new FlatFileNameMapper();
        } else {
            mapper = new IdentityMapper();
        }
        buildMap(fromDir, toDir, files, mapper, fileCopyMap);

        if (includeEmpty) {
            buildMap(fromDir, toDir, dirs, mapper, dirCopyMap);
        }
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
    protected void buildMap(File fromDir, File toDir, String[] names,
                            FileNameMapper mapper, Hashtable map) {
        String[] toCopy = null;
        if (forceOverwrite) {
            Vector v = new Vector();
            for (int i = 0; i < names.length; i++) {
                if (mapper.mapFileName(names[i]) != null) {
                    v.addElement(names[i]);
                }
            }
            toCopy = new String[v.size()];
            v.copyInto(toCopy);
        } else {
            SourceFileScanner ds = new SourceFileScanner(this);
            toCopy = ds.restrict(names, fromDir, toDir, mapper, granularity);
        }
        for (int i = 0; i < toCopy.length; i++) {
            File src = new File(fromDir, toCopy[i]);

            String[] mappedFiles = mapper.mapFileName(toCopy[i]);

            if (!enableMultipleMappings) {
                map.put(src.getAbsolutePath(),
                        new String[] {new File(toDir, mappedFiles[0]).getAbsolutePath()});
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
     * Actually does the file (and possibly empty directory) copies.
     * This is a good method for subclasses to override.
     */
    protected void doFileOperations() {
        if (fileCopyMap.size() > 0) {
            log("Copying " + fileCopyMap.size()
                + " file" + (fileCopyMap.size() == 1 ? "" : "s")
                + " to " + destDir.getAbsolutePath());

            Enumeration e = fileCopyMap.keys();
            while (e.hasMoreElements()) {
                String fromFile = (String) e.nextElement();
                String[] toFiles = (String[]) fileCopyMap.get(fromFile);

                for (int i = 0; i < toFiles.length; i++) {
                    String toFile = toFiles[i];

                    if (fromFile.equals(toFile)) {
                        log("Skipping self-copy of " + fromFile, verbosity);
                        continue;
                    }
                    try {
                        log("Copying " + fromFile + " to " + toFile, verbosity);

                        FilterSetCollection executionFilters =
                            new FilterSetCollection();
                        if (filtering) {
                            executionFilters
                                .addFilterSet(getProject().getGlobalFilterSet());
                        }
                        for (Enumeration filterEnum = filterSets.elements();
                            filterEnum.hasMoreElements();) {
                            executionFilters
                                .addFilterSet((FilterSet) filterEnum.nextElement());
                        }
                        fileUtils.copyFile(fromFile, toFile, executionFilters,
                                           filterChains, forceOverwrite,
                                           preserveLastModified, inputEncoding,
                                           outputEncoding, getProject());
                    } catch (IOException ioe) {
                        String msg = "Failed to copy " + fromFile + " to " + toFile
                            + " due to " + ioe.getMessage();
                        File targetFile = new File(toFile);
                        if (targetFile.exists() && !targetFile.delete()) {
                            msg += " and I couldn't delete the corrupt " + toFile;
                        }
                        throw new BuildException(msg, ioe, getLocation());
                    }
                }
            }
        }
        if (includeEmpty) {
            Enumeration e = dirCopyMap.elements();
            int createCount = 0;
            while (e.hasMoreElements()) {
                String[] dirs = (String[]) e.nextElement();
                for (int i = 0; i < dirs.length; i++) {
                    File d = new File(dirs[i]);
                    if (!d.exists()) {
                        if (!d.mkdirs()) {
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
     * Adds the given strings to a list contained in the given map.
     * The file is the key into the map.
     */
    private static void add(File baseDir, String[] names, Map m) {
	if (names != null) {
	    baseDir = getKeyFile(baseDir);
	    List l = (List) m.get(baseDir);
	    if (l == null) {
		l = new ArrayList(names.length);
		m.put(baseDir, l);
	    }
	    l.addAll(java.util.Arrays.asList(names));
	}
    }

    /**
     * Adds the given string to a list contained in the given map.
     * The file is the key into the map.
     */
    private static void add(File baseDir, String name, Map m) {
	if (name != null) {
	    add(baseDir, new String[] {name}, m);
	}
    }

    /**
     * Either returns its argument or a plaeholder if the argument is null.
     */
    private static File getKeyFile(File f) {
	return f == null ? NULL_FILE_PLACEHOLDER : f;
    }
}
