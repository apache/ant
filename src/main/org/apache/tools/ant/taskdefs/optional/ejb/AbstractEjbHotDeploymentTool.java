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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 *  Abstract class to support vendor-specific hot deployment tools.
 *  This class will validate boilerplate attributes.
 *
 *  Subclassing this class for a vendor specific tool involves the
 *  following.
 *  <ol>
 *  <li>Implement the <code>getClassName()</code> method to supply the
 *  super-task class name of the vendor specific deployment tool to run.
 *  <li>Implement the <code>getArguments()</code> method to supply the
 *  super-task the command line to pass for the vendor specific deployment
 *  tool.
 *  <li>Implement the <code>isActionValid()<code> method to insure the
 *  action supplied as the "action" attribute of EjbDeploy is valid.
 *  <li>Implement the <code>validateAttributes()</code> method to insure
 *  all required attributes are supplied, and are in the correct format.
 *  <li>Add a <code>create&lt;TOOL&gt;</code> method to the EjbDeploy
 *  class.  This method will be called when Ant encounters a
 *  <code>create&lt;TOOL&gt;</code> task nested in the
 *  <code>ejbdeploy</code> task.
 *
 *  @author Christopher A. Longo - cal@cloud9.net
 *
 *  @see EjbHotDeploymentTool
 *  @see EjbDeploy
 */
public abstract class AbstractEjbHotDeploymentTool
        extends Task
        implements EjbHotDeploymentTool
{
    /** The super-task **/
    private EjbDeploy deploy;

    /**
     *  Constructor.
     *  @param deploy The super-task which wraps this one.
     */
    public AbstractEjbHotDeploymentTool(EjbDeploy deploy) {
        this.deploy = deploy;
    }

    /**
     *  Returns the class name of the weblogic.deploy tool to the super-task.
     *  <p>This is called by the super-task, EjbDeploy.
     *  <p>Subclasses should return the fully qualified class name of the
     *  vendor tool to run.  IE: "com.foobar.tools.DeployTool"
     *  @return A String representing the classname of the deployment tool to run
     */
    public abstract String getClassName();

    /**
     *  Returns a String containing the runtime commandline arguments
     *  of the deployment tool.
     *  <p>Subclasses should return the appropriate string from that
     *  vendor's tool.  IE: "-url=http://myserver:31337 -user=foo -passsword=bar"
     *  @return a String containing the runtime commandline arguments
     *  of the deployment tool.
     */
    public abstract String getArguments();

    /**
     *  Validates the passed in attributes.
     *  Subclasses should chain to this super-method to insure
     *  validation of boilerplate attributes.
     *  <p>Only the "action" and "password" attributes are required in the
     *  base class.  Subclasses should check attributes accordingly.
     *  @exception BuildException if the attributes are invalid or incomplete
     */
    public void validateAttributes() throws BuildException {
        String action = deploy.getAction();

        if(action == null)
            throw new BuildException("The \"action\" attribute must be set");

        if(!isActionValid())
            throw new BuildException("Invalid action \"" + action + "\" passed");

        if(deploy.getPassword() == null)
            throw new BuildException("The WebLogic system password must be set");
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
     *  Returns the super-task.
     *  @return An EjbDeploy object representing the super-task.
     */
    protected EjbDeploy getDeploy() {
        return deploy;
    }
}
