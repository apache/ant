/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs;

import java.io.PrintStream;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildLogger;
import org.apache.tools.ant.Project;

/**
 * This is a class that represents a recorder. This is the listener to the build
 * process.
 *
 * @author <a href="mailto:jayglanville@home.com">J D Glanville</a>
 * @version 0.5
 */
public class RecorderEntry
    extends AbstractLogEnabled
    implements BuildLogger
{
    /**
     * the line separator for this OS
     */
    private final static String LINE_SEP = System.getProperty( "line.separator" );

    //////////////////////////////////////////////////////////////////////
    // ATTRIBUTES

    /**
     * The name of the file associated with this recorder entry.
     */
    private String filename = null;
    /**
     * The state of the recorder (recorder on or off).
     */
    private boolean record = true;
    /**
     * The current verbosity level to record at.
     */
    private int loglevel = Project.MSG_INFO;
    /**
     * The output PrintStream to record to.
     */
    private PrintStream out = null;
    /**
     * The start time of the last know target.
     */
    private long targetStartTime = 0l;

    //////////////////////////////////////////////////////////////////////
    // CONSTRUCTORS / INITIALIZERS

    /**
     * @param name The name of this recorder (used as the filename).
     */
    protected RecorderEntry( String name )
    {
        filename = name;
    }

    private static String formatTime( long millis )
    {
        long seconds = millis / 1000;
        long minutes = seconds / 60;

        if( minutes > 0 )
        {
            return Long.toString( minutes ) + " minute"
                + ( minutes == 1 ? " " : "s " )
                + Long.toString( seconds % 60 ) + " second"
                + ( seconds % 60 == 1 ? "" : "s" );
        }
        else
        {
            return Long.toString( seconds ) + " second"
                + ( seconds % 60 == 1 ? "" : "s" );
        }

    }

    public void setEmacsMode( boolean emacsMode )
    {
        throw new java.lang.RuntimeException( "Method setEmacsMode() not yet implemented." );
    }

    public void setErrorPrintStream( PrintStream err )
    {
        out = err;
    }

    public void setMessageOutputLevel( int level )
    {
        if( level >= Project.MSG_ERR && level <= Project.MSG_DEBUG )
            loglevel = level;
    }

    public void setOutputPrintStream( PrintStream output )
    {
        out = output;
    }

    /**
     * Turns off or on this recorder.
     *
     * @param state true for on, false for off, null for no change.
     */
    public void setRecordState( Boolean state )
    {
        if( state != null )
            record = state.booleanValue();
    }

    //////////////////////////////////////////////////////////////////////
    // ACCESSOR METHODS

    /**
     * @return the name of the file the output is sent to.
     */
    public String getFilename()
    {
        return filename;
    }

    public void buildFinished( BuildEvent event )
    {
        getLogger().debug( "< BUILD FINISHED" );

        Throwable error = event.getException();
        if( error == null )
        {
            out.println( LINE_SEP + "BUILD SUCCESSFUL" );
        }
        else
        {
            out.println( LINE_SEP + "BUILD FAILED" + LINE_SEP );
            error.printStackTrace( out );
        }
        out.flush();
        out.close();
    }

    public void buildStarted( BuildEvent event )
    {
        getLogger().debug( "> BUILD STARTED" );
    }

    public void messageLogged( BuildEvent event )
    {
        getLogger().debug( "--- MESSAGE LOGGED" );

        StringBuffer buf = new StringBuffer();
        if( event.getTask() != null )
        {
            String name = "[" + event.getTask() + "]";
            /**
             * @todo replace 12 with DefaultLogger.LEFT_COLUMN_SIZE
             */
            for( int i = 0; i < ( 12 - name.length() ); i++ )
            {
                buf.append( " " );
            }// for
            buf.append( name );
        }// if
        buf.append( event.getMessage() );

        log( buf.toString(), event.getPriority() );
    }

    public void targetFinished( BuildEvent event )
    {
        getLogger().debug( "<< TARGET FINISHED -- " + event.getTarget() );
        String time = formatTime( System.currentTimeMillis() - targetStartTime );
        getLogger().debug( event.getTarget() + ":  duration " + time );
        out.flush();
    }

    public void targetStarted( final BuildEvent event )
    {
        getLogger().debug( ">> TARGET STARTED -- " + event.getTarget() );
        getLogger().info( LINE_SEP + event.getTarget() + ":" );
        targetStartTime = System.currentTimeMillis();
    }

    public void taskFinished( BuildEvent event )
    {
        getLogger().debug( "<<< TASK FINISHED -- " + event.getTask() );
        out.flush();
    }

    public void taskStarted( BuildEvent event )
    {
        getLogger().debug( ">>> TASK STARTED -- " + event.getTask() );
    }

    /**
     * The thing that actually sends the information to the output.
     *
     * @param mesg The message to log.
     * @param level The verbosity level of the message.
     */
    private void log( String mesg, int level )
    {
        if( record && ( level <= loglevel ) )
        {
            out.println( mesg );
        }
    }
}
