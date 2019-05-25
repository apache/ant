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
import org.apache.tools.ant.types.Path;

/**
 *  Abstract class to support vendor-specific hot deployment tools.
 *  This class will validate boilerplate attributes.
 *
 *  Subclassing this class for a vendor specific tool involves the
 *  following.
 *  <ol><li>Implement the <code>isActionValid()</code> method to insure the
 *  action supplied as the "action" attribute of ServerDeploy is valid.</li>
 *  <li>Implement the <code>validateAttributes()</code> method to insure
 *  all required attributes are supplied, and are in the correct format.</li>
 *  <li>Add a <code>add&lt;TOOL&gt;</code> method to the ServerDeploy
 *  class.  This method will be called when Ant encounters a
 *  <code>add&lt;TOOL&gt;</code> task nested in the
 *  <code>serverdeploy</code> task.</li>
 *  <li>Define the <code>deploy</code> method.  This method should perform
 *  whatever task it takes to hot-deploy the component.  IE: spawn a JVM and
 *  run class, exec a native executable, run Java code...</li></ol>
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
        if (classpath == null) {
            classpath = new Path(task.getProject());
        }
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
     *  @throws BuildException if the attributes are invalid or incomplete.
     */
    @Override
    public void validateAttributes() throws BuildException {
        if (task.getAction() == null) {
            throw new BuildException("The \"action\" attribute must be set");
        }

        if (!isActionValid()) {
            throw new BuildException("Invalid action \"%s\" passed", task.getAction());
        }

        if (classpath == null) {
            throw new BuildException("The classpath attribute must be set");
        }
    }

    /**
     *  Sets the parent task.
     *  @param task a ServerDeploy object representing the parent task.
     *  @ant.attribute ignore="true"
     */
    @Override
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
