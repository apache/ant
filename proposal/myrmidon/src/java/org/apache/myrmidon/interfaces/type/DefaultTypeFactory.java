/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.interfaces.type;

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

    ///The parent classLoader (if any)
    private ClassLoader m_classLoader;

    /**
     * Construct a factory that uses specified ClassLoader to load
     * types from.
     */
    public DefaultTypeFactory( final ClassLoader classLoader )
    {
        if( null == classLoader )
        {
            throw new NullPointerException( "classLoader" );
        }

        m_classLoader = classLoader;
    }

    /**
     * No arg constructor used by subclasses who wish to overide getClassLoader().
     */
    protected DefaultTypeFactory()
    {
    }

    /**
     * Map a name to the fully qualified name of the Class that implements type.
     */
    public void addNameClassMapping( final String name, final String className )
    {
        m_classNames.put( name, className );
    }

    /**
     * Determines if this factory can create instances of a particular type.
     */
    public boolean canCreate( String name )
    {
        return ( getClassName( name ) != null );
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
        // Determine the name of the class to instantiate
        final String className = getClassName( name );
        if( null == className )
        {
            final String message = REZ.getString( "no-mapping.error", name );
            throw new TypeException( message );
        }

        // Instantiate the object
        try
        {
            final ClassLoader classLoader = getClassLoader();
            final Class clazz = classLoader.loadClass( className );
            return clazz.newInstance();
        }
        catch( final Exception e )
        {
            final String message = REZ.getString( "no-instantiate.error", name );
            throw new TypeException( message, e );
        }
    }

    private String getClassName( final String name )
    {
        return (String)m_classNames.get( name );
    }

    protected ClassLoader getClassLoader()
    {
        return m_classLoader;
    }
}
