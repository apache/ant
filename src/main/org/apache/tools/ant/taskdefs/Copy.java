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
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
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
import java.util.Enumeration;
import java.util.Hashtable;
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
import org.apache.tools.ant.util.FileNameMapper;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.FlatFileNameMapper;
import org.apache.tools.ant.util.IdentityMapper;
import org.apache.tools.ant.util.SourceFileScanner;

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
 * @author Glenn McAllister
 *         <a href="mailto:glennm@ca.ibm.com">glennm@ca.ibm.com</a>
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @author <A href="gholam@xtra.co.nz">Michael McCallum</A>
 * @author Magesh Umasankar
 *
 * @version $Revision$
 *
 * @since Ant 1.2
 *
 * @ant.task category="filesystem"
 */
public class Copy extends Task {
    protected File file = null;     // the source file
    protected File destFile = null; // the destination file
    protected File destDir = null;  // the destination directory
    protected Vector filesets = new Vector();

    protected boolean filtering = false;
    protected boolean preserveLastModified = false;
    protected boolean forceOverwrite = false;
    protected boolean flatten = false;
    protected int verbosity = Project.MSG_VERBOSE;
    protected boolean includeEmpty = true;
    private boolean failonerror = true;

    protected Hashtable fileCopyMap = new Hashtable();
    protected Hashtable dirCopyMap = new Hashtable();
    protected Hashtable completeDirMap = new Hashtable();

    protected Mapper mapperElement = null;
    private Vector filterChains = new Vector();
    private Vector filterSets = new Vector();
    private FileUtils fileUtils;
    private String encoding = null;

    /**
     * Copy task constructor.
     */
    public Copy() {
        fileUtils = FileUtils.newFileUtils();
    }

    protected FileUtils getFileUtils() {
        return fileUtils;
    }

    /**
     * Sets a single source file to copy.
     */
    public void setFile(File file) {
        this.file = file;
    }

    /**
     * Sets the destination file.
     */
    public void setTofile(File destFile) {
        this.destFile = destFile;
    }

    /**
     * Sets the destination directory.
     */
    public void setTodir(File destDir) {
        this.destDir = destDir;
    }

    /**
     * Adds a FilterChain.
     */
    public FilterChain createFilterChain() {
        FilterChain filterChain = new FilterChain();
        filterChains.addElement(filterChain);
        return filterChain;
    }

    /**
     * Adds a filterset.
     */
    public FilterSet createFilterSet() {
        FilterSet filterSet = new FilterSet();
        filterSets.addElement(filterSet);
        return filterSet;
    }

    /**
     * Give the copied files the same last modified time as the original files.
     * @deprecated setPreserveLastModified(String) has been deprecated and
     *             replaced with setPreserveLastModified(boolean) to
     *             consistently let the Introspection mechanism work.
     */
    public void setPreserveLastModified(String preserve) {
        setPreserveLastModified(Project.toBoolean(preserve));
    }

    /**
     * Give the copied files the same last modified time as the original files.
     */
    public void setPreserveLastModified(boolean preserve) {
        preserveLastModified = preserve;
    }

    /**
     * Whether to give the copied files the same last modified time as
     * the original files.
     *
     * @since 1.32, Ant 1.5
     */
    public boolean getPreserveLastModified() {
        return preserveLastModified;
    }

    /**
     * Get the filtersets being applied to this operation.
     *
     * @return a vector of FilterSet objects
     */
    protected Vector getFilterSets() {
        return filterSets;
    }

    /**
     * Get the filterchains being applied to this operation.
     *
     * @return a vector of FilterChain objects
     */
    protected Vector getFilterChains() {
        return filterChains;
    }

    /**
     * If true, enables filtering.
     */
    public void setFiltering(boolean filtering) {
        this.filtering = filtering;
    }

    /**
     * Overwrite any existing destination file(s).
     */
    public void setOverwrite(boolean overwrite) {
        this.forceOverwrite = overwrite;
    }

    /**
     * When copying directory trees, the files can be "flattened"
     * into a single directory.  If there are multiple files with
     * the same name in the source directory tree, only the first
     * file will be copied into the "flattened" directory, unless
     * the forceoverwrite attribute is true.
     */
    public void setFlatten(boolean flatten) {
        this.flatten = flatten;
    }

    /**
     * Used to force listing of all names of copied files.
     */
    public void setVerbose(boolean verbose) {
        if (verbose) {
            this.verbosity = Project.MSG_INFO;
        } else {
            this.verbosity = Project.MSG_VERBOSE;
        }
    }

    /**
     * Used to copy empty directories.
     */
    public void setIncludeEmptyDirs(boolean includeEmpty) {
        this.includeEmpty = includeEmpty;
    }

    /**
     * If false, note errors to the output but keep going.
     * @param failonerror true or false
     */
     public void setFailOnError(boolean failonerror) {
         this.failonerror = failonerror;
     }

    /**
     * Adds a set of files to copy.
     */
    public void addFileset(FileSet set) {
        filesets.addElement(set);
    }

    /**
     * Defines the mapper to map source to destination files.
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
     * Sets the character encoding
     *
     * @since 1.32, Ant 1.5
     */
    public void setEncoding (String encoding) {
        this.encoding = encoding;
    }

    /**
     * @return the character encoding, <code>null</code> if not set.
     *
     * @since 1.32, Ant 1.5
     */
    public String getEncoding() {
        return encoding;
    }

    /**
     * Performs the copy operation.
     */
    public void execute() throws BuildException {
        File savedFile = file; // may be altered in validateAttributes
        File savedDestFile = destFile;
        File savedDestDir = destDir;
        FileSet savedFileSet = null;
        if (file == null && destFile != null && filesets.size() == 1) {
            // will be removed in validateAttributes
            savedFileSet = (FileSet) filesets.elementAt(0);
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

                    if (forceOverwrite ||
                        !destFile.exists() ||
                        (file.lastModified() > destFile.lastModified())) {
                        fileCopyMap.put(file.getAbsolutePath(),
                                        destFile.getAbsolutePath());
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

            // deal with the filesets
            for (int i = 0; i < filesets.size(); i++) {
                FileSet fs = (FileSet) filesets.elementAt(i);
                DirectoryScanner ds = fs.getDirectoryScanner(getProject());
                File fromDir = fs.getDir(getProject());

                String[] srcFiles = ds.getIncludedFiles();
                String[] srcDirs = ds.getIncludedDirectories();
                boolean isEverythingIncluded = ds.isEverythingIncluded();
                if (isEverythingIncluded
                    && !flatten && mapperElement == null) {
                    completeDirMap.put(fromDir, destDir);
                }
                scan(fromDir, destDir, srcFiles, srcDirs);
            }

            // do all the copy operations now...
            doFileOperations();
        } finally {
            // clean up again, so this instance can be used a second
            // time
            file = savedFile;
            destFile = savedDestFile;
            destDir = savedDestDir;
            if (savedFileSet != null) {
                filesets.insertElementAt(savedFileSet, 0);
            }

            fileCopyMap.clear();
            dirCopyMap.clear();
            completeDirMap.clear();
        }
    }

//************************************************************************
//  protected and private methods
//************************************************************************

    /**
     * Ensure we have a consistent and legal set of attributes, and set
     * any internal flags necessary based on different combinations
     * of attributes.
     */
    protected void validateAttributes() throws BuildException {
        if (file == null && filesets.size() == 0) {
            throw new BuildException("Specify at least one source "
                                     + "- a file or a fileset.");
        }

        if (destFile != null && destDir != null) {
            throw new BuildException("Only one of tofile and todir "
                                     + "may be set.");
        }

        if (destFile == null && destDir == null) {
            throw new BuildException("One of tofile or todir must be set.");
        }

        if (file != null && file.exists() && file.isDirectory()) {
            throw new BuildException("Use a fileset to copy directories.");
        }

        if (destFile != null && filesets.size() > 0) {
            if (filesets.size() > 1) {
                throw new BuildException(
                    "Cannot concatenate multiple files into a single file.");
            } else {
                FileSet fs = (FileSet) filesets.elementAt(0);
                DirectoryScanner ds = fs.getDirectoryScanner(getProject());
                String[] srcFiles = ds.getIncludedFiles();

                if (srcFiles.length == 0) {
                    throw new BuildException(
                        "Cannot perform operation from directory to file.");
                } else if (srcFiles.length == 1) {
                    if (file == null) {
                        file = new File(ds.getBasedir(), srcFiles[0]);
                        filesets.removeElementAt(0);
                    } else {
                        throw new BuildException("Cannot concatenate multiple "
                                                 + "files into a single file.");
                    }
                } else {
                    throw new BuildException("Cannot concatenate multiple "
                                             + "files into a single file.");
                }
            }
        }

        if (destFile != null) {
            destDir = fileUtils.getParentFile(destFile);
        }

    }

    /**
     * Compares source files to destination files to see if they should be
     * copied.
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
            toCopy = ds.restrict(names, fromDir, toDir, mapper);
        }

        for (int i = 0; i < toCopy.length; i++) {
            File src = new File(fromDir, toCopy[i]);
            File dest = new File(toDir, mapper.mapFileName(toCopy[i])[0]);
            map.put(src.getAbsolutePath(), dest.getAbsolutePath());
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
                String toFile = (String) fileCopyMap.get(fromFile);

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
                                       preserveLastModified, encoding,
                                       getProject());
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

        if (includeEmpty) {
            Enumeration e = dirCopyMap.elements();
            int count = 0;
            while (e.hasMoreElements()) {
                File d = new File((String) e.nextElement());
                if (!d.exists()) {
                    if (!d.mkdirs()) {
                        log("Unable to create directory "
                            + d.getAbsolutePath(), Project.MSG_ERR);
                    } else {
                        count++;
                    }
                }
            }

            if (count > 0) {
                log("Copied " + count +
                    " empty director" +
                    (count == 1 ? "y" : "ies") +
                    " to " + destDir.getAbsolutePath());
            }
        }
    }

}
