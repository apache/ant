/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.service.test;

import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.context.DefaultContext;
import org.apache.myrmidon.components.AbstractComponentTest;
import org.apache.myrmidon.components.service.InstantiatingServiceManager;
import org.apache.myrmidon.interfaces.role.RoleInfo;
import org.apache.myrmidon.interfaces.role.RoleManager;
import org.apache.myrmidon.interfaces.service.ServiceFactory;
import org.apache.myrmidon.interfaces.type.DefaultTypeFactory;
import org.apache.myrmidon.interfaces.type.TypeManager;

/**
 * Test cases for the default service manager.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
public class InstantiatingServiceManagerTestCase
    extends AbstractComponentTest
{
    private final static Resources REZ = getResourcesForTested( InstantiatingServiceManagerTestCase.class );

    private InstantiatingServiceManager m_serviceManager;

    public InstantiatingServiceManagerTestCase( final String name )
    {
        super( name );
    }

    /**
     * Setup the test case - prepares the set of components.
     */
    protected void setUp()
        throws Exception
    {
        // Set-up the service manager
        m_serviceManager = new InstantiatingServiceManager();
        m_serviceManager.enableLogging( getLogger() );
        m_serviceManager.contextualize( new DefaultContext() );
        m_serviceManager.service( getServiceManager() );
        m_serviceManager.parameterize( new Parameters() );
    }

    /**
     * Tests service instantiation.
     */
    public void testCreateService() throws Exception
    {
        final String serviceRoleName = "test-service";

        // Setup the test service
        registerFactory( serviceRoleName, TestService.class, TestServiceFactory1.class );

        // Create the service
        Object service = m_serviceManager.lookup( serviceRoleName );

        // Check service is of the expected class (don't use instanceof)
        assertTrue( service.getClass() == TestServiceImpl1.class );
    }

    /**
     * Tests service lookup.
     */
    public void testLookup() throws Exception
    {
        final String serviceRoleName = "test-service";

        // Setup the test service
        registerFactory( serviceRoleName, TestService.class, TestServiceFactory1.class );

        // Check whether the service can be instantiated
        boolean hasService = m_serviceManager.hasService( serviceRoleName );
        assertTrue( hasService );
    }

    /**
     * Tests that a service factory and service instance are taken through
     * the lifecycle steps.
     */
    public void testLifecycle() throws Exception
    {
        final String serviceRoleName = "test-service";

        // Setup the test service
        registerFactory( serviceRoleName, TestService.class, TestServiceFactory2.class );

        // Create the service
        TestService service = (TestService)m_serviceManager.lookup( serviceRoleName );

        // Check service is of the expected class (don't use instanceof)
        assertTrue( service.getClass() == TestServiceImpl2.class );

        // Assert the service has been setup correctly
        LifecycleValidator validate = (LifecycleValidator)service;
        validate.assertSetup();

        // Cleanup
        m_serviceManager.dispose();

        // Assert the service has been shutdown correctly
        validate.assertDisposed();
    }

    /**
     * Tests looking up an unknown service.
     */
    public void testUnknownService() throws Exception
    {
        // Make sure that hasService() works correctly
        final String serviceRole = "some-unknown-service";
        assertTrue( !m_serviceManager.hasService( serviceRole ) );

        // Make sure that lookup() fails
        try
        {
            m_serviceManager.lookup( serviceRole );
            fail();
        }
        catch( ServiceException e )
        {
            final String message = REZ.getString( "create-service.error", serviceRole );
            assertSameMessage( message, e );
        }
    }

    /**
     * Registers a service factory.
     */
    private void registerFactory( final String serviceRoleName,
                                  final Class serviceType,
                                  final Class factoryClass )
        throws Exception
    {
        // TODO - add stuff to TypeDeployer to do this instead
        final RoleManager roleManager = (RoleManager)getServiceManager().lookup( RoleManager.ROLE );
        roleManager.addRole( new RoleInfo( serviceRoleName, null, serviceType ) );
        final DefaultTypeFactory typeFactory = new DefaultTypeFactory( getClass().getClassLoader() );
        typeFactory.addNameClassMapping( serviceRoleName, factoryClass.getName() );
        final TypeManager typeManager = (TypeManager)getServiceManager().lookup( TypeManager.ROLE );
        typeManager.registerType( ServiceFactory.ROLE, serviceRoleName, typeFactory );
    }
}
