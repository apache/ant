/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.interfaces.role;

/**
 * A role definition.  Role definitions are immutable.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
public final class RoleInfo
{
    private final String m_name;
    private final String m_shorthand;
    private final Class m_type;
    private final String m_defaultType;

    /**
     * Creates a role definition.
     *
     * @param name The role name.
     */
    public RoleInfo( final String name )
    {
        this( name, null, null, null );
    }

    /**
     * Creates a role definition.
     *
     * @param name The role name.
     * @param shorthand The role shorthand name.
     */
    public RoleInfo( final String name, final String shorthand )
    {
        this( name, shorthand, null, null );
    }

    /**
     * Creates a role definition.
     *
     * @param name The role name.
     * @param shorthand The role shorthand name.  May be null.
     * @param type The role type.  May be null.
     */
    public RoleInfo( final String name, final String shorthand, final Class type )
    {
        this( name, shorthand, type, null );
    }

    /**
     * Creates a role definition.  The role type's fully-qualified name
     * is used as the role name.
     */
    public RoleInfo( final String shorthand, final Class type )
    {
        this( type.getName(), shorthand, type, null );
    }

    /**
     * Creates a role definition.
     */
    public RoleInfo( final String name,
                     final String shorthand,
                     final Class type,
                     final String defaultType )
    {
        m_name = name;
        m_shorthand = shorthand;
        m_type = type;
        m_defaultType = defaultType;
    }

    /**
     * Compares a role to this role.
     */
    public boolean equals( final RoleInfo role )
    {
        if( role == null )
        {
            return false;
        }
        if( !m_name.equals( role.m_name ) )
        {
            return false;
        }
        if( m_shorthand == null && role.m_shorthand != null )
        {
            return false;
        }
        if( m_shorthand != null && !m_shorthand.equals( role.m_shorthand ) )
        {
            return false;
        }
        if( m_type != role.m_type )
        {
            return false;
        }
        return true;
    }

    /**
     * Returns this role's name.  This name uniquely identifies the role.
     */
    public String getName()
    {
        return m_name;
    }

    /**
     * Returns this role's shorthand name.
     *
     * @return The shorthand name, or null if this role has none.
     */
    public String getShorthand()
    {
        return m_shorthand;
    }

    /**
     * Returns this role's type.  All implementations of this role must be
     * assignable to this type.
     *
     * @return The role type, or null if this role has no type.
     */
    public Class getType()
    {
        return m_type;
    }

    /**
     * Returns the name of the default implementation of this role.
     *
     * @return The default type name, or null if this role has no default type.
     */
    public String getDefaultType()
    {
        return m_defaultType;
    }
}
