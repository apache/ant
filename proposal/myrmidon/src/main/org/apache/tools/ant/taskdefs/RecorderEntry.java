/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs;

import java.io.PrintStream;
import org.apache.avalon.excalibur.util.StringUtil;
import org.apache.avalon.framework.logger.LogEnabled;
import org.apache.avalon.framework.logger.Logger;
import org.apache.myrmidon.listeners.AbstractProjectListener;
import org.apache.tools.ant.Project;

/**
 * This is a class that represents a recorder. This is the listener to the build
 * process.
 *
 * @author <a href="mailto:jayglanville@home.com">J D Glanville</a>
 */
public class RecorderEntry
    extends AbstractProjectListener
    implements LogEnabled
{
    /**
     * The state of the recorder (recorder on or off).
     */
    private boolean m_record;

    /**
     * The current verbosity level to record at.
     */
    private int m_loglevel = Project.MSG_INFO;

    /**
     * The output PrintStream to record to.
     */
    private final PrintStream m_output;

    /**
     * The start time of the last know target.
     */
    private long m_targetStartTime = 0l;

    private Logger m_logger;

    /**
     * @param name The name of this recorder (used as the filename).
     */
    protected RecorderEntry( final PrintStream output )
    {
        m_output = output;
    }

    /**
     * Provide component with a logger.
     *
     * @param logger the logger
     */
    public void enableLogging( final Logger logger )
    {
        m_logger = logger;
    }

    protected final Logger getLogger()
    {
        return m_logger;
    }

    private static String formatTime( long millis )
    {
        long seconds = millis / 1000;
        long minutes = seconds / 60;

        if( minutes > 0 )
        {
            return minutes + " minute" + ( minutes == 1 ? " " : "s " ) +
                ( seconds % 60 ) + " second" + ( seconds % 60 == 1 ? "" : "s" );
        }
        else
        {
            return seconds + " second" + ( seconds % 60 == 1 ? "" : "s" );
        }
    }

    public void setLogLevel( final int loglevel )
    {
        m_loglevel = loglevel;
    }

    /**
     * Turns off or on this recorder.
     *
     * @param state true for on, false for off, null for no change.
     */
    public void setRecordState( final boolean record )
    {
        m_record = record;
    }

    /**
     * Notify listener of log message event.
     *
     * @param message the message
     * @param throwable the throwable
     */
    public void log( final String message, final Throwable throwable )
    {
        m_output.println( StringUtil.LINE_SEPARATOR + "BUILD FAILED" + StringUtil.LINE_SEPARATOR );
        throwable.printStackTrace( m_output );
        finishRecording();
    }

    /**
     * Notify listener of projectFinished event.
     */
    public void projectFinished()
    {
        m_output.println( StringUtil.LINE_SEPARATOR + "BUILD SUCCESSFUL" );
        finishRecording();
    }

    private void finishRecording()
    {
        getLogger().debug( "< BUILD FINISHED" );
        m_output.flush();
        m_output.close();
    }

    /**
     * Notify listener of projectStarted event.
     */
    public void projectStarted()
    {
        getLogger().debug( "> BUILD STARTED" );
    }

    /**
     * Notify listener of log message event.
     *
     * @param message the message
     */
    public void log( final String message )
    {
        getLogger().debug( "--- MESSAGE LOGGED" );

        final StringBuffer sb = new StringBuffer();

        final String task = getTask();
        if( task != null )
        {
            final String name = "[" + task + "]";
            final int padding = 12 - name.length();
            for( int i = 0; i < padding; i++ )
            {
                sb.append( " " );
            }
            sb.append( name );
        }

        sb.append( message );

        //FIXME: Check log level here
        if( m_record )
        {
            m_output.println( sb.toString() );
        }
    }

    /**
     * Notify listener of targetFinished event.
     */
    public void targetFinished()
    {
        getLogger().debug( "<< TARGET FINISHED -- " + getTarget() );
        final long millis = System.currentTimeMillis() - m_targetStartTime;
        final String duration = formatTime( millis );
        getLogger().debug( getTarget() + ":  duration " + duration );
        m_output.flush();
        super.targetFinished();
    }

    /**
     * Notify listener of targetStarted event.
     *
     * @param target the name of target
     */
    public void targetStarted( final String target )
    {
        super.targetStarted( target );
        getLogger().debug( ">> TARGET STARTED -- " + getTarget() );
        getLogger().info( StringUtil.LINE_SEPARATOR + getTarget() + ":" );
        m_targetStartTime = System.currentTimeMillis();

    }

    /**
     * Notify listener of taskStarted event.
     *
     * @param task the name of task
     */
    public void taskStarted( String task )
    {
        super.taskStarted( task );
        getLogger().debug( ">>> TASK STARTED -- " + getTask() );
    }

    /**
     * Notify listener of taskFinished event.
     */
    public void taskFinished()
    {
        getLogger().debug( "<<< TASK FINISHED -- " + getTask() );
        m_output.flush();
        super.taskFinished();
    }
}
