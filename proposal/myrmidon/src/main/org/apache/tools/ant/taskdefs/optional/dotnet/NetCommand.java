/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs.optional.dotnet;// imports

import java.io.File;
import java.io.IOException;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.ExecuteStreamHandler;
import org.apache.tools.ant.taskdefs.LogStreamHandler;
import org.apache.tools.ant.types.Commandline;

/**
 * This is a helper class to spawn net commands out. In its initial form it
 * contains no .net specifics, just contains all the command line/exe
 * construction stuff. However, it may be handy in future to have a means of
 * setting the path to point to the dotnet bin directory; in which case the
 * shared code should go in here.
 *
 * @author Steve Loughran steve_l@iseran.com
 * @version 0.3
 * @created 2000-11-01
 */

public class NetCommand
{

    /**
     * trace flag
     */
    protected boolean _traceCommandLine = false;

    /**
     * what is the command line
     */
    protected Commandline _commandLine;

    /**
     * executabe
     */
    protected Execute _exe;

    /**
     * flag to control action on execution trouble
     */
    protected boolean _failOnError;

    /**
     * owner project
     */
    protected Task _owner;

    /**
     * actual program to invoke
     */
    protected String _program;

    /**
     * title of the command
     */
    protected String _title;

    /**
     * constructor
     *
     * @param title (for logging/errors)
     * @param owner Description of Parameter
     * @param program Description of Parameter
     */

    public NetCommand( Task owner, String title, String program )
    {
        _owner = owner;
        _title = title;
        _program = program;
        _commandLine = new Commandline();
        _commandLine.setExecutable( _program );
        prepareExecutor();
    }

    /**
     * set fail on error flag
     *
     * @param b fail flag -set to true to cause an exception to be raised if the
     *      return value != 0
     */
    public void setFailOnError( boolean b )
    {
        _failOnError = b;
    }

    /**
     * turn tracing on or off
     *
     * @param b trace flag
     */
    public void setTraceCommandLine( boolean b )
    {
        _traceCommandLine = b;
    }

    /**
     * query fail on error flag
     *
     * @return The FailFailOnError value
     */
    public boolean getFailFailOnError()
    {
        return _failOnError;
    }

    /**
     * add an argument to a command line; do nothing if the arg is null or empty
     * string
     *
     * @param argument The feature to be added to the Argument attribute
     */
    public void addArgument( String argument )
    {
        if( argument != null && argument.length() != 0 )
        {
            _commandLine.createArgument().setValue( argument );
        }
    }

    /**
     * Run the command using the given Execute instance.
     *
     * @exception TaskException Description of Exception
     * @throws an exception of something goes wrong and the failOnError flag is
     *      true
     */
    public void runCommand()
        throws TaskException
    {
        int err = -1;// assume the worst
        try
        {
            if( _traceCommandLine )
            {
                _owner.log( _commandLine.toString() );
            }
            else
            {
                //in verbose mode we always log stuff
                logVerbose( _commandLine.toString() );
            }
            _exe.setCommandline( _commandLine.getCommandline() );
            err = _exe.execute();
            if( err != 0 )
            {
                if( _failOnError )
                {
                    throw new TaskException( _title + " returned: " + err );
                }
                else
                {
                    _owner.log( _title + "  Result: " + err, Project.MSG_ERR );
                }
            }
        }
        catch( IOException e )
        {
            throw new TaskException( _title + " failed: " + e, e );
        }
    }

    /**
     * error text log
     *
     * @param msg message to display as an error
     */
    protected void logError( String msg )
    {
        _owner.getProject().log( msg, Project.MSG_ERR );
    }

    /**
     * verbose text log
     *
     * @param msg string to add to log iff verbose is defined for the build
     */
    protected void logVerbose( String msg )
    {
        _owner.getProject().log( msg, Project.MSG_VERBOSE );
    }

    /**
     * set up the command sequence..
     */
    protected void prepareExecutor()
    {
        // default directory to the project's base directory
        File dir = _owner.getProject().getBaseDir();
        ExecuteStreamHandler handler = new LogStreamHandler( _owner,
                                                             Project.MSG_INFO, Project.MSG_WARN );
        _exe = new Execute( handler, null );
        _exe.setAntRun( _owner.getProject() );
        _exe.setWorkingDirectory( dir );
    }
}//class
