/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.role;

import java.util.HashMap;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.myrmidon.interfaces.role.RoleManager;

/**
 * Interface to manage roles and mapping to names.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version CVS $Revision$ $Date$
 */
public class DefaultRoleManager
    implements RoleManager
{
    private final static Resources REZ =
        ResourceManager.getPackageResources( DefaultRoleManager.class );

    /** Parent <code>RoleManager</code> for nested resolution */
    private final RoleManager m_parent;

    /** Map for name to role mapping */
    private final HashMap m_names = new HashMap();

    /** Map for role to name mapping */
    private final HashMap m_roles = new HashMap();

    /**
     *  constructor--this RoleManager has no parent.
     */
    public DefaultRoleManager()
    {
        this( null );
    }

    /**
     * Alternate constructor--this RoleManager has the specified
     * parent.
     *
     * @param parent The parent <code>RoleManager</code>.
     */
    public DefaultRoleManager( final RoleManager parent )
    {
        m_parent = parent;
    }

    /**
     * Find Role name based on shorthand name.
     *
     * @param name the shorthand name
     * @return the role
     */
    public String getRoleForName( final String name )
    {
        final String role = (String)m_names.get( name );

        if( null == role && null != m_parent )
        {
            return m_parent.getRoleForName( name );
        }

        return role;
    }

    /**
     * Find name based on role.
     *
     * @param role the role
     * @return the name
     */
    public String getNameForRole( final String role )
    {
        final String name = (String)m_roles.get( role );

        if( null == name && null != m_parent )
        {
            return m_parent.getNameForRole( name );
        }

        return name;
    }

    /**
     * Add a mapping between name and role
     *
     * @param name the shorthand name
     * @param role the role
     * @exception IllegalArgumentException if an name is already mapped to a different role
     */
    public void addNameRoleMapping( final String name, final String role )
        throws IllegalArgumentException
    {
        final String oldRole = (String)m_names.get( name );
        if( null != oldRole && ! oldRole.equals( role ) )
        {
            final String message = REZ.getString( "duplicate-name.error", oldRole );
            throw new IllegalArgumentException( message );
        }

        final String oldName = (String)m_roles.get( role );
        if( null != oldName && ! oldName.equals( name ) )
        {
            final String message = REZ.getString( "duplicate-role.error", oldName );
            throw new IllegalArgumentException( message );
        }

        m_names.put( name, role );
        m_roles.put( role, name );
    }
}
