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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Path;

/**
 *  Abstract class to support vendor-specific hot deployment tools.
 *  This class will validate boilerplate attributes.
 *
 *  Subclassing this class for a vendor specific tool involves the
 *  following.
 *  <ol><li>Implement the <code>isActionValid()<code> method to insure the
 *  action supplied as the "action" attribute of ServerDeploy is valid.
 *  <li>Implement the <code>validateAttributes()</code> method to insure
 *  all required attributes are supplied, and are in the correct format.
 *  <li>Add a <code>add&lt;TOOL&gt;</code> method to the ServerDeploy
 *  class.  This method will be called when Ant encounters a
 *  <code>add&lt;TOOL&gt;</code> task nested in the
 *  <code>serverdeploy</code> task.
 *  <li>Define the <code>deploy</code> method.  This method should perform
 *  whatever task it takes to hot-deploy the component.  IE: spawn a JVM and
 *  run class, exec a native executable, run Java code...
 *
 *  @author Christopher A. Longo - cal@cloud9.net
 *
 *  @see org.apache.tools.ant.taskdefs.optional.j2ee.HotDeploymentTool
 *  @see org.apache.tools.ant.taskdefs.optional.j2ee.ServerDeploy
 */
public abstract class AbstractHotDeploymentTool implements HotDeploymentTool {
    /** The parent task **/
    private ServerDeploy task;

    /** The classpath passed to the JVM on execution. **/
    private Path classpath;

    /** The username for the deployment server. **/
    private String userName;

    /** The password for the deployment server. **/
    private String password;

    /** The address of the deployment server **/
    private String server;

    /**
     *  Add a classpath as a nested element.
     *  @return A Path object representing the classpath to be used.
     */
    public Path createClasspath() {
        if (classpath == null)
            classpath = new Path(task.getProject());

        return classpath.createPath();
    }

    /**
     *  Determines if the "action" attribute defines a valid action.
     *  <p>Subclasses should determine if the action passed in is
     *  supported by the vendor's deployment tool.
     *  <p>Actions may by "deploy", "delete", etc... It all depends
     *  on the tool.
     *  @return true if the "action" attribute is valid, false if not.
     */
    protected abstract boolean isActionValid();

    /**
     *  Validates the passed in attributes.
     *  Subclasses should chain to this super-method to insure
     *  validation of boilerplate attributes.
     *  <p>Only the "action" attribute is required in the
     *  base class.  Subclasses should check attributes accordingly.
     *  @exception org.apache.tools.ant.BuildException if the attributes are invalid or incomplete.
     */
    public void validateAttributes() throws BuildException {
        if (task.getAction() == null)
            throw new BuildException("The \"action\" attribute must be set");

        if (!isActionValid())
            throw new BuildException("Invalid action \"" + task.getAction() + "\" passed");

        if (classpath == null)
            throw new BuildException("The classpath attribute must be set");
    }

    /**
     *  Perform the actual deployment.
     *  It's up to the subclasses to implement the actual behavior.
     *  @exception org.apache.tools.ant.BuildException if the attributes are invalid or incomplete.
     */
    public abstract void deploy() throws BuildException;

    /**
     *  Sets the parent task.
     *  @param task a ServerDeploy object representing the parent task.
     *  @ant.attribute ignore="true" 
     */
    public void setTask(ServerDeploy task) {
        this.task = task;
    }

    /**
     *  Returns the task field, a ServerDeploy object.
     *  @return An ServerDeploy representing the parent task.
     */
    protected ServerDeploy getTask() {
        return task;
    }

    /**
     *  gets the classpath field.
     *  @return A Path representing the "classpath" attribute.
     */
    public Path getClasspath() {
        return classpath;
    }

    /**
     *  The classpath to be passed to the JVM running the tool; 
     *  optional depending upon the tool. 
     *  The classpath may also be supplied as a nested element.
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
     *  The user with privileges to deploy applications to the server; optional.
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
     *  The password of the user; optional. 
     *  @param password A String representing the "password" attribute.
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     *  Returns the server field.
     *  @return A String representing the "server" attribute.
     */
    public String getServer() {
        return server;
    }

    /**
     *  The address or URL for the server where the component will be deployed.
     *  @param server A String representing the "server" attribute.
     */
    public void setServer(String server) {
        this.server = server;
    }
}
