/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "Ant" and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

package org.apache.tools.ant.taskdefs.optional.j2ee;

import java.io.File;
import java.util.Vector;
import java.util.Enumeration;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 *  Controls hot deployment tools for J2EE servers.
 *
 *  This class is used as a framework for the creation of vendor specific
 *  hot deployment tools.
 *
 *  @author Christopher A. Longo - cal@cloud9.net
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
    private Vector vendorTools = new Vector();

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
        vendorTools.addElement(tool);
    }

    /**
     *  Creates a WebLogic deployment tool, for deployment to WebLogic servers.
     *  <p>Ant calls this method on creation to handle embedded "weblogic" elements
     *  in the ServerDeploy task.
     *  @param tool An instance of WebLogicHotDeployment tool, passed in by Ant.
     */
    public void addWeblogic(WebLogicHotDeploymentTool tool) {
        tool.setTask(this);
        vendorTools.addElement(tool);
    }

    /**
     *  Creates a JOnAS deployment tool, for deployment to JOnAS servers.
     *  <p>Ant calls this method on creation to handle embedded "jonas" elements
     *  in the ServerDeploy task.
     *  @param tool An instance of JonasHotDeployment tool, passed in by Ant.
     */
    public void addJonas(JonasHotDeploymentTool tool) {
        tool.setTask(this);
        vendorTools.addElement(tool);
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
     *  @exception org.apache.tools.ant.BuildException if the attributes are invalid or incomplete, or
     *  a failure occurs in the deployment process.
     */
    public void execute() throws BuildException {
        for (Enumeration enum = vendorTools.elements(); 
             enum.hasMoreElements();) {
            HotDeploymentTool tool = (HotDeploymentTool) enum.nextElement();
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

