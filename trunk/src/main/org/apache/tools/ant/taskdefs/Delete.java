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

import java.io.File;
import java.util.Arrays;
import java.util.Vector;
import java.util.Iterator;
import java.util.Comparator;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.PatternSet;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.Sort;
import org.apache.tools.ant.types.resources.Restrict;
import org.apache.tools.ant.types.resources.Resources;
import org.apache.tools.ant.types.resources.FileResource;
import org.apache.tools.ant.types.resources.FileResourceIterator;
import org.apache.tools.ant.types.resources.comparators.Reverse;
import org.apache.tools.ant.types.resources.comparators.FileSystem;
import org.apache.tools.ant.types.resources.comparators.ResourceComparator;
import org.apache.tools.ant.types.resources.selectors.Exists;
import org.apache.tools.ant.types.resources.selectors.ResourceSelector;
import org.apache.tools.ant.types.selectors.OrSelector;
import org.apache.tools.ant.types.selectors.AndSelector;
import org.apache.tools.ant.types.selectors.NotSelector;
import org.apache.tools.ant.types.selectors.DateSelector;
import org.apache.tools.ant.types.selectors.FileSelector;
import org.apache.tools.ant.types.selectors.NoneSelector;
import org.apache.tools.ant.types.selectors.SizeSelector;
import org.apache.tools.ant.types.selectors.DepthSelector;
import org.apache.tools.ant.types.selectors.DependSelector;
import org.apache.tools.ant.types.selectors.ExtendSelector;
import org.apache.tools.ant.types.selectors.SelectSelector;
import org.apache.tools.ant.types.selectors.PresentSelector;
import org.apache.tools.ant.types.selectors.ContainsSelector;
import org.apache.tools.ant.types.selectors.FilenameSelector;
import org.apache.tools.ant.types.selectors.MajoritySelector;
import org.apache.tools.ant.types.selectors.ContainsRegexpSelector;
import org.apache.tools.ant.types.selectors.modifiedselector.ModifiedSelector;

/**
 * Deletes a file or directory, or set of files defined by a fileset.
 * The original delete task would delete a file, or a set of files
 * using the include/exclude syntax.  The deltree task would delete a
 * directory tree.  This task combines the functionality of these two
 * originally distinct tasks.
 * <p>Currently Delete extends MatchingTask.  This is intended <i>only</i>
 * to provide backwards compatibility for a release.  The future position
 * is to use nested filesets exclusively.</p>
 *
 * @since Ant 1.2
 *
 * @ant.task category="filesystem"
 */
public class Delete extends MatchingTask {
    private static final int DELETE_RETRY_SLEEP_MILLIS = 10;
    private static final ResourceComparator REVERSE_FILESYSTEM = new Reverse(new FileSystem());
    private static final ResourceSelector EXISTS = new Exists();

    private static class ReverseDirs implements ResourceCollection {
        static final Comparator REVERSE = new Comparator() {
            public int compare(Object foo, Object bar) {
                return ((Comparable) foo).compareTo(bar) * -1;
            }
        };
        private File basedir;
        private String[] dirs;
        ReverseDirs(File basedir, String[] dirs) {
            this.basedir = basedir;
            this.dirs = dirs;
            Arrays.sort(this.dirs, REVERSE);
        }
        public Iterator iterator() {
            return new FileResourceIterator(basedir, dirs);
        }
        public boolean isFilesystemOnly() { return true; }
        public int size() { return dirs.length; }
    }

    // CheckStyle:VisibilityModifier OFF - bc
    protected File file = null;
    protected File dir = null;
    protected Vector filesets = new Vector();
    protected boolean usedMatchingTask = false;
    // by default, remove matching empty dirs
    protected boolean includeEmpty = false;

    private int verbosity = Project.MSG_VERBOSE;
    private boolean quiet = false;
    private boolean failonerror = true;
    private boolean deleteOnExit = false;
    private Resources rcs = null;
    // CheckStyle:VisibilityModifier ON

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
        getImplicitFileSet().setDir(dir);
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
     * If true, on failure to delete, note the error and set
     * the deleteonexit flag, and continue
     *
     * @param deleteOnExit true or false
     */
     public void setDeleteOnExit(boolean deleteOnExit) {
         this.deleteOnExit = deleteOnExit;
     }


    /**
     * If true, delete empty directories.
     * @param includeEmpty if true delete empty directories (only
     *                     for filesets). Default is false.
     */
    public void setIncludeEmptyDirs(boolean includeEmpty) {
        this.includeEmpty = includeEmpty;
    }

   /**
    * Adds a set of files to be deleted.
    * @param set the set of files to be deleted
    */
    public void addFileset(FileSet set) {
        filesets.addElement(set);
    }

    /**
     * Add an arbitrary ResourceCollection to be deleted.
     * @param rc the filesystem-only ResourceCollection.
     */
    public void add(ResourceCollection rc) {
        if (rc == null) {
            return;
        }
        rcs = (rcs == null) ? new Resources() : rcs;
        rcs.add(rc);
    }

    /**
     * add a name entry on the include list
     * @return a NameEntry object to be configured
     */
    public PatternSet.NameEntry createInclude() {
        usedMatchingTask = true;
        return super.createInclude();
    }

    /**
     * add a name entry on the include files list
     * @return an NameEntry object to be configured
     */
    public PatternSet.NameEntry createIncludesFile() {
        usedMatchingTask = true;
        return super.createIncludesFile();
    }

    /**
     * add a name entry on the exclude list
     * @return an NameEntry object to be configured
     */
    public PatternSet.NameEntry createExclude() {
        usedMatchingTask = true;
        return super.createExclude();
    }

    /**
     * add a name entry on the include files list
     * @return an NameEntry object to be configured
     */
    public PatternSet.NameEntry createExcludesFile() {
        usedMatchingTask = true;
        return super.createExcludesFile();
    }

    /**
     * add a set of patterns
     * @return PatternSet object to be configured
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
     * @param selector the selector to be added
     */
    public void addSelector(SelectSelector selector) {
        usedMatchingTask = true;
        super.addSelector(selector);
    }

    /**
     * add an "And" selector entry on the selector list
     * @param selector the selector to be added
     */
    public void addAnd(AndSelector selector) {
        usedMatchingTask = true;
        super.addAnd(selector);
    }

    /**
     * add an "Or" selector entry on the selector list
     * @param selector the selector to be added
     */
    public void addOr(OrSelector selector) {
        usedMatchingTask = true;
        super.addOr(selector);
    }

    /**
     * add a "Not" selector entry on the selector list
     * @param selector the selector to be added
     */
    public void addNot(NotSelector selector) {
        usedMatchingTask = true;
        super.addNot(selector);
    }

    /**
     * add a "None" selector entry on the selector list
     * @param selector the selector to be added
     */
    public void addNone(NoneSelector selector) {
        usedMatchingTask = true;
        super.addNone(selector);
    }

    /**
     * add a majority selector entry on the selector list
     * @param selector the selector to be added
     */
    public void addMajority(MajoritySelector selector) {
        usedMatchingTask = true;
        super.addMajority(selector);
    }

    /**
     * add a selector date entry on the selector list
     * @param selector the selector to be added
     */
    public void addDate(DateSelector selector) {
        usedMatchingTask = true;
        super.addDate(selector);
    }

    /**
     * add a selector size entry on the selector list
     * @param selector the selector to be added
     */
    public void addSize(SizeSelector selector) {
        usedMatchingTask = true;
        super.addSize(selector);
    }

    /**
     * add a selector filename entry on the selector list
     * @param selector the selector to be added
     */
    public void addFilename(FilenameSelector selector) {
        usedMatchingTask = true;
        super.addFilename(selector);
    }

    /**
     * add an extended selector entry on the selector list
     * @param selector the selector to be added
     */
    public void addCustom(ExtendSelector selector) {
        usedMatchingTask = true;
        super.addCustom(selector);
    }

    /**
     * add a contains selector entry on the selector list
     * @param selector the selector to be added
     */
    public void addContains(ContainsSelector selector) {
        usedMatchingTask = true;
        super.addContains(selector);
    }

    /**
     * add a present selector entry on the selector list
     * @param selector the selector to be added
     */
    public void addPresent(PresentSelector selector) {
        usedMatchingTask = true;
        super.addPresent(selector);
    }

    /**
     * add a depth selector entry on the selector list
     * @param selector the selector to be added
     */
    public void addDepth(DepthSelector selector) {
        usedMatchingTask = true;
        super.addDepth(selector);
    }

    /**
     * add a depends selector entry on the selector list
     * @param selector the selector to be added
     */
    public void addDepend(DependSelector selector) {
        usedMatchingTask = true;
        super.addDepend(selector);
    }

    /**
     * add a regular expression selector entry on the selector list
     * @param selector the selector to be added
     */
    public void addContainsRegexp(ContainsRegexpSelector selector) {
        usedMatchingTask = true;
        super.addContainsRegexp(selector);
    }

    /**
     * add the modified selector
     * @param selector the selector to add
     * @since ant 1.6
     */
    public void addModified(ModifiedSelector selector) {
        usedMatchingTask = true;
        super.addModified(selector);
    }

    /**
     * add an arbitrary selector
     * @param selector the selector to be added
     * @since Ant 1.6
     */
    public void add(FileSelector selector) {
        usedMatchingTask = true;
        super.add(selector);
    }

    /**
     * Delete the file(s).
     * @exception BuildException if an error occurs
     */
    public void execute() throws BuildException {
        if (usedMatchingTask) {
            log("DEPRECATED - Use of the implicit FileSet is deprecated.  "
                + "Use a nested fileset element instead.", quiet ? Project.MSG_VERBOSE : verbosity);
        }

        if (file == null && dir == null && filesets.size() == 0 && rcs == null) {
            throw new BuildException("At least one of the file or dir "
                                     + "attributes, or a nested resource collection, "
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
                        + "Use dir instead.", quiet ? Project.MSG_VERBOSE : verbosity);
                } else {
                    log("Deleting: " + file.getAbsolutePath());

                    if (!delete(file)) {
                        handle("Unable to delete file " + file.getAbsolutePath());
                    }
                }
            } else {
                log("Could not find file " + file.getAbsolutePath()
                    + " to delete.", quiet ? Project.MSG_VERBOSE : verbosity);
            }
        }

        // delete the directory
        if (dir != null && dir.exists() && dir.isDirectory()
            && !usedMatchingTask) {
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
        Resources resourcesToDelete = new Resources();
        resourcesToDelete.setProject(getProject());
        Resources filesetDirs = new Resources();
        filesetDirs.setProject(getProject());
        FileSet implicit = null;
        if (usedMatchingTask && dir != null && dir.isDirectory()) {
            //add the files from the default fileset:
            implicit = getImplicitFileSet();
            implicit.setProject(getProject());
            filesets.add(implicit);
        }

        for (int i = 0, size = filesets.size(); i < size; i++) {
            FileSet fs = (FileSet) filesets.get(i);
            if (fs.getProject() == null) {
                log("Deleting fileset with no project specified;"
                    + " assuming executing project", Project.MSG_VERBOSE);
                fs = (FileSet) fs.clone();
                fs.setProject(getProject());
            }
            if (!fs.getDir().isDirectory()) {
                handle("Directory does not exist:" + fs.getDir());
            } else {
                resourcesToDelete.add(fs);
                if (includeEmpty) {
                    filesetDirs.add(new ReverseDirs(fs.getDir(), fs
                            .getDirectoryScanner().getIncludedDirectories()));
                }
            }
        }
        resourcesToDelete.add(filesetDirs);
        if (rcs != null) {
            // sort first to files, then dirs
            Restrict exists = new Restrict();
            exists.add(EXISTS);
            exists.add(rcs);
            Sort s = new Sort();
            s.add(REVERSE_FILESYSTEM);
            s.add(exists);
            resourcesToDelete.add(s);
        }
        try {
            if (resourcesToDelete.isFilesystemOnly()) {
                for (Iterator iter = resourcesToDelete.iterator(); iter.hasNext();) {
                    FileResource r = (FileResource) iter.next();
                    // nonexistent resources could only occur if we already
                    // deleted something from a fileset:
                    if (!r.isExists()) {
                        continue;
                    }
                    if (!(r.isDirectory()) || r.getFile().list().length == 0) {
                        log("Deleting " + r, verbosity);
                        if (!delete(r.getFile()) && failonerror) {
                            handle("Unable to delete "
                                + (r.isDirectory() ? "directory " : "file ") + r);
                        }
                    }
                }
            } else {
                 handle(getTaskName() + " handles only filesystem resources");
            }
        } catch (Exception e) {
            handle(e);
        } finally {
            if (implicit != null) {
                filesets.remove(implicit);
            }
        }
    }

//************************************************************************
//  protected and private methods
//************************************************************************

    private void handle(String msg) {
        handle(new BuildException(msg));
    }

    private void handle(Exception e) {
        if (failonerror) {
            throw (e instanceof BuildException)
                ? (BuildException) e : new BuildException(e);
        }
        log(e, quiet ? Project.MSG_VERBOSE : verbosity);
    }

    /**
     * Accommodate Windows bug encountered in both Sun and IBM JDKs.
     * Others possible. If the delete does not work, call System.gc(),
     * wait a little and try again.
     */
    private boolean delete(File f) {
        if (!f.delete()) {
            if (Os.isFamily("windows")) {
                System.gc();
            }
            try {
                Thread.sleep(DELETE_RETRY_SLEEP_MILLIS);
            } catch (InterruptedException ex) {
                // Ignore Exception
            }
            if (!f.delete()) {
                if (deleteOnExit) {
                    int level = quiet ? Project.MSG_VERBOSE : Project.MSG_INFO;
                    log("Failed to delete " + f + ", calling deleteOnExit."
                        + " This attempts to delete the file when the Ant jvm"
                        + " has exited and might not succeed.", level);
                    f.deleteOnExit();
                    return true;
                }
                return false;
            }
        }
        return true;
    }

    /**
     * Delete a directory
     *
     * @param d the directory to delete
     */
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
                log("Deleting " + f.getAbsolutePath(), quiet ? Project.MSG_VERBOSE : verbosity);
                if (!delete(f)) {
                    handle("Unable to delete file " + f.getAbsolutePath());
                }
            }
        }
        log("Deleting directory " + d.getAbsolutePath(), verbosity);
        if (!delete(d)) {
            handle("Unable to delete directory " + dir.getAbsolutePath());
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
                + d.getAbsolutePath(), quiet ? Project.MSG_VERBOSE : verbosity);
            for (int j = 0; j < files.length; j++) {
                File f = new File(d, files[j]);
                log("Deleting " + f.getAbsolutePath(),
                        quiet ? Project.MSG_VERBOSE : verbosity);
                if (!delete(f)) {
                    handle("Unable to delete file " + f.getAbsolutePath());
                }
            }
        }

        if (dirs.length > 0 && includeEmpty) {
            int dirCount = 0;
            for (int j = dirs.length - 1; j >= 0; j--) {
                File currDir = new File(d, dirs[j]);
                String[] dirFiles = currDir.list();
                if (dirFiles == null || dirFiles.length == 0) {
                    log("Deleting " + currDir.getAbsolutePath(),
                            quiet ? Project.MSG_VERBOSE : verbosity);
                    if (!delete(currDir)) {
                        handle("Unable to delete directory "
                                + currDir.getAbsolutePath());
                    } else {
                        dirCount++;
                    }
                }
            }

            if (dirCount > 0) {
                log("Deleted "
                     + dirCount
                     + " director" + (dirCount == 1 ? "y" : "ies")
                     + " form " + d.getAbsolutePath(),
                     quiet ? Project.MSG_VERBOSE : verbosity);
            }
        }
    }
}
