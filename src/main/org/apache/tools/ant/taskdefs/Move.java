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

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
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
 * @author Glenn McAllister
 *         <a href="mailto:glennm@ca.ibm.com">glennm@ca.ibm.com</a>
 * @author Magesh Umasankar
 * @version $Revision$
 *
 * @since Ant 1.2
 *
 * @ant.task category="filesystem"
 */
public class Move extends Copy {

    public Move() {
        super();
        forceOverwrite = true;
    }

//************************************************************************
//  protected and private methods
//************************************************************************

    protected void doFileOperations() {
        //Attempt complete directory renames, if any, first.
        if (completeDirMap.size() > 0) {
            Enumeration e = completeDirMap.keys();
            while (e.hasMoreElements()) {
                File fromDir = (File) e.nextElement();
                File toDir = (File) completeDirMap.get(fromDir);
                try {
                    log("Attempting to rename dir: " + fromDir +
                        " to " + toDir, verbosity);
                    renameFile(fromDir, toDir, filtering, forceOverwrite);
                } catch (IOException ioe) {
                    String msg = "Failed to rename dir " + fromDir
                        + " to " + toDir
                        + " due to " + ioe.getMessage();
                    throw new BuildException(msg, ioe, getLocation());
                }
            }
        }
        if (fileCopyMap.size() > 0) {   // files to move
            log("Moving " + fileCopyMap.size() + " files to " +
                destDir.getAbsolutePath());

            Enumeration e = fileCopyMap.keys();
            while (e.hasMoreElements()) {
                String fromFile = (String) e.nextElement();
                String toFile = (String) fileCopyMap.get(fromFile);

                if (fromFile.equals(toFile)) {
                    log("Skipping self-move of " + fromFile, verbosity);
                    continue;
                }

                boolean moved = false;
                File f = new File(fromFile);

                if (f.exists()) { //Is this file still available to be moved?
                    File d = new File(toFile);

                    try {
                        log("Attempting to rename: " + fromFile +
                            " to " + toFile, verbosity);
                        moved = renameFile(f, d, filtering, forceOverwrite);
                    } catch (IOException ioe) {
                        String msg = "Failed to rename " + fromFile
                            + " to " + toFile
                            + " due to " + ioe.getMessage();
                        throw new BuildException(msg, ioe, getLocation());
                    }

                    if (!moved) {
                        try {
                            log("Moving " + fromFile + " to " + toFile,
                                verbosity);

                            FilterSetCollection executionFilters =
                                new FilterSetCollection();
                            if (filtering) {
                                executionFilters
                                    .addFilterSet(getProject().getGlobalFilterSet());
                            }
                            for (Enumeration filterEnum =
                                     getFilterSets().elements();
                                 filterEnum.hasMoreElements();) {
                                executionFilters
                                    .addFilterSet((FilterSet) filterEnum
                                                  .nextElement());
                            }
                            getFileUtils().copyFile(f, d, executionFilters,
                                                    getFilterChains(),
                                                    forceOverwrite,
                                                    getPreserveLastModified(),
                                                    getEncoding(), 
                                                    getOutputEncoding(),
                                                    getProject());

                            f = new File(fromFile);
                            if (!f.delete()) {
                                throw new BuildException("Unable to delete "
                                                         + "file "
                                                         + f.getAbsolutePath());
                            }
                        } catch (IOException ioe) {
                            String msg = "Failed to copy " + fromFile + " to "
                                + toFile
                                + " due to " + ioe.getMessage();
                            throw new BuildException(msg, ioe, getLocation());
                        }
                    }
                }
            }
        }

        if (includeEmpty) {
            Enumeration e = dirCopyMap.keys();
            int count = 0;
            while (e.hasMoreElements()) {
                String fromDirName = (String) e.nextElement();
                String toDirName = (String) dirCopyMap.get(fromDirName);
                File fromDir = new File(fromDirName);
                File toDir = new File(toDirName);
                if (!toDir.exists()) {
                    if (!toDir.mkdirs()) {
                        log("Unable to create directory "
                            + toDirName, Project.MSG_ERR);
                    } else {
                        count++;
                    }
                }
                if (okToDelete(fromDir)) {
                    deleteDir(fromDir);
                }
            }

            if (count > 0) {
                log("Moved " + count + " empty directories to "
                    + destDir.getAbsolutePath());
            }
        }
    }

    /**
     * Its only ok to delete a directory tree if there are
     * no files in it.
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
     */
    protected void deleteDir(File d) {
        String[] list = d.list();
        if (list == null) {
            return;
        }      // on an io error list() can return null

        for (int i = 0; i < list.length; i++) {
            String s = list[i];
            File f = new File(d, s);
            if (f.isDirectory()) {
                deleteDir(f);
            } else {
                throw new BuildException("UNEXPECTED ERROR - The file "
                                         + f.getAbsolutePath()
                                         + " should not exist!");
            }
        }
        log("Deleting directory " + d.getAbsolutePath(), verbosity);
        if (!d.delete()) {
            throw new BuildException("Unable to delete directory "
                                     + d.getAbsolutePath());
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
     * @throws IOException
     */
    protected boolean renameFile(File sourceFile, File destFile,
                                 boolean filtering, boolean overwrite)
        throws IOException, BuildException {

        boolean renamed = true;
        if ((getFilterSets() != null && getFilterSets().size() > 0) ||
            (getFilterChains() != null && getFilterChains().size() > 0)) {
            renamed = false;
        } else {
            if (!filtering) {
                // ensure that parent dir of dest file exists!
                // not using getParentFile method to stay 1.1 compat
                String parentPath = destFile.getParent();
                if (parentPath != null) {
                    File parent = new File(parentPath);
                    if (!parent.exists()) {
                        parent.mkdirs();
                    }
                }

                if (destFile.exists() && destFile.isFile()) {
                    if (!destFile.delete()) {
                        throw new BuildException("Unable to remove existing "
                                                 + "file " + destFile);
                    }
                }
                renamed = sourceFile.renameTo(destFile);
            } else {
                renamed = false;
            }
        }
        return renamed;
    }
}
