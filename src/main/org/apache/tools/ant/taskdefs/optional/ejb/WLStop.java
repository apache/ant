/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
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

        Java weblogicAdmin = new Java(this);
        weblogicAdmin.setFork(true);
        weblogicAdmin.setClassname("weblogic.Admin");
        String args;

        if (beaHome == null) {
            args = serverURL + " SHUTDOWN " + username + " " + password + " " + delay;
        } else {
            args = " -url " + serverURL
                    + " -username " + username
                    + " -password " + password
                    + " SHUTDOWN " + " " + delay;
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
     * @return the path to be configured.
     */
    public Path createClasspath() {
        if (classpath == null) {
            classpath = new Path(getProject());
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
