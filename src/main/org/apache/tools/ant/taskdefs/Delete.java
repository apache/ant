/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2002 The Apache Software Foundation.  All rights 
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

import org.apache.tools.ant.Project;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.PatternSet;
import org.apache.tools.ant.types.selectors.AndSelector;
import org.apache.tools.ant.types.selectors.ContainsSelector;
import org.apache.tools.ant.types.selectors.DateSelector;
import org.apache.tools.ant.types.selectors.DependSelector;
import org.apache.tools.ant.types.selectors.DepthSelector;
import org.apache.tools.ant.types.selectors.ExtendSelector;
import org.apache.tools.ant.types.selectors.FileSelector;
import org.apache.tools.ant.types.selectors.FilenameSelector;
import org.apache.tools.ant.types.selectors.MajoritySelector;
import org.apache.tools.ant.types.selectors.NoneSelector;
import org.apache.tools.ant.types.selectors.NotSelector;
import org.apache.tools.ant.types.selectors.OrSelector;
import org.apache.tools.ant.types.selectors.PresentSelector;
import org.apache.tools.ant.types.selectors.SelectSelector;
import org.apache.tools.ant.types.selectors.SelectorContainer;
import org.apache.tools.ant.types.selectors.SizeSelector;
import java.io.File;
import java.util.Vector;

/**
 * Deletes a file or directory, or set of files defined by a fileset.
 * The original delete task would delete a file, or a set of files 
 * using the include/exclude syntax.  The deltree task would delete a 
 * directory tree.  This task combines the functionality of these two
 * originally distinct tasks.
 * <p>Currently Delete extends MatchingTask.  This is intend <i>only</i>
 * to provide backwards compatibility for a release.  The future position
 * is to use nested filesets exclusively.</p>
 * 
 * @author Stefano Mazzocchi 
 *         <a href="mailto:stefano@apache.org">stefano@apache.org</a>
 * @author Tom Dimock <a href="mailto:tad1@cornell.edu">tad1@cornell.edu</a>
 * @author Glenn McAllister 
 *         <a href="mailto:glennm@ca.ibm.com">glennm@ca.ibm.com</a>
 * @author Jon S. Stevens <a href="mailto:jon@latchkey.com">jon@latchkey.com</a>
 *
 * @since Ant 1.2
 *
 * @ant.task category="filesystem"
 */
public class Delete extends MatchingTask {
    protected File file = null;
    protected File dir = null;
    protected Vector filesets = new Vector();
    protected boolean usedMatchingTask = false;
    // by default, remove matching empty dirs
    protected boolean includeEmpty = false;

    private int verbosity = Project.MSG_VERBOSE;
    private boolean quiet = false;
    private boolean failonerror = true;

    /**
     * Set the name of a single file to be removed.
     *
     * @param file the file to be deleted
     */
    public void setFile(File file) {
        this.file = file;
    }

    /**
     * Set the directory from which files are to be deleted
     *
     * @param dir the directory path.
     */
    public void setDir(File dir) {
        this.dir = dir;
    }

    /**
     * If true, list all names of deleted files.
     *
     * @param verbose "true" or "on"
     */
    public void setVerbose(boolean verbose) {
        if (verbose) {
            this.verbosity = Project.MSG_INFO;
        } else {
            this.verbosity = Project.MSG_VERBOSE;
        }
    }

    /**
     * If true and the file does not exist, do not display a diagnostic
     * message or modify the exit status to reflect an error.
     * This means that if a file or directory cannot be deleted,
     * then no error is reported. This setting emulates the
     * -f option to the Unix &quot;rm&quot; command.
     * Default is false meaning things are &quot;noisy&quot;
     * @param quiet "true" or "on"
     */
    public void setQuiet(boolean quiet) {
        this.quiet = quiet;
        if (quiet) {
            this.failonerror = false;
        }
    }

    /**
     * If false, note errors but continue.
     *
     * @param failonerror true or false
     */
     public void setFailOnError(boolean failonerror) {
         this.failonerror = failonerror;
     }


    /**
     * If true, delete empty directories.
     */
    public void setIncludeEmptyDirs(boolean includeEmpty) {
        this.includeEmpty = includeEmpty;
    }

   /**
     * Adds a set of files to be deleted.
     */
    public void addFileset(FileSet set) {
        filesets.addElement(set);
    }

    /**
     * add a name entry on the include list
     */
    public PatternSet.NameEntry createInclude() {
        usedMatchingTask = true;
        return super.createInclude();
    }

    /**
     * add a name entry on the include files list
     */
    public PatternSet.NameEntry createIncludesFile() {
        usedMatchingTask = true;
        return super.createIncludesFile();
    }
    
    /**
     * add a name entry on the exclude list
     */
    public PatternSet.NameEntry createExclude() {
        usedMatchingTask = true;
        return super.createExclude();
    }

    /**
     * add a name entry on the include files list
     */
    public PatternSet.NameEntry createExcludesFile() {
        usedMatchingTask = true;
        return super.createExcludesFile();
    }
    
    /**
     * add a set of patterns
     */
    public PatternSet createPatternSet() {
        usedMatchingTask = true;
        return super.createPatternSet();
    }

    /**
     * Sets the set of include patterns. Patterns may be separated by a comma
     * or a space.
     *
     * @param includes the string containing the include patterns
     */
    public void setIncludes(String includes) {
        usedMatchingTask = true;
        super.setIncludes(includes);
    }

    /**
     * Sets the set of exclude patterns. Patterns may be separated by a comma
     * or a space.
     *
     * @param excludes the string containing the exclude patterns
     */
    public void setExcludes(String excludes) {
        usedMatchingTask = true;
        super.setExcludes(excludes);
    }

    /**
     * Sets whether default exclusions should be used or not.
     *
     * @param useDefaultExcludes "true"|"on"|"yes" when default exclusions
     *                           should be used, "false"|"off"|"no" when they
     *                           shouldn't be used.
     */
    public void setDefaultexcludes(boolean useDefaultExcludes) {
        usedMatchingTask = true;
        super.setDefaultexcludes(useDefaultExcludes);
    }

    /**
     * Sets the name of the file containing the includes patterns.
     *
     * @param includesfile A string containing the filename to fetch
     * the include patterns from.
     */
    public void setIncludesfile(File includesfile) {
        usedMatchingTask = true;
        super.setIncludesfile(includesfile);
    }

    /**
     * Sets the name of the file containing the includes patterns.
     *
     * @param excludesfile A string containing the filename to fetch
     * the include patterns from.
     */
    public void setExcludesfile(File excludesfile) {
        usedMatchingTask = true;
        super.setExcludesfile(excludesfile);
    }

    /**
     * Sets case sensitivity of the file system
     *
     * @param isCaseSensitive "true"|"on"|"yes" if file system is case
     *                           sensitive, "false"|"off"|"no" when not.
     */
    public void setCaseSensitive(boolean isCaseSensitive) {
        usedMatchingTask = true;
        super.setCaseSensitive(isCaseSensitive);
    }

    /**
     * Sets whether or not symbolic links should be followed.
     *
     * @param followSymlinks whether or not symbolic links should be followed
     */
    public void setFollowSymlinks(boolean followSymlinks) {
        usedMatchingTask = true;
        super.setFollowSymlinks(followSymlinks);
    }

    /**
     * add a "Select" selector entry on the selector list
     */
    public void addSelector(SelectSelector selector) {
        usedMatchingTask = true;
        super.addSelector(selector);
    }

    /**
     * add an "And" selector entry on the selector list
     */
    public void addAnd(AndSelector selector) {
        usedMatchingTask = true;
        super.addAnd(selector);
    }

    /**
     * add an "Or" selector entry on the selector list
     */
    public void addOr(OrSelector selector) {
        usedMatchingTask = true;
        super.addOr(selector);
    }

    /**
     * add a "Not" selector entry on the selector list
     */
    public void addNot(NotSelector selector) {
        usedMatchingTask = true;
        super.addNot(selector);
    }

    /**
     * add a "None" selector entry on the selector list
     */
    public void addNone(NoneSelector selector) {
        usedMatchingTask = true;
        super.addNone(selector);
    }

    /**
     * add a majority selector entry on the selector list
     */
    public void addMajority(MajoritySelector selector) {
        usedMatchingTask = true;
        super.addMajority(selector);
    }

    /**
     * add a selector date entry on the selector list
     */
    public void addDate(DateSelector selector) {
        usedMatchingTask = true;
        super.addDate(selector);
    }

    /**
     * add a selector size entry on the selector list
     */
    public void addSize(SizeSelector selector) {
        usedMatchingTask = true;
        super.addSize(selector);
    }

    /**
     * add a selector filename entry on the selector list
     */
    public void addFilename(FilenameSelector selector) {
        usedMatchingTask = true;
        super.addFilename(selector);
    }

    /**
     * add an extended selector entry on the selector list
     */
    public void addCustom(ExtendSelector selector) {
        usedMatchingTask = true;
        super.addCustom(selector);
    }

    /**
     * add a contains selector entry on the selector list
     */
    public void addContains(ContainsSelector selector) {
        usedMatchingTask = true;
        super.addContains(selector);
    }

    /**
     * add a present selector entry on the selector list
     */
    public void addPresent(PresentSelector selector) {
        usedMatchingTask = true;
        super.addPresent(selector);
    }

    /**
     * add a depth selector entry on the selector list
     */
    public void addDepth(DepthSelector selector) {
        usedMatchingTask = true;
        super.addDepth(selector);
    }

    /**
     * add a depends selector entry on the selector list
     */
    public void addDepend(DependSelector selector) {
        usedMatchingTask = true;
        super.addDepend(selector);
    }

    /**
     * Delete the file(s).
     */
    public void execute() throws BuildException {
        if (usedMatchingTask) {
            log("DEPRECATED - Use of the implicit FileSet is deprecated.  "
                + "Use a nested fileset element instead.");
        }

        if (file == null && dir == null && filesets.size() == 0) {
            throw new BuildException("At least one of the file or dir "
                                     + "attributes, or a fileset element, "
                                     + "must be set.");
        }

        if (quiet && failonerror) {
            throw new BuildException("quiet and failonerror cannot both be "
                                     + "set to true", getLocation());
        }
        

        // delete the single file
        if (file != null) {
            if (file.exists()) {
                if (file.isDirectory()) {
                    log("Directory " + file.getAbsolutePath() 
                        + " cannot be removed using the file attribute.  "
                        + "Use dir instead.");
                } else {
                    log("Deleting: " + file.getAbsolutePath());

                    if (!file.delete()) {
                        String message = "Unable to delete file " 
                            + file.getAbsolutePath();
                        if (failonerror) {
                            throw new BuildException(message);
                        } else { 
                            log(message, quiet ? Project.MSG_VERBOSE 
                                               : Project.MSG_WARN);
                        }
                    }
                }
            } else {
                log("Could not find file " + file.getAbsolutePath() 
                    + " to delete.", 
                    Project.MSG_VERBOSE);
            }
        }

        // delete the directory
        if (dir != null && dir.exists() && dir.isDirectory() && 
            !usedMatchingTask) {
            /*
               If verbosity is MSG_VERBOSE, that mean we are doing
               regular logging (backwards as that sounds).  In that
               case, we want to print one message about deleting the
               top of the directory tree.  Otherwise, the removeDir
               method will handle messages for _all_ directories.
             */
            if (verbosity == Project.MSG_VERBOSE) {
                log("Deleting directory " + dir.getAbsolutePath());
            }
            removeDir(dir);
        }

        // delete the files in the filesets
        for (int i = 0; i < filesets.size(); i++) {
            FileSet fs = (FileSet) filesets.elementAt(i);
            try {
                DirectoryScanner ds = fs.getDirectoryScanner(getProject());
                String[] files = ds.getIncludedFiles();
                String[] dirs = ds.getIncludedDirectories();
                removeFiles(fs.getDir(getProject()), files, dirs);
            } catch (BuildException be) {
                // directory doesn't exist or is not readable
                if (failonerror) {
                    throw be;
                } else {
                    log(be.getMessage(), 
                        quiet ? Project.MSG_VERBOSE : Project.MSG_WARN);
                }
            }
        }

        // delete the files from the default fileset
        if (usedMatchingTask && dir != null) {
            try {
                DirectoryScanner ds = super.getDirectoryScanner(dir);
                String[] files = ds.getIncludedFiles();
                String[] dirs = ds.getIncludedDirectories();
                removeFiles(dir, files, dirs);
            } catch (BuildException be) {
                // directory doesn't exist or is not readable
                if (failonerror) {
                    throw be;
                } else {
                    log(be.getMessage(), 
                        quiet ? Project.MSG_VERBOSE : Project.MSG_WARN);
                }
            }
        }
    }

//************************************************************************
//  protected and private methods
//************************************************************************

    protected void removeDir(File d) {
        String[] list = d.list();
        if (list == null) {
            list = new String[0];
        }
        for (int i = 0; i < list.length; i++) {
            String s = list[i];
            File f = new File(d, s);
            if (f.isDirectory()) {
                removeDir(f);
            } else {
                log("Deleting " + f.getAbsolutePath(), verbosity);
                if (!f.delete()) {
                    String message = "Unable to delete file " 
                        + f.getAbsolutePath();
                    if (failonerror) {
                        throw new BuildException(message);
                    } else {
                        log(message,
                            quiet ? Project.MSG_VERBOSE : Project.MSG_WARN);
                    }
                }
            }
        }
        log("Deleting directory " + d.getAbsolutePath(), verbosity);
        if (!d.delete()) {
            String message = "Unable to delete directory " 
                + dir.getAbsolutePath();
            if (failonerror) {
                throw new BuildException(message);
            } else {
                log(message,
                    quiet ? Project.MSG_VERBOSE : Project.MSG_WARN);
            }
        }
    }

    /**
     * remove an array of files in a directory, and a list of subdirectories
     * which will only be deleted if 'includeEmpty' is true
     * @param d directory to work from
     * @param files array of files to delete; can be of zero length
     * @param dirs array of directories to delete; can of zero length
     */
    protected void removeFiles(File d, String[] files, String[] dirs) {
        if (files.length > 0) {
            log("Deleting " + files.length + " files from " 
                + d.getAbsolutePath());
            for (int j = 0; j < files.length; j++) {
                File f = new File(d, files[j]);
                log("Deleting " + f.getAbsolutePath(), verbosity);
                if (!f.delete()) {
                    String message = "Unable to delete file " 
                        + f.getAbsolutePath();
                    if (failonerror) {
                        throw new BuildException(message);
                    } else {
                        log(message,
                            quiet ? Project.MSG_VERBOSE : Project.MSG_WARN);
                    }
                }
            }
        }

        if (dirs.length > 0 && includeEmpty) {
            int dirCount = 0;
            for (int j = dirs.length - 1; j >= 0; j--) {
                File dir = new File(d, dirs[j]);
                String[] dirFiles = dir.list();
                if (dirFiles == null || dirFiles.length == 0) {
                    log("Deleting " + dir.getAbsolutePath(), verbosity);
                    if (!dir.delete()) {
                        String message = "Unable to delete directory "
                                + dir.getAbsolutePath();
                        if (failonerror) {
                            throw new BuildException(message);
                        } else {
                            log(message,
                                quiet ? Project.MSG_VERBOSE : Project.MSG_WARN);
                        }
                    } else {
                        dirCount++;
                    }
                }
            }

            if (dirCount > 0) {
                log("Deleted " + dirCount + " director" +
                    (dirCount == 1 ? "y" : "ies") +
                    " from " + d.getAbsolutePath());
            }
        }
    }
}

