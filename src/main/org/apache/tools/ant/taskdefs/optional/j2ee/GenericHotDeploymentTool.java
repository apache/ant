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
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.taskdefs.Java;

/**
 *  A generic tool for J2EE server hot deployment.
 *  <p>The simple implementation spawns a JVM with the supplied
 *  class name, jvm args, and arguments.
 *
 *  @author Christopher A. Longo - cal@cloud9.net
 *
 *  @see org.apache.tools.ant.taskdefs.optional.j2ee.HotDeploymentTool
 *  @see org.apache.tools.ant.taskdefs.optional.j2ee.AbstractHotDeploymentTool
 *  @see org.apache.tools.ant.taskdefs.optional.j2ee.ServerDeploy
 */
public class GenericHotDeploymentTool extends AbstractHotDeploymentTool {
    /** A Java task used to run the deployment tool **/
    private Java java;

    /** The fully qualified class name of the deployment tool **/
    private String className;

    /** List of valid actions **/
    private static final String[] VALID_ACTIONS = { ACTION_DEPLOY };

    /**
     *  Add a nested argument element to hand to the deployment tool; optional.
     *  @return A Commandline.Argument object representing the
     *  command line argument being passed when the deployment
     *  tool is run.  IE: "-user=mark", "-password=venture"...
     */
    public Commandline.Argument createArg() {
        return java.createArg();
    }

    /**
     *  Add a nested argment element to hand to the JVM running the
     *  deployment tool.
     *  Creates a nested arg element.
     *  @return A Commandline.Argument object representing the
     *  JVM command line argument being passed when the deployment
     *  tool is run.  IE: "-ms64m", "-mx128m"...
     */
    public Commandline.Argument createJvmarg() {
        return java.createJvmarg();
    }

    /**
     *  Determines if the "action" attribute defines a valid action.
     *  <p>Subclasses should determine if the action passed in is
     *  supported by the vendor's deployment tool.
     *  For this generic implementation, the only valid action is "deploy"
     *  @return true if the "action" attribute is valid, false if not.
     */
    protected boolean isActionValid() {
        return (getTask().getAction().equals(VALID_ACTIONS[0]));
    }

    /**
     *  Sets the parent task.
     *  @param task An ServerDeploy object representing the parent task.
     *  @ant.attribute ignored="true"
     */
    public void setTask(ServerDeploy task) {
        super.setTask(task);
        java = (Java) task.getProject().createTask("java");
    }

    /**
     *  Perform the actual deployment.
     *  For this generic implementation, a JVM is spawned using the
     *  supplied classpath, classname, JVM args, and command line arguments.
     *  @exception org.apache.tools.ant.BuildException if the attributes are invalid or incomplete.
     */
    public void deploy() throws BuildException {
        java.setClassname(className);
        java.setClasspath(getClasspath());
        java.setFork(true);
        java.setFailonerror(true);
        java.execute();
    }

    /**
     *  Validates the passed in attributes.
     *  Ensures the className and arguments attribute have been set.
     *  @exception org.apache.tools.ant.BuildException if the attributes are invalid or incomplete.
     */
    public void validateAttributes() throws BuildException {
        super.validateAttributes();

        if (className == null)
            throw new BuildException("The classname attribute must be set");
    }

    /**
     *  The name of the class to execute to perfom
     *  deployment; required. 
     *  Example: "com.foobar.tools.deploy.DeployTool"
     *  @param className The fully qualified class name of the class
     *  to perform deployment.
     */
    public void setClassName(String className) {
        this.className = className;
    }
    
    /**
     *
     */
    public Java getJava() {
        return java;
    }
    
    public String getClassName() {
        return className;
    }
}
