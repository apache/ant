/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.deployer;

import org.apache.myrmidon.components.AbstractComponentTest;
import org.apache.myrmidon.framework.DataType;
import org.apache.myrmidon.interfaces.converter.MasterConverter;
import org.apache.myrmidon.interfaces.deployer.ConverterDefinition;
import org.apache.myrmidon.interfaces.deployer.Deployer;
import org.apache.myrmidon.interfaces.deployer.TypeDefinition;
import org.apache.myrmidon.interfaces.deployer.TypeDeployer;
import org.apache.myrmidon.interfaces.role.RoleManager;
import org.apache.myrmidon.interfaces.type.TypeFactory;

/**
 * Test cases for the default deployer.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 */
public class DefaultDeployerTest
    extends AbstractComponentTest
{
    private Deployer m_deployer;

    public DefaultDeployerTest( final String name )
    {
        super( name );
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
        final String destType = TestType1.class.getName();
        final TypeDefinition typeDef =  new TypeDefinition( "data-type", "test-type1", destType );

        // Deploy the type
        final ClassLoader classLoader = getClass().getClassLoader();
        final TypeDeployer typeDeployer = m_deployer.createDeployer( classLoader );
        typeDeployer.deployType( typeDef );

        // Create an instance
        final TypeFactory typeFactory = getTypeManager().getFactory( DataType.class );
        final Object result = typeFactory.create( "test-type1" );

        // Check the type
        assertTrue( result instanceof TestType1 );
    }

    /**
     * Tests deployment of a single converter from a ClassLoader.
     */
    public void testSingleConverter() throws Exception
    {
        // Create the type definition
        final String classname = TestConverter1.class.getName();
        final String source = "java.lang.String";
        final String destClass = TestType1.class.getName();
        final ConverterDefinition typeDef =
            new ConverterDefinition( classname, source, destClass );

        // Deploy the type
        final ClassLoader classLoader = getClass().getClassLoader();
        final TypeDeployer typeDeployer = m_deployer.createDeployer( classLoader );
        typeDeployer.deployType( typeDef );

        // Try to convert from string to test type
        final MasterConverter converter = (MasterConverter)getComponentManager().lookup( MasterConverter.ROLE );
        final Object result = converter.convert( TestType1.class, "some-string", null );

        // Check the type
        assertTrue( result instanceof TestType1 );
    }
}
