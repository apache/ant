/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant;
import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * This is the Ant command line front end to end. This front end works out where
 * ant is installed and loads the ant libraries before starting Ant proper.
 *
 * @author <a href="mailto:conor@apache.org">Conor MacNeill</a>
 */
public class Launcher
{

    public static void main( String[] args )
    {
        File antHome = null;
        ClassLoader systemClassLoader = Launcher.class.getClassLoader();
        if( systemClassLoader == null )
        {
            antHome = determineAntHome11();
        }
        else
        {
            antHome = determineAntHome( systemClassLoader );
        }
        if( antHome == null )
        {
            System.err.println( "Unable to determine ANT_HOME" );
            System.exit( 1 );
        }

        System.out.println( "ANT_HOME is " + antHome );

        // We now create the class loader with which we are going to launch ant
        AntClassLoader antLoader = new AntClassLoader( systemClassLoader, false );

        // need to find tools.jar
        addToolsJar( antLoader );

        // add everything in the lib directory to this classloader
        File libDir = new File( antHome, "lib" );
        addDirJars( antLoader, libDir );

        File optionalDir = new File( antHome, "lib/optional" );
        addDirJars( antLoader, optionalDir );

        Properties launchProperties = new Properties();
        launchProperties.put( "ant.home", antHome.getAbsolutePath() );

        try
        {
            Class mainClass = antLoader.loadClass( "org.apache.tools.ant.Main" );
            antLoader.initializeClass( mainClass );

            final Class[] param = {Class.forName( "[Ljava.lang.String;" ),
                Properties.class, ClassLoader.class};
            final Method startMethod = mainClass.getMethod( "start", param );
            final Object[] argument = {args, launchProperties, systemClassLoader};
            startMethod.invoke( null, argument );
        }
        catch( Exception e )
        {
            System.out.println( "Exception running Ant: " + e.getClass().getName() + ": " + e.getMessage() );
            e.printStackTrace();
        }
    }

    private static void addDirJars( AntClassLoader classLoader, File jarDir )
    {
        String[] fileList = jarDir.list(
            new FilenameFilter()
            {
                public boolean accept( File dir, String name )
                {
                    return name.endsWith( ".jar" );
                }
            } );

        if( fileList != null )
        {
            for( int i = 0; i < fileList.length; ++i )
            {
                File jarFile = new File( jarDir, fileList[i] );
                classLoader.addPathElement( jarFile.getAbsolutePath() );
            }
        }
    }

    private static void addToolsJar( AntClassLoader antLoader )
    {
        String javaHome = System.getProperty( "java.home" );
        if( javaHome.endsWith( "jre" ) )
        {
            javaHome = javaHome.substring( 0, javaHome.length() - 4 );
        }
        System.out.println( "Java home is " + javaHome );
        File toolsJar = new File( javaHome, "lib/tools.jar" );
        if( !toolsJar.exists() )
        {
            System.out.println( "Unable to find tools.jar at " + toolsJar.getPath() );
        }
        else
        {
            antLoader.addPathElement( toolsJar.getAbsolutePath() );
        }
    }

    private static File determineAntHome( ClassLoader systemClassLoader )
    {
        try
        {
            String className = Launcher.class.getName().replace( '.', '/' ) + ".class";
            URL classResource = systemClassLoader.getResource( className );
            String fileComponent = classResource.getFile();
            if( classResource.getProtocol().equals( "file" ) )
            {
                // Class comes from a directory of class files rather than
                // from a jar.
                int classFileIndex = fileComponent.lastIndexOf( className );
                if( classFileIndex != -1 )
                {
                    fileComponent = fileComponent.substring( 0, classFileIndex );
                }
                File classFilesDir = new File( fileComponent );
                File buildDir = new File( classFilesDir.getParent() );
                File devAntHome = new File( buildDir.getParent() );
                return devAntHome;
            }
            else if( classResource.getProtocol().equals( "jar" ) )
            {
                // Class is coming from a jar. The file component of the URL
                // is actually the URL of the jar file
                int classSeparatorIndex = fileComponent.lastIndexOf( "!" );
                if( classSeparatorIndex != -1 )
                {
                    fileComponent = fileComponent.substring( 0, classSeparatorIndex );
                }
                URL antJarURL = new URL( fileComponent );
                File antJarFile = new File( antJarURL.getFile() );
                File libDirectory = new File( antJarFile.getParent() );
                File antHome = new File( libDirectory.getParent() );
                return antHome;
            }
        }
        catch( MalformedURLException e )
        {
            e.printStackTrace();
        }
        return null;
    }

    private static File determineAntHome11()
    {
        String classpath = System.getProperty( "java.class.path" );
        StringTokenizer tokenizer = new StringTokenizer( classpath, System.getProperty( "path.separator" ) );
        while( tokenizer.hasMoreTokens() )
        {
            String path = tokenizer.nextToken();
            if( path.endsWith( "ant.jar" ) )
            {
                File antJarFile = new File( path );
                File libDirectory = new File( antJarFile.getParent() );
                File antHome = new File( libDirectory.getParent() );
                return antHome;
            }
        }
        return null;
    }
}

