/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs;

import java.io.File;
import java.io.IOException;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Commandline;

/**
 * Task as a layer on top of patch. Patch applies a diff file to an original.
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 */
public class Patch extends Task
{
    private boolean havePatchfile = false;
    private Commandline cmd = new Commandline();

    private File originalFile;

    /**
     * Shall patch write backups.
     *
     * @param backups The new Backups value
     */
    public void setBackups( boolean backups )
    {
        if( backups )
        {
            cmd.createArgument().setValue( "-b" );
        }
    }

    /**
     * Ignore whitespace differences.
     *
     * @param ignore The new Ignorewhitespace value
     */
    public void setIgnorewhitespace( boolean ignore )
    {
        if( ignore )
        {
            cmd.createArgument().setValue( "-l" );
        }
    }

    /**
     * The file to patch.
     *
     * @param file The new Originalfile value
     */
    public void setOriginalfile( File file )
    {
        originalFile = file;
    }

    /**
     * The file containing the diff output.
     *
     * @param file The new Patchfile value
     */
    public void setPatchfile( File file )
    {
        if( !file.exists() )
        {
            throw new TaskException( "patchfile " + file + " doesn\'t exist" );
        }
        cmd.createArgument().setValue( "-i" );
        cmd.createArgument().setFile( file );
        havePatchfile = true;
    }

    /**
     * Work silently unless an error occurs.
     *
     * @param q The new Quiet value
     */
    public void setQuiet( boolean q )
    {
        if( q )
        {
            cmd.createArgument().setValue( "-s" );
        }
    }

    /**
     * Assume patch was created with old and new files swapped.
     *
     * @param r The new Reverse value
     */
    public void setReverse( boolean r )
    {
        if( r )
        {
            cmd.createArgument().setValue( "-R" );
        }
    }

    /**
     * Strip the smallest prefix containing <i>num</i> leading slashes from
     * filenames. <p>
     *
     * patch's <i>-p</i> option.
     *
     * @param num The new Strip value
     * @exception TaskException Description of Exception
     */
    public void setStrip( int num )
        throws TaskException
    {
        if( num < 0 )
        {
            throw new TaskException( "strip has to be >= 0" );
        }
        cmd.createArgument().setValue( "-p" + num );
    }

    public void execute()
        throws TaskException
    {
        if( !havePatchfile )
        {
            throw new TaskException( "patchfile argument is required" );
        }

        Commandline toExecute = (Commandline)cmd.clone();
        toExecute.setExecutable( "patch" );

        if( originalFile != null )
        {
            toExecute.createArgument().setFile( originalFile );
        }

        Execute exe = new Execute( new LogStreamHandler( this, Project.MSG_INFO,
                                                         Project.MSG_WARN ),
                                   null );
        exe.setCommandline( toExecute.getCommandline() );
        try
        {
            exe.execute();
        }
        catch( IOException e )
        {
            throw new TaskException( "Error", e );
        }
    }

}// Patch
