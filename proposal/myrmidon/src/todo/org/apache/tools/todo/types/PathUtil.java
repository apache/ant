/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.todo.types;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Locale;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.file.Path;
import org.apache.myrmidon.framework.file.FileList;
import org.apache.myrmidon.interfaces.classloader.ClassLoaderManager;
import org.apache.myrmidon.interfaces.classloader.ClassLoaderException;
import org.apache.aut.nativelib.Os;

/**
 * Utilities for operating on Path objects.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public class PathUtil
{
    /**
     * Formats a path into its native representation.
     */
    public static String formatPath( final String[] path )
    {
        // empty path return empty string
        if( path.length == 0 )
        {
            return "";
        }

        // path containing one or more elements
        final StringBuffer result = new StringBuffer( path[ 0 ].toString() );
        for( int i = 1; i < path.length; i++ )
        {
            result.append( File.pathSeparatorChar );
            result.append( path[ i ] );
        }

        return result.toString();
    }

    /**
     * Formats a path into its native representation.
     */
    public static String formatPath( final File[] path )
    {
        // empty path return empty string
        if( path.length == 0 )
        {
            return "";
        }

        // path containing one or more elements
        final StringBuffer result = new StringBuffer( path[ 0 ].toString() );
        for( int i = 1; i < path.length; i++ )
        {
            result.append( File.pathSeparatorChar );
            result.append( path[ i ].getAbsolutePath() );
        }

        return result.toString();
    }

    /**
     * Formats a path into its native representation.
     */
    public static String formatPath( final FileList path, final TaskContext context )
        throws TaskException
    {
        final String[] list = path.listFiles( context );
        return formatPath( list );
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

    /**
     * Adds the contents of a set of directories to a path.
     */
    public static void addExtdirs( final Path toPath,
                                   final Path extDirs,
                                   final TaskContext context )
        throws TaskException
    {
        final String[] dirs = extDirs.listFiles( context );
        for( int i = 0; i < dirs.length; i++ )
        {
            final File dir = new File( dirs[ i ] );
            if( dir.exists() && dir.isDirectory() )
            {
                final FileSet fileSet = new FileSet();
                fileSet.setDir( dir );
                fileSet.setIncludes( "*" );
                toPath.addFileset( fileSet );
            }
        }
    }
}
