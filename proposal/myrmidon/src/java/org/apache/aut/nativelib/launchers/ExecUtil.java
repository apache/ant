/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.aut.nativelib.launchers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;
import org.apache.aut.nativelib.Environment;
import org.apache.aut.nativelib.ExecException;
import org.apache.aut.nativelib.ExecMetaData;

/**
 * A set of utility functions useful when writing CommandLaunchers.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
class ExecUtil
{
    /**
     * The file representing the current working directory.
     */
    private final static File c_cwd;

    static
    {
        try
        {
            c_cwd = ( new File( "." ) ).getCanonicalFile();
        }
        catch( final IOException ioe )
        {
            //Should never happen
            throw new IllegalStateException();
        }
    }

    /**
     * Private constructor to block instantiation.
     */
    private ExecUtil()
    {
    }

    /**
     * Create a new ExecMetaData representing the same command with the specified
     * prefix. This is useful when you are launching the native executable via a
     * script of some sort.
     */
    protected static ExecMetaData prepend( final ExecMetaData metaData,
                                           final String[] prefix )
    {
        final String[] original = metaData.getCommand();
        final String[] command = new String[ original.length + prefix.length ];

        System.arraycopy( prefix, 0, command, 0, prefix.length );
        System.arraycopy( original, 0, command, prefix.length, original.length );

        return new ExecMetaData( command,
                                 metaData.getEnvironment(),
                                 metaData.getWorkingDirectory(),
                                 metaData.isEnvironmentAdditive() );
    }

    /**
     * Utility method to check if specified file is equal
     * to the current working directory.
     */
    protected static boolean isCwd( final File file )
        throws IOException
    {
        return file.getCanonicalFile().equals( getCwd() );
    }

    private static String[] toNativeEnvironment( final Properties environment )
        throws ExecException
    {
        if( null == environment )
        {
            return null;
        }
        else
        {
            final ArrayList newEnvironment = new ArrayList();

            final Iterator keys = environment.keySet().iterator();
            while( keys.hasNext() )
            {
                final String key = (String)keys.next();
                final String value = environment.getProperty( key );
                newEnvironment.add( key + '=' + value );
            }

            return (String[])newEnvironment.toArray( new String[ newEnvironment.size() ] );
        }
    }

    /**
     * Return the current working directory of the JVM.
     * This value is initialized when this class is first loaded.
     */
    protected static File getCwd()
    {
        return c_cwd;
    }

    /**
     * Get the native environment according to proper rules.
     * Return null if no environment specified, return environment combined
     * with native environment if environment data is additive else just return
     * converted environment data.
     */
    protected static String[] getEnvironmentSpec( final ExecMetaData metaData )
        throws ExecException, IOException
    {
        final Properties environment = metaData.getEnvironment();
        if( 0 == environment.size() )
        {
            return null;
        }
        else
        {
            if( metaData.isEnvironmentAdditive() )
            {
                final Properties newEnvironment = new Properties();
                newEnvironment.putAll( Environment.getNativeEnvironment() );
                newEnvironment.putAll( environment );
                return toNativeEnvironment( newEnvironment );
            }
            else
            {
                return toNativeEnvironment( environment );
            }
        }
    }
}
