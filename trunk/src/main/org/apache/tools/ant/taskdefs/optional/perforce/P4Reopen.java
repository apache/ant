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
 * Reopen Perforce checkout files between changelists.
 *
 * @ant.task category="scm"
 */
public class P4Reopen extends P4Base {

    private String toChange = "";

    /**
     * The changelist to move files to; required.
     * @param toChange new change list number
     * @throws BuildException if the change parameter is null or empty
     */
    public void setToChange(String toChange) throws BuildException {
        if (toChange == null || toChange.equals("")) {
            throw new BuildException("P4Reopen: tochange cannot be null or empty");
        }

        this.toChange = toChange;
    }

    /**
     * do the work
     * @throws BuildException if P4View is null
     */
    public void execute() throws BuildException {
        if (P4View == null) {
            throw new BuildException("No view specified to reopen");
        }
        execP4Command("-s reopen -c " + toChange + " " + P4View, new SimpleP4OutputHandler(this));
    }
}
