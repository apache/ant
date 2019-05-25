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
import java.util.Map;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.FilterSetCollection;

/**
 * Moves a file or directory to a new file or directory.
 * By default, the
 * destination file is overwritten if it already exists.
 * When <i>overwrite</i> is
 * turned off, then files are only moved if the source file is
 * newer than the destination file, or when the destination file does
 * not exist.
 *
 * <p>Source files and directories are only deleted when the file or
 * directory has been copied to the destination successfully.  Filtering
 * also works.</p>
 *
 * <p>This implementation is based on Arnout Kuiper's initial design
 * document, the following mailing list discussions, and the
 * copyfile/copydir tasks.</p>
 *
 * @since Ant 1.2
 *
 * @ant.task category="filesystem"
 */
public class Move extends Copy {

    private boolean performGc = Os.isFamily("windows");

    /**
     * Constructor of object.
     * This sets the forceOverwrite attribute of the Copy parent class
     * to true.
     *
     */
    public Move() {
        super();
        setOverwrite(true);
    }

    /**
     * Whether to perform a garbage collection before retrying a failed delete.
     *
     * <p>This may be required on Windows (where it is set to true by
     * default) but also on other operating systems, for example when
     * deleting directories from an NFS share.</p>
     *
     * @param b boolean
     * @since Ant 1.8.3
     */
    public void setPerformGcOnFailedDelete(boolean b) {
        performGc = b;
    }

    /** {@inheritDoc}. */
    @Override
    protected void validateAttributes() throws BuildException {
        if (file != null && file.isDirectory()) {
            if ((destFile != null && destDir != null)
                || (destFile == null && destDir == null)) {
                throw new BuildException("One and only one of tofile and todir must be set.");
            }
            destFile = destFile == null ? new File(destDir, file.getName()) : destFile;
            destDir = destDir == null ? destFile.getParentFile() : destDir;

            completeDirMap.put(file, destFile);
            file = null;
        } else {
            super.validateAttributes();
        }
    }

//************************************************************************
//  protected and private methods
//************************************************************************

    /**
     * Override copy's doFileOperations to move the files instead of copying them.
     */
    @Override
    protected void doFileOperations() {
        //Attempt complete directory renames, if any, first.
        if (completeDirMap.size() > 0) {
            for (Map.Entry<File, File> entry : completeDirMap.entrySet()) {
                File fromDir = entry.getKey();
                File toDir = entry.getValue();
                boolean renamed = false;
                try {
                    log("Attempting to rename dir: " + fromDir + " to " + toDir, verbosity);
                    renamed = renameFile(fromDir, toDir, filtering, forceOverwrite);
                } catch (IOException ioe) {
                    String msg = "Failed to rename dir " + fromDir
                            + " to " + toDir + " due to " + ioe.getMessage();
                    throw new BuildException(msg, ioe, getLocation());
                }
                if (!renamed) {
                    FileSet fs = new FileSet();
                    fs.setProject(getProject());
                    fs.setDir(fromDir);
                    addFileset(fs);
                    DirectoryScanner ds = fs.getDirectoryScanner(getProject());
                    scan(fromDir, toDir, ds.getIncludedFiles(), ds.getIncludedDirectories());
                }
            }
        }
        int moveCount = fileCopyMap.size();
        if (moveCount > 0) {   // files to move
            log("Moving " + moveCount + " file" + ((moveCount == 1) ? "" : "s")
                    + " to " + destDir.getAbsolutePath());

            for (Map.Entry<String, String[]> entry : fileCopyMap.entrySet()) {
                String fromFile = entry.getKey();
                File f = new File(fromFile);
                boolean selfMove = false;
                if (f.exists()) { //Is this file still available to be moved?
                    String[] toFiles = entry.getValue();
                    for (int i = 0; i < toFiles.length; i++) {
                        String toFile = toFiles[i];

                        if (fromFile.equals(toFile)) {
                            log("Skipping self-move of " + fromFile, verbosity);
                            selfMove = true;

                            // if this is the last time through the loop then
                            // move will not occur, but that's what we want
                            continue;
                        }
                        File d = new File(toFile);
                        if ((i + 1) == toFiles.length && !selfMove) {
                            // Only try to move if this is the last mapped file
                            // and one of the mappings isn't to itself
                            moveFile(f, d, filtering, forceOverwrite);
                        } else {
                            copyFile(f, d, filtering, forceOverwrite);
                        }
                    }
                }
            }
        }

        if (includeEmpty) {
            int createCount = 0;
            for (Map.Entry<String, String[]> entry : dirCopyMap.entrySet()) {
                String fromDirName = entry.getKey();
                boolean selfMove = false;
                for (String toDirName : entry.getValue()) {
                    if (fromDirName.equals(toDirName)) {
                        log("Skipping self-move of " + fromDirName, verbosity);
                        selfMove = true;
                        continue;
                    }
                    File d = new File(toDirName);
                    if (!d.exists()) {
                        if (!d.mkdirs() && !d.exists()) {
                            log("Unable to create directory "
                                    + d.getAbsolutePath(), Project.MSG_ERR);
                        } else {
                            createCount++;
                        }
                    }
                }
                File fromDir = new File(fromDirName);
                if (!selfMove && okToDelete(fromDir)) {
                    deleteDir(fromDir);
                }
            }
            if (createCount > 0) {
                log("Moved " + dirCopyMap.size()
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
     * Try to move the file via a rename, but if this fails or filtering
     * is enabled, copy the file then delete the sourceFile.
     */
    private void moveFile(File fromFile, File toFile, boolean filtering, boolean overwrite) {
        boolean moved = false;
        try {
            log("Attempting to rename: " + fromFile + " to " + toFile, verbosity);
            moved = renameFile(fromFile, toFile, filtering, forceOverwrite);
        } catch (IOException ioe) {
            throw new BuildException("Failed to rename " + fromFile + " to "
                + toFile + " due to " + ioe.getMessage(), ioe, getLocation());
        }

        if (!moved) {
            copyFile(fromFile, toFile, filtering, overwrite);
            if (!getFileUtils().tryHardToDelete(fromFile, performGc)) {
                throw new BuildException("Unable to delete file %s",
                    fromFile.getAbsolutePath());
            }
        }
    }

    /**
     * Copy fromFile to toFile.
     * @param fromFile File
     * @param toFile File
     * @param filtering boolean
     * @param overwrite boolean
     */
    private void copyFile(File fromFile, File toFile, boolean filtering, boolean overwrite) {
        try {
            log("Copying " + fromFile + " to " + toFile, verbosity);

            FilterSetCollection executionFilters = new FilterSetCollection();
            if (filtering) {
                executionFilters.addFilterSet(getProject().getGlobalFilterSet());
            }
            getFilterSets().forEach(executionFilters::addFilterSet);
            getFileUtils().copyFile(fromFile, toFile, executionFilters,
                                    getFilterChains(),
                                    forceOverwrite,
                                    getPreserveLastModified(),
                                    /* append: */ false,
                                    getEncoding(),
                                    getOutputEncoding(),
                                    getProject(), getForce());
        } catch (IOException ioe) {
            throw new BuildException("Failed to copy " + fromFile + " to "
                + toFile + " due to " + ioe.getMessage(), ioe, getLocation());
        }
    }

    /**
     * Its only ok to delete a directory tree if there are no files in it.
     * @param d the directory to check
     * @return true if a deletion can go ahead
     */
    protected boolean okToDelete(File d) {
        String[] list = d.list();
        if (list == null) {
            return false;
        }     // maybe io error?

        for (String s : list) {
            File f = new File(d, s);
            if (f.isDirectory()) {
                if (!okToDelete(f)) {
                    return false;
                }
            } else {
                return false;   // found a file
            }
        }
        return true;
    }

    /**
     * Go and delete the directory tree.
     * @param d the directory to delete
     */
    protected void deleteDir(File d) {
        deleteDir(d, false);
    }

    /**
     * Go and delete the directory tree.
     * @param d the directory to delete
     * @param deleteFiles whether to delete files
     */
    protected void deleteDir(File d, boolean deleteFiles) {
        String[] list = d.list();
        if (list == null) {
            return;
        }      // on an io error list() can return null

        for (String s : list) {
            File f = new File(d, s);
            if (f.isDirectory()) {
                deleteDir(f);
            } else if (deleteFiles
                && !getFileUtils().tryHardToDelete(f, performGc)) {
                throw new BuildException("Unable to delete file %s",
                    f.getAbsolutePath());
            } else {
                throw new BuildException(
                    "UNEXPECTED ERROR - The file %s should not exist!",
                    f.getAbsolutePath());
            }
        }
        log("Deleting directory " + d.getAbsolutePath(), verbosity);
        if (!getFileUtils().tryHardToDelete(d, performGc)) {
            throw new BuildException("Unable to delete directory %s",
                d.getAbsolutePath());
        }
    }

    /**
     * Attempts to rename a file from a source to a destination.
     * If overwrite is set to true, this method overwrites existing file
     * even if the destination file is newer.  Otherwise, the source file is
     * renamed only if the destination file is older than it.
     * Method then checks if token filtering is used.  If it is, this method
     * returns false assuming it is the responsibility to the copyFile method.
     *
     * @param sourceFile the file to rename
     * @param destFile   the destination file
     * @param filtering  if true, filtering is in operation, file will
     *                   be copied/deleted instead of renamed
     * @param overwrite  if true force overwrite even if destination file
     *                   is newer than source file
     * @return true if the file was renamed
     * @exception IOException if an error occurs
     * @exception BuildException if an error occurs
     */
    protected boolean renameFile(File sourceFile, File destFile, boolean filtering,
                                 boolean overwrite) throws IOException, BuildException {
        if (destFile.isDirectory() || filtering || !getFilterSets().isEmpty()
                || !getFilterChains().isEmpty()) {
            return false;
        }

        // identical logic lives in ResourceUtils.copyResource():
        if (destFile.isFile() && !destFile.canWrite()) {
            if (!getForce()) {
                throw new IOException(String.format(
                    "can't replace read-only destination file %s", destFile));
            }
            if (!getFileUtils().tryHardToDelete(destFile)) {
                throw new IOException(String.format(
                    "failed to delete read-only destination file %s",
                    destFile));
            }
        }

        // identical logic lives in FileUtils.rename():
        File parent = destFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        } else if (destFile.isFile()) {
            sourceFile = getFileUtils().normalize(sourceFile.getAbsolutePath()).getCanonicalFile();
            destFile = getFileUtils().normalize(destFile.getAbsolutePath());
            if (destFile.getAbsolutePath().equals(sourceFile.getAbsolutePath())) {
                //no point in renaming a file to its own canonical version...
                log("Rename of " + sourceFile + " to " + destFile
                    + " is a no-op.", Project.MSG_VERBOSE);
                return true;
            }
            if (!getFileUtils().areSame(sourceFile, destFile)
                    && !getFileUtils().tryHardToDelete(destFile, performGc)) {
                throw new BuildException("Unable to remove existing file %s",
                    destFile);
            }
        }
        return sourceFile.renameTo(destFile);
    }
}
