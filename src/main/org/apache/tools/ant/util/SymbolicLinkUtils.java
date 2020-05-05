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
package org.apache.tools.ant.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Execute;

/**
 * Contains methods related to symbolic links - or what Ant thinks is
 * a symbolic link based on the absent support for them in Java.
 *
 * @since Ant 1.8.0
 * @deprecated Starting Ant 1.10.2, this class is now deprecated in favour
 *              of the Java {@link java.nio.file.Files} APIs introduced in
 *              Java 7, for dealing with symbolic links
 */
@Deprecated
public class SymbolicLinkUtils {
    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    /**
     * Shared instance.
     */
    private static final SymbolicLinkUtils PRIMARY_INSTANCE = new SymbolicLinkUtils();

    /**
     * Method to retrieve The SymbolicLinkUtils, which is shared by
     * all users of this method.
     * @return an instance of SymbolicLinkUtils.
     */
    public static SymbolicLinkUtils getSymbolicLinkUtils() {
        // keep the door open for Java X.Y specific subclass if symbolic
        // links ever become supported in the classlib
        return PRIMARY_INSTANCE;
    }

    /**
     * Empty constructor.
     */
    protected SymbolicLinkUtils() {
    }

    /**
     * Checks whether a given file is a symbolic link.
     *
     * <p>It doesn't really test for symbolic links but whether the
     * canonical and absolute paths of the file are identical--this
     * may lead to false positives on some platforms.</p>
     *
     * @param file the file to test.  Must not be null.
     *
     * @return true if the file is a symbolic link.
     * @throws IOException on error.
     */
    public boolean isSymbolicLink(final File file) throws IOException {
        return isSymbolicLink(file.getParentFile(), file.getName());
    }

    /**
     * Checks whether a given file is a symbolic link.
     *
     * <p>It doesn't really test for symbolic links but whether the
     * canonical and absolute paths of the file are identical--this
     * may lead to false positives on some platforms.</p>
     *
     * @param name the name of the file to test.
     *
     * @return true if the file is a symbolic link.
     * @throws IOException on error.
     */
    public boolean isSymbolicLink(final String name) throws IOException {
        return isSymbolicLink(new File(name));
    }

    /**
     * Checks whether a given file is a symbolic link.
     *
     * <p>It doesn't really test for symbolic links but whether the
     * canonical and absolute paths of the file are identical--this
     * may lead to false positives on some platforms.</p>
     *
     * @param parent the parent directory of the file to test
     * @param name the name of the file to test.
     *
     * @return true if the file is a symbolic link.
     * @throws IOException on error.
     */
    public boolean isSymbolicLink(final File parent, final String name)
        throws IOException {
        final File toTest = parent != null
            ? new File(parent.getCanonicalPath(), name)
            : new File(name);
        return !toTest.getAbsolutePath().equals(toTest.getCanonicalPath());
    }

    /**
     * Checks whether a given file is a broken symbolic link.
     *
     * <p>It doesn't really test for symbolic links but whether Java
     * reports that the File doesn't exist but its parent's child list
     * contains it--this may lead to false positives on some
     * platforms.</p>
     *
     * <p>Note that #isSymbolicLink returns false if this method
     * returns true since Java won't produce a canonical name
     * different from the absolute one if the link is broken.</p>
     *
     * @param name the name of the file to test.
     *
     * @return true if the file is a broken symbolic link.
     * @throws IOException on error.
     */
    public boolean isDanglingSymbolicLink(final String name) throws IOException {
        return isDanglingSymbolicLink(new File(name));
    }

    /**
     * Checks whether a given file is a broken symbolic link.
     *
     * <p>It doesn't really test for symbolic links but whether Java
     * reports that the File doesn't exist but its parent's child list
     * contains it--this may lead to false positives on some
     * platforms.</p>
     *
     * <p>Note that #isSymbolicLink returns false if this method
     * returns true since Java won't produce a canonical name
     * different from the absolute one if the link is broken.</p>
     *
     * @param file the file to test.
     *
     * @return true if the file is a broken symbolic link.
     * @throws IOException on error.
     */
    public boolean isDanglingSymbolicLink(final File file) throws IOException {
        return isDanglingSymbolicLink(file.getParentFile(), file.getName());
    }

    /**
     * Checks whether a given file is a broken symbolic link.
     *
     * <p>It doesn't really test for symbolic links but whether Java
     * reports that the File doesn't exist but its parent's child list
     * contains it--this may lead to false positives on some
     * platforms.</p>
     *
     * <p>Note that #isSymbolicLink returns false if this method
     * returns true since Java won't produce a canonical name
     * different from the absolute one if the link is broken.</p>
     *
     * @param parent the parent directory of the file to test
     * @param name the name of the file to test.
     *
     * @return true if the file is a broken symbolic link.
     * @throws IOException on error.
     */
    public boolean isDanglingSymbolicLink(final File parent, final String name)
        throws IOException {
        final File f = new File(parent, name);
        if (!f.exists()) {
            final String localName = f.getName();
            final String[] c = parent.list((d, n) -> localName.equals(n));
            return c != null && c.length > 0;
        }
        return false;
    }

    /**
     * Delete a symlink (without deleting the associated resource).
     *
     * <p>This is a utility method that removes a unix symlink without
     * removing the resource that the symlink points to. If it is
     * accidentally invoked on a real file, the real file will not be
     * harmed, but silently ignored.</p>
     *
     * <p>Normally this method works by
     * getting the canonical path of the link, using the canonical path to
     * rename the resource (breaking the link) and then deleting the link.
     * The resource is then returned to its original name inside a finally
     * block to ensure that the resource is unharmed even in the event of
     * an exception.</p>
     *
     * <p>There may be cases where the algorithm described above doesn't work,
     * in that case the method tries to use the native "rm" command on
     * the symlink instead.</p>
     *
     * @param link A <code>File</code> object of the symlink to delete.
     * @param task An Ant Task required if "rm" needs to be invoked.
     *
     * @throws IOException If calls to <code>File.rename</code>,
     * <code>File.delete</code> or <code>File.getCanonicalPath</code>
     * fail.
     * @throws BuildException if the execution of "rm" failed.
     */
    public void deleteSymbolicLink(File link, final Task task)
        throws IOException {
        if (isDanglingSymbolicLink(link)) {
            if (!link.delete()) {
                throw new IOException("failed to remove dangling symbolic link "
                                      + link);
            }
            return;
        }

        if (!isSymbolicLink(link)) {
            // plain file, not a link
            return;
        }

        if (!link.exists()) {
            throw new FileNotFoundException("No such symbolic link: " + link);
        }

        // find the resource of the existing link:
        final File target = link.getCanonicalFile();

        // no reason to try the renaming algorithm if we aren't allowed to
        // write to the target's parent directory.  Let's hope that
        // File.canWrite works on all platforms.

        if (task == null || target.getParentFile().canWrite()) {

            // rename the resource, thus breaking the link:
            final Project project = task == null ? null : task.getProject();
            final File temp = FILE_UTILS.createTempFile(project, "symlink", ".tmp",
                target.getParentFile(), false, false);

            if (FILE_UTILS.isLeadingPath(target, link)) {
                // link points to a parent directory, renaming the parent
                // will rename the file
                link = new File(temp,
                                FILE_UTILS.removeLeadingPath(target, link));
            }

            boolean renamedTarget = false;
            boolean success = false;
            try {
                try {
                    FILE_UTILS.rename(target, temp);
                    renamedTarget = true;
                } catch (final IOException e) {
                    throw new IOException("Couldn't rename resource when "
                                          + "attempting to delete '" + link
                                          + "'.  Reason: " + e.getMessage());
                }
                // delete the (now) broken link:
                if (!link.delete()) {
                    throw new IOException("Couldn't delete symlink: "
                                          + link
                                          + " (was it a real file? is this "
                                          + "not a UNIX system?)");
                }
                success = true;
            } finally {
                if (renamedTarget) {
                    // return the resource to its original name:
                    try {
                        FILE_UTILS.rename(temp, target);
                    } catch (final IOException e) {
                        String msg = "Couldn't return resource "
                            + temp
                            + " to its original name: "
                            + target.getAbsolutePath()
                            + ". Reason: " + e.getMessage()
                            + "\n THE RESOURCE'S NAME ON DISK"
                            + " HAS BEEN CHANGED BY THIS"
                            + " ERROR!\n";
                        if (success) {
                            throw new IOException(msg); //NOSONAR
                        } else {
                            System.err.println(msg);
                        }
                    }
                }
            }
        } else {
            Execute.runCommand(task, "rm", link.getAbsolutePath());
        }
    }

}
