/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs.optional.ccm;

import java.io.File;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Commandline;

/**
 * Class common to all check commands (checkout, checkin,checkin default task);
 *
 * @author Benoit Moussaud benoit.moussaud@criltelecom.com
 */
public class CCMCheck extends Continuus
{

    /**
     * -comment flag -- comment to attach to the file
     */
    public final static String FLAG_COMMENT = "/comment";

    /**
     * -task flag -- associate checckout task with task
     */
    public final static String FLAG_TASK = "/task";

    private File _file = null;
    private String _comment = null;
    private String _task = null;

    public CCMCheck()
    {
        super();
    }

    /**
     * Set the value of comment.
     *
     * @param v Value to assign to comment.
     */
    public void setComment( String v )
    {
        this._comment = v;
    }

    /**
     * Set the value of file.
     *
     * @param v Value to assign to file.
     */
    public void setFile( File v )
    {
        this._file = v;
    }

    /**
     * Set the value of task.
     *
     * @param v Value to assign to task.
     */
    public void setTask( String v )
    {
        this._task = v;
    }

    /**
     * Get the value of comment.
     *
     * @return value of comment.
     */
    public String getComment()
    {
        return _comment;
    }

    /**
     * Get the value of file.
     *
     * @return value of file.
     */
    public File getFile()
    {
        return _file;
    }

    /**
     * Get the value of task.
     *
     * @return value of task.
     */
    public String getTask()
    {
        return _task;
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
        Commandline commandLine = new Commandline();
        Project aProj = getProject();
        int result = 0;

        // build the command line from what we got the format is
        // ccm co /t .. files
        // as specified in the CLEARTOOL.EXE help
        commandLine.setExecutable( getCcmCommand() );
        commandLine.createArgument().setValue( getCcmAction() );

        checkOptions( commandLine );

        result = run( commandLine );
        if( result != 0 )
        {
            String msg = "Failed executing: " + commandLine.toString();
            throw new TaskException( msg );
        }
    }

    /**
     * Check the command line options.
     *
     * @param cmd Description of Parameter
     */
    private void checkOptions( Commandline cmd )
    {
        if( getComment() != null )
        {
            cmd.createArgument().setValue( FLAG_COMMENT );
            cmd.createArgument().setValue( getComment() );
        }

        if( getTask() != null )
        {
            cmd.createArgument().setValue( FLAG_TASK );
            cmd.createArgument().setValue( getTask() );
        }// end of if ()

        if( getFile() != null )
        {
            cmd.createArgument().setValue( _file.getAbsolutePath() );
        }// end of if ()
    }
}

