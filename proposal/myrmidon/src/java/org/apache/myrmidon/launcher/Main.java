/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.launcher;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.Map;
import java.util.HashMap;

/**
 * Basic Loader that is responsible for all the hackery to get classloader to work.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public final class Main
{
    /**
     * Magic entry point.
     *
     * @param args the CLI arguments
     * @exception Exception if an error occurs
     */
    public static final void main( final String[] args )
        throws Exception
    {
        try
        {
            final Map properties = new HashMap();

            // Try to discover the install directory based on where the
            // launcher Jar is
            final File installDirectory = findInstallDir();
            properties.put( "myrmidon.home", installDirectory );

            // Build the shared classloader
            final URL[] sharedClassPath = getSharedClassPath( installDirectory );
            properties.put( "myrmidon.shared.classpath", sharedClassPath );
            final URLClassLoader sharedClassLoader = new URLClassLoader( sharedClassPath );
            properties.put( "myrmidon.shared.classloader", sharedClassLoader );

            // Build the container classloader
            final URL[] containerClassPath = getContainerClassPath( installDirectory );
            properties.put( "myrmidon.container.classpath", containerClassPath );
            final URLClassLoader containerClassLoader = new URLClassLoader( containerClassPath, sharedClassLoader );
            properties.put( "myrmidon.container.classloader", containerClassLoader );

            execMainClass( containerClassLoader, properties, args );
        }
        catch( final InvocationTargetException ite )
        {
            System.err.println( "Error: " + ite.getTargetException().getMessage() );
            ite.getTargetException().printStackTrace();
        }
        catch( final Throwable throwable )
        {
            System.err.println( "Error: " + throwable.getMessage() );
            throwable.printStackTrace();
        }
    }

    /**
     * Executes the main class.
     */
    private static void execMainClass( final ClassLoader classLoader,
                                       final Map properties,
                                       final String[] args )
        throws Exception
    {
        //load class and retrieve appropriate main method.
        final Class clazz = classLoader.loadClass( "org.apache.myrmidon.frontends.CLIMain" );
        final Method method = clazz.getMethod( "main", new Class[]{Map.class, args.getClass()} );

        //kick the tires and light the fires....
        method.invoke( null, new Object[]{properties, args} );
    }

    /**
     * Builds the classpath for the container classloader.
     */
    private static URL[] getContainerClassPath( final File installDirectory )
        throws Exception
    {
        // Include everything from the bin/lib/ directory
        final File containerLibDir = new File( installDirectory, "bin" + File.separator + "lib" );
        return buildURLList( containerLibDir );
    }

    /**
     * Builds the classpath for the shared classloader.
     */
    private static URL[] getSharedClassPath( final File installDirectory )
        throws Exception
    {
        // Include everything from the lib/ directory
        final File libDir = new File( installDirectory, "lib" );
        return buildURLList( libDir );
    }

    private static final URL[] buildURLList( final File dir )
        throws Exception
    {
        final ArrayList urlList = new ArrayList();

        final File[] contents = dir.listFiles();

        if( null == contents )
        {
            return new URL[ 0 ];
        }

        for( int i = 0; i < contents.length; i++ )
        {
            File file = contents[ i ];

            if( !file.isFile() || !file.canRead() )
            {
                //ignore non-files or unreadable files
                continue;
            }

            final String name = file.getName();
            if( !name.endsWith( ".jar" ) && !name.endsWith( ".zip" ) )
            {
                //Ignore files in lib dir that are not jars or zips
                continue;
            }

            file = file.getCanonicalFile();

            urlList.add( file.toURL() );
        }

        return (URL[])urlList.toArray( new URL[ 0 ] );
    }

    /**
     *  Finds the myrmidon.jar file in the classpath.
     */
    private static final File findInstallDir()
        throws Exception
    {
        final String classpath = System.getProperty( "java.class.path" );
        final String pathSeparator = System.getProperty( "path.separator" );
        final StringTokenizer tokenizer = new StringTokenizer( classpath, pathSeparator );
        final String jarName = "myrmidon-launcher.jar";

        while( tokenizer.hasMoreTokens() )
        {
            final String element = tokenizer.nextToken();
            File file = ( new File( element ) ).getAbsoluteFile();
            if( file.isFile() && file.getName().equals( jarName ) )
            {
                file = file.getParentFile();
                if( null != file )
                {
                    file = file.getParentFile();
                }
                return file;
            }
        }

        throw new Exception( "Unable to locate " + jarName + " in classpath." );
    }
}
