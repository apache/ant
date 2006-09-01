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
 * Revert Perforce open files or files in a changelist
 *
 * @ant.task category="scm"
 */
public class P4Revert extends P4Base {

    private String revertChange = null;
    private boolean onlyUnchanged = false;

    /**
     * The changelist to revert; optional.
     * @param revertChange : the change list to revert
     * @throws BuildException if the change list is null or empty string
     */
    public void setChange(String revertChange) throws BuildException {
        if (revertChange == null || revertChange.equals("")) {
            throw new BuildException("P4Revert: change cannot be null or empty");
        }

        this.revertChange = revertChange;

    }

    /**
     * flag to revert only unchanged files (p4 revert -a); optional, default false.
     * @param onlyUnchanged if set to true revert only unchanged files
     */
    public void setRevertOnlyUnchanged(boolean onlyUnchanged) {
        this.onlyUnchanged = onlyUnchanged;
    }

    /**
     * do the work
     * @throws BuildException if an error occurs during the execution of the Perforce command
     * and failonError is set to true
     */
    public void execute() throws BuildException {

        /* Here we can either revert any unchanged files in a changelist
         * or
         * any files regardless of whether they have been changed or not
         *
         *
         * The whole process also accepts a p4 filespec
         */
        String p4cmd = "-s revert";
        if (onlyUnchanged) {
            p4cmd += " -a";
        }

        if (revertChange != null) {
            p4cmd += " -c " + revertChange;
        }

        execP4Command(p4cmd + " " + P4View, new SimpleP4OutputHandler(this));
    }
}
