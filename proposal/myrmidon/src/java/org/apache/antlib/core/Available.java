/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.core;

import java.net.URL;
import java.net.URLClassLoader;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.conditions.Condition;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.PathUtil;

/**
 * A condition that evaluates to true if the requested class or resource
 * is available at runtime.
 *
 * @author Stefano Mazzocchi <a href="mailto:stefano@apache.org">
 *      stefano@apache.org</a>
 * @author <a href="mailto:umagesh@apache.org">Magesh Umasankar</a>
 *
 * @ant:type type="condition" name="available"
 */
public class Available
    implements Condition
{
    private String m_classname;
    private Path m_classpath;
    private ClassLoader m_classLoader;
    private String m_resource;

    /**
     * Sets the name of the class to search for.
     */
    public void setClassname( final String classname )
    {
        if( !"".equals( classname ) )
        {
            m_classname = classname;
        }
    }

    /**
     * Adds a classpath element.
     */
    public void addClasspath( final Path classpath )
        throws TaskException
    {
        if( m_classpath == null )
        {
            m_classpath = classpath;
        }
        else
        {
            m_classpath.addPath( classpath );
        }
    }

    /**
     * Sets the name of the resource to look for.
     */
    public void setResource( final String resource )
    {
        m_resource = resource;
    }

    /**
     * Evaluates the condition.
     */
    public boolean evaluate( final TaskContext context )
        throws TaskException
    {
        if( m_classname == null && m_resource == null )
        {
            throw new TaskException( "At least one of (classname|file|resource) is required" );
        }

        if( m_classpath != null )
        {
            final URL[] urls = PathUtil.toURLs( m_classpath );
            m_classLoader = new URLClassLoader( urls );
        }

        if( ( m_classname != null ) && !checkClass( m_classname ) )
        {
            return false;
        }

        if( ( m_resource != null ) && !checkResource( m_resource ) )
        {
            return false;
        }

        return true;
    }

    private boolean checkClass( String classname )
    {
        try
        {
            final ClassLoader classLoader = getClassLoader();
            classLoader.loadClass( classname );
            return true;
        }
        catch( ClassNotFoundException e )
        {
            return false;
        }
        catch( NoClassDefFoundError e )
        {
            return false;
        }
    }

    private boolean checkResource( String resource )
    {
        final ClassLoader classLoader = getClassLoader();
        return ( null != classLoader.getResourceAsStream( resource ) );
    }

    private ClassLoader getClassLoader()
    {
        if( null == m_classLoader )
        {
            return ClassLoader.getSystemClassLoader();
        }
        else
        {
            return m_classLoader;
        }
    }
}
