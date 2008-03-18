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

/**
 * Integrate file(s).
 * P4Change should be used to obtain a new changelist for P4Integrate,
 * although P4Integrate can open files to the default change,
 * P4Submit cannot yet submit to it.
 * Example Usage:<br>
 * &lt;p4integrate change="${p4.change}"
 * fromfile="//depot/project/dev/foo.txt" tofile="//depot/project/main/foo.txt" /&gt;
 *
 *
 * @ant.task category="scm"
 */

public class P4Integrate extends P4Base {

    private String change = null;
    private String fromfile = null;
    private String tofile = null;
    private String branch = null;
    private boolean restoredeletedrevisions = false;
    private boolean forceintegrate = false;
    private boolean leavetargetrevision = false;
    private boolean enablebaselessmerges = false;
    private boolean simulationmode = false;
    private boolean reversebranchmappings = false;
    private boolean propagatesourcefiletype = false;
    private boolean nocopynewtargetfiles = false;

    /**
     * get the changelist number
     *
     * @return the changelist number set for this task
     */
    public String getChange() {
        return change;
    }

    /**
     * set the changelist number for the operation
     *
     * @param change An existing changelist number to assign files to; optional
     * but strongly recommended.
     */
    public void setChange(String change) {
        this.change = change;
    }

    /**
     * get the from file specification
     *
     * @return the from file specification
     */
    public String getFromfile() {
        return fromfile;
    }

    /**
     * sets the from file specification
     *
     * @param fromf the from file specification
     */
    public void setFromfile(String fromf) {
        this.fromfile = fromf;
    }

    /**
     * get the to file specification
     *
     * @return the to file specification
     */
    public String getTofile() {
        return tofile;
    }

    /**
     * sets the to file specification
     *
     * @param tof the to file specification
     */
    public void setTofile(String tof) {
        this.tofile = tof;
    }

    /**
     * get the branch
     *
     * @return the name of the branch
     */
    public String getBranch() {
        return branch;
    }

    /**
     * sets the branch
     *
     * @param br the name of the branch to use
     */
    public void setBranch(String br) {
        this.branch = br;
    }

    /**
     * gets the restoredeletedrevisions flag
     *
     * @return restore deleted revisions
     */
    public boolean isRestoreDeletedRevisions() {
        return restoredeletedrevisions;
    }

    /**
     * sets the restoredeletedrevisions flag
     *
     * @param setrest value chosen for restoredeletedrevisions
     */
    public void setRestoreDeletedRevisions(boolean setrest) {
        this.restoredeletedrevisions = setrest;
    }

    /**
     * gets the forceintegrate flag
     *
     * @return restore deleted revisions
     */
    public boolean isForceIntegrate() {
        return forceintegrate;
    }

    /**
     * sets the forceintegrate flag
     *
     * @param setrest value chosen for forceintegrate
     */
    public void setForceIntegrate(boolean setrest) {
        this.forceintegrate = setrest;
    }

    /**
     * gets the leavetargetrevision flag
     *
     * @return flag indicating if the target revision should be preserved
     */
    public boolean isLeaveTargetRevision() {
        return leavetargetrevision;
    }

    /**
     * sets the leavetargetrevision flag
     *
     * @param setrest value chosen for leavetargetrevision
     */
    public void setLeaveTargetRevision(boolean setrest) {
        this.leavetargetrevision = setrest;
    }

    /**
     * gets the enablebaselessmerges flag
     *
     * @return boolean indicating if baseless merges are desired
     */
    public boolean isEnableBaselessMerges() {
        return enablebaselessmerges;
    }

    /**
     * sets the enablebaselessmerges flag
     *
     * @param setrest value chosen for enablebaselessmerges
     */
    public void setEnableBaselessMerges(boolean setrest) {
        this.enablebaselessmerges = setrest;
    }

    /**
     * gets the simulationmode flag
     *
     * @return simulation mode flag
     */
    public boolean isSimulationMode() {
        return simulationmode;
    }

    /**
     * sets the simulationmode flag
     *
     * @param setrest value chosen for simulationmode
     */
    public void setSimulationMode(boolean setrest) {
        this.simulationmode = setrest;
    }
    /**
     * returns the flag indicating if reverse branch mappings are sought
     *
     * @return reversebranchmappings flag
     */
    public boolean isReversebranchmappings() {
        return reversebranchmappings;
    }

    /**
     *  sets the reversebranchmappings flag
     *
     *  @param reversebranchmappings flag indicating if reverse branch mappings are sought
     */
    public void setReversebranchmappings(boolean reversebranchmappings) {
        this.reversebranchmappings = reversebranchmappings;
    }
    /**
     *  returns flag indicating if propagation of source file type is sought
     *
     *  @return flag set to true if you want to propagate source file type for existing target files
     */
    public boolean isPropagatesourcefiletype() {
        return propagatesourcefiletype;
    }
    /**
     *   sets flag indicating if one wants to propagate the source file type
     *
     *   @param propagatesourcefiletype
     *   set it to true if you want to change the type of existing target files
     *   according to type of source file.
     */
    public void setPropagatesourcefiletype(boolean propagatesourcefiletype) {
        this.propagatesourcefiletype = propagatesourcefiletype;
    }
    /**
     *   indicates intention to suppress the copying on the local hard disk of new target files.
     *
     *   @return indicates intention to suppress the copying
     *   on the local hard disk of new target files.
     */
    public boolean isNocopynewtargetfiles() {
        return nocopynewtargetfiles;
    }

    /**
     *   sets nocopynewtargetfiles flag
     *
     *   @param nocopynewtargetfiles set it to true to gain speed in integration by not copying on
     *   the local Perforce client new target files
     */
    public void setNocopynewtargetfiles(boolean nocopynewtargetfiles) {
        this.nocopynewtargetfiles = nocopynewtargetfiles;
    }

    /**
     *  execute the p4 integrate
     *  @throws BuildException if there are missing parameters
     */
    public void execute() throws BuildException {
        if (change != null) {
            P4CmdOpts = "-c " + change;
        }
        if (this.forceintegrate) {
            P4CmdOpts = P4CmdOpts + " -f";
        }
        if (this.restoredeletedrevisions) {
                P4CmdOpts = P4CmdOpts + " -d";
            }
        if (this.leavetargetrevision) {
            P4CmdOpts = P4CmdOpts + " -h";
        }
        if (this.enablebaselessmerges) {
            P4CmdOpts = P4CmdOpts + " -i";
        }
        if (this.simulationmode) {
            P4CmdOpts = P4CmdOpts + " -n";
        }
        if (this.reversebranchmappings) {
            P4CmdOpts = P4CmdOpts + " -r";
        }
        if (this.propagatesourcefiletype) {
            P4CmdOpts = P4CmdOpts + " -t";
        }
        if (this.nocopynewtargetfiles) {
            P4CmdOpts = P4CmdOpts + "-v";
        }
        String command;
        if (branch == null && fromfile != null && tofile != null) {
           command = P4CmdOpts + " " + fromfile + " " + tofile;
        } else if (branch != null && fromfile == null && tofile != null) {
            command = P4CmdOpts + " -b " + branch + " " + tofile;
        } else if (branch != null && fromfile != null) {
            command = P4CmdOpts + " -b " + branch + " -s " + fromfile + " " + tofile;
        } else {
            throw new BuildException("you need to specify fromfile and tofile, "
            + "or branch and tofile, or branch and fromfile, or branch and fromfile and tofile ");
        }
        execP4Command("-s integrate " + command, new SimpleP4OutputHandler(this));
    }
}
