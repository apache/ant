/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.optional.ejb;

import java.io.File;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.types.Path;

/**
 * Shutdown a Weblogic server.
 *
 * @author <a href="mailto:conor@cortexebusiness.com.au">Conor MacNeill</a> ,
 *      Cortex ebusiness Pty Limited
 */
public class WLStop extends Task
{

    /**
     * The delay (in seconds) to wait before shutting down.
     */
    private int delay = 0;

    /**
     * The location of the BEA Home under which this server is run. WL6 only
     */
    private File beaHome = null;
    /**
     * The classpath to be used. It must contains the weblogic.Admin class.
     */
    private Path classpath;

    /**
     * The password to use to shutdown the weblogic server.
     */
    private String password;

    /**
     * The URL which the weblogic server is listening on.
     */
    private String serverURL;

    /**
     * The weblogic username to use to request the shutdown.
     */
    private String username;

    /**
     * The location of the BEA Home.
     *
     * @param beaHome the BEA Home directory.
     */
    public void setBEAHome( File beaHome )
    {
        this.beaHome = beaHome;
    }

    /**
     * Set the classpath to be used for this compilation.
     *
     * @param path The new Classpath value
     */
    public void setClasspath( Path path )
    {
        this.classpath = path;
    }

    /**
     * Set the delay (in seconds) before shutting down the server.
     *
     * @param s the selay.
     */
    public void setDelay( String s )
    {
        delay = Integer.parseInt( s );
    }

    /**
     * Set the password to use to request shutdown of the server.
     *
     * @param s the password.
     */
    public void setPassword( String s )
    {
        this.password = s;
    }

    /**
     * Set the URL to which the weblogic server is listening.
     *
     * @param s the url.
     */
    public void setUrl( String s )
    {
        this.serverURL = s;
    }

    /**
     * Set the username to use to request shutdown of the server.
     *
     * @param s the username.
     */
    public void setUser( String s )
    {
        this.username = s;
    }

    /**
     * Add the classpath for the user classes
     *
     * @return Description of the Returned Value
     */
    public Path createClasspath()
    {
        if( classpath == null )
        {
            classpath = new Path();
        }
        return classpath.createPath();
    }

    /**
     * Do the work. The work is actually done by creating a separate JVM to run
     * the weblogic admin task This approach allows the classpath of the helper
     * task to be set.
     *
     * @exception TaskException if someting goes wrong with the build
     */
    public void execute()
        throws TaskException
    {
        if( username == null || password == null )
        {
            throw new TaskException( "weblogic username and password must both be set" );
        }

        if( serverURL == null )
        {
            throw new TaskException( "The url of the weblogic server must be provided." );
        }

        Java weblogicAdmin = (Java)getProject().createTask( "java" );
        weblogicAdmin.setFork( true );
        weblogicAdmin.setClassname( "weblogic.Admin" );
        String args;

        if( beaHome == null )
        {
            args = serverURL + " SHUTDOWN " + username + " " + password + " " + delay;
        }
        else
        {
            args = " -url " + serverURL +
                " -username " + username +
                " -password " + password +
                " SHUTDOWN " + " " + delay;
        }

        weblogicAdmin.setArgs( args );
        weblogicAdmin.setClasspath( classpath );
        weblogicAdmin.execute();
    }

}
