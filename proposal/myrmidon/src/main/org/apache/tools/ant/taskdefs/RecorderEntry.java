/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs;

import java.io.PrintStream;
import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.util.StringUtils;

/**
 * This is a class that represents a recorder. This is the listener to the build
 * process.
 *
 * @author <a href="mailto:jayglanville@home.com">J D Glanville</a>
 * @version 0.5
 */
public class RecorderEntry implements BuildLogger
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
        log( "< BUILD FINISHED", Project.MSG_DEBUG );

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
        log( "> BUILD STARTED", Project.MSG_DEBUG );
    }

    public void messageLogged( BuildEvent event )
    {
        log( "--- MESSAGE LOGGED", Project.MSG_DEBUG );

        StringBuffer buf = new StringBuffer();
        if( event.getTask() != null )
        {
            String name = "[" + event.getTask().getName() + "]";
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
        log( "<< TARGET FINISHED -- " + event.getTarget(), Project.MSG_DEBUG );
        String time = formatTime( System.currentTimeMillis() - targetStartTime );
        log( event.getTarget() + ":  duration " + time, Project.MSG_VERBOSE );
        out.flush();
    }

    public void targetStarted( BuildEvent event )
    {
        log( ">> TARGET STARTED -- " + event.getTarget(), Project.MSG_DEBUG );
        log( LINE_SEP + event.getTarget().getName() + ":", Project.MSG_INFO );
        targetStartTime = System.currentTimeMillis();
    }

    public void taskFinished( BuildEvent event )
    {
        log( "<<< TASK FINISHED -- " + event.getTask(), Project.MSG_DEBUG );
        out.flush();
    }

    public void taskStarted( BuildEvent event )
    {
        log( ">>> TASK STARTED -- " + event.getTask(), Project.MSG_DEBUG );
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
