/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.myrmidon.components.type;

import java.util.HashMap;
import org.apache.avalon.framework.component.Component;
import org.apache.avalon.framework.component.ComponentSelector;
import org.apache.avalon.framework.component.ComponentException;

/**
 * This is a ComponentSelector implementation that acts as factory
 * for objects and checks type on creation.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class TypedComponentSelector
    implements ComponentSelector
{
    ///Parent Selector
    private final TypedComponentSelector  m_parent;

    ///Map of name->factory list
    private final HashMap                 m_factorys = new HashMap();

    ///Type expected to be created from factorys
    private final Class                   m_type;

    public TypedComponentSelector( final Class type )
    {
        m_type = type;
        m_parent = null;
    }

    public TypedComponentSelector( final TypedComponentSelector parent )
    {
        m_type = parent.getType();
        m_parent = parent;
    }

    /**
     * Select the desired component.  
     * This creates component and checks if type appropriate.
     *
     * @param hint the hint to retrieve Component 
     * @return the Component
     * @exception ComponentException if an error occurs
     */
    public Component select( Object hint )
        throws ComponentException
    {
        if( !(hint instanceof String) )
        {
            throw new ComponentException( "Invalid hint, expected a string not a " + 
                                          hint.getClass().getName() );
        }

        final String name = (String)hint;
        final Component component = createComponent( name );

        if( null != component )
        {
            if( m_type.isInstance( component ) )
            {
                throw new ComponentException( "Implementation of " + name + " is not of " +
                                              "correct type (" + m_type.getClass().getName() + ")" );
            }

            return component;
        }
        else
        {
            throw new ComponentException( "Unable to provide implementation for " + name );
        }
    }

    /**
     * Release component.
     *
     * @param component the component
     */
    public void release( final Component component )
    {
    }

    /**
     * Populate the ComponentSelector.
     */
    public void register( final String name, final ComponentFactory factory )
    {
        m_factorys.put( name, factory );
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

    /**
     * Helper method for subclasses to retrieve component map.
     *
     * @return the component map
     */
    private Component createComponent( final String name )
        throws ComponentException
    {
        final ComponentFactory factory = (ComponentFactory)m_factorys.get( name );
        
        if( null == factory ) return null;
        else
        {
            return factory.create( name );
        }
    }
}
