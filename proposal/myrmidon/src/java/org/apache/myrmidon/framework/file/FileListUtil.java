/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.framework.file;

import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.interfaces.classloader.ClassLoaderManager;
import org.apache.myrmidon.interfaces.classloader.ClassLoaderException;
import org.apache.aut.nativelib.PathUtil;
import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * Utility methods for dealing with {@link FileList} objects.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
public final class FileListUtil
{
    private FileListUtil()
    {
    }

    /**
     * Formats a path into its native representation.
     */
    public static String formatPath( final FileList path, final TaskContext context )
        throws TaskException
    {
        final String[] list = path.listFiles( context );
        return PathUtil.formatPath( list );
    }

    /**
     * Converts a path into an array of Files.
     */
    public static File[] toFiles( final FileList path, final TaskContext context )
        throws TaskException
    {
        final String[] list = path.listFiles( context );
        final File[] result = new File[ list.length ];
        for( int i = 0; i < list.length; i++ )
        {
            result[ i ] = new File( list[ i ] );
        }
        return result;
    }

    /**
     * Converts a path into an array of URLs - useful for building a ClassLoader.
     */
    public static URL[] toURLs( final FileList path, final TaskContext context )
        throws TaskException
    {
        try
        {
            final String[] list = path.listFiles( context );

            final URL[] result = new URL[ list.length ];

            // path containing one or more elements
            for( int i = 0; i < list.length; i++ )
            {
                result[ i ] = new File( list[ i ] ).toURL();
            }

            return result;
        }
        catch( final IOException ioe )
        {
            throw new TaskException( "Malformed path entry.", ioe );
        }
    }

    /**
     * Creates a ClassLoader from a class-path.
     */
    public static ClassLoader createClassLoader( final FileList classpath,
                                                 final TaskContext context )
        throws TaskException
    {
        final File[] files = toFiles( classpath, context );
        final ClassLoaderManager manager = (ClassLoaderManager)context.getService( ClassLoaderManager.class );
        try
        {
            return manager.createClassLoader( files );
        }
        catch( final ClassLoaderException e )
        {
            throw new TaskException( e.getMessage(), e );
        }
    }

}
