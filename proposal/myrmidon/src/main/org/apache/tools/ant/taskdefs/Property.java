/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Iterator;
import java.util.Properties;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Path;

/**
 * Will set a Project property. Used to be a hack in ProjectHelper Will not
 * override values set by the command line or parent projects.
 *
 * @author costin@dnt.ro
 * @author <a href="mailto:rubys@us.ibm.com">Sam Ruby</a>
 * @author <a href="mailto:glennm@ca.ibm.com">Glenn McAllister</a>
 */
public class Property
    extends Task
{
    private Path m_classpath;

    private String m_name;
    private String m_resource;
    private String m_value;

    public void setClasspath( Path classpath )
        throws TaskException
    {
        if( m_classpath == null )
        {
            m_classpath = classpath;
        }
        else
        {
            m_classpath.append( classpath );
        }
    }

    public void setLocation( File location )
    {
        setValue( location.getAbsolutePath() );
    }

    public void setName( String name )
    {
        m_name = name;
    }

    public void setResource( String resource )
    {
        m_resource = resource;
    }

    public void setValue( String value )
    {
        m_value = value;
    }

    public Path createClasspath()
        throws TaskException
    {
        if( m_classpath == null )
        {
            m_classpath = new Path();
        }
        return m_classpath.createPath();
    }

    public void execute()
        throws TaskException
    {
        validate();

        if( ( m_name != null ) && ( m_value != null ) )
        {
            setProperty( m_name, m_value );
        }

        if( m_resource != null )
            loadResource( m_resource );
    }

    private void validate() throws TaskException
    {
        if( m_name != null )
        {
            if( m_value == null )
            {
                throw new TaskException( "You must specify value, location or refid with the name attribute" );
            }
        }
        else
        {
            if( m_resource == null )
            {
                throw new TaskException( "You must specify resource when not using the name attribute" );
            }
        }
    }

    public String toString()
    {
        return m_value == null ? "" : m_value;
    }

    protected void addProperties( Properties props )
        throws TaskException
    {
        final Iterator e = props.keySet().iterator();
        while( e.hasNext() )
        {
            final String name = (String)e.next();
            final String value = (String)props.getProperty( name );
            setProperty( name, value );
        }
    }

    protected void loadResource( String name )
        throws TaskException
    {
        Properties props = new Properties();
        getLogger().debug( "Resource Loading " + name );
        try
        {
            ClassLoader classLoader = null;

            if( m_classpath != null )
            {
                final URL[] urls = m_classpath.toURLs();
                classLoader = new URLClassLoader( urls );
            }
            else
            {
                classLoader = ClassLoader.getSystemClassLoader();
            }
            final InputStream is = classLoader.getResourceAsStream( name );

            if( is != null )
            {
                props.load( is );
                addProperties( props );
            }
            else
            {
                getLogger().warn( "Unable to find resource " + name );
            }
        }
        catch( IOException ex )
        {
            throw new TaskException( "Error", ex );
        }
    }
}
