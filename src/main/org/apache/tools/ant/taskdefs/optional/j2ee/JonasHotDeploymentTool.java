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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.types.Path;

/**
 *  An Ant wrapper task for the weblogic.deploy tool. This is used
 *  to hot-deploy J2EE applications to a running WebLogic server.
 *  This is <b>not</b> the same as creating the application
 *  archive. This task assumes the archive (EAR, JAR, or WAR) file
 *  has been assembled and is supplied as the "source" attribute.
 *  <p>
 *
 *  In the end, this task assembles the commandline parameters and
 *  runs the weblogic.deploy tool in a separate JVM.
 *
 *@see org.apache.tools.ant.taskdefs.optional.j2ee.HotDeploymentTool
 *@see org.apache.tools.ant.taskdefs.optional.j2ee.AbstractHotDeploymentTool
 *@see org.apache.tools.ant.taskdefs.optional.j2ee.ServerDeploy
 */
public class JonasHotDeploymentTool extends GenericHotDeploymentTool implements HotDeploymentTool {

    /**
     *  Description of the Field
     */
    protected static final String DEFAULT_ORB = "RMI";

    /**
     *  The classname of the tool to run *
     */
    private static final String JONAS_DEPLOY_CLASS_NAME = "org.objectweb.jonas.adm.JonasAdmin";

    /**
     *  All the valid actions that weblogic.deploy permits *
     */
    private static final String[] VALID_ACTIONS = {ACTION_DELETE, ACTION_DEPLOY, ACTION_LIST,
            ACTION_UNDEPLOY, ACTION_UPDATE};

    /**
     *  Description of the Field
     */
    private File jonasroot;

    /**
     *  Description of the Field
     */
    private String orb = null;

    /**
     *  Description of the Field
     */
    private String davidHost;

    /**
     *  Description of the Field
     */
    private int davidPort;

    /**
     *  Set the host for the David ORB; required if
     *  ORB==david.
     *
     *@param  inValue  The new davidhost value
     */
    public void setDavidhost(final String inValue) {
        davidHost = inValue;
    }

    /**
     *  Set the port for the David ORB; required if
     *  ORB==david.
     *
     *@param  inValue  The new davidport value
     */
    public void setDavidport(final int inValue) {
        davidPort = inValue;
    }

    /**
     *  set the jonas root directory (-Dinstall.root=). This
     *  element is required.
     *
     *@param  inValue  The new jonasroot value
     */
    public void setJonasroot(final File inValue) {
        jonasroot = inValue;
    }

    /**
     *
     * Choose your ORB : RMI, JEREMIE, DAVID, ...; optional.
     * If omitted, it defaults
     * to the one present in classpath. The corresponding JOnAS JAR is
     * automatically added to the classpath. If your orb is DAVID (RMI/IIOP) you must
     * specify davidhost and davidport properties.
     *
     *@param  inValue  RMI, JEREMIE, DAVID,...
     */
    public void setOrb(final String inValue) {
        orb = inValue;
    }

    /**
     *  gets the classpath field.
     *
     *@return    A Path representing the "classpath" attribute.
     */
    @Override
    public Path getClasspath() {
        Path aClassPath = super.getClasspath();

        if (aClassPath == null) {
            aClassPath = new Path(getTask().getProject());
        }
        if (orb != null) {
            String aOrbJar = new File(jonasroot, "lib/" + orb + "_jonas.jar").toString();
            String aConfigDir = new File(jonasroot, "config/").toString();
            Path aJOnASOrbPath = new Path(aClassPath.getProject(),
                    aOrbJar + File.pathSeparator + aConfigDir);
            aClassPath.append(aJOnASOrbPath);
        }
        return aClassPath;
    }

    /**
     * Validates the passed in attributes.
     *
     * <p>The rules are:</p>
     * <ol>
     *    <li> If action is "deploy" or "update" the "application"
     *    and "source" attributes must be supplied.</li>
     *    <li> If action is "delete" or "undeploy" the
     *    "application" attribute must be supplied.</li>
     * </ol>
     *
     * @exception BuildException if something goes wrong
     */
    @Override
    public void validateAttributes() throws BuildException {
        // super.validateAttributes(); // don't want to call this method

        Java java = getJava();

        String action = getTask().getAction();
        if (action == null) {
            throw new BuildException("The \"action\" attribute must be set");
        }

        if (!isActionValid()) {
            throw new BuildException("Invalid action \"%s\" passed", action);
        }

        if (getClassName() == null) {
            setClassName(JONAS_DEPLOY_CLASS_NAME);
        }

        if (jonasroot == null || jonasroot.isDirectory()) {
            java.createJvmarg().setValue("-Dinstall.root=" + jonasroot);
            java.createJvmarg().setValue("-Djava.security.policy=" + jonasroot
                + "/config/java.policy");

            if ("DAVID".equals(orb)) {
                java.createJvmarg().setValue("-Dorg.omg.CORBA.ORBClass"
                    + "=org.objectweb.david.libs.binding.orbs.iiop.IIOPORB");
                java.createJvmarg().setValue("-Dorg.omg.CORBA.ORBSingletonClass="
                    + "org.objectweb.david.libs.binding.orbs.ORBSingletonClass");
                java.createJvmarg().setValue("-Djavax.rmi.CORBA.StubClass="
                    + "org.objectweb.david.libs.stub_factories.rmi.StubDelegate");
                java.createJvmarg().setValue("-Djavax.rmi.CORBA.PortableRemoteObjectClass="
                    + "org.objectweb.david.libs.binding.rmi.ORBPortableRemoteObjectDelegate");
                java.createJvmarg().setValue("-Djavax.rmi.CORBA.UtilClass="
                    + "org.objectweb.david.libs.helpers.RMIUtilDelegate");
                java.createJvmarg().setValue("-Ddavid.CosNaming.default_method=0");
                java.createJvmarg().setValue("-Ddavid.rmi.ValueHandlerClass="
                    + "com.sun.corba.se.internal.io.ValueHandlerImpl");
                if (davidHost != null) {
                    java.createJvmarg().setValue("-Ddavid.CosNaming.default_host="
                        + davidHost);
                }
                if (davidPort != 0) {
                    java.createJvmarg().setValue("-Ddavid.CosNaming.default_port="
                        + davidPort);
                }
            }
        }

        if (getServer() != null) {
            java.createArg().setLine("-n " + getServer());
        }

        if (ACTION_DEPLOY.equals(action)
            || ACTION_UPDATE.equals(action)
            || "redeploy".equals(action)) {
            java.createArg().setLine("-a " + getTask().getSource());
        } else if (action.equals(ACTION_DELETE) || action.equals(ACTION_UNDEPLOY)) {
            java.createArg().setLine("-r " + getTask().getSource());
        } else if (action.equals(ACTION_LIST)) {
            java.createArg().setValue("-l");
        }
    }

    /**
     *  Determines if the action supplied is valid. <p>
     *
     *  Valid actions are contained in the static array
     *  VALID_ACTIONS
     *
     *@return    true if the action attribute is valid, false if
     *      not.
     */
    @Override
    protected boolean isActionValid() {
        String action = getTask().getAction();

        for (String validAction : VALID_ACTIONS) {
            if (action.equals(validAction)) {
                return true;
            }
        }
        // TODO what about redeploy, mentioned in #validateAttribute
        return false;
    }
}

