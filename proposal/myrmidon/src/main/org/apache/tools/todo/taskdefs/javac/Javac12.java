/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.todo.taskdefs.javac;

import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.api.TaskContext;
import org.apache.tools.todo.types.Commandline;
import org.apache.tools.todo.taskdefs.javac.DefaultCompilerAdapter;

/**
 * The implementation of the javac compiler for JDK 1.2 This is primarily a
 * cut-and-paste from the original javac task before it was refactored.
 *
 * @author James Davidson <a href="mailto:duncan@x180.com">duncan@x180.com</a>
 * @author Robin Green <a href="mailto:greenrd@hotmail.com">greenrd@hotmail.com
 *      </a>
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @author <a href="mailto:jayglanville@home.com">J D Glanville</a>
 */
public class Javac12 extends DefaultCompilerAdapter
{

    public boolean execute()
        throws TaskException
    {
        getTaskContext().debug( "Using classic compiler" );
        Commandline cmd = setupJavacCommand( true );

        try
        {
            // Create an instance of the compiler, redirecting output to
            // the project log
            Class c = Class.forName( "sun.tools.javac.Main" );
            Constructor cons = c.getConstructor( new Class[]{OutputStream.class, String.class} );
            Object compiler = cons.newInstance( new Object[]{System.out, "javac"} );

            // Call the compile() method
            Method compile = c.getMethod( "compile", new Class[]{String[].class} );
            Boolean ok = (Boolean)compile.invoke( compiler, new Object[]{cmd.getArguments()} );
            return ok.booleanValue();
        }
        catch( ClassNotFoundException ex )
        {
            throw new TaskException( "Cannot use classic compiler, as it is not available" +
                                     " A common solution is to set the environment variable" +
                                     " JAVA_HOME to your jdk directory." );
        }
        catch( Exception ex )
        {
            if( ex instanceof TaskException )
            {
                throw (TaskException)ex;
            }
            else
            {
                throw new TaskException( "Error starting classic compiler: ", ex );
            }
        }
    }
}
