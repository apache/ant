/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.optional.ccm;

import org.apache.aut.nativelib.ExecOutputHandler;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskContext;
import org.apache.tools.todo.types.Commandline;

/**
 * Task allows to create new ccm task and set it as the default
 *
 * @author Benoit Moussaud benoit.moussaud@criltelecom.com
 */
public class CCMCreateTask
    extends Continuus
    implements ExecOutputHandler
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
        final Commandline commandLine = determineTask();
        if( null == m_task )
        {
            final String message = "Error determining task";
            throw new TaskException( message );
        }

        //create task ok, set this task as the default one
        final Commandline cmd = new Commandline();
        cmd.setExecutable( getCcmCommand() );
        cmd.addArgument( COMMAND_DEFAULT_TASK );
        cmd.addArgument( m_task );

        getContext().debug( commandLine.toString() );

        final int result2 = run( cmd, null );
        if( result2 != 0 )
        {
            final String message = "Failed executing: " + cmd.toString();
            throw new TaskException( message );
        }
    }

    private Commandline determineTask()
        throws TaskException
    {
        final Commandline commandLine = new Commandline();

        // build the command line from what we got the format
        // as specified in the CCM.EXE help
        commandLine.setExecutable( getCcmCommand() );
        commandLine.addArgument( getCcmAction() );

        checkOptions( commandLine );

        final int result = run( commandLine, this );
        if( result != 0 )
        {
            final String message = "Failed executing: " + commandLine.toString();
            throw new TaskException( message );
        }
        return commandLine;
    }

    /**
     * Check the command line options.
     */
    private void checkOptions( final Commandline cmd )
    {
        if( m_comment != null )
        {
            cmd.addArgument( FLAG_COMMENT );
            cmd.addArgument( "\"" + m_comment + "\"" );
        }

        if( m_platform != null )
        {
            cmd.addArgument( FLAG_PLATFORM );
            cmd.addArgument( m_platform );
        }

        if( m_resolver != null )
        {
            cmd.addArgument( FLAG_RESOLVER );
            cmd.addArgument( m_resolver );
        }

        if( m_subSystem != null )
        {
            cmd.addArgument( FLAG_SUBSYSTEM );
            cmd.addArgument( "\"" + m_subSystem + "\"" );
        }

        if( m_release != null )
        {
            cmd.addArgument( FLAG_RELEASE );
            cmd.addArgument( m_release );
        }
    }

    /**
     * Receive notification about the process writing
     * to standard output.
     */
    public void stdout( final String line )
    {
        getContext().debug( "buffer:" + line );
        final String task = getTask( line );

        setTask( task );
        getContext().debug( "task is " + m_task );
    }

    private String getTask( final String line )
    {
        try
        {
            final String task = line.substring( line.indexOf( ' ' ) ).trim();
            return task.substring( 0, task.lastIndexOf( ' ' ) ).trim();
        }
        catch( final Exception e )
        {
            final String message = "error procession stream " + e.getMessage();
            getContext().error( message, e );
        }

        return null;
    }

    /**
     * Receive notification about the process writing
     * to standard error.
     */
    public void stderr( final String line )
    {
        getContext().debug( "err " + line );
    }
}

