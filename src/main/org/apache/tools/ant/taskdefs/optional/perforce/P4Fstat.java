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
/*
 * Portions of this software are based upon public domain software
 * originally written at the National Center for Supercomputing Applications,
 * University of Illinois, Urbana-Champaign.
 */

package org.apache.tools.ant.taskdefs.optional.perforce;


import java.io.File;
import java.util.Vector;
import java.util.ArrayList;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;

/**
 * P4Fstat--find out which files are under Perforce control and which are not.
 *
 * <br><b>Example Usage:</b><br>
 * <pre>
 * &lt;project name=&quot;p4fstat&quot; default=&quot;p4fstat&quot;
 * basedir=&quot;C:\dev\gnu&quot;&gt;
 *     &lt;target name=&quot;p4fstat&quot; &gt;
 *         &lt;p4fstat showfilter=&quot;all&quot;&gt;
 *             &lt;fileset dir=&quot;depot&quot; includes=&quot;**\/*&quot;/&gt;
 *         &lt;/p4fstat&gt;
 *     &lt;/target&gt;
 * &lt;/project&gt;
 * </pre>
 *
 * @ant.task category="scm"
 */
public class P4Fstat extends P4Base {

    private int changelist;
    private String addCmd = "";
    private Vector filesets = new Vector();
    private static final int DEFAULT_CMD_LENGTH = 300;
    private int cmdLength = DEFAULT_CMD_LENGTH;
    private static final int SHOW_ALL = 0;
    private static final int SHOW_EXISTING = 1;
    private static final int SHOW_NON_EXISTING = 2;
    private int show = SHOW_NON_EXISTING;
    private FStatP4OutputHandler handler;
    private StringBuffer filelist;
    private int fileNum = 0;
    private int doneFileNum = 0;
    private boolean debug = false;

    private static final String EXISTING_HEADER
        = "Following files exist in perforce";
    private static final String NONEXISTING_HEADER
        = "Following files do not exist in perforce";

    /**
     * Sets the filter that one wants applied.
     * <table>
     * <tr><th>Option</th><th>Meaning</th></tr>
     * <tr><td>all</td><td>all files under Perforce control or not</td></tr>
     * <tr><td>existing</td><td>only files under Perforce control</td></tr>
     * <tr><td>non-existing</td><td>only files not under Perforce control or not</td></tr>
     * </table>
     * @param filter should be one of all|existing|non-existing.
     */
    public void setShowFilter(String filter) {
        if (filter.equalsIgnoreCase("all")) {
            show = SHOW_ALL;
        } else if (filter.equalsIgnoreCase("existing")) {
            show = SHOW_EXISTING;
        } else if (filter.equalsIgnoreCase("non-existing")) {
            show = SHOW_NON_EXISTING;
        } else {
            throw new BuildException("P4Fstat: ShowFilter should be one of: "
                + "all, existing, non-existing");
        }
    }

    /**
     * Sets optionally a change list number.
     * @param changelist change list that one wants information about.
     * @throws BuildException if the change list number is negative.
     */
    public void setChangelist(int changelist) throws BuildException {
        if (changelist <= 0) {
            throw new BuildException("P4FStat: Changelist# should be a "
                + "positive number");
        }
        this.changelist = changelist;
    }

    /**
     * Adds a fileset to be examined by p4fstat.
     * @param set the fileset to add.
     */
    public void addFileset(FileSet set) {
        filesets.addElement(set);
    }

    /**
     * Executes the p4fstat task.
     * @throws BuildException if no files are specified.
     */
    public void execute() throws BuildException {
        handler = new FStatP4OutputHandler(this);
        if (P4View != null) {
            addCmd = P4View;
        }
        P4CmdOpts = (changelist > 0) ? ("-c " + changelist) : "";

        filelist = new StringBuffer();

        for (int i = 0; i < filesets.size(); i++) {
            FileSet fs = (FileSet) filesets.elementAt(i);
            DirectoryScanner ds = fs.getDirectoryScanner(getProject());

            String[] srcFiles = ds.getIncludedFiles();
            fileNum = srcFiles.length;

            if (srcFiles != null) {
                for (int j = 0; j < srcFiles.length; j++) {
                    File f = new File(ds.getBasedir(), srcFiles[j]);
                    filelist.append(" ").append('"').append(f.getAbsolutePath()).append('"');
                    doneFileNum++;
                    if (filelist.length() > cmdLength) {

                        execP4Fstat(filelist);
                        filelist = new StringBuffer();
                    }
                }
                if (filelist.length() > 0) {
                    execP4Fstat(filelist);
                }
            } else {
                log("No files specified to query status on!", Project.MSG_WARN);
            }
        }
        if (show == SHOW_ALL || show == SHOW_EXISTING) {
            printRes(handler.getExisting(), EXISTING_HEADER);
        }
        if (show == SHOW_ALL || show == SHOW_NON_EXISTING) {
            printRes(handler.getNonExisting(), NONEXISTING_HEADER);
        }
    }

    /**
     * Return the number of files seen.
     * @return the number of files seen.
     */
    public int getLengthOfTask() {
        return fileNum;
    }

    /**
     * Return the number of passes to make.
     * IS THIS BEING USED?
     * @return number of passes (how many filesets).
     */
    int getPasses() {
        return filesets.size();
    }

    private void printRes(ArrayList ar, String header) {
        log(header, Project.MSG_INFO);
        for (int i = 0; i < ar.size(); i++) {
            log((String) ar.get(i), Project.MSG_INFO);
        }
    }

    private void execP4Fstat(StringBuffer list) {
        String l = list.substring(0);
        if (debug) {
            log("Executing fstat " + P4CmdOpts + " " + addCmd + l + "\n",
                Project.MSG_INFO);
        }
        execP4Command("fstat " + P4CmdOpts + " " + addCmd + l, handler);
    }

}
