/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE file.
 */
package org.apache.myrmidon.components.deployer;

import java.util.HashMap;
import org.apache.myrmidon.api.DataType;
import org.apache.myrmidon.api.Task;
import org.apache.avalon.framework.activity.Initializable;

/**
 * Interface to manage roles and mapping to shorthand names.
 *
 * @author <a href="mailto:bloritsch@apache.org">Berin Loritsch</a>
 * @author <a href="mailto:ricardo@apache,org">Ricardo Rocha</a>
 * @author <a href="mailto:giacomo@apache,org">Giacomo Pati</a>
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 * @version CVS $Revision$ $Date$
 */
public class DefaultRoleManager
    implements RoleManager, Initializable
{
    /** Parent <code>RoleManager</code> for nested resolution */
    private final RoleManager  m_parent;

    /** Map for name to role mapping */
    private final HashMap      m_names = new HashMap();

    /** Map for role to name mapping */
    private final HashMap      m_roles = new HashMap();

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

    public void initialize()
        throws Exception
    {
        ///UGLY HACK!!!!!!!!!!!!!!!!!!!!!!!
        addNameRoleMapping( "task", Task.ROLE );
        addNameRoleMapping( "data-type", DataType.ROLE );

        //getClass().getClassLoader().getResources( "META-INF/ant-types.xml" );
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

        if( null != oldRole && oldRole.equals( role ) )
        {
            throw new IllegalArgumentException( "Name already mapped to another role (" +
                                                oldRole + ")" );
        }

        m_names.put( name, role );
        m_roles.put( role, name );
    }
}
