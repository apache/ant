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

/**
 * Utilities for operating on Path objects.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public class PathUtil
{
    /**
     * Formats a Path into its native representation.
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
     * Formats a Path into its native representation.
     */
    public static String formatPath( final Path path, final TaskContext context )
        throws TaskException
    {
        final String[] list = path.listFiles( context );
        return formatPath( list );
    }

    /**
     * Returns an array of URLs - useful for building a ClassLoader.
     */
    public static URL[] toURLs( final Path path, final TaskContext context )
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
     * Adds the JVM's runtime to a path.
     */
    public static void addJavaRuntime( final Path path )
        throws TaskException
    {
        if( System.getProperty( "java.vendor" ).toLowerCase( Locale.US ).indexOf( "microsoft" ) >= 0 )
        {
            // Pull in *.zip from packages directory
            FileSet msZipFiles = new FileSet();
            msZipFiles.setDir( new File( System.getProperty( "java.home" ) + File.separator + "Packages" ) );
            msZipFiles.setIncludes( "*.ZIP" );
            path.addFileset( msZipFiles );
        }
        else if( "Kaffe".equals( System.getProperty( "java.vm.name" ) ) )
        {
            FileSet kaffeJarFiles = new FileSet();
            kaffeJarFiles.setDir( new File( System.getProperty( "java.home" )
                                            + File.separator + "share"
                                            + File.separator + "kaffe" ) );

            kaffeJarFiles.setIncludes( "*.jar" );
            path.addFileset( kaffeJarFiles );
        }
        else
        {
            // JDK > 1.1 seems to set java.home to the JRE directory.
            final String rt = System.getProperty( "java.home" ) +
                File.separator + "lib" + File.separator + "rt.jar";
            path.addLocation( new File( rt ) );
            // Just keep the old version as well and let addExisting
            // sort it out.
            final String rt2 = System.getProperty( "java.home" ) +
                File.separator + "jre" + File.separator + "lib" +
                File.separator + "rt.jar";
            path.addLocation( new File( rt2 ) );

            // Added for MacOS X
            final String classes = System.getProperty( "java.home" ) +
                File.separator + ".." + File.separator + "Classes" +
                File.separator + "classes.jar";
            path.addLocation( new File( classes ) );
            final String ui = System.getProperty( "java.home" ) +
                File.separator + ".." + File.separator + "Classes" +
                File.separator + "ui.jar";
            path.addLocation( new File( ui ) );
        }
    }

    /**
     * Adds the contents of a set of directories to a path.
     */
    public static void addExtdirs( final Path toPath, final Path extDirs, TaskContext context )
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
