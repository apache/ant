/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.myrmidon.components.type;

import java.net.URL;
import java.util.HashMap;
import java.net.URLClassLoader;
import org.apache.avalon.framework.component.Component;
import org.apache.avalon.framework.component.ComponentException;

/**
 * Create a component based on name.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 * @version CVS $Revision$ $Date$
 */
public class DefaultComponentFactory
    implements ComponentFactory
{
    ///A Map of shortnames to classnames
    private final HashMap        m_classNames = new HashMap();

    ///A list of URLs from which classLoader is constructed
    private final URL[]          m_urls;

    ///The parent classLoader (if any)
    private final ClassLoader    m_parent;

    ///The parent classLoader (if any)
    private ClassLoader          m_classLoader;

    public DefaultComponentFactory( final URL[] urls )
    {
        this( urls, Thread.currentThread().getContextClassLoader() );
    }

    public DefaultComponentFactory( final URL[] urls, final ClassLoader parent )
    {
        m_urls = urls;
        m_parent = parent;
    }

    public DefaultComponentFactory( final ClassLoader classLoader )
    {
        this( null, null );
        m_classLoader = classLoader;
    }

    public void addNameClassMapping( final String name, final String className )
    {
        m_classNames.put( name, className );
    }

    /**
     * Create a Component with appropriate name.
     *
     * @param name the name
     * @return the created component
     * @exception ComponentException if an error occurs
     */
    public Component create( final String name )
        throws ComponentException
    {
        final String className = getClassName( name );

        try
        {
            return (Component)getClassLoader().loadClass( className ).newInstance();
        }
        catch( final Exception e )
        {
            throw new ComponentException( "Unable to instantiate '" + name + "'", e );
        }
    }

    private String getClassName( final String name )
        throws ComponentException
    {
        final String className = (String)m_classNames.get( name );

        if( null == className )
        {
            throw new ComponentException( "Malconfigured factory, no clasname for '" + 
                                          name + "'" );
        }
        
        return className;
    }

    private ClassLoader getClassLoader()
    {
        if( null == m_classLoader )
        {
            m_classLoader = new URLClassLoader( m_urls, m_parent );
        }
        
        return m_classLoader;
    }
}
