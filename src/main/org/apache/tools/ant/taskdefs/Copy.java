/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999 The Apache Software Foundation.  All rights
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

import org.apache.tools.ant.*;
import org.apache.tools.ant.types.*;
import org.apache.tools.ant.util.*;

import java.io.*;
import java.util.*;

/**
 * A consolidated copy task.  Copies a file or directory to a new file 
 * or directory.  Files are only copied if the source file is newer
 * than the destination file, or when the destination file does not 
 * exist.  It is possible to explicitly overwrite existing files.</p>
 *
 * <p>This implementation is based on Arnout Kuiper's initial design
 * document, the following mailing list discussions, and the 
 * copyfile/copydir tasks.</p>
 *
 * @author Glenn McAllister <a href="mailto:glennm@ca.ibm.com">glennm@ca.ibm.com</a>
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
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

    protected Hashtable fileCopyMap = new Hashtable();
    protected Hashtable dirCopyMap = new Hashtable();

    protected Mapper mapperElement = null;

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
     * Give the copied files the same last modified time as the original files.
     */
    public void setPreserveLastModified(String preserve) {
        preserveLastModified = Project.toBoolean(preserve);
    }

    /**
     * Sets filtering.
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
     * Adds a set of files (nested fileset attribute).
     */
    public void addFileset(FileSet set) {
        filesets.addElement(set);
    }

    /**
     * Defines the FileNameMapper to use (nested mapper element).
     */
    public Mapper createMapper() throws BuildException {
        if (mapperElement != null) {
            throw new BuildException("Cannot define more than one mapper",
                                     location);
        }
        mapperElement = new Mapper(project);
        return mapperElement;
    }

    /**
     * Performs the copy operation.
     */
    public void execute() throws BuildException {
        // make sure we don't have an illegal set of options
        validateAttributes();   

        // deal with the single file
        if (file != null) {
            if (file.exists()) {
                if (destFile == null) {
                    destFile = new File(destDir, file.getName());
                }
                
                if (forceOverwrite || 
                    (file.lastModified() > destFile.lastModified())) {
                    fileCopyMap.put(file.getAbsolutePath(), destFile.getAbsolutePath());
                }
            } else {
                log("Could not find file " + file.getAbsolutePath() + " to copy.");
            }
        }

        // deal with the filesets
        for (int i=0; i<filesets.size(); i++) {
            FileSet fs = (FileSet) filesets.elementAt(i);
            DirectoryScanner ds = fs.getDirectoryScanner(project);
            File fromDir = fs.getDir(project);

            String[] srcFiles = ds.getIncludedFiles();
            String[] srcDirs = ds.getIncludedDirectories();

            scan(fromDir, destDir, srcFiles, srcDirs);
        }

        // do all the copy operations now...
        doFileOperations();

        // clean up destDir again - so this instance can be used a second
        // time without throwing an exception
        if (destFile != null) {
            destDir = null;
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
            throw new BuildException("Specify at least one source - a file or a fileset.");
        }

        if (destFile != null && destDir != null) {
            throw new BuildException("Only one of destfile and destdir may be set.");
        }

        if (destFile == null && destDir == null) {
            throw new BuildException("One of destfile or destdir must be set.");
        }

        if (file != null && file.exists() && file.isDirectory()) {
            throw new BuildException("Use a fileset to copy directories.");
        }
           
        if (destFile != null && filesets.size() > 0) {
            throw new BuildException("Cannot concatenate multple files into a single file.");
        }

        if (destFile != null) {
            destDir = new File(destFile.getParent());   // be 1.1 friendly
        }

    }

    /**
     * Compares source files to destination files to see if they should be
     * copied.
     */
    protected void scan(File fromDir, File toDir, String[] files, String[] dirs) {
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
            for (int i=0; i<names.length; i++) {
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
            map.put( src.getAbsolutePath(), dest.getAbsolutePath() );
        }
    }

    /**
     * Actually does the file (and possibly empty directory) copies.
     * This is a good method for subclasses to override.
     */
    protected void doFileOperations() {
        if (fileCopyMap.size() > 0) {
            log("Copying " + fileCopyMap.size() + 
                " file" + (fileCopyMap.size() == 1 ? "" : "s") + 
                " to " + destDir.getAbsolutePath() );

            Enumeration e = fileCopyMap.keys();
            while (e.hasMoreElements()) {
                String fromFile = (String) e.nextElement();
                String toFile = (String) fileCopyMap.get(fromFile);

                if( fromFile.equals( toFile ) ) {
                    log("Skipping self-copy of " + fromFile, verbosity);
                    continue;
                }

                try {
                    log("Copying " + fromFile + " to " + toFile, verbosity);
                    
                    project.copyFile(fromFile, 
                                     toFile, 
                                     filtering, 
                                     forceOverwrite,
                                     preserveLastModified);
                } catch (IOException ioe) {
                    String msg = "Failed to copy " + fromFile + " to " + toFile
                        + " due to " + ioe.getMessage();
                    throw new BuildException(msg, ioe, location);
                }
            }
        }

        if (includeEmpty) {
            Enumeration e = dirCopyMap.elements();
            int count = 0;
            while (e.hasMoreElements()) {
                File d = new File((String)e.nextElement());
                if (!d.exists()) {
                    if (!d.mkdirs()) {
                        log("Unable to create directory " + d.getAbsolutePath(), Project.MSG_ERR);
                    } else {
                        count++;
                    }
                }
            }

            if (count > 0) {
                log("Copied " + count + 
                    " empty director" + 
                    (count==1?"y":"ies") + 
                    " to " + destDir.getAbsolutePath());
            }
        }
    }

}
