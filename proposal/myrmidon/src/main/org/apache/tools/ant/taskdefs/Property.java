/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Iterator;
import java.util.Properties;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.exec.Environment;
import org.apache.myrmidon.framework.exec.ExecException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;

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
    private String m_env;
    private File m_file;

    private String m_name;
    private Reference m_ref;
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

    public void setEnvironment( String env )
    {
        m_env = env;
    }

    public void setFile( File file )
    {
        m_file = file;
    }

    public void setLocation( File location )
    {
        setValue( location.getAbsolutePath() );
    }

    public void setName( String name )
    {
        m_name = name;
    }

    public void setRefid( Reference ref )
    {
        m_ref = ref;
    }

    public void setResource( String resource )
    {
        m_resource = resource;
    }

    public void setValue( String value )
    {
        m_value = value;
    }

    public String getEnvironment()
    {
        return m_env;
    }

    public File getFile()
    {
        return m_file;
    }

    public Reference getRefid()
    {
        return m_ref;
    }

    public String getResource()
    {
        return m_resource;
    }

    public String getValue()
    {
        return m_value;
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
        if( m_name != null )
        {
            if( m_value == null && m_ref == null )
            {
                throw new TaskException( "You must specify value, location or refid with the name attribute" );
            }
        }
        else
        {
            if( m_file == null && m_resource == null && m_env == null )
            {
                throw new TaskException( "You must specify file, resource or environment when not using the name attribute" );
            }
        }

        if( ( m_name != null ) && ( m_value != null ) )
        {
            setProperty( m_name, m_value );
        }

        if( m_file != null )
            loadFile( m_file );

        if( m_resource != null )
            loadResource( m_resource );

        if( m_env != null )
            loadEnvironment( m_env );

        if( ( m_name != null ) && ( m_ref != null ) )
        {
            Object obj = m_ref.getReferencedObject( getProject() );
            if( obj != null )
            {
                setProperty( m_name, obj.toString() );
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

    protected void loadEnvironment( String prefix )
        throws TaskException
    {
        final Properties props = new Properties();
        if( !prefix.endsWith( "." ) )
            prefix += ".";

        getLogger().debug( "Loading EnvironmentData " + prefix );
        try
        {
            final Properties environment = Environment.getNativeEnvironment();
            for( Iterator e = environment.keySet().iterator(); e.hasNext(); )
            {
                final String key = (String)e.next();
                final String value = environment.getProperty( key );

                if( value.equals( "" ) )
                {
                    getLogger().warn( "Ignoring: " + key );
                }
                else
                {
                    props.put( prefix + key, value );
                }
            }
        }
        catch( final ExecException ee )
        {
            throw new TaskException( ee.getMessage(), ee );
        }
        catch( final IOException ioe )
        {
            throw new TaskException( ioe.getMessage(), ioe );
        }

        addProperties( props );
    }

    protected void loadFile( File file )
        throws TaskException
    {
        Properties props = new Properties();
        getLogger().debug( "Loading " + file.getAbsolutePath() );
        try
        {
            if( file.exists() )
            {
                FileInputStream fis = new FileInputStream( file );
                try
                {
                    props.load( fis );
                }
                finally
                {
                    if( fis != null )
                    {
                        fis.close();
                    }
                }
                addProperties( props );
            }
            else
            {
                getLogger().debug( "Unable to find property file: " + file.getAbsolutePath() );
            }
        }
        catch( IOException ex )
        {
            throw new TaskException( "Error", ex );
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
