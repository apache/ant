/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2002 The Apache Software Foundation.  All rights
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
package org.apache.tools.ant.taskdefs.optional.ejb;


import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.types.Path;

/**
 * Shuts down a WebLogic server.
 * To shut down an instance you must supply both a username and
 * a password.
 *
 * @author Conor MacNeill, Cortex ebusiness Pty Limited
 */
public class WLStop extends Task {
    /**
     * The classpath to be used. It must contains the weblogic.Admin class.
     */
    private Path classpath;

    /**
     * The weblogic username to use to request the shutdown.
     */
    private String username;

    /**
     * The password to use to shutdown the weblogic server.
     */
    private String password;

    /**
     * The URL which the weblogic server is listening on.
     */
    private String serverURL;

    /**
     * The delay (in seconds) to wait before shutting down.
     */
    private int delay = 0;

    /**
     * The location of the BEA Home under which this server is run.
     * WL6 only
     */
    private File beaHome = null;

    /**
     * Do the work.
     *
     * The work is actually done by creating a separate JVM to run the weblogic admin task
     * This approach allows the classpath of the helper task to be set.
     *
     * @exception BuildException if someting goes wrong with the build
     */
    public void execute() throws BuildException {
        if (username == null || password == null) {
            throw new BuildException("weblogic username and password must both be set");
        }

        if (serverURL == null) {
            throw new BuildException("The url of the weblogic server must be provided.");
        }

        Java weblogicAdmin = (Java) project.createTask("java");
        weblogicAdmin.setFork(true);
        weblogicAdmin.setClassname("weblogic.Admin");
        String args;

        if (beaHome == null) {
            args = serverURL + " SHUTDOWN " + username + " " + password + " " + delay;
        } else {
            args = " -url " + serverURL +
                    " -username " + username +
                    " -password " + password +
                    " SHUTDOWN " + " " + delay;
        }

        weblogicAdmin.createArg().setLine(args);
        weblogicAdmin.setClasspath(classpath);
        weblogicAdmin.execute();
    }

    /**
     * The classpath to be used with the Java Virtual Machine that runs the Weblogic
     * Shutdown command;
     *
     * @param path the classpath to use when executing the weblogic admin task.
     */
    public void setClasspath(Path path) {
        this.classpath = path;
    }

    /**
     * The classpath to be used with the Java Virtual Machine that runs the Weblogic
     * Shutdown command;
     */
    public Path createClasspath() {
        if (classpath == null) {
            classpath = new Path(project);
        }
        return classpath.createPath();
    }

    /**
     * The username of the account which will be used to shutdown the server;
     * required.
     *
     * @param s the username.
     */
    public void setUser(String s) {
        this.username = s;
    }

    /**
     * The password for the account specified in the
     * user parameter; required
     *
     * @param s the password.
     */
    public void setPassword(String s) {
        this.password = s;
    }

    /**
     * Set the URL to which the weblogic server is listening
     * for T3 connections; required.
     *
     * @param s the url.
     */
    public void setUrl(String s) {
        this.serverURL = s;
    }


    /**
     * Set the delay (in seconds) before shutting down the server;
     * optional.
     *
     * @param s the selay.
     */
    public void setDelay(String s) {
        delay = Integer.parseInt(s);
    }

    /**
     * The location of the BEA Home; implicitly
     * selects Weblogic 6.0 shutdown; optional.
     *
     * @param beaHome the BEA Home directory.
     *
     */
    public void setBEAHome(File beaHome) {
        this.beaHome = beaHome;
    }

}
