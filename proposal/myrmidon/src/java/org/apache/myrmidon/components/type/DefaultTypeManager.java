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
import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.ComponentSelector;

/**
 * The interface that is used to manage types.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class DefaultTypeManager
    implements TypeManager
{
    ///Parent type manager to inherit values from.
    private final TypeManager  m_parent;

    ///Maps role to TypedComponentSelector.
    private final HashMap      m_roleMap      = new HashMap();

    public DefaultTypeManager()
    {
        this( null );
    }

    public DefaultTypeManager( final TypeManager parent )
    {
        m_parent = parent;
    }

    public Component lookup( final String role )
        throws ComponentException
    {
        final ComponentSelector selector = (ComponentSelector)m_roleMap.get( role );
        if( null != selector )
        {
            return selector;
        }
        else
        {
            throw new ComponentException( "Unable to provide implementation for '" + 
                                          role + "'" );
        }
    }

    public void release( final Component component )
    {
    }

    public void registerType( final String role, 
                              final String shorthandName, 
                              final ComponentFactory factory ) 
        throws Exception
    {
        final TypedComponentSelector selector = createSelector( role );
        selector.register( shorthandName, factory );
    }

    /**
     * Get a selector of appropriate role.
     * Create a Selector if none exists with same name.
     *
     * @param role the role name(must be name of work interface)
     * @return the Selector for interface
     * @exception ComponentException if role exists and not a selector, role does not 
     *            specify accessible work interface, or 
     */
    private TypedComponentSelector createSelector( final String role )
        throws ComponentException
    {
        TypedComponentSelector selector = (TypedComponentSelector)m_roleMap.get( role );
        if( null != selector ) return selector;

        if( null != m_parent )
        {
            final TypedComponentSelector parentSelector = getTypedSelector( m_parent, role );

            if( null != parentSelector )
            {
                selector = new TypedComponentSelector( parentSelector );
            }
        }

        ///If we haven't goa selector try to create a new one
        if( null == selector )
        {
            try
            {
                //TODO: Should we use ContextClassLoader here ??? Or perhaps try that on failure??
                final Class clazz = Class.forName( role );
                selector = new TypedComponentSelector( clazz );
            }
            catch( final Exception e )
            {
                throw new ComponentException( "Role '" + role + "' does not specify " +
                                              "accessible work interface" );
            }
        }        

        m_roleMap.put( role, selector );

        return selector;
    }

    private TypedComponentSelector getTypedSelector( final TypeManager typeManager, 
                                                     final String role )
    {
        try
        {
            return (TypedComponentSelector)typeManager.lookup( role );
        }
        catch( final ComponentException ce ) {}
        catch( final ClassCastException cce ) {}
        
        return null;
    }
}
