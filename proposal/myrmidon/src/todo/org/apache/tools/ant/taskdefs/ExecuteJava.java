/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.CommandlineJava;
import org.apache.tools.ant.types.Path;

/*
 * @author thomas.haas@softwired-inc.com
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 */

public class ExecuteJava
{

    private Commandline javaCommand = null;
    private Path classpath = null;
    private CommandlineJava.SysProperties sysProperties = null;

    public void setClasspath( Path p )
    {
        classpath = p;
    }

    public void setJavaCommand( Commandline javaCommand )
    {
        this.javaCommand = javaCommand;
    }

    public void setSystemProperties( CommandlineJava.SysProperties s )
    {
        sysProperties = s;
    }

    public void execute( Project project )
        throws TaskException
    {
        final String classname = javaCommand.getExecutable();
        final Object[] argument = {javaCommand.getArguments()};

        AntClassLoader loader = null;
        try
        {
            if( sysProperties != null )
            {
                sysProperties.setSystem();
            }

            final Class[] param = {Class.forName( "[Ljava.lang.String;" )};
            Class target = null;
            if( classpath == null )
            {
                target = Class.forName( classname );
            }
            else
            {
                loader = new AntClassLoader( Project.class.getClassLoader(), project, classpath, false );
                loader.setIsolated( true );
                loader.setThreadContextLoader();
                target = loader.forceLoadClass( classname );
                AntClassLoader.initializeClass( target );
            }
            final Method main = target.getMethod( "main", param );
            main.invoke( null, argument );
        }
        catch( NullPointerException e )
        {
            throw new TaskException( "Could not find main() method in " + classname );
        }
        catch( ClassNotFoundException e )
        {
            throw new TaskException( "Could not find " + classname + ". Make sure you have it in your classpath" );
        }
        catch( InvocationTargetException e )
        {
            Throwable t = e.getTargetException();
            if( !( t instanceof SecurityException ) )
            {
                throw new TaskException( "Error", t );
            }
            else
            {
                throw (SecurityException)t;
            }
        }
        catch( Exception e )
        {
            throw new TaskException( "Error", e );
        }
        finally
        {
            if( loader != null )
            {
                loader.resetThreadContextLoader();
                loader.cleanup();
            }
            if( sysProperties != null )
            {
                sysProperties.restoreSystem();
            }
        }
    }
}
