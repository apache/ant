/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE file.
 */
package org.apache.myrmidon.components.deployer;

import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.DefaultConfigurationBuilder;
import org.xml.sax.InputSource;

/**
 * Interface to manage roles and mapping to names.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 * @version CVS $Revision$ $Date$
 */
public class DefaultRoleManager
    implements RoleManager, Initializable
{
    private final static String ROLE_DESCRIPTOR = "META-INF/ant-roles.xml";

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

    /**
     * initialize the RoleManager.
     * This involves reading all Role descriptors in common classloader.
     *
     * @exception Exception if an error occurs
     */
    public void initialize()
        throws Exception
    {
        final DefaultConfigurationBuilder builder = new DefaultConfigurationBuilder();

        final Enumeration enum = getClass().getClassLoader().getResources( ROLE_DESCRIPTOR );
        while( enum.hasMoreElements() )
        {
            final URL url = (URL)enum.nextElement();
            final Configuration descriptor = builder.build( new InputSource( url.toString() ) );
            handleDescriptor( descriptor );
        }
    }

    /**
     * Configure RoleManager based on contents of single descriptor.
     *
     * @param descriptor the descriptor
     * @exception ConfigurationException if an error occurs
     */
    private void handleDescriptor( final Configuration descriptor )
        throws ConfigurationException
    {
        final Configuration[] types = descriptor.getChildren( "role" );
        for( int i = 0; i < types.length; i++ )
        {
            final String name = types[ i ].getAttribute( "shorthand" );
            final String role = types[ i ].getAttribute( "name" );
            addNameRoleMapping( name, role );
        }
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

        final String oldName = (String)m_roles.get( role );
        if( null != oldName && oldName.equals( name ) )
        {
            throw new IllegalArgumentException( "Role already mapped to another name (" +
                                                oldName + ")" );
        }

        m_names.put( name, role );
        m_roles.put( role, name );
    }
}
