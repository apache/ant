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

import java.io.File;
import java.util.List;
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 *  Controls hot deployment tools for J2EE servers.
 *
 *  This class is used as a framework for the creation of vendor specific
 *  hot deployment tools.
 *
 *  @see org.apache.tools.ant.taskdefs.optional.j2ee.HotDeploymentTool
 *  @see org.apache.tools.ant.taskdefs.optional.j2ee.AbstractHotDeploymentTool
 *  @see org.apache.tools.ant.taskdefs.optional.j2ee.GenericHotDeploymentTool
 *  @see org.apache.tools.ant.taskdefs.optional.j2ee.WebLogicHotDeploymentTool
 */
public class ServerDeploy extends Task {
    /** The action to be performed.  IE: "deploy", "delete", etc... **/
    private String action;

    /** The source (fully-qualified path) to the component being deployed **/
    private File source;

    /** The vendor specific tool for deploying the component **/
    private List<AbstractHotDeploymentTool> vendorTools = new Vector<>();

    ///////////////////////////////////////////////////////////////////////////
    //
    // Place vendor specific tool creations here.
    //
    ///////////////////////////////////////////////////////////////////////////

    /**
     *  Creates a generic deployment tool.
     *  <p>Ant calls this method on creation to handle embedded "generic" elements
     *  in the ServerDeploy task.
     *  @param tool An instance of GenericHotDeployment tool, passed in by Ant.
     */
    public void addGeneric(GenericHotDeploymentTool tool) {
        tool.setTask(this);
        vendorTools.add(tool);
    }

    /**
     *  Creates a WebLogic deployment tool, for deployment to WebLogic servers.
     *  <p>Ant calls this method on creation to handle embedded "weblogic" elements
     *  in the ServerDeploy task.
     *  @param tool An instance of WebLogicHotDeployment tool, passed in by Ant.
     */
    public void addWeblogic(WebLogicHotDeploymentTool tool) {
        tool.setTask(this);
        vendorTools.add(tool);
    }

    /**
     *  Creates a JOnAS deployment tool, for deployment to JOnAS servers.
     *  <p>Ant calls this method on creation to handle embedded "jonas" elements
     *  in the ServerDeploy task.
     *  @param tool An instance of JonasHotDeployment tool, passed in by Ant.
     */
    public void addJonas(JonasHotDeploymentTool tool) {
        tool.setTask(this);
        vendorTools.add(tool);
    }


    ///////////////////////////////////////////////////////////////////////////
    //
    // Execute method
    //
    ///////////////////////////////////////////////////////////////////////////

    /**
     *  Execute the task.
     *  <p>This method calls the deploy() method on each of the vendor-specific tools
     *  in the <code>vendorTools</code> collection.  This performs the actual
     *  process of deployment on each tool.
     *  @throws BuildException if the attributes
     *  are invalid or incomplete, or a failure occurs in the deployment process.
     */
    @Override
    public void execute() throws BuildException {
        for (HotDeploymentTool tool : vendorTools) {
            tool.validateAttributes();
            tool.deploy();
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    //
    // Set/get methods
    //
    ///////////////////////////////////////////////////////////////////////////

    /**
     *  Returns the action field.
     *  @return A string representing the "action" attribute.
     */
    public String getAction() {
        return action;
    }

    /**
     *  The action to be performed, usually "deploy"; required.
     *   Some tools support additional actions, such as "delete", "list", "undeploy", "update"...
     *  @param action A String representing the "action" attribute.
     */
    public void setAction(String action) {
        this.action = action;
    }

    /**
     *  Returns the source field (the path/filename of the component to be
     *  deployed.
     *  @return A File object representing the "source" attribute.
     */
    public File getSource() {
        return source;
    }

    /**
     *  The filename of the component to be deployed; optional
     *  depending upon the tool and the action.
     *  @param source String representing the "source" attribute.
     */
    public void setSource(File source) {
        this.source = source;
    }
}

