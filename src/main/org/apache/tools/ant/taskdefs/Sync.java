/*
 * Copyright  2003-2004 Apache Software Foundation
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

/*
 * This code is based on code Copyright (c) 2002, Landmark Graphics
 * Corp that has been kindly donated to the Apache Software
 * Foundation.
 */

package org.apache.tools.ant.taskdefs;

import java.io.File;

import java.util.Hashtable;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.util.FileNameMapper;
import org.apache.tools.ant.util.IdentityMapper;

/**
 * Synchronize a local target directory from the files defined
 * in one or more filesets.
 *
 * <p>Uses a &lt;copy&gt; task internally, but forbidding the use of
 * mappers and filter chains. Files of the destination directory not
 * present in any of the source fileset are removed.</p>
 *
 * @author <a href="mailto:ddevienne@lgc.com">Dominique Devienne</a>
 * @version $Revision$
 * @since Ant 1.6
 *
 * revised by <a href="mailto:daniel.armbrust@mayo.edu">Dan Armbrust</a>
 * to remove orphaned directories.
 *
 * @ant.task category="filesystem"
 */
public class Sync extends Task {

    // Same as regular <copy> task... see at end-of-file!
    private MyCopy _copy;

    // Override Task#init
    public void init()
        throws BuildException {
        // Instantiate it
        _copy = new MyCopy();
        configureTask(_copy);

        // Default config of <mycopy> for our purposes.
        _copy.setFiltering(false);
        _copy.setIncludeEmptyDirs(false);
        _copy.setPreserveLastModified(true);
    }

    private void configureTask(Task helper) {
        helper.setProject(getProject());
        helper.setTaskName(getTaskName());
        helper.setOwningTarget(getOwningTarget());
        helper.init();
    }

    // Override Task#execute
    public void execute()
        throws BuildException {
        // The destination of the files to copy
        File toDir = _copy.getToDir();

        // The complete list of files to copy
        Hashtable allFiles = _copy._dest2src;

        // If the destination directory didn't already exist,
        // or was empty, then no previous file removal is necessary!
        boolean noRemovalNecessary = !toDir.exists() || toDir.list().length < 1;

        // Copy all the necessary out-of-date files
        log("PASS#1: Copying files to " + toDir, Project.MSG_DEBUG);
        _copy.execute();

        // Do we need to perform further processing?
        if (noRemovalNecessary) {
            log("NO removing necessary in " + toDir, Project.MSG_DEBUG);
            return; // nope ;-)
        }

        // Get rid of all files not listed in the source filesets.
        log("PASS#2: Removing orphan files from " + toDir, Project.MSG_DEBUG);
        int[] removedFileCount = removeOrphanFiles(allFiles, toDir);
        logRemovedCount(removedFileCount[0], "dangling director", "y", "ies");
        logRemovedCount(removedFileCount[1], "dangling file", "", "s");

        // Get rid of empty directories on the destination side
        if (!_copy.getIncludeEmptyDirs()) {
            log("PASS#3: Removing empty directories from " + toDir,
                Project.MSG_DEBUG);
            int removedDirCount = removeEmptyDirectories(toDir, false);
            logRemovedCount(removedDirCount, "empty director", "y", "ies");
        }
    }

    private void logRemovedCount(int count, String prefix,
                                 String singularSuffix, String pluralSuffix) {
        File toDir = _copy.getToDir();

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
     * @param  file the initial file or directory to scan or test.
     * @return the number of orphaned files and directories actually removed.
     * Position 0 of the array is the number of orphaned directories.
     * Position 1 of the array is the number or orphaned files.
     * Position 2 is meaningless.
     */
    private int[] removeOrphanFiles(Hashtable nonOrphans, File file) {
        int[] removedCount = new int[] {0, 0, 0};
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            for (int i = 0; i < children.length; ++i) {
                int[] temp = removeOrphanFiles(nonOrphans, children[i]);
                removedCount[0] += temp[0];
                removedCount[1] += temp[1];
                removedCount[2] += temp[2];
            }

            if (nonOrphans.get(file) == null && removedCount[2] == 0) {
                log("Removing orphan directory: " + file, Project.MSG_DEBUG);
                file.delete();
                ++removedCount[0];
            } else {
                /*
                  Contrary to what is said above, position 2 is not
                  meaningless inside the recursion.
                  Position 2 is used to carry information back up the
                  recursion about whether or not a directory contains
                  a directory or file at any depth that is not an
                  orphan
                  This has to be done, because if you have the
                  following directory structure: c:\src\a\file and
                  your mapper src files were constructed like so:
                  <include name="**\a\**\*"/>
                  The folder 'a' will not be in the hashtable of
                  nonorphans.  So, before deleting it as an orphan, we
                  have to know whether or not any of its children at
                  any level are orphans.
                  If no, then this folder is also an orphan, and may
                  be deleted.  I do this by changing position 2 to a
                  '1'.
                */
                removedCount[2] = 1;
            }

        } else {
            if (nonOrphans.get(file) == null) {
                log("Removing orphan file: " + file, Project.MSG_DEBUG);
                file.delete();
                ++removedCount[1];
            } else {
                removedCount[2] = 1;
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
     * @return the number of empty directories actually removed.
     */
    private int removeEmptyDirectories(File dir, boolean removeIfEmpty) {
        int removedCount = 0;
        if (dir.isDirectory()) {
            File[] children = dir.listFiles();
            for (int i = 0; i < children.length; ++i) {
                File file = children[i];
                // Test here again to avoid method call for non-directories!
                if (file.isDirectory()) {
                    removedCount += removeEmptyDirectories(file, true);
                }
            }
            if (children.length > 0) {
                // This directory may have become empty...
                // We need to re-query its children list!
                children = dir.listFiles();
            }
            if (children.length < 1 && removeIfEmpty) {
                log("Removing empty directory: " + dir, Project.MSG_DEBUG);
                dir.delete();
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
     */
    public void setTodir(File destDir) {
        _copy.setTodir(destDir);
    }

    /**
     * Used to force listing of all names of copied files.
     */
    public void setVerbose(boolean verbose) {
        _copy.setVerbose(verbose);
    }

    /**
     * Overwrite any existing destination file(s).
     */
    public void setOverwrite(boolean overwrite) {
        _copy.setOverwrite(overwrite);
    }

    /**
     * Used to copy empty directories.
     */
    public void setIncludeEmptyDirs(boolean includeEmpty) {
        _copy.setIncludeEmptyDirs(includeEmpty);
    }

    /**
     * If false, note errors to the output but keep going.
     * @param failonerror true or false
     */
    public void setFailOnError(boolean failonerror) {
        _copy.setFailOnError(failonerror);
    }

    /**
     * Adds a set of files to copy.
     */
    public void addFileset(FileSet set) {
        _copy.addFileset(set);
    }

    /**
     * The number of milliseconds leeway to give before deciding a
     * target is out of date.
     *
     * <p>Default is 0 milliseconds, or 2 seconds on DOS systems.</p>
     *
     * @since Ant 1.6
     */
    public void setGranularity(long granularity) {
        _copy.setGranularity(granularity);
    }

    /**
     * Subclass Copy in order to access it's file/dir maps.
     */
    public static class MyCopy extends Copy {

        // List of files that must be copied, irrelevant from the
        // fact that they are newer or not than the destination.
        private Hashtable _dest2src = new Hashtable();

        public MyCopy() {
        }

        protected void buildMap(File fromDir, File toDir, String[] names,
                                FileNameMapper mapper, Hashtable map) {
            assertTrue("No mapper", mapper instanceof IdentityMapper);

            super.buildMap(fromDir, toDir, names, mapper, map);

            for (int i = 0; i < names.length; ++i) {
                String name = names[i];
                File dest = new File(toDir, name);
                // No need to instantiate the src file, as we use the
                // table as a set (to remain Java 1.1 compatible!!!).
                //File src = new File(fromDir, name);
                //_dest2src.put(dest, src);
                _dest2src.put(dest, fromDir);
            }
        }

        public File getToDir() {
            return destDir;
        }

        public boolean getIncludeEmptyDirs() {
            return includeEmpty;
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
