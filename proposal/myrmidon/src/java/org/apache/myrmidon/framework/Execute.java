/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.framework;

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
 * This is a utility class designed to make executing native
 * processes easier in the context of ant.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @author <a href="mailto:thomas.haas@softwired-inc.com">Thomas Haas</a>
 * @version $Revision$ $Date$
 */
public class Execute
{
    private Commandline m_command;
    private Properties m_environment = new Properties();
    private File m_workingDirectory = new File( "." );
    private boolean m_newEnvironment;
    private ExecOutputHandler m_handler;
    private long m_timeout;
    private ExecManager m_execManager;
    private Integer m_returnCode;

    public Execute( final ExecManager execManager )
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

    public void setReturnCode( final int returnCode )
    {
        m_returnCode = new Integer( returnCode );
    }

    /**
     * Runs a process defined by the command line and returns its exit status.
     *
     * @return the exit status of the subprocess or <code>INVALID</code>
     */
    public int execute()
        throws TaskException
    {
        final int returnCode = executeNativeProcess();
        checkReturnCode( returnCode );
        return returnCode;
    }

    /**
     * Utility method to verify that specified return code was the
     * return code expected (if any).
     */
    private void checkReturnCode( final int returnCode )
        throws TaskException
    {
        if( null != m_returnCode &&
            returnCode != m_returnCode.intValue() )
        {
            throw new TaskException( "Unexpected return code " + returnCode );
        }
    }

    /**
     * Actually execute the native process.
     */
    private int executeNativeProcess()
        throws TaskException
    {
        final ExecMetaData metaData = buildExecMetaData();
        try
        {
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
        catch( final IOException ioe )
        {
            throw new TaskException( ioe.getMessage(), ioe );
        }
    }

    /**
     * Utility method to create an ExecMetaData object
     * to pass to the ExecManager service.
     */
    private ExecMetaData buildExecMetaData()
    {
        final String[] command = m_command.getCommandline();

        return new ExecMetaData( command,
                                 m_environment,
                                 m_workingDirectory,
                                 m_newEnvironment );
    }
}
