/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.configurer;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import junit.framework.AssertionFailedError;
import junit.framework.TestCase;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.avalon.framework.component.Component;
import org.apache.avalon.framework.component.Composable;
import org.apache.avalon.framework.component.DefaultComponentManager;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.apache.avalon.framework.logger.LogEnabled;
import org.apache.avalon.framework.logger.LogKitLogger;
import org.apache.avalon.framework.logger.Logger;
import org.apache.log.Hierarchy;
import org.apache.log.LogTarget;
import org.apache.log.Priority;
import org.apache.log.format.PatternFormatter;
import org.apache.log.output.io.StreamTarget;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.components.converter.DefaultConverterRegistry;
import org.apache.myrmidon.components.converter.DefaultMasterConverter;
import org.apache.myrmidon.components.type.DefaultTypeManager;
import org.apache.myrmidon.components.workspace.DefaultTaskContext;
import org.apache.myrmidon.interfaces.configurer.Configurer;
import org.apache.myrmidon.interfaces.converter.ConverterRegistry;
import org.apache.myrmidon.interfaces.converter.MasterConverter;
import org.apache.myrmidon.interfaces.type.TypeManager;

/**
 * Test cases for the default configurer and related classes.
 *
 * @author Adam Murdoch
 */
public class DefaultConfigurerTest
    extends TestCase
{
    private final static Resources REZ =
        ResourceManager.getPackageResources( DefaultConfigurerTest.class );

    private DefaultComponentManager m_componentManager;
    private Configurer m_configurer;
    private Logger m_logger;
    private DefaultTaskContext m_context;

    private final static String PATTERN = "[%8.8{category}] %{message}\\n%{throwable}";

    public DefaultConfigurerTest( String name )
    {
        super( name );
    }

    /**
     * Setup the test case - prepares a set of components, including the
     * configurer.
     *
     * TODO - refactor to a sub-class, so this setup can be reused.
     */
    protected void setUp() throws Exception
    {
        final Priority priority = Priority.DEBUG;
        final org.apache.log.Logger targetLogger = Hierarchy.getDefaultHierarchy().getLoggerFor( "myrmidon" );

        final PatternFormatter formatter = new PatternFormatter( PATTERN );
        final StreamTarget target = new StreamTarget( System.out, formatter );
        targetLogger.setLogTargets( new LogTarget[]{target} );

        targetLogger.setPriority( priority );

        // Create the logger
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

        // Setup a context
        m_context = new DefaultTaskContext();
        components.add( m_context );

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

        // Configure the context
        final File baseDir = new File( "." ).getAbsoluteFile();
        m_context.setProperty( TaskContext.BASE_DIRECTORY, baseDir );

        // Find the configurer
        m_configurer = (Configurer)m_componentManager.lookup( Configurer.ROLE );

    }

    /**
     * Tests setting an attribute, via adder and setter methods.
     */
    public void testSetAttribute()
        throws Exception
    {
        // Setup test data
        final DefaultConfiguration config = new DefaultConfiguration( "test", "test" );
        final String value1 = "some value";
        config.setAttribute( "some-prop", value1 );
        final String value2 = "some other value";
        config.setAttribute( "prop", value2 );

        final ConfigTest1 test = new ConfigTest1();

        // Configure the object
        m_configurer.configure( test, config, m_context );

        // Check result
        final ConfigTest1 expected = new ConfigTest1();
        expected.setSomeProp( value1 );
        expected.addProp( value2 );
        assertEquals( expected, test );
    }

    /**
     * Tests setting an unknown attribute.
     */
    public void testSetUnknownAttribute()
        throws Exception
    {
        // Setup test data
        final DefaultConfiguration config = new DefaultConfiguration( "test", "test" );
        config.setAttribute( "unknown", "some value" );

        final ConfigTest1 test = new ConfigTest1();

        // Configure the object
        try
        {
            m_configurer.configure( test, config, m_context );
            throw new AssertionFailedError();
        }
        catch( final ConfigurationException ce )
        {
            final String message = REZ.getString( "no-such-attribute.error", "test", "unknown" );
            assertSameMessage( message, ce );
        }
    }

    /**
     * Tests setting a nested element, via adder and setter methods.
     */
    public void testSetElement()
        throws Exception
    {
        // Setup test data
        final DefaultConfiguration config = new DefaultConfiguration( "test", "test" );
        final DefaultConfiguration child1 = new DefaultConfiguration( "prop", "test" );
        final String value1 = "some value";
        child1.setAttribute( "some-prop", value1 );
        config.addChild( child1 );
        final DefaultConfiguration child2 = new DefaultConfiguration( "another-prop", "test" );
        final String value2 = "another value";
        child2.setAttribute( "some-prop", value2 );
        config.addChild( child2 );

        final ConfigTest2 test = new ConfigTest2();

        // Configure the object
        m_configurer.configure( test, config, m_context );

        // Check result
        final ConfigTest2 expected = new ConfigTest2();
        ConfigTest1 elem = new ConfigTest1();
        elem.setSomeProp( value1 );
        expected.setProp( elem );
        elem = new ConfigTest1();
        elem.setSomeProp( value2 );
        expected.addAnotherProp( elem );
        assertEquals( expected, test );
    }

    /**
     * Tests setting an unknown element.
     */
    public void testSetUnknownElement()
        throws Exception
    {
        // Setup test data
        final DefaultConfiguration config = new DefaultConfiguration( "test", "test" );
        final DefaultConfiguration elem = new DefaultConfiguration( "unknown", "test" );
        config.addChild( elem );

        final ConfigTest1 test = new ConfigTest1();

        // Configure the object
        try
        {
            m_configurer.configure( test, config, m_context );
            throw new AssertionFailedError();
        }
        catch( final ConfigurationException ce )
        {
            final String message = REZ.getString( "no-such-element.error", "test", "unknown" );
            assertSameMessage( message, ce );
        }
    }

    /**
     * Tests setting the content of an object.
     */
    public void testContent()
        throws Exception
    {
        // Create the test data
        final DefaultConfiguration config = new DefaultConfiguration( "test", "test" );
        final String value1 = "some value";
        config.setValue( value1 );

        final ConfigTest1 test = new ConfigTest1();

        // Configure the object
        m_configurer.configure( test, config, m_context );

        // Check result
        final ConfigTest1 expected = new ConfigTest1();
        expected.addContent( value1 );
        assertEquals( expected, test );
    }

    /**
     * Tests setting the content of an object that does not handle it.
     */
    public void testUnexpectedContent()
        throws Exception
    {
        // Create the test data
        final DefaultConfiguration config = new DefaultConfiguration( "test", "test" );
        config.setValue( "some value" );

        final ConfigTest2 test = new ConfigTest2();

        // Configure the object
        try
        {
            m_configurer.configure( test, config, m_context );
            throw new AssertionFailedError();
        }
        catch( final ConfigurationException ce )
        {
            final String message = REZ.getString( "no-content.error", "test" );
            assertSameMessage( message, ce );
        }
    }

    /**
     * Tests property resolution.
     */
    public void testPropResolution()
        throws Exception
    {
        // Setup test data
        final DefaultConfiguration config = new DefaultConfiguration( "test", "test" );
        config.setAttribute( "prop", "some ${prop-a} value" );

        final ConfigTest1 test = new ConfigTest1();

        m_context.setProperty( "prop-a", "other" );

        // Configure the object
        m_configurer.configure( test, config, m_context );

        // Check the configured object
        final ConfigTest1 expected = new ConfigTest1();
        expected.addProp( "some other value" );
        assertEquals( expected, test );
    }

    /**
     * Tests reference resolution via an attribute.
     */
    public void testReferenceAttribute() throws Exception
    {
        // Setup test data
        final DefaultConfiguration config = new DefaultConfiguration( "test", "test" );
        config.setAttribute( "some-prop-ref", "prop-a" );

        final ConfigTest1 test = new ConfigTest1();

        m_context.setProperty( "prop-a", "some value" );

        // Configure the object
        m_configurer.configure( test, config, m_context );

        // Check the configured object
        final ConfigTest1 expected = new ConfigTest1();
        expected.setSomeProp( "some value" );
        assertEquals( expected, test );
    }

    /**
     * Tests reference resolution via a nested element.
     */
    public void testReferenceElement() throws Exception
    {
        // Setup test data
        final DefaultConfiguration config = new DefaultConfiguration( "test", "test" );
        final DefaultConfiguration elem = new DefaultConfiguration( "some-prop-ref", "test" );
        elem.setAttribute( "id", "prop-a" );
        config.addChild( elem );

        final ConfigTest1 test = new ConfigTest1();

        m_context.setProperty( "prop-a", "some value" );

        // Configure the object
        m_configurer.configure( test, config, m_context );

        // Check the configured object
        final ConfigTest1 expected = new ConfigTest1();
        expected.setSomeProp( "some value" );
        assertEquals( expected, test );
    }

    /**
     * Test resolving properties in an id.
     */
    public void testIdResolve()
        throws Exception
    {
        // Setup test data
        final DefaultConfiguration config = new DefaultConfiguration( "test", "test" );
        config.setAttribute( "some-prop-ref", "${id}" );

        final ConfigTest1 test = new ConfigTest1();

        m_context.setProperty( "id", "prop-a" );
        m_context.setProperty( "prop-a", "some indirect value" );

        // Configure the object
        m_configurer.configure( test, config, m_context );

        // Check the configured object
        final ConfigTest1 expected = new ConfigTest1();
        expected.setSomeProp( "some indirect value" );
        assertEquals( expected, test );
    }

    /**
     * Test an unknown reference.
     */
    public void testUnknownReference()
        throws Exception
    {
        // Setup test data
        final DefaultConfiguration config = new DefaultConfiguration( "test", "test" );
        config.setAttribute( "some-prop-ref", "unknown-prop" );

        final ConfigTest1 test = new ConfigTest1();

        // Configure the object
        try
        {
            m_configurer.configure( test, config, m_context );
            throw new AssertionFailedError();
        }
        catch( ConfigurationException e )
        {
            final String message = REZ.getString( "bad-set-attribute.error",
                                                  "test",
                                                  "some-prop-ref" );
            assertSameMessage( message, e );
        }
    }

    /**
     * Tests handling of mismatched reference type.
     */
    public void testMismatchedRefType()
        throws Exception
    {
        // Setup test data
        final DefaultConfiguration config = new DefaultConfiguration( "test", "test" );
        config.setAttribute( "some-prop-ref", "prop-a" );

        final ConfigTest1 test = new ConfigTest1();

        m_context.setProperty( "prop-a", new ConfigTest2() );

        // Configure the object
        try
        {
            m_configurer.configure( test, config, m_context );
            throw new AssertionFailedError();
        }
        catch( ConfigurationException e )
        {
            final String message = REZ.getString( "bad-set-attribute.error",
                                                  "test",
                                                  "some-prop-ref" );
            assertSameMessage( message, e );
        }
    }

    /**
     * Tests reporting of nested errors.
     */
    public void testNestedErrors() throws Exception
    {
        // Setup test data
        final DefaultConfiguration config = new DefaultConfiguration( "test", "test" );
        final DefaultConfiguration elem = new DefaultConfiguration( "prop", "test" );
        elem.setAttribute( "not-a-prop", "not-a-value" );
        config.addChild( elem );

        final ConfigTest2 test = new ConfigTest2();

        try
        {
            // Configure the object
            m_configurer.configure( test, config, m_context );
            throw new AssertionFailedError();
        }
        catch( ConfigurationException e )
        {
            final String message = REZ.getString( "no-such-attribute.error",
                                                  "prop",
                                                  "not-a-prop" );
            assertSameMessage( message, e );
        }
    }

    /**
     * Tests that string setter/adder/creators are ignored when there
     * are multiple.
     */
    public void testIgnoreStringMethods() throws Exception
    {
        // Setup test data
        final DefaultConfiguration config = new DefaultConfiguration( "test", "test" );
        DefaultConfiguration elem = new DefaultConfiguration( "prop1", "test" );
        config.addChild( elem );
        elem = new DefaultConfiguration( "prop2", "test" );
        config.addChild( elem );
        elem = new DefaultConfiguration( "prop3", "test" );
        config.addChild( elem );

        final ConfigTest3 test = new ConfigTest3();

        // Configure the object
        m_configurer.configure( test, config, m_context );

        // Test expected value
        final ConfigTest3 expected = new ConfigTest3();
        expected.setProp1( new ConfigTest1() );
        expected.setProp2( new ConfigTest1() );
        expected.addProp3( new ConfigTest1() );
        assertEquals( expected, test );
    }

    /**
     * Asserts that an exception contains the expected message.
     *
     * TODO - should take the expected exception, rather than the message,
     * to check the entire cause chain.
     */
    protected void assertSameMessage( final String msg, final Throwable exc )
    {
        assertEquals( msg, exc.getMessage() );
    }

    /**
     * Compares 2 objects for equality, nulls are equal.  Used by the test
     * classes' equals() methods.
     */
    public static boolean equals( final Object o1, final Object o2 )
    {
        if( o1 == null && o2 == null )
        {
            return true;
        }
        if( o1 == null || o2 == null )
        {
            return false;
        }
        return o1.equals( o2 );
    }
}
