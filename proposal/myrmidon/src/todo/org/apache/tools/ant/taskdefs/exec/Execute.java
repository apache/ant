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
import java.io.OutputStream;
import java.util.Properties;
import org.apache.aut.nativelib.ExecException;
import org.apache.aut.nativelib.ExecManager;
import org.apache.aut.nativelib.ExecMetaData;
import org.apache.avalon.excalibur.io.IOUtil;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.factorys.ExecManagerFactory;
import org.apache.myrmidon.interfaces.service.ServiceException;

/**
 * Runs an external program.
 *
 * @author thomas.haas@softwired-inc.com
 */
public class Execute
{
    private String[] m_command;
    private Properties m_environment = new Properties();
    private File m_workingDirectory = new File( "." );
    private boolean m_newEnvironment;
    private OutputStream m_output;
    private OutputStream m_error;
    private long m_timeout;

    /**
     * Controls whether the VM is used to launch commands, where possible
     */
    private boolean m_useVMLauncher = true;

    public void setTimeout( final long timeout )
    {
        m_timeout = timeout;
    }

    public void setOutput( final OutputStream output )
    {
        m_output = output;
    }

    public void setError( final OutputStream error )
    {
        m_error = error;
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
     * Launch this execution through the VM, where possible, rather than through
     * the OS's shell. In some cases and operating systems using the shell will
     * allow the shell to perform additional processing such as associating an
     * executable with a script, etc
     *
     * @param useVMLauncher The new VMLauncher value
     */
    public void setVMLauncher( boolean useVMLauncher )
    {
        m_useVMLauncher = useVMLauncher;
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
        try
        {
            final ExecManagerFactory factory = new ExecManagerFactory();
            final ExecManager manager = (ExecManager)factory.createService();

            final ExecMetaData metaData =
                new ExecMetaData( m_command, m_environment,
                                  m_workingDirectory, m_newEnvironment );
            return manager.execute( metaData, null, m_output, m_error, m_timeout );
        }
        catch( final ExecException ee )
        {
            throw new TaskException( ee.getMessage(), ee );
        }
        catch( final ServiceException se )
        {
            throw new TaskException( se.getMessage(), se );
        }
        finally
        {
            IOUtil.shutdownStream( m_output );
            IOUtil.shutdownStream( m_error );
        }
    }
}
