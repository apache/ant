/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999 The Apache Software Foundation.  All rights
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
 *
 */
package org.apache.tools.ant.taskdefs.optional.starteam;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;

import com.starbase.starteam.File;
import com.starbase.starteam.Folder;
import com.starbase.starteam.Item;
import com.starbase.starteam.Status;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.BuildException;

/**
 * This class logs into StarTeam checks out any changes that have occurred since
 * the last successful build. It also creates all working directories on the
 * local directory if appropriate. Ant Usage: <taskdef name="starteamcheckout"
 * classname="org.apache.tools.ant.taskdefs.StarTeamCheckout"/>
 * <starteamcheckout username="BuildMaster" password="ant" starteamFolder="Source"
 * starteamurl="servername:portnum/project/view"
 * createworkingdirectories="true"/>
 *
 * @author Christopher Charlier, ThoughtWorks, Inc. 2001
 * @author <a href="mailto:jcyip@thoughtworks.com">Jason Yip</a>
 * @author Jason Pettiss
 * @version 1.1
 * @author <a href="mailto:stevec@ignitesports.com">Steve Cohen</a>
 * @see <A HREF="http://www.starbase.com/">StarBase Web Site</A>
 */
public class StarTeamCheckout extends TreeBasedTask {

    private boolean createDirs = true;
    private boolean deleteUncontrolled = true;
    /**
     * Set the attribute that tells ant if we want to create all directories
     * that are in the Starteam repository regardless if they are empty.
     */
    public void setCreateWorkingDirs(boolean value) {
        this.createDirs = value;
    }

    /**
     * Sets the attribute that tells ant whether or not to remove local files
     * that are NOT found in the Starteam repository.
     */
    public void setDeleteUncontrolled(boolean value) {
        this.deleteUncontrolled = value;
    }


    /**
     * Implements base-class abstract function to perform the checkout operation
     * on the files in each folder of the tree.
     *
     * @param starteamFolder the StarTeam folder from which files to be 
     *                       checked out
     * @param targetFolder the local mapping of rootStarteamFolder
     */
    protected void visit(Folder starteamFolder, java.io.File targetFolder ) 
	throws BuildException 
    {
	try {
	    Hashtable localFiles = getLocalFiles(targetFolder);

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
		java.io.File localFile = new java.io.File(targetFolder, filename);
		localFiles.remove(localFile.toString());
		
		int fileStatus = (eachFile.getStatus());

		// We try to update the status once to give StarTeam another chance.
		if (fileStatus == Status.MERGE || fileStatus == Status.UNKNOWN) {
		    eachFile.updateStatus(true, true);
		}

		// If the file is current then skip it.
		// If the file doesn't pass the include/exclude tests, skip it. 
		if (fileStatus == Status.CURRENT || !shouldProcess(filename)) {
		    continue;
		}

		// Check out anything else.
		// Just a note: StarTeam has a status for NEW which implies that there 
		// is an item  on your local machine that is not in the repository. 
		// These are the items that show up as NOT IN VIEW in the Starteam GUI.
		// One would think that we would want to perhaps checkin the NEW items
		// (not in all cases! - Steve Cohen 15 Dec 2001)
		// Unfortunately, the sdk doesn't really work, and we can't actually see
		// anything with a status of NEW. That is why we can just check out 
		// everything here without worrying about losing anything.

		log("Checking Out: " + (localFile.toString()), Project.MSG_INFO);
		eachFile.checkoutTo(localFile, Item.LockType.
				    UNCHANGED, true, true, true);
	    }

	    // Now we recursively call this method on all sub folders in this folder.
	    Folder[] subFolders = starteamFolder.getSubFolders();
	    for (int i = 0; i < subFolders.length; i++) {
		localFiles.remove(subFolders[i].getPath());
		visit(subFolders[i], 
		      new java.io.File(targetFolder, subFolders[i].getName()));
	    }

	    // Delete all folders or files that are not in Starteam.
	    if (this.deleteUncontrolled && !localFiles.isEmpty()) {
		delete(localFiles);
	    }
	} catch (IOException e) {
	    throw new BuildException(e);
	}
    }

    /**
     * Deletes everything on the local machine that is not in Starteam.
     *
     * @param files 
     */
    /**
     * Deletes everything on the local machine that is not in Starteam.
     *
     * @param files an "identity" <code>Hashtable</code> which we use only because
     *              of ant's requirement to be JDK 1.1 compatible.  Otherwise, we 
     *              could  use a set.   We are only interested in the keys,
     *              not the associated values in this Hashtable.  Each of its keys 
     *              represents the name of a local file to be deleted. 
     */
    private void delete(Hashtable files) {
        try {

            Enumeration e = files.keys();
            while (e.hasMoreElements()) {
                java.io.File file = new java.io.File(e.nextElement().toString());
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
        // If the current file is a Directory, we need to delete all its children as well.
        if (file.isDirectory()) {
            java.io.File[] children = file.listFiles();
            for (int i = 0; i < children.length; i++) {
                delete(children[i]);
            }
        }

        log("Deleting: " + file.getAbsolutePath(), Project.MSG_INFO);
        return file.delete();
    }

    /**
     * Gets the collection of the local file names in the current directory We
     * need to check this collection against what we find in Starteam to
     * understand what we need to delete in order to synch with the repos.
     *
     * @param folder
     * @return
     */
    private static Hashtable getLocalFiles(java.io.File localFolder) {

	// we can't use java 2 collections so we will use an identity Hashtable to 
        // hold the file names.  We only care about the keys, not the values 
	// (which will all be "").

	Hashtable results = new Hashtable();

        if (localFolder.exists()) {
            String[] localFiles = localFolder.list();
            for (int i = 0; i < localFiles.length; i++) {
		results.put( localFolder.toString() + 
			     java.io.File.separatorChar + localFiles[i], "");
            }
        }
	return results;
    }
}










