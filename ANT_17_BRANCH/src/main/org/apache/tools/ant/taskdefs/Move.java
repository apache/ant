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
import java.io.IOException;
import java.util.Iterator;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.FilterSet;
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

    /** {@inheritDoc}. */
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
    protected void doFileOperations() {
        //Attempt complete directory renames, if any, first.
        if (completeDirMap.size() > 0) {
            for (Iterator fromDirs = completeDirMap.keySet().iterator(); fromDirs.hasNext();) {
                File fromDir = (File) fromDirs.next();
                File toDir = (File) completeDirMap.get(fromDir);
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
                    String[] files = ds.getIncludedFiles();
                    String[] dirs = ds.getIncludedDirectories();
                    scan(fromDir, toDir, files, dirs);
                }
            }
        }
        int moveCount = fileCopyMap.size();
        if (moveCount > 0) {   // files to move
            log("Moving " + moveCount + " file" + ((moveCount == 1) ? "" : "s")
                    + " to " + destDir.getAbsolutePath());

            for (Iterator fromFiles = fileCopyMap.keySet().iterator(); fromFiles.hasNext();) {
                String fromFile = (String) fromFiles.next();
                File f = new File(fromFile);
                boolean selfMove = false;
                if (f.exists()) { //Is this file still available to be moved?
                    String[] toFiles = (String[]) fileCopyMap.get(fromFile);
                    for (int i = 0; i < toFiles.length; i++) {
                        String toFile = (String) toFiles[i];

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
            for (Iterator fromDirNames = dirCopyMap.keySet().iterator(); fromDirNames.hasNext();) {
                String fromDirName = (String) fromDirNames.next();
                String[] toDirNames = (String[]) dirCopyMap.get(fromDirName);
                boolean selfMove = false;
                for (int i = 0; i < toDirNames.length; i++) {
                    if (fromDirName.equals(toDirNames[i])) {
                        log("Skipping self-move of " + fromDirName, verbosity);
                        selfMove = true;
                        continue;
                    }
                    File d = new File(toDirNames[i]);
                    if (!d.exists()) {
                        if (!d.mkdirs()) {
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
            String msg = "Failed to rename " + fromFile
                + " to " + toFile + " due to " + ioe.getMessage();
            throw new BuildException(msg, ioe, getLocation());
        }

        if (!moved) {
            copyFile(fromFile, toFile, filtering, overwrite);
            if (!fromFile.delete()) {
                throw new BuildException("Unable to delete " + "file "
                        + fromFile.getAbsolutePath());
            }
        }
    }

    /**
     * Copy fromFile to toFile.
     * @param fromFile
     * @param toFile
     * @param filtering
     * @param overwrite
     */
    private void copyFile(File fromFile, File toFile, boolean filtering, boolean overwrite) {
        try {
            log("Copying " + fromFile + " to " + toFile, verbosity);

            FilterSetCollection executionFilters = new FilterSetCollection();
            if (filtering) {
                executionFilters.addFilterSet(getProject().getGlobalFilterSet());
            }
            for (Iterator filterIter = getFilterSets().iterator(); filterIter.hasNext();) {
                executionFilters.addFilterSet((FilterSet) filterIter.next());
            }
            getFileUtils().copyFile(fromFile, toFile, executionFilters,
                                    getFilterChains(),
                                    forceOverwrite,
                                    getPreserveLastModified(),
                                    getEncoding(),
                                    getOutputEncoding(),
                                    getProject());
        } catch (IOException ioe) {
            String msg = "Failed to copy " + fromFile
                    + " to " + toFile + " due to " + ioe.getMessage();
            throw new BuildException(msg, ioe, getLocation());
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

        for (int i = 0; i < list.length; i++) {
            String s = list[i];
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

        for (int i = 0; i < list.length; i++) {
            String s = list[i];
            File f = new File(d, s);
            if (f.isDirectory()) {
                deleteDir(f);
            } else if (deleteFiles && !(f.delete())) {
                throw new BuildException("Unable to delete file " + f.getAbsolutePath());
            } else {
                throw new BuildException("UNEXPECTED ERROR - The file "
                        + f.getAbsolutePath() + " should not exist!");
            }
        }
        log("Deleting directory " + d.getAbsolutePath(), verbosity);
        if (!d.delete()) {
            throw new BuildException("Unable to delete directory " + d.getAbsolutePath());
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
        if (destFile.isDirectory() || filtering || getFilterSets().size() > 0
                || getFilterChains().size() > 0) {
            return false;
        }
        // identical logic lives in FileUtils.rename():
        File parent = destFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        } else if (destFile.isFile()) {
            sourceFile = getFileUtils().normalize(sourceFile.getAbsolutePath()).getCanonicalFile();
            destFile = getFileUtils().normalize(destFile.getAbsolutePath());
            if (destFile.equals(sourceFile)) {
                //no point in renaming a file to its own canonical version...
                return true;
            }
            if (!(sourceFile.equals(destFile.getCanonicalFile()) || destFile.delete())) {
                throw new BuildException("Unable to remove existing file " + destFile);
            }
        }
        return sourceFile.renameTo(destFile);
    }
}
