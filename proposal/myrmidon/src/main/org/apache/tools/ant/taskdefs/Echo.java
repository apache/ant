/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.EnumeratedAttribute;

/**
 * Log
 *
 * @author costin@dnt.ro
 */
public class Echo extends Task
{
    protected String message = "";// required
    protected File file = null;
    protected boolean append = false;

    // by default, messages are always displayed
    protected int logLevel = Project.MSG_WARN;

    /**
     * Shall we append to an existing file?
     *
     * @param append The new Append value
     */
    public void setAppend( boolean append )
    {
        this.append = append;
    }

    /**
     * Sets the file attribute.
     *
     * @param file The new File value
     */
    public void setFile( File file )
    {
        this.file = file;
    }

    /**
     * Set the logging level to one of
     * <ul>
     *   <li> error</li>
     *   <li> warning</li>
     *   <li> info</li>
     *   <li> verbose</li>
     *   <li> debug</li>
     *   <ul><p>
     *
     *     The default is &quot;warning&quot; to ensure that messages are
     *     displayed by default when using the -quiet command line option.</p>
     *
     * @param echoLevel The new Level value
     */
    public void setLevel( EchoLevel echoLevel )
    {
        String option = echoLevel.getValue();
        if( option.equals( "error" ) )
        {
            logLevel = Project.MSG_ERR;
        }
        else if( option.equals( "warning" ) )
        {
            logLevel = Project.MSG_WARN;
        }
        else if( option.equals( "info" ) )
        {
            logLevel = Project.MSG_INFO;
        }
        else if( option.equals( "verbose" ) )
        {
            logLevel = Project.MSG_VERBOSE;
        }
        else
        {
            // must be "debug"
            logLevel = Project.MSG_DEBUG;
        }
    }

    /**
     * Sets the message variable.
     *
     * @param msg Sets the value for the message variable.
     */
    public void setMessage( String msg )
    {
        this.message = msg;
    }

    /**
     * Set a multiline message.
     *
     * @param msg The feature to be added to the Text attribute
     */
    public void addText( String msg )
        throws TaskException
    {
        message += getProject().replaceProperties( msg );
    }

    /**
     * Does the work.
     *
     * @exception TaskException if someting goes wrong with the build
     */
    public void execute()
        throws TaskException
    {
        if( file == null )
        {
            log( message, logLevel );
        }
        else
        {
            FileWriter out = null;
            try
            {
                out = new FileWriter( file.getAbsolutePath(), append );
                out.write( message, 0, message.length() );
            }
            catch( IOException ioe )
            {
                throw new TaskException( "Error", ioe );
            }
            finally
            {
                if( out != null )
                {
                    try
                    {
                        out.close();
                    }
                    catch( IOException ioex )
                    {
                    }
                }
            }
        }
    }

    public static class EchoLevel extends EnumeratedAttribute
    {
        public String[] getValues()
        {
            return new String[]{"error", "warning", "info", "verbose", "debug"};
        }
    }
}
