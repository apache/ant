/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.exec;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import org.apache.aut.nativelib.ExecException;
import org.apache.aut.nativelib.ExecManager;
import org.apache.aut.nativelib.ExecMetaData;
import org.apache.aut.nativelib.ExecOutputHandler;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.types.Commandline;

/**
 * Runs an external program.
 *
 * @author thomas.haas@softwired-inc.com
 */
public class Execute2
{
    private Commandline m_command;
    private Properties m_environment = new Properties();
    private File m_workingDirectory = new File( "." );
    private boolean m_newEnvironment;
    private ExecOutputHandler m_handler;
    private long m_timeout;
    private ExecManager m_execManager;

    public Execute2( final ExecManager execManager )
    {
        m_execManager = execManager;
    }

    public void setTimeout( final long timeout )
    {
        m_timeout = timeout;
    }

    public void setExecOutputHandler( final ExecOutputHandler handler )
    {
        m_handler = handler;
    }

    /**
     * Sets the commandline of the subprocess to launch.
     *
     * @param command the commandline of the subprocess to launch
     */
    public void setCommandline( final Commandline command )
    {
        m_command = command;
    }

    public Commandline getCommandline()
    {
        if( null == m_command )
        {
            m_command = new Commandline();
        }
        return m_command;
    }

    public void setEnvironment( final Properties environment )
    {
        if( null == environment )
        {
            throw new NullPointerException( "environment" );
        }
        m_environment = environment;
    }

    /**
     * Set whether to propagate the default environment or not.
     *
     * @param newEnvironment whether to propagate the process environment.
     */
    public void setNewenvironment( boolean newEnvironment )
    {
        m_newEnvironment = newEnvironment;
    }

    /**
     * Sets the working directory of the process to execute. <p>
     *
     * @param workingDirectory the working directory of the process.
     */
    public void setWorkingDirectory( final File workingDirectory )
    {
        m_workingDirectory = workingDirectory;
    }

    /**
     * Runs a process defined by the command line and returns its exit status.
     *
     * @return the exit status of the subprocess or <code>INVALID</code>
     */
    public int execute()
        throws IOException, TaskException
    {
        try
        {
            final ExecMetaData metaData = buildExecMetaData();

            if( null != m_handler )
            {
                return m_execManager.execute( metaData, m_handler, m_timeout );
            }
            else
            {
                return m_execManager.execute( metaData,
                                              null,
                                              System.out,
                                              System.err,
                                              m_timeout );
            }
        }
        catch( final ExecException ee )
        {
            throw new TaskException( ee.getMessage(), ee );
        }
    }

    private ExecMetaData buildExecMetaData()
    {
        final String[] command = m_command.getCommandline();

        final ExecMetaData metaData =
            new ExecMetaData( command, m_environment,
                              m_workingDirectory, m_newEnvironment );
        return metaData;
    }
}
