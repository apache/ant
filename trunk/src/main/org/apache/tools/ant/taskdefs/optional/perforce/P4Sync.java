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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

/** Synchronize client space to a Perforce depot view.
 *
 *  The API allows additional functionality of the "p4 sync" command
 * (such as "p4 sync -f //...#have" or other exotic invocations).</P>
 *
 * <b>Example Usage:</b>
 * <table border="1">
 * <th>Function</th><th>Command</th>
 * <tr><td>Sync to head using P4USER, P4PORT and P4CLIENT settings specified</td>
 * <td>&lt;P4Sync <br>P4view="//projects/foo/main/source/..." <br>
 * P4User="fbloggs" <br>P4Port="km01:1666" <br>P4Client="fbloggsclient" /&gt;</td></tr>
 * <tr><td>Sync to head using P4USER, P4PORT and P4CLIENT settings defined in environment</td>
 * <td>&lt;P4Sync P4view="//projects/foo/main/source/..." /&gt;</td></tr>
 * <tr><td>Force a re-sync to head, refreshing all files</td>
 * <td>&lt;P4Sync force="yes" P4view="//projects/foo/main/source/..." /&gt;</td></tr>
 * <tr><td>Sync to a label</td><td>&lt;P4Sync label="myPerforceLabel" /&gt;</td></tr>
 * </table>
 *
 * @todo Add decent label error handling for non-exsitant labels
 *
 * @ant.task category="scm"
 */
public class P4Sync extends P4Base {

    // CheckStyle:VisibilityModifier OFF - bc
    String label;
    private String syncCmd = "";
    // CheckStyle:VisibilityModifier ON

    /**
     * Label to sync client to; optional.
     * @param label name of a label against which one want to sync
     * @throws BuildException if label is null or empty string
     */
    public void setLabel(String label) throws BuildException {
        if (label == null || label.equals("")) {
            throw new BuildException("P4Sync: Labels cannot be Null or Empty");
        }

        this.label = label;

    }


    /**
     * force a refresh of files, if this attribute is set; false by default.
     * @param force sync all files, whether they are supposed to be already uptodate or not.
     * @throws BuildException if a label is set and force is null
     */
    public void setForce(String force) throws BuildException {
        if (force == null && !label.equals("")) {
            throw new BuildException("P4Sync: If you want to force, set force to non-null string!");
        }
        P4CmdOpts = "-f";
    }

    /**
     * do the work
     * @throws BuildException if an error occurs during the execution of the Perforce command
     * and failOnError is set to true
     */
    public void execute() throws BuildException {


        if (P4View != null) {
            syncCmd = P4View;
        }


        if (label != null && !label.equals("")) {
            syncCmd = syncCmd + "@" + label;
        }


        log("Execing sync " + P4CmdOpts + " " + syncCmd, Project.MSG_VERBOSE);

        execP4Command("-s sync " + P4CmdOpts + " " + syncCmd, new SimpleP4OutputHandler(this));
    }
}
