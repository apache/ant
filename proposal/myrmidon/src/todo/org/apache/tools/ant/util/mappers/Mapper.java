/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.util.mappers;

import java.net.URL;
import java.net.URLClassLoader;
import org.apache.myrmidon.api.TaskException;
import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.types.Path;

/**
 * Element to define a FileNameMapper.
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 */
public class Mapper
    extends ProjectComponent
    implements Cloneable
{
    private MapperType m_type;
    private String m_classname;
    private Path m_classpath;
    private String m_from;
    private String m_to;

    /**
     * Set the class name of the FileNameMapper to use.
     *
     * @param classname The new Classname value
     */
    public void setClassname( final String classname )
    {
        m_classname = classname;
    }

    /**
     * Set the classpath to load the FileNameMapper through (attribute).
     *
     * @param classpath The new Classpath value
     */
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

    /**
     * Set the argument to FileNameMapper.setFrom
     */
    public void setFrom( final String from )
    {
        m_from = from;
    }

    /**
     * Set the argument to FileNameMapper.setTo
     */
    public void setTo( final String to )
    {
        m_to = to;
    }

    /**
     * Set the type of FileNameMapper to use.
     */
    public void setType( MapperType type )
    {
        m_type = type;
    }

    /**
     * Returns a fully configured FileNameMapper implementation.
     *
     * @return The Implementation value
     * @exception TaskException Description of Exception
     */
    public FileNameMapper getImplementation()
        throws TaskException
    {
        if( m_type == null && m_classname == null )
        {
            throw new TaskException( "one of the attributes type or classname is required" );
        }

        if( m_type != null && m_classname != null )
        {
            throw new TaskException( "must not specify both type and classname attribute" );
        }

        try
        {
            if( m_type != null )
            {
                m_classname = m_type.getImplementation();
            }

            Class c = null;
            if( m_classpath == null )
            {
                c = Class.forName( m_classname );
            }
            else
            {
                final URL[] urls = m_classpath.toURLs();
                final URLClassLoader classLoader = new URLClassLoader( urls );
                c = classLoader.loadClass( m_classname );
            }

            FileNameMapper m = (FileNameMapper)c.newInstance();
            m.setFrom( m_from );
            m.setTo( m_to );
            return m;
        }
        catch( TaskException be )
        {
            throw be;
        }
        catch( Throwable t )
        {
            throw new TaskException( "Error", t );
        }
        finally
        {
            if( m_type != null )
            {
                m_classname = null;
            }
        }
    }

    /**
     * Set the classpath to load the FileNameMapper through (nested element).
     *
     * @return Description of the Returned Value
     */
    public Path createClasspath()
        throws TaskException
    {
        if( m_classpath == null )
        {
            m_classpath = new Path();
        }
        return m_classpath.createPath();
    }
}
