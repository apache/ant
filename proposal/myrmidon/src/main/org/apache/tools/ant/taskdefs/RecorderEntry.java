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
import org.apache.myrmidon.listeners.LogEvent;
import org.apache.myrmidon.listeners.ProjectEvent;
import org.apache.myrmidon.listeners.TargetEvent;
import org.apache.myrmidon.listeners.TaskEvent;
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
     * @param record true for on, false for off, null for no change.
     */
    public void setRecordState( final boolean record )
    {
        m_record = record;
    }

    /**
     * Notify listener of log message event.
     */
    public void log( final LogEvent event )
    {
        final Throwable throwable = event.getThrowable();
        if( throwable != null )
        {
            m_output.println( StringUtil.LINE_SEPARATOR + "BUILD FAILED" + StringUtil.LINE_SEPARATOR );
            throwable.printStackTrace( m_output );
            finishRecording();
        }
        else
        {
            getLogger().debug( "--- MESSAGE LOGGED" );

            final StringBuffer sb = new StringBuffer();

            final String task = event.getTaskName();
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

            sb.append( event.getMessage() );

            //FIXME: Check log level here
            if( m_record )
            {
                m_output.println( sb.toString() );
            }
        }
    }

    /**
     * Notify listener of projectFinished event.
     */
    public void projectFinished( final ProjectEvent event )
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
    public void projectStarted( final ProjectEvent event )
    {
        getLogger().debug( "> BUILD STARTED" );
    }


    /**
     * Notify listener of targetFinished event.
     */
    public void targetFinished( final TargetEvent event )
    {
        getLogger().debug( "<< TARGET FINISHED -- " + event.getTargetName() );
        final long millis = System.currentTimeMillis() - m_targetStartTime;
        final String duration = formatTime( millis );
        getLogger().debug( event.getTargetName() + ":  duration " + duration );
        m_output.flush();
    }

    /**
     * Notify listener of targetStarted event.
     */
    public void targetStarted( final TargetEvent event )
    {
        getLogger().debug( ">> TARGET STARTED -- " + event.getTargetName() );
        getLogger().info( StringUtil.LINE_SEPARATOR + event.getTargetName() + ":" );
        m_targetStartTime = System.currentTimeMillis();

    }

    /**
     * Notify listener of taskStarted event.
     */
    public void taskStarted( final TaskEvent event )
    {
        getLogger().debug( ">>> TASK STARTED -- " + event.getTaskName() );
    }

    /**
     * Notify listener of taskFinished event.
     */
    public void taskFinished( final TaskEvent event )
    {
        getLogger().debug( "<<< TASK FINISHED -- " + event.getTaskName() );
        m_output.flush();
    }
}
