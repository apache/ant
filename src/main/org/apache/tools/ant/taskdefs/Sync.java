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
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.AbstractFileSet;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.PatternSet;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.Resources;
import org.apache.tools.ant.types.resources.Restrict;
import org.apache.tools.ant.types.resources.selectors.Exists;
import org.apache.tools.ant.types.selectors.FileSelector;
import org.apache.tools.ant.types.selectors.NoneSelector;
import org.apache.tools.ant.util.FileUtils;

/**
 * Synchronize a local target directory from the files defined
 * in one or more filesets.
 *
 * <p>Uses a &lt;copy&gt; task internally, but forbidding the use of
 * mappers and filter chains. Files of the destination directory not
 * present in any of the source fileset are removed.</p>
 *
 * @since Ant 1.6
 *
 * revised by <a href="mailto:daniel.armbrust@mayo.edu">Dan Armbrust</a>
 * to remove orphaned directories.
 *
 * @ant.task category="filesystem"
 */
public class Sync extends Task {

    // Same as regular <copy> task... see at end-of-file!
    private MyCopy myCopy;

    // Similar to a fileset, but doesn't allow dir attribute to be set
    private SyncTarget syncTarget;

    private Resources resources = null;

    // Override Task#init
    /**
     * Initialize the sync task.
     * @throws BuildException if there is a problem.
     * @see Task#init()
     */
    @Override
    public void init()
        throws BuildException {
        // Instantiate it
        myCopy = new MyCopy();
        configureTask(myCopy);

        // Default config of <mycopy> for our purposes.
        myCopy.setFiltering(false);
        myCopy.setIncludeEmptyDirs(false);
        myCopy.setPreserveLastModified(true);
    }

    private void configureTask(Task helper) {
        helper.setProject(getProject());
        helper.setTaskName(getTaskName());
        helper.setOwningTarget(getOwningTarget());
        helper.init();
    }

    // Override Task#execute
    /**
     * Execute the sync task.
     * @throws BuildException if there is an error.
     * @see Task#execute()
     */
    @Override
    public void execute()
        throws BuildException {
        // The destination of the files to copy
        File toDir = myCopy.getToDir();

        // The complete list of files to copy
        Set<String> allFiles = myCopy.nonOrphans;

        // If the destination directory didn't already exist,
        // or was empty, then no previous file removal is necessary!
        boolean noRemovalNecessary = !toDir.exists() || toDir.list().length < 1;

        // Copy all the necessary out-of-date files
        log("PASS#1: Copying files to " + toDir, Project.MSG_DEBUG);
        myCopy.execute();

        // Do we need to perform further processing?
        if (noRemovalNecessary) {
            log("NO removing necessary in " + toDir, Project.MSG_DEBUG);
            return; // nope ;-)
        }

        // will hold the directories matched by SyncTarget in reversed
        // lexicographic order (order is important, that's why we use
        // a LinkedHashSet
        Set<File> preservedDirectories = new LinkedHashSet<>();

        // Get rid of all files not listed in the source filesets.
        log("PASS#2: Removing orphan files from " + toDir, Project.MSG_DEBUG);
        int[] removedFileCount = removeOrphanFiles(allFiles, toDir,
                                                   preservedDirectories);
        logRemovedCount(removedFileCount[0], "dangling director", "y", "ies");
        logRemovedCount(removedFileCount[1], "dangling file", "", "s");

        // Get rid of empty directories on the destination side
        if (!myCopy.getIncludeEmptyDirs()
            || getExplicitPreserveEmptyDirs() == Boolean.FALSE) {
            log("PASS#3: Removing empty directories from " + toDir,
                Project.MSG_DEBUG);

            int removedDirCount = 0;
            if (!myCopy.getIncludeEmptyDirs()) {
                removedDirCount =
                    removeEmptyDirectories(toDir, false, preservedDirectories);
            } else { // must be syncTarget.preserveEmptydirs == FALSE
                removedDirCount =
                    removeEmptyDirectories(preservedDirectories);
            }
            logRemovedCount(removedDirCount, "empty director", "y", "ies");
        }
    }

    private void logRemovedCount(int count, String prefix,
                                 String singularSuffix, String pluralSuffix) {
        File toDir = myCopy.getToDir();

        String what = (prefix == null) ? "" : prefix;
        what += (count < 2) ? singularSuffix : pluralSuffix;

        if (count > 0) {
            log("Removed " + count + " " + what + " from " + toDir,
                Project.MSG_INFO);
        } else {
            log("NO " + what + " to remove from " + toDir,
                Project.MSG_VERBOSE);
        }
    }

    /**
     * Removes all files and folders not found as keys of a table
     * (used as a set!).
     *
     * <p>If the provided file is a directory, it is recursively
     * scanned for orphaned files which will be removed as well.</p>
     *
     * <p>If the directory is an orphan, it will also be removed.</p>
     *
     * @param  nonOrphans the table of all non-orphan <code>File</code>s.
     * @param  toDir the initial file or directory to scan or test.
     * @param  preservedDirectories will be filled with the directories
     *         matched by preserveInTarget - if any.  Will not be
     *         filled unless preserveEmptyDirs and includeEmptyDirs
     *         conflict.
     * @return the number of orphaned files and directories actually removed.
     * Position 0 of the array is the number of orphaned directories.
     * Position 1 of the array is the number or orphaned files.
     */
    private int[] removeOrphanFiles(Set<String> nonOrphans, File toDir,
                                    Set<File> preservedDirectories) {
        int[] removedCount = new int[] {0, 0};
        String[] excls =
            nonOrphans.toArray(new String[nonOrphans.size() + 1]);
        // want to keep toDir itself
        excls[nonOrphans.size()] = "";

        DirectoryScanner ds;
        if (syncTarget != null) {
            FileSet fs = syncTarget.toFileSet(false);
            fs.setDir(toDir);

            // preserveInTarget would find all files we want to keep,
            // but we need to find all that we want to delete - so the
            // meaning of all patterns and selectors must be inverted
            PatternSet ps = syncTarget.mergePatterns(getProject());
            fs.appendExcludes(ps.getIncludePatterns(getProject()));
            fs.appendIncludes(ps.getExcludePatterns(getProject()));
            fs.setDefaultexcludes(!syncTarget.getDefaultexcludes());

            // selectors are implicitly ANDed in DirectoryScanner.  To
            // revert their logic we wrap them into a <none> selector
            // instead.
            FileSelector[] s = syncTarget.getSelectors(getProject());
            if (s.length > 0) {
                NoneSelector ns = new NoneSelector();
                for (FileSelector element : s) {
                    ns.appendSelector(element);
                }
                fs.appendSelector(ns);
            }
            ds = fs.getDirectoryScanner(getProject());
        } else {
            ds = new DirectoryScanner();
            ds.setBasedir(toDir);
            // set the case sensitivity of the directory scanner based on the
            // directory we are scanning, if we are able to determine that detail.
            // Else let the directory scanner default it to whatever it does internally
            FileUtils.isCaseSensitiveFileSystem(toDir.toPath()).ifPresent(ds::setCaseSensitive);
        }
        ds.addExcludes(excls);

        ds.scan();
        for (String file : ds.getIncludedFiles()) {
            File f = new File(toDir, file);
            log("Removing orphan file: " + f, Project.MSG_DEBUG);
            f.delete();
            ++removedCount[1];
        }
        String[] dirs = ds.getIncludedDirectories();
        // ds returns the directories in lexicographic order.
        // iterating through the array backwards means we are deleting
        // leaves before their parent nodes - thus making sure (well,
        // more likely) that the directories are empty when we try to
        // delete them.
        for (int i = dirs.length - 1; i >= 0; --i) {
            File f = new File(toDir, dirs[i]);
            String[] children = f.list();
            if (children == null || children.length < 1) {
                log("Removing orphan directory: " + f, Project.MSG_DEBUG);
                f.delete();
                ++removedCount[0];
            }
        }

        Boolean ped = getExplicitPreserveEmptyDirs();
        if (ped != null && ped != myCopy.getIncludeEmptyDirs()) {
            FileSet fs = syncTarget.toFileSet(true);
            fs.setDir(toDir);
            String[] preservedDirs =
                fs.getDirectoryScanner(getProject()).getIncludedDirectories();
            for (int i = preservedDirs.length - 1; i >= 0; --i) {
                preservedDirectories.add(new File(toDir, preservedDirs[i]));
            }
        }

        return removedCount;
    }

    /**
     * Removes all empty directories from a directory.
     *
     * <p><em>Note that a directory that contains only empty
     * directories, directly or not, will be removed!</em></p>
     *
     * <p>Recurses depth-first to find the leaf directories
     * which are empty and removes them, then unwinds the
     * recursion stack, removing directories which have
     * become empty themselves, etc...</p>
     *
     * @param  dir the root directory to scan for empty directories.
     * @param  removeIfEmpty whether to remove the root directory
     *         itself if it becomes empty.
     * @param  preservedEmptyDirectories directories matched by
     *         syncTarget
     * @return the number of empty directories actually removed.
     */
    private int removeEmptyDirectories(File dir, boolean removeIfEmpty,
                                       Set<File> preservedEmptyDirectories) {
        int removedCount = 0;
        if (dir.isDirectory()) {
            File[] children = dir.listFiles();
            for (File file : children) {
                // Test here again to avoid method call for non-directories!
                if (file.isDirectory()) {
                    removedCount += removeEmptyDirectories(file, true,
                            preservedEmptyDirectories);
                }
            }
            if (children.length > 0) {
                // This directory may have become empty...
                // We need to re-query its children list!
                children = dir.listFiles();
            }
            if (children.length < 1 && removeIfEmpty
                && !preservedEmptyDirectories.contains(dir)) {
                log("Removing empty directory: " + dir, Project.MSG_DEBUG);
                dir.delete();
                ++removedCount;
            }
        }
        return removedCount;
    }

    /**
     * Removes all empty directories preserved by preserveInTarget in
     * the preserveEmptyDirs == FALSE case.
     *
     * <p>Relies on the set to be ordered in reversed lexicographic
     * order so that directories will be removed depth-first.</p>
     *
     * @param  preservedEmptyDirectories directories matched by
     *         syncTarget
     * @return the number of empty directories actually removed.
     *
     * @since Ant 1.8.0
     */
    private int removeEmptyDirectories(Set<File> preservedEmptyDirectories) {
        int removedCount = 0;
        for (File f : preservedEmptyDirectories) {
            String[] s = f.list();
            if (s == null || s.length == 0) {
                log("Removing empty directory: " + f, Project.MSG_DEBUG);
                f.delete();
                ++removedCount;
            }
        }
        return removedCount;
    }

    //
    // Various copy attributes/subelements of <copy> passed thru to <mycopy>
    //

    /**
     * Sets the destination directory.
     * @param destDir the destination directory
     */
    public void setTodir(File destDir) {
        myCopy.setTodir(destDir);
    }

    /**
     * Used to force listing of all names of copied files.
     * @param verbose if true force listing of all names of copied files.
     */
    public void setVerbose(boolean verbose) {
        myCopy.setVerbose(verbose);
    }

    /**
     * Overwrite any existing destination file(s).
     * @param overwrite if true overwrite any existing destination file(s).
     */
    public void setOverwrite(boolean overwrite) {
        myCopy.setOverwrite(overwrite);
    }

    /**
     * Used to copy empty directories.
     * @param includeEmpty If true copy empty directories.
     */
    public void setIncludeEmptyDirs(boolean includeEmpty) {
        myCopy.setIncludeEmptyDirs(includeEmpty);
    }

    /**
     * If false, note errors to the output but keep going.
     * @param failonerror true or false
     */
    public void setFailOnError(boolean failonerror) {
        myCopy.setFailOnError(failonerror);
    }

    /**
     * Adds a set of files to copy.
     * @param set a fileset
     */
    public void addFileset(FileSet set) {
        add(set);
    }

    /**
     * Adds a collection of filesystem resources to copy.
     * @param rc a resource collection
     * @since Ant 1.7
     */
    public void add(ResourceCollection rc) {
        if (rc instanceof FileSet && rc.isFilesystemOnly()) {
            // receives special treatment in copy that this task relies on
            myCopy.add(rc);
        } else {
            if (resources == null) {
                Restrict r = new Restrict();
                r.add(new Exists());
                resources = new Resources();
                r.add(resources);
                myCopy.add(r);
            }
            resources.add(rc);
        }
    }

    /**
     * The number of milliseconds leeway to give before deciding a
     * target is out of date.
     *
     * <p>Default is 0 milliseconds, or 2 seconds on DOS systems.</p>
     * @param granularity a <code>long</code> value
     * @since Ant 1.6.2
     */
    public void setGranularity(long granularity) {
        myCopy.setGranularity(granularity);
    }

    /**
     * A container for patterns and selectors that can be used to
     * specify files that should be kept in the target even if they
     * are not present in any source directory.
     *
     * <p>You must not invoke this method more than once.</p>
     * @param s a preserveintarget nested element
     * @since Ant 1.7
     */
    public void addPreserveInTarget(SyncTarget s) {
        if (syncTarget != null) {
            throw new BuildException(
                "you must not specify multiple preserveintarget elements.");
        }
        syncTarget = s;
    }

    /**
     * The value of preserveTarget's preserveEmptyDirs attribute if
     * specified and preserveTarget has been used in the first place.
     *
     * @since Ant 1.8.0.
     */
    private Boolean getExplicitPreserveEmptyDirs() {
        return syncTarget == null ? null : syncTarget.getPreserveEmptyDirs();
    }

    /**
     * Subclass Copy in order to access it's file/dir maps.
     */
    public static class MyCopy extends Copy {

        // List of files that must be copied, irrelevant from the
        // fact that they are newer or not than the destination.
        private Set<String> nonOrphans = new HashSet<>();

        /**
         * @see Copy#scan(File, File, String[], String[])
         * {@inheritDoc}
         */
        @Override
        protected void scan(File fromDir, File toDir, String[] files,
                            String[] dirs) {
            assertTrue("No mapper", mapperElement == null);

            super.scan(fromDir, toDir, files, dirs);

            Collections.addAll(nonOrphans, files);
            Collections.addAll(nonOrphans, dirs);
        }

        /**
         * @see Copy#scan(Resource[], File)
         * {@inheritDoc}
         */
        @Override
        protected Map<Resource, String[]> scan(Resource[] resources, File toDir) {
            assertTrue("No mapper", mapperElement == null);

            Stream.of(resources).map(Resource::getName).forEach(nonOrphans::add);

            return super.scan(resources, toDir);
        }

        /**
         * Get the destination directory.
         * @return the destination directory
         */
        public File getToDir() {
            return destDir;
        }

        /**
         * Get the includeEmptyDirs attribute.
         * @return true if emptyDirs are to be included
         */
        public boolean getIncludeEmptyDirs() {
            return includeEmpty;
        }

        /**
         * Yes, we can.
         * @return true always.
         * @since Ant 1.7
         */
        @Override
        protected boolean supportsNonFileResources() {
            return true;
        }
    }

    /**
     * Inner class used to hold exclude patterns and selectors to save
     * stuff that happens to live in the target directory but should
     * not get removed.
     *
     * @since Ant 1.7
     */
    public static class SyncTarget extends AbstractFileSet {

        private Boolean preserveEmptyDirs;

        /**
         * Constructor for SyncTarget.
         * This just changes the default value of "defaultexcludes" from
         * true to false.
         */
        // TODO does it? ^
        public SyncTarget() {
            super();
        }

        /**
         * Override AbstractFileSet#setDir(File) to disallow
         * setting the directory.
         * @param dir ignored
         * @throws BuildException always
         */
        @Override
        public void setDir(File dir) throws BuildException {
            throw new BuildException(
                "preserveintarget doesn't support the dir attribute");
        }

        /**
         * Whether empty directories matched by this fileset should be
         * preserved.
         *
         * @param b boolean
         * @since Ant 1.8.0
         */
        public void setPreserveEmptyDirs(boolean b) {
            preserveEmptyDirs = b;
        }

        /**
         * Whether empty directories matched by this fileset should be
         * preserved.
         *
         * @return Boolean
         * @since Ant 1.8.0
         */
        public Boolean getPreserveEmptyDirs() {
            return preserveEmptyDirs;
        }

        private FileSet toFileSet(boolean withPatterns) {
            FileSet fs = new FileSet();
            fs.setCaseSensitive(isCaseSensitive());
            fs.setFollowSymlinks(isFollowSymlinks());
            fs.setMaxLevelsOfSymlinks(getMaxLevelsOfSymlinks());
            fs.setProject(getProject());

            if (withPatterns) {
                PatternSet ps = mergePatterns(getProject());
                fs.appendIncludes(ps.getIncludePatterns(getProject()));
                fs.appendExcludes(ps.getExcludePatterns(getProject()));
                for (FileSelector sel : getSelectors(getProject())) {
                    fs.appendSelector(sel);
                }
                fs.setDefaultexcludes(getDefaultexcludes());
            }
            return fs;
        }
    }

    /**
     * Pseudo-assert method.
     */
    private static void assertTrue(String message, boolean condition) {
        if (!condition) {
            throw new BuildException("Assertion Error: " + message);
        }
    }

}
