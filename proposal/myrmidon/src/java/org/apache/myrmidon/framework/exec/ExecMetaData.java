/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.framework.exec;

import java.io.File;
import org.apache.tools.ant.Project;

/**
 * This class holds meta data that is used to launch a native executable.
 * This class should be populated with valid data and passed to the
 * <code>ExecManager</code> and it will be the responsibility of the
 * <code>ExecManager</code> to actually launch the native executable.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public class ExecMetaData
{
    /**
     * The working directory in which the applicaiton is launched.
     */
    private File m_workingDirectory;

    /**
     * The array of strings that make up the command line for the command.
     */
    private String[] m_command;

    /**
     * The array of strings that make up the command line for the command.
     * Note that these variables are in the ugly format expected by the
     * Runtime.exec() call. For most systems this means that each entry
     * must be of the form <code>key=value</code>
     *
     * <p>This set of variables is combined with the environment of current
     * process if <code>isEnvironmentAdditive=true</code> else it specifies
     * full environment.
     */
    private String[] m_environment;

    /**
     * If this variable is true then then the environment specified is
     * added to the environment variables for current process. If this
     * value is false then the specified environment replaces the environment
     * for the command.
     */
    private boolean m_isEnvironmentAdditive;

    /**
     * Construct the meta data for executable as appropriate.
     * Note that it is invalid to specify a <code>null</code>
     * workingDirectory or command. It is also invalid to specify
     * a null environment and an additive environment.
     */
    public ExecMetaData( final String[] command,
                         final String[] environment,
                         final File workingDirectory,
                         final boolean environmentAdditive )
    {
        m_command = command;
        m_environment = environment;
        m_workingDirectory = workingDirectory;
        m_isEnvironmentAdditive = environmentAdditive;

        if( null == m_workingDirectory )
        {
            throw new NullPointerException( "workingDirectory" );
        }

        if( null == m_command )
        {
            throw new NullPointerException( "command" );
        }

        if( null == m_environment && m_isEnvironmentAdditive )
        {
            throw new IllegalArgumentException( "isEnvironmentAdditive" );
        }
    }

    public File getWorkingDirectory()
    {
        return m_workingDirectory;
    }

    public String[] getCommand()
    {
        return m_command;
    }

    public String[] getEnvironment()
    {
        return m_environment;
    }

    public boolean isEnvironmentAdditive()
    {
        return m_isEnvironmentAdditive;
    }
}
