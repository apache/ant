/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.todo.taskdefs.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.api.TaskContext;

/**
 * Class to provide automated telnet protocol support for the Ant build tool
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @author <a href="mailto:ScottCarlson@email.com">ScottCarlson@email.com</a>
 * @version $Revision$
 */
public class TelnetTask
    extends AbstractTask
{
    /**
     * The userid to login with, if automated login is used
     */
    private String m_userid;

    /**
     * The password to login with, if automated login is used
     */
    private String m_password;

    /**
     * The server to connect to.
     */
    private String m_server;

    /**
     * The tcp port to connect to.
     */
    private int m_port = 23;

    /**
     * The list of read/write commands for this session
     */
    private ArrayList m_telnetTasks = new ArrayList();

    /**
     * If true, adds a CR to beginning of login script
     */
    private boolean m_addCarriageReturn;

    /**
     * Default time allowed for waiting for a valid response for all child
     * reads. A value of 0 means no limit.
     */
    private Integer m_defaultTimeout;

    /**
     * Set the tcp port to connect to attribute
     */
    public void setInitialCR( final boolean addCarriageReturn )
    {
        m_addCarriageReturn = addCarriageReturn;
    }

    /**
     * Set the password attribute
     */
    public void setPassword( final String password )
    {
        m_password = password;
    }

    /**
     * Set the tcp port to connect to attribute
     */
    public void setPort( final int port )
    {
        m_port = port;
    }

    /**
     * Set the server address attribute
     */
    public void setServer( final String server )
    {
        m_server = server;
    }

    /**
     * Change the default timeout to wait for valid responses
     */
    public void setTimeout( final Integer defaultTimeout )
    {
        m_defaultTimeout = defaultTimeout;
    }

    /**
     * Set the userid attribute
     */
    public void setUserid( final String userid )
    {
        m_userid = userid;
    }

    /**
     * A subTask &lt;read&gt; tag was found. Create the object, Save it in our
     * list, and return it.
     */
    public void addRead( final TelnetRead read )
    {
        m_telnetTasks.add( read );
    }

    /**
     * A subTask &lt;write&gt; tag was found. Create the object, Save it in our
     * list, and return it.
     */
    public void addWrite( final TelnetWrite write )
    {
        m_telnetTasks.add( write );
    }

    /**
     * Verify that all parameters are included. Connect and possibly login
     * Iterate through the list of Reads and writes
     */
    public void execute()
        throws TaskException
    {
        validate();

        /**
         * Create the telnet client object
         */
        final AntTelnetClient telnet = new AntTelnetClient( this );
        try
        {
            telnet.connect( m_server, m_port );
        }
        catch( final IOException ioe )
        {
            throw new TaskException( "Can't connect to " + m_server, ioe );
        }
        /**
         * Login if userid and password were specified
         */
        if( m_userid != null && m_password != null )
        {
            login( telnet );
        }

        processTasks( telnet );
    }

    /**
     * Process each sub command
     */
    private void processTasks( final AntTelnetClient telnet )
        throws TaskException
    {
        final Iterator tasks = m_telnetTasks.iterator();
        while( tasks != null && tasks.hasNext() )
        {
            final TelnetSubTask task = (TelnetSubTask)tasks.next();
            if( task instanceof TelnetRead && m_defaultTimeout != null )
            {
                ( (TelnetRead)task ).setDefaultTimeout( m_defaultTimeout );
            }
            task.execute( telnet );
        }
    }

    private void validate()
        throws TaskException
    {
        //A server name is required to continue
        if( m_server == null )
        {
            throw new TaskException( "No Server Specified" );
        }

        //A userid and password must appear together if they appear. They are
        //not required.
        if( m_userid == null && m_password != null )
        {
            throw new TaskException( "No Userid Specified" );
        }
        if( m_password == null && m_userid != null )
        {
            throw new TaskException( "No Password Specified" );
        }
    }

    /**
     * Process a 'typical' login. If it differs, use the read and write tasks
     * explicitely
     */
    private void login( final AntTelnetClient telnet )
        throws TaskException
    {
        if( m_addCarriageReturn )
        {
            telnet.sendString( "\n", true );
        }
        telnet.waitForString( "ogin:" );
        telnet.sendString( m_userid, true );
        telnet.waitForString( "assword:" );
        telnet.sendString( m_password, false );
    }

    protected void doSendString( final OutputStream output,
                                 final String string,
                                 final boolean echoString )
        throws TaskException
    {
        try
        {
            output.write( ( string + "\n" ).getBytes() );
            if( echoString )
            {
                getContext().info( string );
            }
            output.flush();
        }
        catch( final Exception e )
        {
            throw new TaskException( e.getMessage(), e );
        }
    }

    protected void doWaitForString( final InputStream input,
                                    final String string,
                                    final Integer timeout )
        throws TaskException
    {
        try
        {
            final StringBuffer sb = new StringBuffer();
            if( timeout == null || timeout.intValue() == 0 )
            {
                while( sb.toString().indexOf( string ) == -1 )
                {
                    sb.append( (char)input.read() );
                }
            }
            else
            {
                final Calendar endTime = Calendar.getInstance();
                endTime.add( Calendar.SECOND, timeout.intValue() );
                while( sb.toString().indexOf( string ) == -1 )
                {
                    while( Calendar.getInstance().before( endTime ) &&
                        input.available() == 0 )
                    {
                        Thread.sleep( 250 );
                    }
                    if( input.available() == 0 )
                    {
                        throw new TaskException( "Response Timed-Out" );
                    }
                    sb.append( (char)input.read() );
                }
            }
            getContext().info( sb.toString() );
        }
        catch( final TaskException te )
        {
            throw te;
        }
        catch( final Exception e )
        {
            throw new TaskException( e.getMessage(), e );
        }
    }
}
