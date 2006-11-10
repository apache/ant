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
import org.apache.tools.ant.util.StringUtils;

/**
 *  This method syncs an existing Perforce label against the Perforce client
 *  or against a set of files/revisions.
 *
 *
 * Example Usage:
 * <pre>
 *   &lt;p4labelsync name="MyLabel-${TSTAMP}-${DSTAMP}"
 *   view="//depot/...#head;//depot2/file1#25" /&gt;
 * </pre>
 *
 * @ant.task category="scm"
 */
public class P4Labelsync extends P4Base {

    // CheckStyle:VisibilityModifier OFF - bc
    protected String name;
    private boolean add; /* -a */
    private boolean delete; /* -n */
    private boolean simulationmode;  /* -n */
    // CheckStyle:VisibilityModifier ON
    /**
     * -a flag of p4 labelsync - preserve files which exist in the label,
     * but not in the current view
     * @return  add attribute
     * if set to true the task will not remove any files from the label
     * only add files which were not there previously or update these where the revision has changed
     * the add attribute is the -a flag of p4 labelsync
     */
    public boolean isAdd() {
        return add;
    }
    /**
     * -a flag of p4 labelsync - preserve files which exist in the label,
     * but not in the current view
     * @param add  if set to true the task will not remove any files from the label
     * only add files which were not there previously or update these where the revision has changed
     * the add attribute is the -a flag of p4 labelsync
     */
    public void setAdd(boolean add) {
        this.add = add;
    }
    /**
     * -d flag of p4 labelsync; indicates an intention of deleting from the label
     * the files specified in the view
     * @return  delete attribute
     */
    public boolean isDelete() {
        return delete;
    }

    /**
     * -d flag of p4 labelsync; indicates an intention of deleting from the label
     *  the files specified in the view
     * @param delete indicates intention of deleting from the label
     * the files specified in the view
     */
    public void setDelete(boolean delete) {
        this.delete = delete;
    }


    /**
     * The name of the label; optional, default "AntLabel"
     * @param name of the label
     */
    public void setName(String name) {
        this.name = name;
    }
    /**
     * -n flag of p4 labelsync - display changes without actually doing them
     * @return -n flag of p4 labelsync
     */
    public boolean isSimulationmode() {
        return simulationmode;
    }
    /**
     * -n flag of p4 labelsync - display changes without actually doing them
     * @param simulationmode display changes without actually doing them
     */
    public void setSimulationmode(boolean simulationmode) {
        this.simulationmode = simulationmode;
    }


    /**
     *  do the work
     * @throws BuildException if the label name is not supplied
     */
    public void execute() throws BuildException {
        log("P4Labelsync exec:", Project.MSG_INFO);

        if (P4View != null && P4View.length() >= 1) {
            P4View = StringUtils.replace(P4View, ":", "\n\t");
            P4View = StringUtils.replace(P4View, ";", "\n\t");
        }
        if (P4View == null) {
            P4View = "";
        }

        if (name == null || name.length() < 1) {
            throw new BuildException("name attribute is compulsory for labelsync");
        }

        if (this.isSimulationmode()) {
            P4CmdOpts = P4CmdOpts + " -n";
        }
        if (this.isDelete()) {
            P4CmdOpts = P4CmdOpts + " -d";
        }
        if (this.isAdd()) {
            P4CmdOpts = P4CmdOpts + " -a";
        }

        execP4Command("-s labelsync -l " + name + " " + P4CmdOpts + " " + P4View,
            new SimpleP4OutputHandler(this));


    }
}

