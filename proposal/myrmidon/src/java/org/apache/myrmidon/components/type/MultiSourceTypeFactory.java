/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.type;

import java.util.HashMap;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.myrmidon.interfaces.type.TypeException;
import org.apache.myrmidon.interfaces.type.TypeFactory;

/**
 * This factory acts as a proxy to set of object factories.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public class MultiSourceTypeFactory
    implements TypeFactory
{
    private final static Resources REZ =
        ResourceManager.getPackageResources( MultiSourceTypeFactory.class );

    ///Parent Selector
    private final MultiSourceTypeFactory m_parent;

    ///Map of name->factory list
    private final HashMap m_factories = new HashMap();

    ///Type expected to be created from factories
    private final Class m_type;

    public MultiSourceTypeFactory( final Class type )
    {
        m_type = type;
        m_parent = null;
    }

    public MultiSourceTypeFactory( final MultiSourceTypeFactory parent )
    {
        m_type = parent.getType();
        m_parent = parent;
    }

    /**
     * Populate the ComponentSelector.
     */
    public void register( final String name, final TypeFactory factory )
    {
        m_factories.put( name, factory );
    }

    /**
     * Determines if this factory can create instances of a particular type.
     */
    public boolean canCreate( final String name )
    {
        return ( findFactory( name ) != null );
    }

    /**
     * Create a type instance based on name.
     *
     * @param name the name
     * @return the type instance
     * @exception TypeException if an error occurs
     */
    public Object create( final String name )
        throws TypeException
    {
        // Locate the factory to use
        TypeFactory factory = findFactory( name );
        if( null == factory )
        {
            final String message = REZ.getString( "no-factory.error", name );
            throw new TypeException( message );
        }

        // Create the object
        final Object object = factory.create( name );
        if( m_type != null && !m_type.isInstance( object ) )
        {
            final String message = REZ.getString( "mismatched-type.error", name, object.getClass().getName() );
            throw new TypeException( message );
        }

        return object;
    }

    /**
     * Locates the type factory to use for a particular type.
     */
    private TypeFactory findFactory( final String name )
    {
        TypeFactory factory = getTypeFactory( name );
        if( null == factory && null != m_parent )
        {
            factory = m_parent.getTypeFactory( name );
        }

        return factory;
    }

    /**
     * Retrieve type managed by selector.
     * Used by other instances of TypedComponentSelector.
     *
     * @return the type class
     */
    private final Class getType()
    {
        return m_type;
    }

    private final TypeFactory getTypeFactory( final String name )
    {
        return (TypeFactory)m_factories.get( name );
    }
}
