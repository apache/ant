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
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
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

package org.apache.tools.ant.taskdefs.optional.ejb;

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.types.Path;

/**
 *  A task to support "hot" deployment tools for J2EE servers.
 *
 *  This class is used as a framework for the creation of vendor specific
 *  "hot" deployment tools.
 *
 *  @author Christopher A. Longo - cal@cloud9.net
 *
 *  @see EjbHotDeploymentTool
 *  @see AbstractEjbHotDeploymentTool
 *  @see WebLogicHotDeploymentTool
 */
public class EjbDeploy extends Task
{
    /** The action to be performed.  IE: "deploy", "delete", etc... **/
    private String action;

    /** The classpath passed to the JVM on execution. **/
    private Path classpath;

    /** The username for the deployment server. **/
    private String userName;

    /** The password for the deployment server. **/
    private String password;

    /** The URL of the deployment server **/
    private String serverUrl;

    /** The source (fully-qualified path) to the component being deployed **/
    private File source;

    /** The vendor specific tool for deploying the component **/
    private EjbHotDeploymentTool vendorTool;

    /**
     *  Creates a classpath.  Used to handle the nested classpath
     *  element.
     *  @return A Path object representing the classpath to be used.
     */
    public Path createClasspath() {
        if(classpath == null)
            classpath = new Path(project);

        return classpath.createPath();
    }

    ///////////////////////////////////////////////////////////////////////////
    //
    //	Place vendor specific tool creations here.
    //
    ///////////////////////////////////////////////////////////////////////////

    /**
     *  Creates a WebLogic deployment tool, for deployment to WebLogic servers.
     *  <p>Ant calls this method on creation to handle embedded "weblogic" tags
     *  in the EjbDeploy task.
     *  @return An instance of WebLogicHotDeployment tool.
     */
    public WebLogicHotDeploymentTool createWeblogic() {
        WebLogicHotDeploymentTool weblogic = new WebLogicHotDeploymentTool(this);
        vendorTool = (EjbHotDeploymentTool)weblogic;
        return weblogic;
    }

    /**
     *  Execute the task.
     *  <p>This will fork a JVM and run the vendor-specific deployment tool.
     *  The process will fail if the tool returns an error.
     *  @exception BuildException if the attributes are invalid or incomplete
     */
    public void execute() throws BuildException {
        if (vendorTool == null) {
            throw new BuildException("No vendor tool specified");
        }
        
        vendorTool.validateAttributes();

        Java deploy = (Java)project.createTask("java");
        deploy.setFork(true);
        deploy.setFailonerror(true);
        deploy.setClasspath(classpath);

        deploy.setClassname(vendorTool.getClassName());
        deploy.createArg().setLine(vendorTool.getArguments());
        deploy.execute();

        deploy = null;
    }

    ///////////////////////////////////////////////////////////////////////////
    //
    //	Set/get methods
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
     *  Sets the action field.
     *  This is a required attribute.
     *  @param action A String representing the "action" attribute.
     */
    public void setAction(String action) {
        this.action = action;
    }

    /**
     *  gets the classpath field.
     *  @return A Path representing the "classpath" attribute.
     */
    public Path getClasspath() {
        return classpath;
    }

    /**
     *  Sets the classpath field.
     *  This is not a required attribute.
     *  @param classpath A Path object representing the "classpath" attribute.
     */
    public void setClasspath(Path classpath) {
        this.classpath = classpath;
    }

    /**
     *  Returns the userName field.
     *  @return A String representing the "userName" attribute.
     */
    public String getUserName() {
        return userName;
    }

    /**
     *  Sets the userName field.
     *  This is not a required attribute.
     *  @param userName A String representing the "userName" attribute.
     */
    public void setUserName(String userName) {
        this.userName = userName;
    }

    /**
     *  Returns the password field.
     *  @return A String representing the "password" attribute.
     */
    public String getPassword() {
        return password;
    }

    /**
     *  Set the password field.
     *  This is a required attribute.
     *  @param A String representing the "password" attribute.
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     *  Returns the serverUrl field.
     *  @return A String representing the "serverUrl" attribute.
     */
    public String getServerUrl() {
        return serverUrl;
    }

    /**
     *  Sets the serverUrl field.
     *  This is not a required attribute.
     *  @param serverUrl A String representing the "serverUrl" attribute.
     */
    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
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
     *  Sets the source field (the path/filename of the component to be
     *  deployed.
     *  This is not a required attribute.
     *  @param A String representing the "source" attribute.
     */
    public void setSource(File source) {
        this.source = source;
    }
}
