/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.myrmidon.components.type;

import java.util.HashMap;

/**
 * This factory acts as a proxy to set of object factorys.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class MultiSourceTypeFactory
    implements TypeFactory               
{
    ///Parent Selector
    private final MultiSourceTypeFactory  m_parent;

    ///Map of name->factory list
    private final HashMap                 m_factorys = new HashMap();

    ///Type expected to be created from factorys
    private final Class                   m_type;

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
        m_factorys.put( name, factory );
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
        TypeFactory factory = getTypeFactory( name );

        if( null == factory && null != m_parent ) 
        {
            factory = m_parent.getTypeFactory( name );
        }

        if( null == factory ) 
        {
            throw new TypeException( "Failed to locate factory for '" + name + "'" );
        }
        else
        {
            final Object object = factory.create( name );

            if( !m_type.isInstance( object ) )
            {
                throw new TypeException( "Object '" + name + "' is not of " +
                                         "correct Type (" + m_type.getName() + ")" );
            }

            return object;
        }
    }

    /**
     * Retrieve type managed by selector.
     * Used by other instances of TypedComponentSelector.
     *
     * @return the type class
     */
    protected final Class getType()
    {
        return m_type;
    }

    protected final TypeFactory getTypeFactory( final String name )
    {
        return (TypeFactory)m_factorys.get( name );
    }
}
