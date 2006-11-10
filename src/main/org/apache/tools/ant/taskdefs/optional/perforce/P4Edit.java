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
 * Open file(s) for edit.
 * P4Change should be used to obtain a new changelist for P4Edit as,
 * although P4Edit can open files to the default change,
 * P4Submit cannot yet submit to it.
 * Example Usage:<br>
 * &lt;p4edit change="${p4.change}" view="//depot/project/foo.txt" /&gt;
 *
 * @todo Should call reopen if file is already open in one of our changelists perhaps?
 *
 * @ant.task category="scm"
 */

public class P4Edit extends P4Base {

    // CheckStyle:VisibilityModifier OFF - bc
    /**
     * number of the change list to work on
     */
    public String change = null;

    // CheckStyle:VisibilityModifier ON

    /**
     * An existing changelist number to assign files to; optional
     * but strongly recommended.
     * @param change the change list number
     */
    public void setChange(String change) {
        this.change = change;
    }

    /**
     * Run the p4 edit command
     * @throws BuildException if there is no view specified
     */
    public void execute() throws BuildException {
        if (change != null) {
            P4CmdOpts = "-c " + change;
        }
        if (P4View == null) {
            throw new BuildException("No view specified to edit");
        }
        execP4Command("-s edit " + P4CmdOpts + " " + P4View, new SimpleP4OutputHandler(this));
    }
}
