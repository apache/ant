/*
 * Copyright  2002,2004 Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
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
package org.apache.tools.ant.taskdefs.optional.j2ee;

import org.apache.tools.ant.BuildException;

/**
 *  An interface for vendor-specific "hot" deployment tools.
 *
 *  @author Christopher A. Longo - cal@cloud9.net
 *
 *  @see org.apache.tools.ant.taskdefs.optional.j2ee.AbstractHotDeploymentTool
 *  @see org.apache.tools.ant.taskdefs.optional.j2ee.ServerDeploy
 */
public interface HotDeploymentTool {
    /** The delete action String **/
    public static final String ACTION_DELETE = "delete";

    /** The deploy action String **/
    public static final String ACTION_DEPLOY = "deploy";

    /** The list action String **/
    public static final String ACTION_LIST = "list";

    /** The undeploy action String **/
    public static final String ACTION_UNDEPLOY = "undeploy";

    /** The update action String **/
    public static final String ACTION_UPDATE = "update";

    /**
     *  Validates the passed in attributes.
     *  @exception org.apache.tools.ant.BuildException if the attributes are invalid or incomplete.
     */
    public void validateAttributes() throws BuildException;

    /**
     *  Perform the actual deployment.
     *  @exception org.apache.tools.ant.BuildException if the attributes are invalid or incomplete.
     */
    public void deploy() throws BuildException;

    /**
     *  Sets the parent task.
     *  @param task A ServerDeploy object representing the parent task.
     */
    public void setTask(ServerDeploy task);
}
