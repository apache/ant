/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.myrmidon.interfaces.type;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;

/**
 * Create a type instance based on name.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version CVS $Revision$ $Date$
 */
public class DefaultTypeFactory
    implements TypeFactory
{
    private static final Resources REZ =
        ResourceManager.getPackageResources( DefaultTypeFactory.class );

    ///A Map of shortnames to classnames
    private final HashMap m_classNames = new HashMap();

    ///A list of URLs from which classLoader is constructed
    private final URL[] m_urls;

    ///The parent classLoader (if any)
    private final ClassLoader m_parent;

    ///The parent classLoader (if any)
    private ClassLoader m_classLoader;

    public DefaultTypeFactory( final URL url )
    {
        this( new URL[]{url} );
    }

    public DefaultTypeFactory( final URL[] urls )
    {
        this( urls, Thread.currentThread().getContextClassLoader() );
    }

    public DefaultTypeFactory( final URL[] urls, final ClassLoader parent )
    {
        m_urls = urls;
        m_parent = parent;
    }

    public DefaultTypeFactory( final ClassLoader classLoader )
    {
        this( null, null );
        m_classLoader = classLoader;
    }

    public void addNameClassMapping( final String name, final String className )
    {
        m_classNames.put( name, className );
    }

    /**
     * Create a type instance with appropriate name.
     *
     * @param name the name
     * @return the created instance
     * @exception TypeException if an error occurs
     */
    public Object create( final String name )
        throws TypeException
    {
        final String className = getClassName( name );

        try
        {
            return getClassLoader().loadClass( className ).newInstance();
        }
        catch( final Exception e )
        {
            final String message = REZ.getString( "no-instantiate.error", name );
            throw new TypeException( message, e );
        }
    }

    private String getClassName( final String name )
        throws TypeException
    {
        final String className = (String)m_classNames.get( name );

        if( null == className )
        {
            final String message = REZ.getString( "no-mapping.error", name );
            throw new TypeException( message );
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
