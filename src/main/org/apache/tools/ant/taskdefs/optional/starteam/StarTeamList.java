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

import java.io.IOException;
import java.util.Hashtable;

import com.starbase.starteam.File;
import com.starbase.starteam.Folder;
import com.starbase.starteam.Item;
import com.starbase.starteam.Status;
import com.starbase.starteam.View;
import com.starbase.starteam.ViewConfiguration;

import org.apache.tools.ant.BuildException;

/**
 * Produces a listing of the contents of the StarTeam repository
 * at the specified view and StarTeamFolder.
 *
 * Created: Tue Dec 25 06:51:14 2001
 *
 * @author <a href="mailto:stevec@ignitesports.com">Steve Cohen</a>
 * @version 1.0
 *
 * @ant.task name="stlist" category="scm"
 */

public class StarTeamList extends TreeBasedTask {
    /**
     * List files, dates, and statuses as of this label; optional.
     * The label must exist in starteam or an exception will be thrown.  
     * If not specified, the most recent version of each file will be listed.
     *
     * @param label the label to be listed
     */
    public void setLabel(String label) {
        _setLabel(label);
    }

    /**
     * Override of base-class abstract function creates an
     * appropriately configured view for checkoutlists - either
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
     * Required base-class abstract function implementation is a no-op here.
     *
     * @exception BuildException not thrown in this implementation
     */
    protected void testPreconditions() throws BuildException {
        //intentionally do nothing.
    }

    /**
     * Implements base-class abstract function to perform the checkout
     * operation on the files in each folder of the tree.
     *
     * @param starteamFolder the StarTeam folder from which files to be
     *                       checked out
     * @param targetFolder the local mapping of rootStarteamFolder
     */
    protected void visit(Folder starteamFolder, java.io.File targetFolder)
            throws BuildException {
        try {
            if (null == getRootLocalFolder()) {
                log("Folder: " + starteamFolder.getName() + " (Default folder: " + targetFolder + ")");
            } else {
                log("Folder: " + starteamFolder.getName() + " (Local folder: " + targetFolder + ")");
            }
            Hashtable localFiles = listLocalFiles(targetFolder);

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
                    continue;
                }

                list(eachFile, localFile);
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

        } catch (IOException e) {
            throw new BuildException(e);
        }
    }

    protected void list(File reposFile, java.io.File localFile)
            throws IOException {
        StringBuffer b = new StringBuffer();
        if (null == getRootLocalFolder()) {
            // status is irrelevant to us if we have specified a
            // root local folder.
            b.append(pad(Status.name(reposFile.getStatus()), 12)).append(' ');
        }
        b.append(pad(getUserName(reposFile.getLocker()), 20))
                .append(' ')
                .append(reposFile.getModifiedTime().toString())
                .append(rpad(String.valueOf(reposFile.getSize()), 9))
                .append(' ')
                .append(reposFile.getName());

        log(b.toString());
    }

    private static final String blankstr = blanks(30);

    private static String blanks(int len) {
        StringBuffer b = new StringBuffer();
        for (int i = 0; i < len; i++) {
            b.append(' ');
        }
        return b.toString();
    }

    protected static String pad(String s, int padlen) {
        return (s + blankstr).substring(0, padlen);
    }

    protected static String rpad(String s, int padlen) {
        s = blankstr + s;
        return s.substring(s.length() - padlen);
    }


}// StarTeamList


