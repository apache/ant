/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.role.test;

import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.myrmidon.AbstractMyrmidonTest;
import org.apache.myrmidon.components.role.DefaultRoleManager;
import org.apache.myrmidon.api.Task;
import org.apache.myrmidon.interfaces.role.RoleException;
import org.apache.myrmidon.interfaces.role.RoleInfo;
import org.apache.myrmidon.interfaces.role.RoleManager;

/**
 * Test cases for the DefaultRoleManager.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
public class DefaultRoleManagerTestCase
    extends AbstractMyrmidonTest
{
    private final static Resources REZ = getResourcesForTested( DefaultRoleManagerTestCase.class );

    private RoleManager m_roleManager;

    public DefaultRoleManagerTestCase( String name )
    {
        super( name );
    }

    protected void setUp() throws Exception
    {
        m_roleManager = new DefaultRoleManager();
    }

    /**
     * Tests looking up a role by name, shorthand and type.
     */
    public void testLookup() throws Exception
    {
        final String roleName = "role-name";
        final String shorthand = "role-shorthand";
        final RoleInfo origRole = new RoleInfo( roleName, shorthand, Task.class );
        m_roleManager.addRole( origRole );

        // Lookup the role
        RoleInfo role = m_roleManager.getRole( roleName );
        assertTrue( origRole.equals( role ) );

        // Lookup the role by shorthand
        role = m_roleManager.getRoleByShorthandName( shorthand );
        assertTrue( origRole.equals( role ) );

        // Lookup the role by type
        role = m_roleManager.getRoleByType( Task.class );
        assertTrue( origRole.equals( role ) );

        // Lookup an unknown role
        RoleInfo unknownRole = m_roleManager.getRole( "unknown" );
        assertNull( unknownRole );

        // Lookup an unknown shorthand
        unknownRole = m_roleManager.getRoleByShorthandName( "unknown" );
        assertNull( unknownRole );

        // Lookup an unknown role
        unknownRole = m_roleManager.getRoleByType( DefaultRoleManagerTestCase.class );
        assertNull( unknownRole );
    }

    /**
     * Tests inheriting roles from parent role manager.
     */
    public void testParent() throws Exception
    {
        final String roleName = "role-name";
        final String shorthand = "shorthand";
        final RoleInfo origRole = new RoleInfo( roleName, shorthand, Task.class );
        m_roleManager.addRole( origRole );
        final RoleManager roleManager = new DefaultRoleManager( m_roleManager );

        // Lookup by name
        RoleInfo roleInfo = roleManager.getRole( roleName );
        assertTrue( origRole.equals( roleInfo ) );

        // Lookup by shorthand
        roleInfo = roleManager.getRoleByShorthandName( shorthand );
        assertTrue( origRole.equals( roleInfo ) );

        // Lookup by type
        roleInfo = roleManager.getRoleByType( Task.class );
        assertTrue( origRole.equals( roleInfo ) );
    }

    /**
     * Tests overriding a role in a child role manager.
     */
    public void testOverrideName() throws Exception
    {
        final String roleName = "role-name";
        final String shorthand = "shorthand";

        // Add original role
        final RoleInfo origRole = new RoleInfo( roleName, shorthand, Task.class );
        m_roleManager.addRole( origRole );

        // Override role
        final RoleManager roleManager = new DefaultRoleManager( m_roleManager );
        final RoleInfo overrideNameRole = new RoleInfo( roleName, "shorthand1" );
        roleManager.addRole( overrideNameRole );
        final RoleInfo overrideShorthandRole = new RoleInfo( "role2", shorthand );
        roleManager.addRole( overrideShorthandRole );
        final RoleInfo overrideTypeRole = new RoleInfo( "role3", "shorthand3", Task.class );
        roleManager.addRole( overrideTypeRole );

        // Lookup role by name
        RoleInfo roleInfo = roleManager.getRole( roleName );
        assertTrue( overrideNameRole.equals( roleInfo ) );

        // Lookup role by shorthand
        roleInfo = roleManager.getRoleByShorthandName( shorthand );
        assertTrue( overrideShorthandRole.equals( roleInfo ) );

        // Lookup role by type
        roleInfo = roleManager.getRoleByType( Task.class );
        assertTrue( overrideTypeRole.equals( roleInfo ) );
    }

    /**
     * Tests adding duplicate roles.
     */
    public void testDuplicate() throws Exception
    {
        final String roleName = "role-name";
        final String shorthand = "shorthand";
        final RoleInfo origRole = new RoleInfo( roleName, shorthand, Task.class );
        m_roleManager.addRole( origRole );

        // Duplicate role name
        try
        {
            m_roleManager.addRole( new RoleInfo( roleName ) );
            fail();
        }
        catch( RoleException exc )
        {
            final String message = REZ.getString( "duplicate-role.error", roleName );
            assertSameMessage( message, exc );
        }

        // Duplicate shorthand
        try
        {
            m_roleManager.addRole( new RoleInfo( "another-role", shorthand ) );
            fail();
        }
        catch( RoleException exc )
        {
            final String message = REZ.getString( "duplicate-shorthand.error", shorthand );
            assertSameMessage( message, exc );
        }

        // Duplicate type
        try
        {
            m_roleManager.addRole( new RoleInfo( null, Task.class ) );
            fail();
        }
        catch( RoleException exc )
        {
            final String message = REZ.getString( "duplicate-type.error", Task.class.getName() );
            assertSameMessage( message, exc );
        }
    }

}
