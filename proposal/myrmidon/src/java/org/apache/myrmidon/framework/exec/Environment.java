/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.framework.exec;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;
import java.util.Properties;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.Os;
import org.apache.tools.ant.taskdefs.exec.Execute;

/**
 * This is the class that can be used to retrieve the environment
 * variables of the native process.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @author <a href="mailto:thomas.haas@softwired-inc.com">Thomas Haas</a>
 * @version $Revision$ $Date$
 */
public final class Environment
{
    private static final String LINE_SEP = System.getProperty( "line.separator" );

    private static final String[] COMMAND_COM = new String[]{"command.com", "/c", "set"};
    private static final String[] CMD_EXE = new String[]{"cmd", "/c", "set"};

    // Alternatively one could use: /bin/sh -c env
    private static final String[] ENV_CMD = new String[]{"/usr/bin/env"};
    private static final String[] ENV_RAW = new String[]{"env"};

    private static Properties c_procEnvironment;

    /**
     * Private constructor to block instantiation.
     */
    private Environment()
    {
    }

    public static void addNativeEnvironment( final Properties environment )
        throws ExecException, IOException
    {
        final Properties nativeEnvironment = getEnvironmentVariables();
        final Iterator nativeKeys = nativeEnvironment.keySet().iterator();
        while( nativeKeys.hasNext() )
        {
            final String key = (String)nativeKeys.next();
            if( environment.contains( key ) )
            {
                //Skip environment variables that are overidden
                continue;
            }

            final String value = nativeEnvironment.getProperty( key );
            environment.setProperty( key, value );
        }
    }

    /**
     * Retrieve an array of EnvironmentData vars that contains a list of all
     * native EnvironmentData Variables for the current process.
     */
    private static String[] getNativeEnvironmentAsArray()
        throws IOException, ExecException
    {
        final Properties environment = getEnvironmentVariables();

        final String[] env = new String[ environment.size() ];
        final Iterator keys = environment.keySet().iterator();
        int index = 0;
        while( keys.hasNext() )
        {
            final String key = (String)keys.next();
            final String value = environment.getProperty( key );
            env[ index ] = key + '=' + value;
            index++;
        }

        return env;
    }

    /**
     * Retrieve a Properties object that contains the list of all
     * native EnvironmentData Variables for the current process.
     */
    public static Properties getNativeEnvironment()
        throws IOException, ExecException
    {
        return new Properties( getEnvironmentVariables() );
    }

    /**
     * Get the Property object with all environment variables and
     * attempt to load it if it has not already been loaded.
     */
    private synchronized static Properties getEnvironmentVariables()
        throws IOException, ExecException
    {
        if( null == c_procEnvironment )
        {
            c_procEnvironment = retrieveEnvironmentVariables();
        }

        return c_procEnvironment;
    }

    /**
     * Retrieve a last of environment variables from the native OS.
     */
    private static synchronized Properties retrieveEnvironmentVariables()
        throws IOException, ExecException
    {
        final Properties properties = new Properties();
        final String data = getEnvironmentText();

        final BufferedReader in = new BufferedReader( new StringReader( data ) );
        String var = null;
        String line;
        while( ( line = in.readLine() ) != null )
        {
            if( line.indexOf( '=' ) == -1 )
            {
                // Chunk part of previous env var (UNIX env vars can
                // contain embedded new lines).
                if( var == null )
                {
                    var = LINE_SEP + line;
                }
                else
                {
                    var += LINE_SEP + line;
                }
            }
            else
            {
                // New env var...append the previous one if we have it.
                if( var != null )
                {
                    addProperty( properties, var );
                }
                var = line;
            }
        }

        // Since we "look ahead" before adding, there's one last env var.
        addProperty( properties, var );
        return properties;
    }

    /**
     * Parse the specified data into a key=value pair. If there is no
     * '=' character then generate an exception. After parsed data place
     * the key-value pair into the specified Properties object.
     */
    private static void addProperty( final Properties properties,
                                     final String data )
        throws ExecException
    {
        final int index = data.indexOf( '=' );
        if( -1 == index )
        {
            //Our env variable does not have any = in it.
            final String message = "EnvironmentData variable '" + data +
                "' does not have a '=' character in it";
            throw new ExecException( message );
        }
        else
        {
            final String key = data.substring( 0, index );
            final String value = data.substring( index + 1 );
            properties.setProperty( key, value );
        }
    }

    /**
     * Retrieve the text of data that is the result of
     * running the environment command.
     */
    private static String getEnvironmentText()
        throws IOException, ExecException
    {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        final Execute exe = new Execute();
        exe.setOutput( output );
        exe.setError( output );

        exe.setCommandline( getEnvCommand() );

        // Make sure we do not recurse forever
        exe.setNewenvironment( true );

        try
        {
            final int retval = exe.execute();
            if( retval != 0 )
            {
                // Just try to use what we got
            }
        }
        catch( final TaskException te )
        {
            throw new ExecException( te.getMessage(), te );
        }

        return output.toString();
    }

    /**
     * Retrieve the command that will display a list of environment
     * variables.
     */
    private static String[] getEnvCommand()
        throws ExecException
    {
        if( Os.isFamily( "os/2" ) )
        {
            // OS/2 - use same mechanism as Windows 2000
            return CMD_EXE;
        }
        else if( Os.isFamily( "windows" ) )
        {
            final String osname =
                System.getProperty( "os.name" ).toLowerCase( Locale.US );
            // Determine if we're running under 2000/NT or 98/95
            if( osname.indexOf( "nt" ) >= 0 || osname.indexOf( "2000" ) >= 0 )
            {
                // Windows 2000/NT
                return CMD_EXE;
            }
            else
            {
                // Windows 98/95 - need to use an auxiliary script
                return COMMAND_COM;
            }
        }
        else if( Os.isFamily( "unix" ) )
        {
            // Generic UNIX
            return ENV_CMD;
        }
        else if( Os.isFamily( "netware" ) )
        {
            return ENV_RAW;
        }
        else
        {
            final String message =
                "Unable to determine native environment variables";
            throw new ExecException( message );
        }
    }
}
