/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE file.
 */
package org.apache.myrmidon.components.type;

import java.util.HashMap;

/**
 * Interface to manage roles and mapping to shorthand names.
 *
 * @author <a href="mailto:bloritsch@apache.org">Berin Loritsch</a>
 * @author <a href="mailto:ricardo@apache,org">Ricardo Rocha</a>
 * @author <a href="mailto:giacomo@apache,org">Giacomo Pati</a>
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 * @version CVS $Revision$ $Date$
 */
public class RoleManager
{
    /** Parent <code>RoleManager</code> for nested resolution */
    private final RoleManager  m_parent;

    /** Map for shorthand to role mapping */
    private final HashMap      m_shorthands = new HashMap();

    /**
     *  constructor--this RoleManager has no parent.
     */
    public RoleManager()
    {
        this( null );
    }

    /**
     * Alternate constructor--this RoleManager has the specified
     * parent.
     *
     * @param parent The parent <code>RoleManager</code>.
     */
    public RoleManager( final RoleManager parent )
    {
        m_parent = parent;
    }

    /**
     * Find Role name based on shorthand name.
     *
     * @param shorthandName the shorthand name
     * @return the role
     */
    public String getRoleForName( final String shorthandName )
    {
        final String role = (String)m_shorthands.get( shorthandName );

        if( null == role && null != m_parent )
        {
            return m_parent.getRoleForName( shorthandName );
        }

        return role;
    }

    /**
     * Add a mapping between shorthand name and role
     *
     * @param shorthandName the shorthand name
     * @param role the role
     * @exception IllegalArgumentException if an name is already mapped to a different role
     */
    public void addNameRoleMapping( final String shorthandName, final String role )
        throws IllegalArgumentException
    {
        final String oldRole = (String)m_shorthands.get( shorthandName );

        if( null != oldRole && oldRole.equals( role ) )
        {
            throw new IllegalArgumentException( "Name already mapped to another role (" +
                                                oldRole + ")" );
        }

        m_shorthands.put( shorthandName, role );
    }
}
