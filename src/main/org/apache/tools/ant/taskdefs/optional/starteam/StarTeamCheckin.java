/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002-2003 The Apache Software Foundation.  All rights
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

import com.starbase.starteam.File;
import com.starbase.starteam.Folder;
import com.starbase.starteam.Item;
import com.starbase.starteam.Status;
import com.starbase.starteam.TypeNames;
import com.starbase.starteam.View;
import com.starbase.starteam.ViewConfiguration;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

/**
 * Checks files into a StarTeam project.  
 * Optionally adds files and in the local tree that
 * are not managed by the repository to its control.
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
     * any preconditons required by the task.
     *
     * @exception BuildException thrown if both rootLocalFolder 
     * and viewRootLocalFolder are defined
     */
    protected void testPreconditions() throws BuildException {
    }
    /**
     * Implements base-class abstract function to emit to the log an 
     * entry describing the parameters that will be used by this operation.
     *
     * @param starteamrootFolder
     *               root folder in StarTeam for the operation
     * @param targetrootFolder
     *               root local folder for the operation 
     * (whether specified by the user or not).
     */
    protected void logOperationDescription(
        Folder starteamrootFolder, java.io.File targetrootFolder) 
    {
        log((this.isRecursive() ? "Recursive" : "Non-recursive")
            +" Checkin from" 
            + (null == getRootLocalFolder() ? " (default): " : ": ") 
            + targetrootFolder.getAbsolutePath());
        
        log("Checking in to: " + starteamrootFolder.getFolderHierarchy());
        logIncludes();
        logExcludes();

        if (this.lockStatus == Item.LockType.UNLOCKED) {
            log("  Items will be checked in unlocked.");
        } 
        else {
            log("  Items will be checked in with no change in lock status.");
        }

        if (this.isForced()) {
            log("  Items will be checked in in accordance with repository status and regardless of lock status.");
        } 
        else {
            log("  Items will be checked in regardless of repository status only if locked." );
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
            throws BuildException 
    {
        try {
            if (null != getRootLocalFolder()) {
                starteamFolder.setAlternatePathFragment(
                    targetFolder.getAbsolutePath());
            }

            Folder[] foldersList = starteamFolder.getSubFolders();
            Item[] stFiles = starteamFolder.getItems(getTypeNames().FILE);
            
            // note, it's important to scan the items BEFORE we make the
            // UnmatchedFileMap because that creates a bunch of NEW
            // folders and files (unattached to repository) and we
            // don't want to include those in our traversal.

            UnmatchedFileMap ufm = 
                new CheckinMap().init(
                    targetFolder.getAbsoluteFile(), starteamFolder);


            for (int i = 0, size = foldersList.length; i < size; i++) {
                Folder stFolder = foldersList[i];
                java.io.File subfolder = 
                    new java.io.File(targetFolder, stFolder.getName());

                ufm.removeControlledItem(subfolder);

                if (isRecursive()) {
                    visit(stFolder, subfolder);
                }
            }

           
            for (int i = 0, size = stFiles.length; i < size; i++) {
                com.starbase.starteam.File stFile = 
                    (com.starbase.starteam.File) stFiles[i];
                processFile(stFile);
                
                ufm.removeControlledItem(
                    new java.io.File(targetFolder, stFile.getName()));
            }

            if (this.addUncontrolled) {
                ufm.processUncontrolledItems();
            }

        } catch (IOException e) {
            throw new BuildException(e);
        }

    }

    /**
     * provides a string showing from and to full paths for logging
     * 
     * @param remotefile the Star Team file being processed.
     * 
     * @return a string showing from and to full paths
     */
    private String describeCheckin(com.starbase.starteam.File remotefile)
    {
        StringBuffer sb = new StringBuffer();
        sb.append(remotefile.getFullName())
          .append(" --> ")
          .append(getFullRepositoryPath(remotefile));
        return sb.toString();
    }

    /**
     * Processes (checks-out) <code>stFiles</code>files from StarTeam folder.
     *
     * @param eachFile repository file to process
     * @param targetFolder a java.io.File (Folder) to work
     * @throws IOException when StarTeam API fails to work with files
     */
    private void processFile(com.starbase.starteam.File eachFile)
        throws IOException {
        String filename = eachFile.getName();

        // If the file doesn't pass the include/exclude tests, skip it.
        if (!shouldProcess(filename)) {
            log("Excluding " + getFullRepositoryPath(eachFile));
                return;
        }

        boolean checkin = true;
        int fileStatus = (eachFile.getStatus());

        // We try to update the status once to give StarTeam
        // another chance.

        if (fileStatus == Status.MERGE || fileStatus == Status.UNKNOWN) {
            eachFile.updateStatus(true, true);
            fileStatus = (eachFile.getStatus());
        }

        if (fileStatus == Status.MODIFIED) {
            log("Checking in: " + describeCheckin(eachFile));
        } 
        else if (fileStatus == Status.MISSING) {
            log("Local file missing: " + describeCheckin(eachFile));
            checkin = false;
        }
        else {
            if (isForced()) {
                log("Forced checkin of " + describeCheckin(eachFile) + 
                    " over status " + Status.name(fileStatus));
            } else {
                log("Skipping: " + getFullRepositoryPath(eachFile) + 
                    " - status: " + Status.name(fileStatus));
                checkin = false;
            }
        }
        if (checkin) {
            eachFile.checkin(this.comment, this.lockStatus, 
                             this.isForced(), true, true);
        }
    }

    /**
     * handles the deletion of uncontrolled items
     */
    private class CheckinMap extends UnmatchedFileMap {
        protected boolean isActive() {
            return StarTeamCheckin.this.addUncontrolled;
        }

    
        /**
         * This override adds all its members to the repository.  It is assumed 
         * that this method will not be called until all the items in the 
         * corresponding folder have been processed, and that the internal map
         * will contain only uncontrolled items.
         */
        void processUncontrolledItems() throws BuildException {
            if (this.isActive()) {
                Enumeration e = this.keys();
                while (e.hasMoreElements()) {
                    java.io.File local = (java.io.File) e.nextElement();
                    Item remoteItem = (Item) this.get(local);
                    remoteItem.update();
    
                    // once we find a folder that isn't in the repository, 
                    // we know we can add it.
                    if (local.isDirectory()) {
                        Folder folder = (Folder) remoteItem;
                        log("Added uncontrolled folder " 
                            + folder.getFolderHierarchy()
                            + " from " + local.getAbsoluteFile());
                        if (isRecursive()) {
                            UnmatchedFileMap submap = 
                                new CheckinMap().init(local, folder);
                            submap.processUncontrolledItems();
                        }
                    } else {
                        com.starbase.starteam.File remoteFile =
                            (com.starbase.starteam.File) remoteItem;
                        log("Added uncontrolled file " 
                            + TreeBasedTask.getFullRepositoryPath(remoteFile)
                            + " from " + local.getAbsoluteFile());
    
                    }
                }
            }
        }
    }

}


