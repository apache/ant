/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2003 The Apache Software Foundation.  All rights
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
package org.apache.tools.ant.taskdefs.optional.starteam;

import com.starbase.starteam.BuildNumber;
import com.starbase.starteam.Server;
import com.starbase.starteam.StarTeamFinder;
import com.starbase.starteam.TypeNames;
import com.starbase.starteam.User;
import com.starbase.starteam.View;
import java.util.StringTokenizer;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

/**
 * Common super class for all StarTeam tasks.
 * At this level of the hierarchy we are concerned only with obtaining a
 * connection to the StarTeam server.  The subclass <code>TreeBasedTask</code>,
 * also abstract defines the tree-walking behavior common to many subtasks.
 *
 * @see TreeBasedTask
 * @author <a href="mailto:jcyip@thoughtworks.com">Jason Yip</a>
 * @version 1.1
 * @author <a href="mailto:stevec@ignitesports.com">Steve Cohen</a>
 */

public abstract class StarTeamTask extends Task {

    // ATTRIBUTES

    /**
     * The username of the connection
     */
    private String userName;

    /**
     * The username of the connection
     */
    private String password;

    /**
     * name of Starteam server to connect to
     */
    private String servername;

    /**
     * port of Starteam server to connect to
     */
    private String serverport;

    /**
     * name of Starteam project to connect to
     */
    private String projectname;

    /**
     * name of Starteam view to connect to
     */
    private String viewname;

    /**
     *The starteam server through which all activities will be done.
     */
    private Server server = null;

    private void logStarteamVersion() {
        log("StarTeam version: "+ 
            BuildNumber.getDisplayString(), Project.MSG_DEBUG);
    }


    /////////////////////////////////////////////////////////
    // GET/SET methods.
    // Setters, of course are where ant user passes in values.
    /////////////////////////////////////////////////////////

    /**
     * Set the name of StarTeamServer;
     * required if <tt>URL</tt> is not set.
     * @param servername a <code>String</code> value
     * @see #setURL(String)
     */
    public final void setServername(String servername) {
        this.servername = servername;
    }

    /**
     * returns the name of the StarTeamServer
     *
     * @return the name of the StarTeam server
     * @see #getURL()
     */
    public final String getServername() {
        return this.servername;
    }

    /**
     * set the port number of the StarTeam connection;
     * required if <tt>URL</tt> is not set.
     * @param serverport port number to be set
     * @see #setURL(String)
     */
    public final void setServerport(String serverport) {
        this.serverport = serverport;
    }

    /**
     * returns the port number of the StarTeam connection
     *
     * @return the port number of the StarTeam connection
     * @see #getURL()
     */
    public final String getServerport() {
        return this.serverport;
    }

    /**
     * set the name of the StarTeam project to be acted on;
     * required if <tt>URL</tt> is not set.
     *
     * @param projectname the name of the StarTeam project to be acted on
     * @see #setURL(String)
     */
    public final void setProjectname(String projectname) {
        this.projectname = projectname;
    }

    /**
     * returns the name of the StarTeam project to be acted on
     *
     * @return the name of the StarTeam project to be acted on
     * @see #getURL()
     */
    public final String getProjectname() {
        return this.projectname;
    }

    /**
     * set the name of the StarTeam view to be acted on;
     * required if <tt>URL</tt> is not set.
     *
     * @param projectname the name of the StarTeam view to be acted on
     * @see #setURL(String)
     */
    public final void setViewname(String viewname) {
        this.viewname = viewname;
    }

    /**
     * returns the name of the StarTeam view to be acted on
     *
     * @return the name of the StarTeam view to be acted on
     * @see #getURL()
     */
    public final String getViewname() {
        return this.viewname;
    }


    /**
     * Set the server name, server port,
     * project name and project folder in one shot;
     * optional, but the server connection must be specified somehow. 
     *
     * @param url a <code>String</code> of the form
     *             "servername:portnum/project/view"
     * @see #setServername(String)
     * @see #setServerport(String)
     * @see #setProjectname(String)
     * @see #setViewname(String)
     */
    public final void setURL(String url) {
        StringTokenizer t = new StringTokenizer(url, "/");
        if (t.hasMoreTokens()) {
            String unpw = t.nextToken();
            int pos = unpw.indexOf(":");
            if (pos > 0) {
                this.servername = unpw.substring(0, pos);
                this.serverport = unpw.substring(pos + 1);
                if (t.hasMoreTokens()) {
                    this.projectname = t.nextToken();
                    if (t.hasMoreTokens()) {
                        this.viewname = t.nextToken();
                    }
                }
            }
        }
    }

    /**
     * convenience method returns whole URL at once
     * returns
     * as a single string
     */
    /**
     * a convenience method which returns the whole StarTeam
     * connection information as a single URL string of
     *
     * @return a <code>String</code> of the form
     *         "servername:portnum/project/view"
     * @see #getServername()
     * @see #getServerport()
     * @see #getProjectname()
     * @see #getViewname()
     */
    public final String getURL() {
        return
                this.servername + ":" +
                this.serverport + "/" +
                this.projectname + "/" +
                ((null == this.viewname) ? "" : this.viewname);
    }

    /**
     * returns an URL string useful for interacting with many StarTeamFinder
     * methods.
     * 
     * @return the URL string for this task.
     */
    protected final String getViewURL() {
        return getUserName() + ":" + getPassword() + "@" + getURL();
    }
    /**
     * set the name of the StarTeam user, needed for the connection
     *
     * @param userName name of the user to be logged in
     */
    public final void setUserName(String userName) {
        this.userName = userName;
    }

    /**
     * returns the name of the StarTeam user
     *
     * @return the name of the StarTeam user
     */
    public final String getUserName() {
        return this.userName;
    }

    /**
     * set the password to be used for login; required.
     *
     * @param password the password to be used for login
     */
    public final void setPassword(String password) {
        this.password = password;
    }

    /**
     * returns the password used for login
     *
     * @return the password used for login
     */
    public final String getPassword() {
        return this.password;
    }

    /**
     * returns a reference to the server which may be used for informational
     * purposes by subclasses.
     *
     * @return a reference to the server
     */
    protected final Server getServer() {
        return this.server;
    }

    /**
     * returns a list of TypeNames known to the server.
     *
     * @return a reference to the server's TypeNames
     */
    protected final TypeNames getTypeNames() {
        return this.server.getTypeNames();
    }
    /**
     * Derived classes must override <code>createSnapshotView</code>
     * defining the kind of configured view appropriate to its task.
     *
     * @param rawview the unconfigured <code>View</code>
     * @return the snapshot <code>View</code> appropriately configured.
     */
    protected abstract View createSnapshotView(View rawview) 
    throws BuildException;

    /**
     * All subclasses will call on this method to open the view needed for
     * processing.  This method also saves a reference to the
     * <code>Server</code> that may be accessed for information at various
     * points in the process.
     *
     * @return the <code>View</code> that will be used for processing.
     * @see #createSnapshotView(View)
     * @see #getServer()
     */
    protected View openView() throws BuildException {

        logStarteamVersion();
        View view = null;
        try {
            view = StarTeamFinder.openView(getViewURL());
        } catch (Exception e) {
            throw new BuildException(
                "Failed to connect to " + getURL(), e);
        }

        if (null == view) {
            throw new BuildException("Cannot find view" + getURL() +
                    " in repository()");
        }

        View snapshot = createSnapshotView(view);
        this.server = snapshot.getServer();
        return snapshot;
    }

    /**
     * Returns the name of the user with the supplied ID or a blank string
     * if user not found.
     *
     * @param userID a user's ID
     * @return the name of the user with ID userID
     */
    protected final String getUserName(int userID) {
        User u = this.server.getUser(userID);
        if (null == u) {
            return "";
        }
        return u.getName();
    }

}












