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
import java.net.URL;
import java.net.URLClassLoader;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.SysProperties;

/*
 * @author thomas.haas@softwired-inc.com
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 */

public class ExecuteJava
{
    private Commandline m_javaCommand;
    private Path m_classpath;
    private SysProperties m_sysProperties;

    public void setClasspath( final Path classpath )
    {
        m_classpath = classpath;
    }

    public void setJavaCommand( final Commandline javaCommand )
    {
        m_javaCommand = javaCommand;
    }

    public void setSystemProperties( final SysProperties sysProperties )
    {
        m_sysProperties = sysProperties;
    }

    public void execute()
        throws TaskException
    {
        final String classname = m_javaCommand.getExecutable();
        final Object[] argument = new Object[]{m_javaCommand.getArguments()};

        try
        {
            if( m_sysProperties != null )
            {
                m_sysProperties.setSystem();
            }

            final Class[] param = {Class.forName( "[Ljava.lang.String;" )};
            Class target = null;
            if( m_classpath == null )
            {
                target = Class.forName( classname );
            }
            else
            {
                final URL[] urls = m_classpath.toURLs();
                final URLClassLoader classLoader = new URLClassLoader( urls );
                target = classLoader.loadClass( classname );
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
            if( m_sysProperties != null )
            {
                m_sysProperties.restoreSystem();
            }
        }
    }
}
