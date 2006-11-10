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
package org.apache.tools.ant.taskdefs.optional.perforce;

import org.apache.tools.ant.BuildException;

/** Checkout files for deletion.
 *
 * Example Usage:<br>
 * &lt;p4delete change="${p4.change}" view="//depot/project/foo.txt" /&gt;<br>
 *
 * Simple re-write of P4Edit changing 'edit' to 'delete'.<br>
 *
 * @todo What to do if file is already open in one of our changelists perhaps
 * (See also {@link P4Edit P4Edit})?<br>
 *
 * @ant.task category="scm"
 */
public class P4Delete extends P4Base {

    // CheckStyle:VisibilityModifier OFF - bc
    /**
     * number of the change list to work on
     */
    public String change = null;
    // CheckStyle:VisibilityModifier ON

    /**
     * An existing changelist number for the deletion; optional
     * but strongly recommended.
     * @param change the number of a change list
     */
    public void setChange(String change) {
        this.change = change;
    }

    /**
     * executes the p4 delete task
     * @throws BuildException if there is no view specified
     */
    public void execute() throws BuildException {
        if (change != null) {
            P4CmdOpts = "-c " + change;
        }
        if (P4View == null) {
            throw new BuildException("No view specified to delete");
        }
        execP4Command("-s delete " + P4CmdOpts + " " + P4View, new SimpleP4OutputHandler(this));
    }
}
