/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Hashtable;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.myrmidon.framework.LogLevel;

/**
 * This task is the manager for RecorderEntry's. It is this class that holds all
 * entries, modifies them every time the &lt;recorder&gt; task is called, and
 * addes them to the build listener process.
 *
 * @author <a href="mailto:jayglanville@home.com">J D Glanville</a>
 * @version 0.5
 * @see RecorderEntry
 */
public class Recorder
    extends Task
{
    /**
     * The list of recorder entries.
     */
    private final static Hashtable c_recorderEntries = new Hashtable();

    /**
     * The name of the file to record to.
     */
    private String m_filename;

    /**
     * Whether or not to append. Need Boolean to record an unset state (null).
     */
    private Boolean m_append;

    /**
     * Whether to start or stop recording. Need Boolean to record an unset state
     * (null).
     */
    private Boolean m_start;

    /**
     * What level to log? -1 means not initialized yet.
     */
    private int loglevel = -1;

    /**
     * Sets the action for the associated recorder entry.
     *
     * @param action The action for the entry to take: start or stop.
     */
    public void setAction( final ActionChoices action )
    {
        if( action.getValue().equalsIgnoreCase( "start" ) )
        {
            m_start = Boolean.TRUE;
        }
        else
        {
            m_start = Boolean.FALSE;
        }
    }

    /**
     * Whether or not the logger should append to a previous file.
     *
     * @param append The new Append value
     */
    public void setAppend( final boolean append )
    {
        m_append = new Boolean( append );
    }

    /**
     * Sets the level to which this recorder entry should log to.
     *
     * @param level The new Loglevel value
     * @see VerbosityLevelChoices
     */
    public void setLoglevel( final LogLevel level )
    {
        //I hate cascading if/elseif clauses !!!
        if( LogLevel.ERROR == level )
        {
            loglevel = Project.MSG_ERR;
        }
        else if( LogLevel.WARN == level )
        {
            loglevel = Project.MSG_WARN;
        }
        else if( LogLevel.INFO == level )
        {
            loglevel = Project.MSG_INFO;
        }
        else if( LogLevel.DEBUG == level )
        {
            loglevel = Project.MSG_VERBOSE;
        }
    }

    /**
     * Sets the name of the file to log to, and the name of the recorder entry.
     *
     * @param fname File name of logfile.
     */
    public void setName( String fname )
    {
        m_filename = fname;
    }

    /**
     * The main execution.
     *
     * @exception TaskException Description of Exception
     */
    public void execute()
        throws TaskException
    {
        if( m_filename == null )
        {
            throw new TaskException( "No filename specified" );
        }

        getLogger().debug( "setting a recorder for name " + m_filename );

        // get the recorder entry
        final RecorderEntry recorder = getRecorder( m_filename );

        getProject().addProjectListener( recorder );

        // set the values on the recorder
        recorder.setLogLevel( loglevel );

        if( null != m_start )
        {
            recorder.setRecordState( m_start.booleanValue() );
        }
    }

    /**
     * Gets the recorder that's associated with the passed in name. If the
     * recorder doesn't exist, then a new one is created.
     */
    protected RecorderEntry getRecorder( final String name )
        throws TaskException
    {
        final Object o = c_recorderEntries.get( name );
        if( null == o )
        {
            return (RecorderEntry)o;
        }

        // create a recorder entry
        try
        {
            final PrintStream output = createOutput( name );
            final RecorderEntry entry = new RecorderEntry( output );
            c_recorderEntries.put( name, entry );
            return entry;
        }
        catch( final IOException ioe )
        {
            throw new TaskException( "Problems creating a recorder entry",
                                     ioe );
        }
    }

    private PrintStream createOutput( final String name )
        throws FileNotFoundException
    {
        FileOutputStream outputStream = null;
        if( m_append == null )
        {
            outputStream = new FileOutputStream( name );
        }
        else
        {
            outputStream = new FileOutputStream( name, m_append.booleanValue() );
        }

        return new PrintStream( outputStream );
    }
}
