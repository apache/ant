/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.condition;

import java.io.IOException;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.conditions.Condition;

/**
 * Condition to wait for a TCP/IP socket to have a listener. Its attribute(s)
 * are: server - the name of the server. port - the port number of the socket.
 *
 * @author <a href="mailto:denis@network365.com">Denis Hennessy</a>
 *
 * @ant:type type="condition" name="socket"
 */
public class Socket
    implements Condition
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

    /**
     * Evaluates this condition.
     */
    public boolean evaluate( TaskContext context )
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
        context.debug( "Checking for listener at " + server + ":" + port );
        try
        {
            java.net.Socket socket = new java.net.Socket( server, port );
            socket.close();
        }
        catch( IOException e )
        {
            return false;
        }
        return true;
    }

}
