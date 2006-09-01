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
package org.apache.tools.ant.taskdefs.optional.starteam;

import com.starbase.starteam.File;
import com.starbase.starteam.Folder;
import com.starbase.starteam.Item;
import com.starbase.starteam.Status;
import com.starbase.starteam.View;
import com.starbase.starteam.ViewConfiguration;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Enumeration;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

/**
 * Produces a listing of the contents of the StarTeam repository
 * at the specified view and StarTeamFolder.
 *
 * Created: Tue Dec 25 06:51:14 2001
 *
 * @version 1.0
 *
 * @ant.task name="stlist" category="scm"
 */

public class StarTeamList extends TreeBasedTask {
    private boolean listUncontrolled = true;
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
     * List files, dates, and statuses as of this date; optional.
     * If not specified, the most recent version of each file will be listed.
     *
     * @param asOfDateParam the date as of which the listing to be made
     * @since Ant 1.6
     */
    public void setAsOfDate(String asOfDateParam) {
        _setAsOfDate(asOfDateParam);
    }

    /**
     * Date Format with which asOfDate parameter to be parsed; optional.
     * Must be a SimpleDateFormat compatible string.
     * If not specified, and asOfDateParam is specified, parse will use ISO8601
     * datetime and date formats.
     *
     * @param asOfDateFormat the SimpleDateFormat-compatible format string
     * @since Ant 1.6
     */
    public void setAsOfDateFormat(String asOfDateFormat) {
        _setAsOfDateFormat(asOfDateFormat);
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
        }
        // if a date has been supplied use a view configured to the date.
        View view = getViewConfiguredByDate(raw);
        if (view != null) {
            return view;
        // otherwise, use this view configured as the tip.
        } else {
            return new View(raw, ViewConfiguration.createTip());
        }
    }

    /**
     * Required base-class abstract function implementation checks for
     * incompatible parameters.
     *
     * @exception BuildException thrown on incompatible params specified
     */
    protected void testPreconditions() throws BuildException {
        if (null != getLabel() && null != getAsOfDate()) {
            throw new BuildException(
                "Both label and asOfDate specified.  "
                + "Unable to process request.");
        }
    }

    /**
     * extenders should emit to the log an entry describing the parameters
     * that will be used by this operation.
     *
     * @param starteamrootFolder
     *               root folder in StarTeam for the operation
     * @param targetrootFolder
     *               root local folder for the operation (whether specified by the user or not.
     */
    protected void logOperationDescription(Folder starteamrootFolder,
                                           java.io.File targetrootFolder) {
        log((this.isRecursive() ? "Recursive" : "Non-recursive")
            + " Listing of: " + starteamrootFolder.getFolderHierarchy());

        log("Listing against local folder"
            + (null == getRootLocalFolder() ? " (default): " : ": ")
            + targetrootFolder.getAbsolutePath(),
                    Project.MSG_INFO);
        logLabel();
        logAsOfDate();
        logIncludes();
        logExcludes();


    }
    /**
     * Implements base-class abstract function to perform the checkout
     * operation on the files in each folder of the tree.
     *
     * @param starteamFolder the StarTeam folder from which files to be
     *                       checked out
     * @param targetFolder the local mapping of rootStarteamFolder
     * @throws BuildException on error
     */
    protected void visit(Folder starteamFolder, java.io.File targetFolder)
            throws BuildException {
        try {
            if (null != getRootLocalFolder()) {
                starteamFolder.setAlternatePathFragment(
                    targetFolder.getAbsolutePath());

            }
            Folder[] subFolders = starteamFolder.getSubFolders();
            Item[] files = starteamFolder.getItems(getTypeNames().FILE);

            UnmatchedFileMap ufm =
                new UnmatchedListingMap().init(
                    targetFolder.getAbsoluteFile(), starteamFolder);

            log("");
            log("Listing StarTeam folder "
                + starteamFolder.getFolderHierarchy());
            log(" against local folder " + targetFolder.getAbsolutePath());


            // For all Files in this folder, we need to check
            // if there have been modifications.

            for (int i = 0; i < files.length; i++) {
                File eachFile = (File) files[i];
                String filename = eachFile.getName();
                java.io.File localFile =
                        new java.io.File(targetFolder, filename);

                ufm.removeControlledItem(localFile);

                // If the file doesn't pass the include/exclude tests, skip it.
                if (!shouldProcess(filename)) {
                    continue;
                }

                list(eachFile, localFile);
            }


            // Now we recursively call this method on all sub folders in this
            // folder unless recursive attribute is off.
            for (int i = 0; i < subFolders.length; i++) {
                java.io.File targetSubfolder =
                        new java.io.File(targetFolder, subFolders[i].getName());
                ufm.removeControlledItem(targetSubfolder);
                if (isRecursive()) {
                    visit(subFolders[i], targetSubfolder);
                }
            }
            if (this.listUncontrolled) {
                ufm.processUncontrolledItems();
            }

        } catch (IOException e) {
            throw new BuildException(e);
        }
    }

    private static final SimpleDateFormat SDF =
        new SimpleDateFormat("yyyy-MM-dd hh:mm:ss zzz");

    /**
     * Log a repositary file and it's corresponding local file.
     * @param reposFile the repositary file to log
     * @param localFile the corresponding local file
     * @throws IOException on error getting information from files
     */
    protected void list(File reposFile, java.io.File localFile)
            throws IOException {
        StringBuffer b = new StringBuffer();
        int status = reposFile.getStatus();
        java.util.Date displayDate = null;
        if (status == Status.NEW) {
            displayDate = new java.util.Date(localFile.lastModified());
        } else {
            displayDate = reposFile.getModifiedTime().createDate();
        }
        b.append(pad(Status.name(status), 12)).append(' ');
        b.append(pad(getUserName(reposFile.getLocker()), 20))
                .append(' ')
                .append(SDF.format(displayDate))
                .append(rpad(String.valueOf(reposFile.getSize()), 9))
                .append(' ')
                .append(reposFile.getName());

        log(b.toString());
    }

    private static final String BLANK_STRING = blanks(30);

    private static String blanks(int len) {
        StringBuffer b = new StringBuffer();
        for (int i = 0; i < len; i++) {
            b.append(' ');
        }
        return b.toString();
    }

    /**
     * Return a padded string.
     * @param s the string to pad
     * @param padlen the size of the padded string
     * @return the padded string
     */
    protected static String pad(String s, int padlen) {
        return (s + BLANK_STRING).substring(0, padlen);
    }

    /**
     * Return a right padded string.
     * @param s the string to pad
     * @param padlen the size of the padded string
     * @return the padded string
     */
    protected static String rpad(String s, int padlen) {
        s = BLANK_STRING + s;
        return s.substring(s.length() - padlen);
    }

    /**
     * handles the list of uncontrolled items
     */
    private class UnmatchedListingMap extends UnmatchedFileMap {

        protected boolean isActive() {
            return StarTeamList.this.listUncontrolled;
        }

        /**
         * lists uncontrolled items from the local tree.  It is assumed
         * that this method will not be called until all the items in the
         * corresponding folder have been processed, and that the internal map
         * will contain only uncontrolled items.
         */
        void processUncontrolledItems() throws BuildException {
            if (this.isActive()) {
                Enumeration e = this.keys();

                // handle the files so they appear first
                while (e.hasMoreElements()) {
                    java.io.File local = (java.io.File) e.nextElement();
                    Item remoteItem = (Item) this.get(local);

                    // once we find a folder that isn't in the repository,
                    // we know we can add it.
                    if (local.isFile()) {
                        com.starbase.starteam.File remoteFile =
                            (com.starbase.starteam.File) remoteItem;
                        try {
                            list(remoteFile, local);
                        } catch (IOException ie) {
                            throw new BuildException("IOError in stlist", ie);
                        }
                    }
                }
                // now do it again for the directories so they appear last.
                e = this.keys();
                while (e.hasMoreElements()) {
                    java.io.File local = (java.io.File) e.nextElement();
                    Item remoteItem = (Item) this.get(local);

                    // once we find a folder that isn't in the repository,
                    // we know we can add it.
                    if (local.isDirectory()) {
                        Folder folder = (Folder) remoteItem;
                        if (isRecursive()) {
                            log("Listing uncontrolled folder "
                                + folder.getFolderHierarchy()
                                + " from " + local.getAbsoluteFile());
                            UnmatchedFileMap submap =
                                new UnmatchedListingMap().init(local, folder);
                            submap.processUncontrolledItems();
                        }
                    }
                }
            }
        }
    }
}


