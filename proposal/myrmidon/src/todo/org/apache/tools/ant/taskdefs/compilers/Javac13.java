/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.taskdefs.compilers;
import java.lang.reflect.Method;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Commandline;


/**
 * The implementation of the javac compiler for JDK 1.3 This is primarily a
 * cut-and-paste from the original javac task before it was refactored.
 *
 * @author James Davidson <a href="mailto:duncan@x180.com">duncan@x180.com</a>
 * @author Robin Green <a href="mailto:greenrd@hotmail.com">greenrd@hotmail.com
 *      </a>
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @author <a href="mailto:jayglanville@home.com">J D Glanville</a>
 */
public class Javac13 extends DefaultCompilerAdapter
{

    /**
     * Integer returned by the "Modern" jdk1.3 compiler to indicate success.
     */
    private final static int MODERN_COMPILER_SUCCESS = 0;

    public boolean execute()
        throws BuildException
    {
        attributes.log( "Using modern compiler", Project.MSG_VERBOSE );
        Commandline cmd = setupModernJavacCommand();

        // Use reflection to be able to build on all JDKs >= 1.1:
        try
        {
            Class c = Class.forName( "com.sun.tools.javac.Main" );
            Object compiler = c.newInstance();
            Method compile = c.getMethod( "compile",
                new Class[]{( new String[]{} ).getClass()} );
            int result = ( ( Integer )compile.invoke
                ( compiler, new Object[]{cmd.getArguments()} ) ).intValue();
            return ( result == MODERN_COMPILER_SUCCESS );
        }
        catch( Exception ex )
        {
            if( ex instanceof BuildException )
            {
                throw ( BuildException )ex;
            }
            else
            {
                throw new BuildException( "Error starting modern compiler", ex );
            }
        }
    }
}
