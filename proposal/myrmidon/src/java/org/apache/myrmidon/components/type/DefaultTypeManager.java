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
import org.apache.myrmidon.interfaces.type.TypeManager;

/**
 * The interface that is used to manage types.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 */
public class DefaultTypeManager
    implements TypeManager
{
    private static final Resources REZ =
        ResourceManager.getPackageResources( DefaultTypeManager.class );

    ///Parent type manager to inherit values from.
    private final DefaultTypeManager m_parent;

    ///Maps role to MultiSourceTypeFactory.
    private final HashMap m_roleMap = new HashMap();

    public DefaultTypeManager()
    {
        this( null );
    }

    public DefaultTypeManager( final DefaultTypeManager parent )
    {
        m_parent = parent;
    }

    public void registerType( final String role,
                              final String shorthandName,
                              final TypeFactory factory )
        throws TypeException
    {
        final MultiSourceTypeFactory msFactory = createFactory( role );
        msFactory.register( shorthandName, factory );
    }

    public TypeFactory getFactory( final String role )
        throws TypeException
    {
        return createFactory( role );
    }

    public TypeManager createChildTypeManager()
    {
        return new DefaultTypeManager( this );
    }

    protected final MultiSourceTypeFactory lookupFactory( final String role )
    {
        return (MultiSourceTypeFactory)m_roleMap.get( role );
    }

    /**
     * Get a factory of appropriate role.
     * Create a Factory if none exists with same name.
     *
     * @param role the role name(must be name of work interface)
     * @return the Factory for interface
     * @exception TypeException role does not specify accessible work interface
     */
    private MultiSourceTypeFactory createFactory( final String role )
        throws TypeException
    {
        MultiSourceTypeFactory factory = (MultiSourceTypeFactory)m_roleMap.get( role );
        if( null != factory )
        {
            return factory;
        }

        final MultiSourceTypeFactory parentFactory = getParentTypedFactory( role );
        if( null != parentFactory )
        {
            factory = new MultiSourceTypeFactory( parentFactory );
        }

        ///If we haven't got factory try to create a new one
        if( null == factory )
        {
            try
            {
                //TODO: Should we use ContextClassLoader here ??? Or perhaps try that on failure??
                final Class clazz = Class.forName( role );
                factory = new MultiSourceTypeFactory( clazz );
            }
            catch( final Exception e )
            {
                final String message = REZ.getString( "no-work-interface.error", role );
                throw new TypeException( message );
            }
        }

        m_roleMap.put( role, factory );

        return factory;
    }

    private MultiSourceTypeFactory getParentTypedFactory( final String role )
    {
        if( null != m_parent )
        {
            return m_parent.lookupFactory( role );
        }
        else
        {
            return null;
        }
    }
}
