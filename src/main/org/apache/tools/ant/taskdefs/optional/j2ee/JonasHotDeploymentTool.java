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
 *  In the end, this task assembles the commadline parameters and
 *  runs the weblogic.deploy tool in a seperate JVM.
 *
 *@author Cyrille Morvan
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
    private static final String[] VALID_ACTIONS =
            {ACTION_DELETE, ACTION_DEPLOY, ACTION_LIST, ACTION_UNDEPLOY, ACTION_UPDATE};

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
     *  Validates the passed in attributes. <p>
     *
     *  The rules are:
     *  <ol>
     *    <li> If action is "deploy" or "update" the "application"
     *    and "source" attributes must be supplied.
     *    <li> If action is "delete" or "undeploy" the
     *    "application" attribute must be supplied.
     *
     *@exception  BuildException                       Description
     *      of Exception
     */
    public void validateAttributes() throws BuildException {
        // super.validateAttributes(); // don't want to call this method

        Java java = getJava();

        String action = getTask().getAction();
        if (action == null) {
            throw new BuildException("The \"action\" attribute must be set");
        }

        if (!isActionValid()) {
            throw new BuildException("Invalid action \"" + action + "\" passed");
        }

        if (getClassName() == null) {
            setClassName(JONAS_DEPLOY_CLASS_NAME);
        }

        if (jonasroot == null || jonasroot.isDirectory()) {
            java.createJvmarg().setValue("-Dinstall.root=" + jonasroot);
            java.createJvmarg().setValue("-Djava.security.policy=" + jonasroot + "/config/java.policy");

            if ("DAVID".equals(orb)) {
                java.createJvmarg().setValue("-Dorg.omg.CORBA.ORBClass=org.objectweb.david.libs.binding.orbs.iiop.IIOPORB");
                java.createJvmarg().setValue("-Dorg.omg.CORBA.ORBSingletonClass=org.objectweb.david.libs.binding.orbs.ORBSingletonClass");
                java.createJvmarg().setValue("-Djavax.rmi.CORBA.StubClass=org.objectweb.david.libs.stub_factories.rmi.StubDelegate");
                java.createJvmarg().setValue("-Djavax.rmi.CORBA.PortableRemoteObjectClass=org.objectweb.david.libs.binding.rmi.ORBPortableRemoteObjectDelegate");
                java.createJvmarg().setValue("-Djavax.rmi.CORBA.UtilClass=org.objectweb.david.libs.helpers.RMIUtilDelegate");
                java.createJvmarg().setValue("-Ddavid.CosNaming.default_method=0");
                java.createJvmarg().setValue("-Ddavid.rmi.ValueHandlerClass=com.sun.corba.se.internal.io.ValueHandlerImpl");
                if (davidHost != null) {
                    java.createJvmarg().setValue("-Ddavid.CosNaming.default_host=" + davidHost);
                }
                if (davidPort != 0) {
                    java.createJvmarg().setValue("-Ddavid.CosNaming.default_port=" + davidPort);
                }
            }
        }

        String anAction = getTask().getAction();

        if (getServer() != null) {
            java.createArg().setLine("-n " + getServer());
        }

        if (action.equals(ACTION_DEPLOY) ||
                action.equals(ACTION_UPDATE) ||
                action.equals("redeploy")) {
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
    protected boolean isActionValid() {
        boolean valid = false;

        String action = getTask().getAction();

        for (int i = 0; i < VALID_ACTIONS.length; i++) {
            if (action.equals(VALID_ACTIONS[i])) {
                valid = true;
                break;
            }
        }

        return valid;
    }
}

