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
import org.apache.tools.ant.Task;

/**
 * Log
 *
 * @author costin@dnt.ro
 */
public class Echo
    extends Task
{
    private String m_message = "";// required
    private File m_file;
    private boolean m_append;
    private EchoLevel m_echoLevel;

    /**
     * Shall we append to an existing file?
     *
     * @param append The new Append value
     */
    public void setAppend( final boolean append )
    {
        m_append = append;
    }

    /**
     * Sets the file attribute.
     *
     * @param file The new File value
     */
    public void setFile( final File file )
    {
        m_file = file;
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
    public void setLevel( final EchoLevel echoLevel )
    {
        m_echoLevel = echoLevel;
    }

    /**
     * Sets the message variable.
     *
     * @param msg Sets the value for the message variable.
     */
    public void setMessage( final String message )
    {
        m_message = message;
    }

    /**
     * Set a multiline message.
     *
     * @param msg The feature to be added to the Text attribute
     */
    public void addText( final String message )
        throws TaskException
    {
        m_message += getProject().replaceProperties( message );
    }

    /**
     * Does the work.
     *
     * @exception TaskException if someting goes wrong with the build
     */
    public void execute()
        throws TaskException
    {
        if( m_file == null )
        {
            doLog();
        }
        else
        {
            FileWriter out = null;
            try
            {
                out = new FileWriter( m_file.getAbsolutePath(), m_append );
                out.write( m_message, 0, m_message.length() );
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

    private void doLog()
    {
        final String option = m_echoLevel.getValue();
        if( option.equals( "error" ) )
        {
            getLogger().error( m_message );
        }
        else if( option.equals( "warning" ) )
        {
            getLogger().warn( m_message );
        }
        else if( option.equals( "info" ) )
        {
            getLogger().info( m_message );
        }
        else
        {
            getLogger().debug( m_message );
        }
    }
}
