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
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.exec.DefaultExecOutputHandler;
import org.apache.myrmidon.framework.exec.ExecException;
import org.apache.myrmidon.framework.exec.ExecMetaData;
import org.apache.myrmidon.framework.exec.ExecOutputHandler;
import org.apache.myrmidon.framework.exec.impl.DefaultExecManager;

/**
 * Runs an external program.
 *
 * @author thomas.haas@softwired-inc.com
 */
public class Execute2
    extends AbstractLogEnabled
{
    private String[] m_command;
    private Properties m_environment = new Properties();
    private File m_workingDirectory = new File( "." );
    private boolean m_newEnvironment;
    private ExecOutputHandler m_handler;
    private long m_timeout;

    /**
     * Controls whether the VM is used to launch commands, where possible
     */
    private boolean m_useVMLauncher = true;

    private static File getAntHomeDirectory()
    {
        final String antHome = System.getProperty( "myrmidon.home" );
        if( null == antHome )
        {
            final String message =
                "Cannot locate antRun script: Property 'ant.home' not specified";
            throw new IllegalStateException( message );
        }

        return new File( antHome );
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
     * @param commandline the commandline of the subprocess to launch
     */
    public void setCommandline( String[] commandline )
    {
        m_command = commandline;
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
     * @param newenv whether to propagate the process environment.
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
     * @exception IOException Description of Exception
     */
    public int execute()
        throws IOException, TaskException
    {
        if( null == m_handler )
        {
            m_handler = new DefaultExecOutputHandler();
            setupLogger( m_handler );
        }

        try
        {
            final DefaultExecManager manager =
                new DefaultExecManager( getAntHomeDirectory() );

            final ExecMetaData metaData =
                new ExecMetaData( m_command, m_environment,
                                  m_workingDirectory, m_newEnvironment );

            return manager.execute( metaData, m_handler, m_timeout );
        }
        catch( final ExecException ee )
        {
            throw new TaskException( ee.getMessage(), ee );
        }
    }
}
