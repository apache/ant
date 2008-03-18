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

import org.apache.tools.ant.Project;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;

/**
 * Adds specified files to Perforce.
 *
 * <b>Example Usage:</b>
 * <table border="1">
 * <th>Function</th><th>Command</th>
 * <tr><td>Add files using P4USER, P4PORT and P4CLIENT settings specified</td>
 * <td>&lt;P4add <br>P4view="//projects/foo/main/source/..." <br>P4User="fbloggs"
 * <br>P4Port="km01:1666"
 * <br>P4Client="fbloggsclient"&gt;<br>&lt;fileset basedir="dir" includes="**&#47;*.java"&gt;<br>
 * &lt;/p4add&gt;</td></tr>
 * <tr><td>Add files using P4USER, P4PORT and P4CLIENT settings defined in environment</td><td>
 * &lt;P4add P4view="//projects/foo/main/source/..." /&gt;<br>&lt;fileset basedir="dir"
 * includes="**&#47;*.java"&gt;<br>&lt;/p4add&gt;</td></tr>
 * <tr><td>Specify the length of command line arguments to pass to each invocation of p4</td>
 * <td>&lt;p4add Commandlength="450"&gt;</td></tr>
 * </table>
 *
 * @ant.task category="scm"
 */
public class P4Add extends P4Base {
    private static final int DEFAULT_CMD_LENGTH = 450;
    private int changelist;
    private String addCmd = "";
    private Vector filesets = new Vector();
    private int cmdLength = DEFAULT_CMD_LENGTH;

    /**
     *   Set the maximum length
     *   of the commandline when calling Perforce to add the files.
     *   Defaults to 450, higher values mean faster execution,
     *   but also possible failures.
     *   @param len maximum length of command line default is 450.
     *   @throws BuildException if trying to set the command line length to 0 or less.
     */

    public void setCommandlength(int len) throws BuildException {
        if (len <= 0) {
            throw new BuildException("P4Add: Commandlength should be a positive number");
        }
        this.cmdLength = len;
    }

    /**
     * If specified the open files are associated with the
     * specified pending changelist number; otherwise the open files are
     * associated with the default changelist.
     *
     * @param changelist the change list number.
     *
     * @throws BuildException if trying to set a change list number &lt;=0.
     */
    public void setChangelist(int changelist) throws BuildException {
        if (changelist <= 0) {
            throw new BuildException("P4Add: Changelist# should be a positive number");
        }
        this.changelist = changelist;
    }

    /**
     * Add a fileset whose files will be added to Perforce.
     *
     * @param set the FileSet that one wants to add to Perforce Source Control.
     */
    public void addFileset(FileSet set) {
        filesets.addElement(set);
    }

    /**
     * Run the task.
     *
     * @throws BuildException if the execution of the Perforce command fails.
     */
    public void execute() throws BuildException {
        if (P4View != null) {
            addCmd = P4View;
        }
        P4CmdOpts = (changelist > 0) ? ("-c " + changelist) : "";

        StringBuffer filelist = new StringBuffer();

        for (int i = 0; i < filesets.size(); i++) {
            FileSet fs = (FileSet) filesets.elementAt(i);
            DirectoryScanner ds = fs.getDirectoryScanner(getProject());

            String[] srcFiles = ds.getIncludedFiles();
            if (srcFiles != null) {
                for (int j = 0; j < srcFiles.length; j++) {
                    File f = new File(ds.getBasedir(), srcFiles[j]);
                    filelist.append(" ").append('"').append(f.getAbsolutePath()).append('"');
                    if (filelist.length() > cmdLength) {
                        execP4Add(filelist);
                        filelist = new StringBuffer();
                    }
                }
                if (filelist.length() > 0) {
                    execP4Add(filelist);
                }
            } else {
                log("No files specified to add!", Project.MSG_WARN);
            }
        }
    }

    private void execP4Add(StringBuffer list) {
        log("Execing add " + P4CmdOpts + " " + addCmd + list, Project.MSG_INFO);
        execP4Command("-s add " + P4CmdOpts + " " + addCmd + list, new SimpleP4OutputHandler(this));
    }
}
