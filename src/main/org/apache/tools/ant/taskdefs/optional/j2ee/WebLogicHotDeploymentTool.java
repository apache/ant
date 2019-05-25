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
import org.apache.tools.ant.taskdefs.Java;

/**
 *  An Ant wrapper task for the weblogic.deploy tool.  This is used to
 *  hot-deploy J2EE applications to a running WebLogic server.
 *  This is <b>not</b> the same as creating the application archive.
 *  This task assumes the archive (EAR, JAR, or WAR) file has been
 *  assembled and is supplied as the "source" attribute.
 *  <p>In the end, this task assembles the commandline parameters
 *  and runs the weblogic.deploy tool in a separate JVM.</p>
 *
 *  @see org.apache.tools.ant.taskdefs.optional.j2ee.HotDeploymentTool
 *  @see org.apache.tools.ant.taskdefs.optional.j2ee.AbstractHotDeploymentTool
 *  @see org.apache.tools.ant.taskdefs.optional.j2ee.ServerDeploy
 */
public class WebLogicHotDeploymentTool extends AbstractHotDeploymentTool
                                       implements HotDeploymentTool {
    private static final int STRING_BUFFER_SIZE = 1024;
    /** The classname of the tool to run **/
    private static final String WEBLOGIC_DEPLOY_CLASS_NAME = "weblogic.deploy";

    /** All the valid actions that weblogic.deploy permits **/
    private static final String[] VALID_ACTIONS
        = {ACTION_DELETE, ACTION_DEPLOY, ACTION_LIST, ACTION_UNDEPLOY, ACTION_UPDATE};

    /** Represents the "-debug" flag from weblogic.deploy **/
    private boolean debug;

    /** The application name that is being deployed **/
    private String application;

    /** The component name:target(s) for the "-component" argument of weblogic.deploy **/
    private String component;

    /**
     *  Perform the actual deployment.
     *  For this implementation, a JVM is spawned and the weblogic.deploy
     *  tools is executed.
     *  @exception BuildException if the attributes are invalid or incomplete.
     */
    @Override
    public void deploy() {
        Java java = new Java(getTask());
        java.setFork(true);
        java.setFailonerror(true);
        java.setClasspath(getClasspath());

        java.setClassname(WEBLOGIC_DEPLOY_CLASS_NAME);
        java.createArg().setLine(getArguments());
        java.execute();
    }

    /**
     *  Validates the passed in attributes.
     *  <p>The rules are:</p>
     *  <ol><li>If action is "deploy" or "update" the "application" and "source"
     *  attributes must be supplied.</li>
     *  <li>If action is "delete" or "undeploy" the "application" attribute must
     *  be supplied.</li></ol>
     *
     *  @exception BuildException if the attributes are invalid or incomplete
     */
    @Override
    public void validateAttributes() throws BuildException {
        super.validateAttributes();

        String action = getTask().getAction();

        // check that the password has been set
        if (getPassword() == null) {
            throw new BuildException("The password attribute must be set.");
        }

        // check for missing application on deploy & update
        if ((action.equals(ACTION_DEPLOY) || action.equals(ACTION_UPDATE))
            && application == null) {
            throw new BuildException(
                "The application attribute must be set if action = %s", action);
        }

        // check for missing source on deploy & update
        if ((action.equals(ACTION_DEPLOY) || action.equals(ACTION_UPDATE))
            && getTask().getSource() == null) {
            throw new BuildException(
                "The source attribute must be set if action = %s", action);
        }

        // check for missing application on delete & undeploy
        if ((action.equals(ACTION_DELETE) || action.equals(ACTION_UNDEPLOY))
            && application == null) {
            throw new BuildException(
                "The application attribute must be set if action = %s", action);
        }
    }

    /**
     * Builds the arguments to pass to weblogic.deploy according to the
     * supplied action.
     * @return A String containing the arguments for the weblogic.deploy tool.
     * @throws BuildException if there is an error.
     */
    public String getArguments() throws BuildException {
        String action = getTask().getAction();

        if (action.equals(ACTION_DEPLOY) || action.equals(ACTION_UPDATE)) {
            return buildDeployArgs();
        }
        if (action.equals(ACTION_DELETE) || action.equals(ACTION_UNDEPLOY)) {
            return buildUndeployArgs();
        }
        if (action.equals(ACTION_LIST)) {
            return buildListArgs();
        }
        return null;
    }

    /**
     * Determines if the action supplied is valid.
     * <p>Valid actions are contained in the static array VALID_ACTIONS</p>
     * @return true if the action attribute is valid, false if not.
     */
    @Override
    protected boolean isActionValid() {
        String action = getTask().getAction();

        for (String validAction : VALID_ACTIONS) {
            if (action.equals(validAction)) {
                return true;
            }
        }
        return false;
    }

    /**
     *  Builds the prefix arguments to pass to weblogic.deploy.
     *  These arguments are generic across all actions.
     *  @return A StringBuffer containing the prefix arguments.
     *  The action-specific build methods will append to this StringBuffer.
     */
    protected StringBuffer buildArgsPrefix() {
        ServerDeploy task = getTask();
        // constructs the "-url <url> -debug <action> <password>" portion
        // of the command line
        return new StringBuffer(STRING_BUFFER_SIZE)
                .append((getServer() != null)
                    ? "-url " + getServer()
                    : "")
                .append(" ")
                .append(debug ? "-debug " : "")
                .append((getUserName() != null)
                    ? "-username " + getUserName()
                    : "")
                .append(" ")
                .append(task.getAction()).append(" ")
                .append(getPassword()).append(" ");
    }

    /**
     *  Builds the arguments to pass to weblogic.deploy for deployment actions
     *  ("deploy" and "update").
     *  @return A String containing the full argument string for weblogic.deploy.
     */
    protected String buildDeployArgs() {
        String args = buildArgsPrefix()
                .append(application).append(" ")
                .append(getTask().getSource())
                .toString();

        if (component != null) {
            args = "-component " + component + " " + args;
        }

        return args;
    }

    /**
     *  Builds the arguments to pass to weblogic.deploy for undeployment actions
     *  ("undeploy" and "delete").
     *  @return A String containing the full argument string for weblogic.deploy.
     */
    protected String buildUndeployArgs() {
        return buildArgsPrefix()
                .append(application).append(" ")
                .toString();
    }

    /**
     *  Builds the arguments to pass to weblogic.deploy for the list action
     *  @return A String containing the full argument string for weblogic.deploy.
     */
    protected String buildListArgs() {
        return buildArgsPrefix()
                .toString();
    }

    /**
     *  If set to true, additional information will be
     *  printed during the deployment process; optional.
     *  @param debug A boolean representing weblogic.deploy "-debug" flag.
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    /**
     *  The name of the application being deployed; required.
     *  @param application A String representing the application portion of the
     *  weblogic.deploy command line.
     */
    public void setApplication(String application) {
        this.application = application;
    }

    /**
     * the component string for the deployment targets; optional.
     * It is in the form <code>&lt;component&gt;:&lt;target1&gt;,&lt;target2&gt;...</code>
     * Where component is the archive name (minus the .jar, .ear, .war
     * extension).  Targets are the servers where the components will be deployed

     *  @param component A String representing the value of the "-component"
     *  argument of the weblogic.deploy command line argument.
     */
    public void setComponent(String component) {
        this.component = component;
    }
}
