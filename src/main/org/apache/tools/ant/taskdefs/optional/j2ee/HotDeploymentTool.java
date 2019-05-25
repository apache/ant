/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
 *  @see org.apache.tools.ant.taskdefs.optional.j2ee.AbstractHotDeploymentTool
 *  @see org.apache.tools.ant.taskdefs.optional.j2ee.ServerDeploy
 */
public interface HotDeploymentTool {
    /** The delete action String **/
    String ACTION_DELETE = "delete";

    /** The deploy action String **/
    String ACTION_DEPLOY = "deploy";

    /** The list action String **/
    String ACTION_LIST = "list";

    /** The undeploy action String **/
    String ACTION_UNDEPLOY = "undeploy";

    /** The update action String **/
    String ACTION_UPDATE = "update";

    /**
     *  Validates the passed in attributes.
     *  @exception BuildException if the attributes are invalid or incomplete.
     */
    void validateAttributes() throws BuildException;

    /**
     *  Perform the actual deployment.
     *  @throws BuildException if the attributes are invalid or incomplete.
     */
    void deploy() throws BuildException;

    /**
     *  Sets the parent task.
     *  @param task A ServerDeploy object representing the parent task.
     */
    void setTask(ServerDeploy task);
}
