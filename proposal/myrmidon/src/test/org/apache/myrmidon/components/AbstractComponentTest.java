/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import junit.framework.TestCase;
import org.apache.avalon.framework.component.Component;
import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.component.Composable;
import org.apache.avalon.framework.component.DefaultComponentManager;
import org.apache.avalon.framework.logger.LogEnabled;
import org.apache.avalon.framework.logger.LogKitLogger;
import org.apache.avalon.framework.logger.Logger;
import org.apache.log.Hierarchy;
import org.apache.log.LogTarget;
import org.apache.log.Priority;
import org.apache.log.format.PatternFormatter;
import org.apache.log.output.io.StreamTarget;
import org.apache.myrmidon.components.configurer.DefaultConfigurer;
import org.apache.myrmidon.components.converter.DefaultConverterRegistry;
import org.apache.myrmidon.components.converter.DefaultMasterConverter;
import org.apache.myrmidon.components.deployer.DefaultDeployer;
import org.apache.myrmidon.components.deployer.DefaultClassLoaderManager;
import org.apache.myrmidon.components.deployer.ClassLoaderManager;
import org.apache.myrmidon.components.extensions.DefaultExtensionManager;
import org.apache.myrmidon.components.role.DefaultRoleManager;
import org.apache.myrmidon.components.type.DefaultTypeManager;
import org.apache.myrmidon.interfaces.configurer.Configurer;
import org.apache.myrmidon.interfaces.converter.ConverterRegistry;
import org.apache.myrmidon.interfaces.converter.MasterConverter;
import org.apache.myrmidon.interfaces.deployer.Deployer;
import org.apache.myrmidon.interfaces.extensions.ExtensionManager;
import org.apache.myrmidon.interfaces.role.RoleManager;
import org.apache.myrmidon.interfaces.type.TypeManager;
import org.apache.myrmidon.interfaces.type.TypeException;
import org.apache.myrmidon.interfaces.type.DefaultTypeFactory;
import org.apache.myrmidon.converter.Converter;
import org.apache.myrmidon.AbstractMyrmidonTest;

/**
 * A base class for tests for the default components.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 */
public abstract class AbstractComponentTest
    extends AbstractMyrmidonTest
{
    private DefaultComponentManager m_componentManager;
    private Logger m_logger;

    private final static String PATTERN = "[%8.8{category}] %{message}\\n%{throwable}";

    public AbstractComponentTest( final String name )
    {
        super( name );
    }

    /**
     * Returns the component manager containing the components to test.
     */
    protected ComponentManager getComponentManager()
    {
        return m_componentManager;
    }

    /**
     * Returns the type manager.
     */
    protected TypeManager getTypeManager() throws ComponentException
    {
        return (TypeManager)getComponentManager().lookup( TypeManager.ROLE );
    }

    /**
     * Setup the test case - prepares the set of components.
     */
    protected void setUp()
        throws Exception
    {
        // Setup a logger
        final Priority priority = Priority.DEBUG;
        final org.apache.log.Logger targetLogger = Hierarchy.getDefaultHierarchy().getLoggerFor( "myrmidon" );

        final PatternFormatter formatter = new PatternFormatter( PATTERN );
        final StreamTarget target = new StreamTarget( System.out, formatter );
        targetLogger.setLogTargets( new LogTarget[]{target} );
        targetLogger.setPriority( priority );

        m_logger = new LogKitLogger( targetLogger );

        // Create the components
        m_componentManager = new DefaultComponentManager();
        List components = new ArrayList();

        Component component = new DefaultMasterConverter();
        m_componentManager.put( MasterConverter.ROLE, component );
        components.add( component );

        component = new DefaultConverterRegistry();
        m_componentManager.put( ConverterRegistry.ROLE, component );
        components.add( component );

        component = new DefaultTypeManager();
        m_componentManager.put( TypeManager.ROLE, component );
        components.add( component );

        component = new DefaultConfigurer();
        m_componentManager.put( Configurer.ROLE, component );
        components.add( component );

        component = new DefaultDeployer();
        m_componentManager.put( Deployer.ROLE, component );
        components.add( component );

        final DefaultClassLoaderManager classLoaderMgr = new DefaultClassLoaderManager();
        classLoaderMgr.setBaseClassLoader( getClass().getClassLoader() );
        m_componentManager.put( ClassLoaderManager.ROLE, classLoaderMgr );
        components.add( classLoaderMgr );

        component = new DefaultExtensionManager();
        m_componentManager.put( ExtensionManager.ROLE, component );
        components.add( component );

        component = new DefaultRoleManager();
        m_componentManager.put( RoleManager.ROLE, component );
        components.add( component );

        // Log enable the components
        for( Iterator iterator = components.iterator(); iterator.hasNext(); )
        {
            Object obj = iterator.next();
            if( obj instanceof LogEnabled )
            {
                final LogEnabled logEnabled = (LogEnabled)obj;
                logEnabled.enableLogging( m_logger );
            }
        }

        // Compose the components
        for( Iterator iterator = components.iterator(); iterator.hasNext(); )
        {
            Object obj = iterator.next();
            if( obj instanceof Composable )
            {
                final Composable composable = (Composable)obj;
                composable.compose( m_componentManager );
            }
        }
    }

    /**
     * Utility method to register a Converter.
     */
    protected void registerConverter( final Class converterClass,
                                      final Class sourceClass,
                                      final Class destClass )
        throws ComponentException, TypeException
    {
        ConverterRegistry converterRegistry = (ConverterRegistry)getComponentManager().lookup( ConverterRegistry.ROLE );
        converterRegistry.registerConverter( converterClass.getName(), sourceClass.getName(), destClass.getName() );
        DefaultTypeFactory factory = new DefaultTypeFactory( getClass().getClassLoader() );
        factory.addNameClassMapping( converterClass.getName(), converterClass.getName() );
        getTypeManager().registerType( Converter.class, converterClass.getName(), factory );
    }
}
