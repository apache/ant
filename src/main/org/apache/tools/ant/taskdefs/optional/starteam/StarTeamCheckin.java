/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
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
package org.apache.tools.ant.taskdefs.optional.starteam;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;

import com.starbase.starteam.File;
import com.starbase.starteam.Folder;
import com.starbase.starteam.Item;
import com.starbase.starteam.Status;
import com.starbase.starteam.View;
import com.starbase.starteam.ViewConfiguration;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

/**
 * Checks files into a StarTeam project.  
 * Optionally adds files and in the local tree that
 * are not managed by the repository to its control.
 *
 *
 * Created: Sat Dec 15 20:26:07 2001
 *
 * @author <a href="mailto:scohen@localhost.localdomain">Steve Cohen</a>
 * @version 1.0
 *
 * @ant.task name="stcheckin" category="scm" product="Starteam"
 */
public class StarTeamCheckin extends TreeBasedTask {

    public StarTeamCheckin() {
        // we want this to have a false default, unlike for Checkin.
        setRecursive(false);
    }

    private boolean createFolders = true;

    /**
     * The comment which will be stored with the checkin.
     */
    private String comment = null;

    /**
     * holder for the add Uncontrolled attribute.  If true, all
     * local files not in StarTeam will be added to the repository.
     */
    private boolean addUncontrolled = false;

    /**
     * Sets the value of createFolders
     *
     * @param argCreateFolders Value to assign to this.createFolders
     */
    public void setCreateFolders(boolean argCreateFolders) {
        this.createFolders = argCreateFolders;
    }


    /**
     * Get the comment attribute for this operation
     * @return value of comment.
     */
    public String getComment() {
        return this.comment;
    }

    /**
     * Optional checkin comment to be saved with the file.
     * @param comment  Value to assign to comment.
     */
    public void setComment(String comment) {
        this.comment = comment;
    }

    /**
     * Get the value of addUncontrolled.
     * @return value of addUncontrolled.
     */
    public boolean isAddUncontrolled() {
        return this.addUncontrolled;
    }

    /**
     * if true, any files or folders NOT in StarTeam will be 
     * added to the repository.  Defaults to "false".
     * @param addUncontrolled  Value to assign to addUncontrolled.
     */
    public void setAddUncontrolled(boolean addUncontrolled) {
        this.addUncontrolled = addUncontrolled;
    }

    /**
     * This attribute tells whether unlocked files on checkin (so that
     * other users may access them) checkout or to leave the checkout status
     * alone (default).
     * @see #setUnlocked(boolean)
     */
    private int lockStatus = Item.LockType.UNCHANGED;

    /**
     * Set to do an unlocked checkout; optional, default is false;
     * If true, file will be unlocked so that other users may
     * change it.  If false, lock status will not change.     
     * @param v  true means do an unlocked checkout
     *           false means leave status alone.
     */
    public void setUnlocked(boolean v) {
        if (v) {
            this.lockStatus = Item.LockType.UNLOCKED;
        } else {
            this.lockStatus = Item.LockType.UNCHANGED;
        }
    }

    /**
     * Override of base-class abstract function creates an
     * appropriately configured view.  For checkins this is
     * always the current or "tip" view.
     *
     * @param raw the unconfigured <code>View</code>
     * @return the snapshot <code>View</code> appropriately configured.
     */
    protected View createSnapshotView(View raw) {
        return new View(raw, ViewConfiguration.createTip());
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
     * @param starteamFolder the StarTeam folder to which files
     *                       will be checked in
     * @param localFolder local folder from which files will be checked in
     * @exception BuildException if any error occurs
     */
    protected void visit(Folder starteamFolder, java.io.File targetFolder)
            throws BuildException {
        try {
            Hashtable localFiles = listLocalFiles(targetFolder);

            // If we have been told to create the working folders
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
                    if (fileStatus == Status.MERGE
                            || fileStatus == Status.UNKNOWN) {
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

                // Check in anything else.

                log("Checking In: " + (localFile.toString()), Project.MSG_INFO);
                eachFile.checkinFrom(localFile, this.comment,
                        this.lockStatus,
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
            if (this.addUncontrolled) {
                addUncontrolledItems(localFiles, starteamFolder);
            }


        } catch (IOException e) {
            throw new BuildException(e);
        }
    }

    /**
     * Adds to the StarTeam repository everything on the local machine that
     * is not currently in the repository.
     * @param folder - StarTeam folder to which these items are to be added.
     */
    private void addUncontrolledItems(Hashtable localFiles, Folder folder)
            throws IOException {
        try {
            Enumeration e = localFiles.keys();
            while (e.hasMoreElements()) {
                java.io.File file =
                        new java.io.File(e.nextElement().toString());
                add(folder, file);
            }
        } catch (SecurityException e) {
            log("Error adding file: " + e, Project.MSG_ERR);
        }
    }

    /**
     * Deletes the file from the local drive.
     * @param file the file or directory to delete.
     * @return true if the file was successfully deleted otherwise false.
     */
    private void add(Folder parentFolder, java.io.File file)
            throws IOException {
        // If the current file is a Directory, we need to process all
        // of its children as well.
        if (file.isDirectory()) {
            log("Adding new folder to repository: " + file.getAbsolutePath(),
                    Project.MSG_INFO);
            Folder newFolder = new Folder(parentFolder);
            newFolder.setName(file.getName());
            newFolder.update();

            // now visit this new folder to take care of adding any files
            // or subfolders within it.
            if (isRecursive()) {
                visit(newFolder, file);
            }
        } else {
            log("Adding new file to repository: " + file.getAbsolutePath(),
                    Project.MSG_INFO);
            File newFile = new File(parentFolder);
            newFile.addFromStream(new FileInputStream(file),
                    file.getName(),
                    null, this.comment, 3, true);
        }
    }
}
