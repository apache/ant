/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.optional.ccm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.taskdefs.exec.ExecuteStreamHandler;
import org.apache.tools.ant.types.Commandline;

/**
 * Task allows to create new ccm task and set it as the default
 *
 * @author Benoit Moussaud benoit.moussaud@criltelecom.com
 */
public class CCMCreateTask
    extends Continuus
    implements ExecuteStreamHandler
{
    /**
     * /comment -- comments associated to the task
     */
    private final static String FLAG_COMMENT = "/synopsis";

    /**
     * /platform flag -- target platform
     */
    private final static String FLAG_PLATFORM = "/plat";

    /**
     * /resolver flag
     */
    private final static String FLAG_RESOLVER = "/resolver";

    /**
     * /release flag
     */
    private final static String FLAG_RELEASE = "/release";

    /**
     * /release flag
     */
    private final static String FLAG_SUBSYSTEM = "/subsystem";

    /**
     * -task flag -- associate checckout task with task
     */
    private final static String FLAG_TASK = "/task";

    private String m_comment;
    private String m_platform;
    private String m_resolver;
    private String m_release;
    private String m_subSystem;
    private String m_task;

    public CCMCreateTask()
    {
        setCcmAction( COMMAND_CREATE_TASK );
    }

    /**
     * Set the value of comment.
     *
     * @param v Value to assign to comment.
     */
    public void setComment( final String comment )
    {
        m_comment = comment;
    }

    /**
     * Set the value of platform.
     *
     * @param v Value to assign to platform.
     */
    public void setPlatform( final String platform )
    {
        m_platform = platform;
    }

    /**
     * Set the value of release.
     *
     * @param v Value to assign to release.
     */
    public void setRelease( final String release )
    {
        m_release = release;
    }

    /**
     * Set the value of resolver.
     *
     * @param v Value to assign to resolver.
     */
    public void setResolver( final String resolver )
    {
        m_resolver = resolver;
    }

    /**
     * Set the value of subSystem.
     *
     * @param v Value to assign to subSystem.
     */
    public void setSubSystem( final String subSystem )
    {
        m_subSystem = subSystem;
    }

    /**
     * Set the value of task.
     *
     * @param v Value to assign to task.
     */
    public void setTask( final String task )
    {
        m_task = task;
    }

    /**
     * Executes the task. <p>
     *
     * Builds a command line to execute ccm and then calls Exec's run method to
     * execute the command line. </p>
     *
     * @exception TaskException Description of Exception
     */
    public void execute()
        throws TaskException
    {
        final Commandline commandLine = new Commandline();

        // build the command line from what we got the format
        // as specified in the CCM.EXE help
        commandLine.setExecutable( getCcmCommand() );
        commandLine.createArgument().setValue( getCcmAction() );

        checkOptions( commandLine );

        final int result = run( commandLine, this );
        if( result != 0 )
        {
            String msg = "Failed executing: " + commandLine.toString();
            throw new TaskException( msg );
        }

        //create task ok, set this task as the default one
        final Commandline commandLine2 = new Commandline();
        commandLine2.setExecutable( getCcmCommand() );
        commandLine2.createArgument().setValue( COMMAND_DEFAULT_TASK );
        commandLine2.createArgument().setValue( m_task );

        getLogger().debug( commandLine.toString() );

        final int result2 = run( commandLine2 );
        if( result2 != 0 )
        {
            String msg = "Failed executing: " + commandLine2.toString();
            throw new TaskException( msg );
        }
    }

    // implementation of org.apache.tools.ant.taskdefs.ExecuteStreamHandler interface

    public void start()
        throws IOException
    {
    }

    public void stop()
    {
    }

    /**
     * Check the command line options.
     */
    private void checkOptions( final Commandline cmd )
    {
        if( m_comment != null )
        {
            cmd.createArgument().setValue( FLAG_COMMENT );
            cmd.createArgument().setValue( "\"" + m_comment + "\"" );
        }

        if( m_platform != null )
        {
            cmd.createArgument().setValue( FLAG_PLATFORM );
            cmd.createArgument().setValue( m_platform );
        }

        if( m_resolver != null )
        {
            cmd.createArgument().setValue( FLAG_RESOLVER );
            cmd.createArgument().setValue( m_resolver );
        }

        if( m_subSystem != null )
        {
            cmd.createArgument().setValue( FLAG_SUBSYSTEM );
            cmd.createArgument().setValue( "\"" + m_subSystem + "\"" );
        }

        if( m_release != null )
        {
            cmd.createArgument().setValue( FLAG_RELEASE );
            cmd.createArgument().setValue( m_release );
        }
    }

    /**
     * @param is The new ProcessErrorStream value
     * @exception IOException Description of Exception
     */
    public void setProcessErrorStream( final InputStream error )
        throws IOException
    {
        final BufferedReader reader = new BufferedReader( new InputStreamReader( error ) );
        final String errorLine = reader.readLine();
        if( errorLine != null )
        {
            getLogger().debug( "err " + errorLine );
        }
    }

    public void setProcessInputStream( final OutputStream output )
        throws IOException
    {
    }

    /**
     * read the output stream to retrieve the new task number.
     */
    public void setProcessOutputStream( final InputStream input )
        throws TaskException, IOException
    {
        try
        {
            final BufferedReader reader =
                new BufferedReader( new InputStreamReader( input ) );
            final String buffer = reader.readLine();
            if( buffer != null )
            {
                getLogger().debug( "buffer:" + buffer );
                String taskstring = buffer.substring( buffer.indexOf( ' ' ) ).trim();
                taskstring = taskstring.substring( 0, taskstring.lastIndexOf( ' ' ) ).trim();
                setTask( taskstring );
                getLogger().debug( "task is " + m_task );
            }
        }
        catch( final NullPointerException npe )
        {
            getLogger().error( "error procession stream , null pointer exception", npe );
            throw new TaskException( npe.getClass().getName(), npe );
        }
        catch( final Exception e )
        {
            getLogger().error( "error procession stream " + e.getMessage() );
            throw new TaskException( e.getMessage(), e );
        }
    }
}

