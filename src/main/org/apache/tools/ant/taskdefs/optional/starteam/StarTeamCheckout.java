/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2002 The Apache Software Foundation.  All rights
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
package org.apache.tools.ant.taskdefs.optional.starteam;

import com.starbase.starteam.File;
import com.starbase.starteam.Folder;
import com.starbase.starteam.Item;
import com.starbase.starteam.Status;
import com.starbase.starteam.View;
import com.starbase.starteam.ViewConfiguration;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

/**
 * Checks out files from a StarTeam project.
 * It also creates all working directories on the
 * local directory if appropriate. Ant Usage:
 * <pre>
 * &lt;taskdef name="starteamcheckout"
 * classname="org.apache.tools.ant.taskdefs.StarTeamCheckout"/&gt;
 * &lt;starteamcheckout username="BuildMaster" password="ant" starteamFolder="Source"
 * starteamurl="servername:portnum/project/view"
 * createworkingdirectories="true"/&gt;
 * </pre>
 *
 * @author Christopher Charlier, ThoughtWorks, Inc. 2001
 * @author <a href="mailto:jcyip@thoughtworks.com">Jason Yip</a>
 * @author Jason Pettiss
 * @author <a href="mailto:stevec@ignitesports.com">Steve Cohen</a>
 * @version 1.1
 * @see <A HREF="http://www.starbase.com/">StarBase Web Site</A>
 *
 * @ant.task name="stcheckout" category="scm"
 */
public class StarTeamCheckout extends TreeBasedTask {

    /**
     * holder for the createDirs attribute
     */
    private boolean createDirs = true;

    /**
     * holder for the deleteUncontrolled attribute.  If true,
     * all local files not in StarTeam will be deleted.
     */
    private boolean deleteUncontrolled = true;

    /**
     * flag (defaults to true) to create all directories
     * that are in the Starteam repository even if they are empty.
     *
     * @param value  the value to set the attribute to.
     */
    public void setCreateWorkingDirs(boolean value) {
        this.createDirs = value;
    }

    /**
     * Whether or not all local files <i>not<i> in StarTeam should be deleted.
     * Optional, defaults to <code>true</code>.
     * @param value  the value to set the attribute to.
     */
    public void setDeleteUncontrolled(boolean value) {
        this.deleteUncontrolled = value;
    }

    /**
     * Sets the label StarTeam is to use for checkout; defaults to the most recent file.
     * The label must exist in starteam or an exception will be thrown. 
     * @param label the label to be used
     */
    public void setLabel(String label) {
        _setLabel(label);
    }

    /**
     * This attribute tells whether to do a locked checkout, an unlocked
     * checkout or to leave the checkout status alone (default).  A locked
     * checkout locks all other users out from making changes.  An unlocked
     * checkout reverts all local files to their previous repository status
     * and removes the lock.
     * @see #setLocked(boolean)
     * @see #setUnlocked(boolean)
     */
    private int lockStatus = Item.LockType.UNCHANGED;

    /**
     * Set to do a locked checkout; optional default is false. 
     * @param v  True to do a locked checkout, false to checkout without
     *           changing status/.
     * @exception BuildException if both locked and unlocked are set true
     */
    public void setLocked(boolean v) throws BuildException {
        setLockStatus(v, Item.LockType.EXCLUSIVE);
    }


    /**
     * Set to do an unlocked checkout. Default is false;
     * @param v  True to do an unlocked checkout, false to checkout without
     *           changing status.
     * @exception BuildException if both locked and unlocked are set true
     */
    public void setUnlocked(boolean v) throws BuildException {
        setLockStatus(v, Item.LockType.UNLOCKED);
    }

    private void setLockStatus(boolean v, int newStatus)
            throws BuildException {
        if (v) {
            if (this.lockStatus == Item.LockType.UNCHANGED) {
                this.lockStatus = newStatus;
            } else if (this.lockStatus != newStatus) {
                throw new BuildException(
                        "Error: cannot set locked and unlocked both true.");
            }
        }
    }

    /**
     * Override of base-class abstract function creates an
     * appropriately configured view for checkouts - either
     * the current view or a view from this.label.
     *
     * @param raw the unconfigured <code>View</code>
     * @return the snapshot <code>View</code> appropriately configured.
     */
    protected View createSnapshotView(View raw) {

        int labelID = getLabelID(raw);

        // if a label has been supplied, use it to configure the view
        // otherwise use current view
        if (labelID >= 0) {
            return new View(raw, ViewConfiguration.createFromLabel(labelID));
        } else {
            return new View(raw, ViewConfiguration.createTip());
        }
    }

    /**
     * Implements base-class abstract function to define tests for
     * any preconditons required by the task
     *
     * @exception BuildException not thrown in this implementation
     */
    protected void testPreconditions() throws BuildException {
        if (null != getRootLocalFolder() && !isForced()) {
            log("Warning: rootLocalFolder specified, but forcing off.",
                    Project.MSG_WARN);
        }
    }

    /**
     * Implements base-class abstract function to perform the checkout
     * operation on the files in each folder of the tree.
     *
     * @param starteamFolder the StarTeam folder from which files to be
     *                       checked out
     * @param targetFolder the local mapping of rootStarteamFolder
     * @exception BuildException if any error occurs
     */
    protected void visit(Folder starteamFolder, java.io.File targetFolder)
            throws BuildException {
        try {
            Hashtable localFiles = listLocalFiles(targetFolder);

            // If we have been told to create the working folders
            if (createDirs) {
                // Create if it doesn't exist
                if (!targetFolder.exists()) {
                    targetFolder.mkdir();
                }
            }
            // For all Files in this folder, we need to check
            // if there have been modifications.

            Item[] files = starteamFolder.getItems("File");
            for (int i = 0; i < files.length; i++) {
                File eachFile = (File) files[i];
                String filename = eachFile.getName();
                java.io.File localFile =
                        new java.io.File(targetFolder, filename);

                delistLocalFile(localFiles, localFile);

                // If the file doesn't pass the include/exclude tests, skip it.
                if (!shouldProcess(filename)) {
                    log("Skipping " + eachFile.toString(), Project.MSG_INFO);
                    continue;
                }


                // If forced is not set then we may save ourselves some work by
                // looking at the status flag.
                // Otherwise, we care nothing about these statuses.

                if (!isForced()) {
                    int fileStatus = (eachFile.getStatus());

                    // We try to update the status once to give StarTeam
                    // another chance.
                    if (fileStatus == Status.MERGE || fileStatus == Status.UNKNOWN) {
                        eachFile.updateStatus(true, true);
                        fileStatus = (eachFile.getStatus());
                    }
                    if (fileStatus == Status.CURRENT) {
                        log("Not processing " + eachFile.toString()
                                + " as it is current.",
                                Project.MSG_INFO);
                        continue;
                    }
                }


                // Check out anything else.
                // Just a note: StarTeam has a status for NEW which implies
                // that there is an item  on your local machine that is not
                // in the repository.  These are the items that show up as
                // NOT IN VIEW in the Starteam GUI.
                // One would think that we would want to perhaps checkin the
                // NEW items (not in all cases! - Steve Cohen 15 Dec 2001)
                // Unfortunately, the sdk doesn't really work, and we can't
                // actually see  anything with a status of NEW. That is why
                // we can just check out  everything here without worrying
                // about losing anything.

                log("Checking Out: " + (localFile.toString()), Project.MSG_INFO);
                eachFile.checkoutTo(localFile, this.lockStatus,
                        true, true, true);
            }

            // Now we recursively call this method on all sub folders in this
            // folder unless recursive attribute is off.
            Folder[] subFolders = starteamFolder.getSubFolders();
            for (int i = 0; i < subFolders.length; i++) {
                java.io.File targetSubfolder =
                        new java.io.File(targetFolder, subFolders[i].getName());
                delistLocalFile(localFiles, targetSubfolder);
                if (isRecursive()) {
                    visit(subFolders[i], targetSubfolder);
                }
            }

            if (this.deleteUncontrolled) {
                deleteUncontrolledItems(localFiles);
            }

        } catch (IOException e) {
            throw new BuildException(e);
        }
    }


    /**
     * Deletes everything on the local machine that is not in the repository.
     *
     * @param localFiles the list of filenames whose elements are to be deleted
     */
    private void deleteUncontrolledItems(Hashtable localFiles) {
        try {
            Enumeration e = localFiles.keys();
            while (e.hasMoreElements()) {
                java.io.File file =
                        new java.io.File(e.nextElement().toString());
                delete(file);
            }
        } catch (SecurityException e) {
            log("Error deleting file: " + e, Project.MSG_ERR);
        }
    }

    /**
     * Deletes the file from the local drive.
     * @param file the file or directory to delete.
     * @return true if the file was successfully deleted otherwise false.
     */
    private boolean delete(java.io.File file) {
        // If the current file is a Directory, we need to delete all
        // of its children as well.
        if (file.isDirectory()) {
            java.io.File[] children = file.listFiles();
            for (int i = 0; i < children.length; i++) {
                delete(children[i]);
            }
        }

        log("Deleting: " + file.getAbsolutePath(), Project.MSG_INFO);
        return file.delete();
    }


}










