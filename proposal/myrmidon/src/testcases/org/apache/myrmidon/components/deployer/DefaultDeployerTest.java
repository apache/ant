/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.deployer;

import org.apache.myrmidon.components.ComponentTestBase;
import org.apache.myrmidon.framework.DataType;
import org.apache.myrmidon.interfaces.converter.MasterConverter;
import org.apache.myrmidon.interfaces.deployer.ConverterDefinition;
import org.apache.myrmidon.interfaces.deployer.Deployer;
import org.apache.myrmidon.interfaces.deployer.GeneralTypeDefinition;
import org.apache.myrmidon.interfaces.deployer.TypeDeployer;
import org.apache.myrmidon.interfaces.role.RoleManager;
import org.apache.myrmidon.interfaces.type.TypeFactory;

/**
 * Test cases for the default deployer.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 */
public class DefaultDeployerTest
    extends ComponentTestBase
{
    private Deployer m_deployer;

    public DefaultDeployerTest( String s )
    {
        super( s );
    }

    /**
     * Setup the test case - prepares the set of components, including the
     * deployer.
     */
    protected void setUp() throws Exception
    {
        super.setUp();
        m_deployer = (Deployer)getComponentManager().lookup( Deployer.ROLE );
    }

    /**
     * Tests deployment of a single type from a ClassLoader.
     */
    public void testSingleType() throws Exception
    {
        // Determine the shorthand for the DataType role
        final RoleManager roleManager = (RoleManager)getComponentManager().lookup( RoleManager.ROLE );
        roleManager.addNameRoleMapping( "data-type", DataType.ROLE );

        // Create the type definition
        final GeneralTypeDefinition typeDef = new GeneralTypeDefinition();
        typeDef.setType( "data-type" );
        typeDef.setName( "test-type1" );
        typeDef.setClassname( TestType1.class.getName() );

        // Deploy the type
        final ClassLoader classLoader = getClass().getClassLoader();
        final TypeDeployer typeDeployer = m_deployer.createDeployer( classLoader );
        typeDeployer.deployType( typeDef );

        // Create an instance
        final TypeFactory typeFactory = getTypeManager().getFactory( DataType.class );
        Object obj = typeFactory.create( "test-type1" );

        // Check the type
        assertTrue( obj instanceof TestType1 );
    }

    /**
     * Tests deployment of a single converter from a ClassLoader.
     */
    public void testSingleConverter() throws Exception
    {
        // Create the type definition
        final ConverterDefinition typeDef = new ConverterDefinition();
        typeDef.setClassname( TestConverter1.class.getName() );
        typeDef.setSourceType( "java.lang.String" );
        typeDef.setDestinationType( TestType1.class.getName() );

        // Deploy the type
        final ClassLoader classLoader = getClass().getClassLoader();
        final TypeDeployer typeDeployer = m_deployer.createDeployer( classLoader );
        typeDeployer.deployType( typeDef );

        // Try to convert from string to test type
        final MasterConverter converter = (MasterConverter)getComponentManager().lookup( MasterConverter.ROLE );
        Object obj = converter.convert( TestType1.class, "some-string", null );

        // Check the type
        assertTrue( obj instanceof TestType1 );
    }
}
