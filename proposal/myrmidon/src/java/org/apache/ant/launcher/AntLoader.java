/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.launcher;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.StringTokenizer;

/**
 * Basic Loader that is responsible for all the hackery to get classloader to work.
 * Other classes can call AntLoader.getLoader() and add to their own classloader.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 * @author <a href="mailto:mpfoemme@thoughtworks.com">Matthew Foemmel</a>
 */
public final class AntLoader
{
    /**
     * Magic entry point.
     *
     * @param argsthe CLI arguments
     * @exception Exception if an error occurs
     */
    public final static void main( final String[] args ) 
        throws Exception
    {        
        try
        {
            //actually try to discover the install directory based on where
            // the ant.jar is
            final File installDirectory = findInstallDir();
            System.setProperty( "ant.home", installDirectory.toString() );

            //setup classloader appropriately for myrmidon jar
            final File archive = 
                new File( installDirectory, "lib" + File.separator + "myrmidon.jar" );
            final AntClassLoader classLoader = 
                new AntClassLoader( new URL[] { archive.toURL() } );

            //load class and retrieve appropriate main method.
            final Class clazz = classLoader.loadClass( "org.apache.ant.Main" );
            final Method method = clazz.getMethod( "main", new Class[] { args.getClass() } );
            
            //kick the tires and light the fires....
            method.invoke( null, new Object[] { args } );
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
     *  Finds the ant.jar file in the classpath.
     */
    protected final static File findInstallDir() 
        throws Exception
    {
        final String classpath = System.getProperty( "java.class.path" );
        final String pathSeparator = System.getProperty( "path.separator" );
        final StringTokenizer tokenizer = new StringTokenizer( classpath, pathSeparator );
        
        while( tokenizer.hasMoreTokens() )
        {
            final String element = tokenizer.nextToken();

            if( element.endsWith( "ant.jar" ) )
            {
                File file = (new File( element )).getAbsoluteFile();
                file = file.getParentFile();
                
                if( null != file )
                {
                    file = file.getParentFile();
                }

                return file;                
            }
        }
        
        throw new Exception( "Unable to locate ant.jar in classpath" );
    }
}
