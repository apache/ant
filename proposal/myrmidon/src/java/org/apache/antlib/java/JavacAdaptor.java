/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.java;

import java.io.File;
import java.lang.reflect.Method;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.file.FileListUtil;
import org.apache.myrmidon.framework.file.Path;
import org.apache.tools.todo.types.ArgumentList;

/**
 * An adaptor for the in-process Javac compiler.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 *
 * @ant.type type="java-compiler" name="javac"
 */
public class JavacAdaptor
    extends JavaCompilerAdaptor
{

    /**
     * Compiles as set of files.
     */
    protected void compile( final File[] compileList )
        throws TaskException
    {
        final ArgumentList cmd = new ArgumentList();
        setupModernJavacCommand( cmd, compileList );

        final String[] args = cmd.getArguments();

        // Use reflection to be able to build on all JDKs >= 1.2:
        final Class compilerClass;
        try
        {
            compilerClass = Class.forName( "com.sun.tools.javac.Main" );
        }
        catch( ClassNotFoundException exc )
        {
            throw new TaskException( "Could not find the javac compiler.", exc );
        }

        try
        {
            final Object compiler = compilerClass.newInstance();
            final Class[] paramTypes = new Class[] { args.getClass() };
            final Method compile = compilerClass.getMethod( "compile", paramTypes );
            final Object[] params = new Object[]{ args };
            final Integer result = (Integer)compile.invoke( compiler, params );
            if( result.intValue() != 0  )
            {
                throw new TaskException( "Javac finished with non-zero return code." );
            }
        }
        catch( final TaskException exc )
        {
            throw exc;
        }
        catch( final Exception exc )
        {
            throw new TaskException( "Could not start javac compiler", exc );
        }
    }

    /**
     * Builds the command-line to invoke the compiler with.
     */
    private void setupModernJavacCommand( final ArgumentList cmd,
                                          final File[] files )
        throws TaskException
    {
        // Build the classpath
        Path classpath = new Path();

        classpath.addLocation( getDestDir() );
        classpath.add( getClassPath() );

        cmd.addArgument( "-classpath" );
        cmd.addArgument( FileListUtil.formatPath( classpath, getContext() ) );

        if( isDeprecation() )
        {
            cmd.addArgument( "-deprecation" );
        }

        cmd.addArgument( "-d" );
        cmd.addArgument( getDestDir() );


        if( isDebug() )
        {
            cmd.addArgument( "-g" );
        }
        else
        {
            cmd.addArgument( "-g:none" );
        }

        // Add the files to compile
        for( int i = 0; i < files.length; i++ )
        {
            final File file = files[i ];
            cmd.addArgument( file );
        }
    }
}
