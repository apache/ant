/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs.condition;

import java.io.IOException;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectComponent;

/**
 * Condition to wait for a TCP/IP socket to have a listener. Its attribute(s)
 * are: server - the name of the server. port - the port number of the socket.
 *
 * @author <a href="mailto:denis@network365.com">Denis Hennessy</a>
 */
public class Socket extends ProjectComponent implements Condition
{
    String server = null;
    int port = 0;

    public void setPort( int port )
    {
        this.port = port;
    }

    public void setServer( String server )
    {
        this.server = server;
    }

    public boolean eval()
        throws TaskException
    {
        if( server == null )
        {
            throw new TaskException( "No server specified in Socket task" );
        }
        if( port == 0 )
        {
            throw new TaskException( "No port specified in Socket task" );
        }
        log( "Checking for listener at " + server + ":" + port, Project.MSG_VERBOSE );
        try
        {
            java.net.Socket socket = new java.net.Socket( server, port );
        }
        catch( IOException e )
        {
            return false;
        }
        return true;
    }

}
