/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.framework.exec.launchers;

import java.io.File;
import java.io.IOException;
import org.apache.myrmidon.framework.exec.ExecMetaData;

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
    private static final File c_cwd;

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

    /**
     * Return the current working directory of the JVM.
     * This value is initialized when this class is first loaded.
     */
    protected static File getCwd()
    {
        return c_cwd;
    }
}
