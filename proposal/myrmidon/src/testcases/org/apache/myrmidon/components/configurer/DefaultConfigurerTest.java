/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.configurer;

import java.io.File;
import org.apache.antlib.core.StringToIntegerConverter;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.apache.avalon.framework.context.Context;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.components.AbstractComponentTest;
import org.apache.myrmidon.components.workspace.DefaultTaskContext;
import org.apache.myrmidon.framework.DataType;
import org.apache.myrmidon.interfaces.configurer.Configurer;
import org.apache.myrmidon.interfaces.configurer.TaskContextAdapter;
import org.apache.myrmidon.interfaces.role.RoleManager;
import org.apache.myrmidon.interfaces.role.RoleInfo;
import org.apache.myrmidon.interfaces.type.DefaultTypeFactory;

/**
 * Test cases for the default configurer and related classes.
 *
 * @author Adam Murdoch
 */
public class DefaultConfigurerTest
    extends AbstractComponentTest
{
    private final static Resources REZ =
        ResourceManager.getPackageResources( DefaultConfigurerTest.class );

    private Configurer m_configurer;
    private DefaultTaskContext m_context;
    private Context m_adaptor;

    public DefaultConfigurerTest( String name )
    {
        super( name );
    }

    /**
     * Setup the test case - prepares a set of components, including the
     * configurer.
     */
    protected void setUp() throws Exception
    {
        super.setUp();

        // Find the configurer
        m_configurer = (Configurer)getServiceManager().lookup( Configurer.ROLE );

        // Setup a context
        m_context = new DefaultTaskContext();
        final File baseDir = new File( "." ).getAbsoluteFile();
        m_context.setProperty( TaskContext.BASE_DIRECTORY, baseDir );
        m_adaptor = new TaskContextAdapter( m_context );
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
        m_configurer.configure( test, config, m_adaptor );

        // Check result
        final ConfigTest1 expected = new ConfigTest1();
        expected.setSomeProp( value1 );
        expected.addProp( value2 );
        assertEquals( expected, test );
    }

    /**
     * Tests attribute conversion.
     */
    public void testAttributeConvert()
        throws Exception
    {
        // Setup test data
        final DefaultConfiguration config = new DefaultConfiguration( "test", "test" );
        config.setAttribute( "int-prop", "90" );
        config.setAttribute( "integer-prop", "-401" );

        // Register the converter
        final Class converterClass = StringToIntegerConverter.class;
        final Class sourceClass = String.class;
        final Class destClass = Integer.class;
        registerConverter( converterClass, sourceClass, destClass );

        final ConfigTest10 test = new ConfigTest10();

        // Configure the object
        m_configurer.configure( test, config, m_adaptor );

        // Check result
        final ConfigTest10 expected = new ConfigTest10();
        expected.setIntProp( 90 );
        expected.setIntegerProp( new Integer(-401) );
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
            m_configurer.configure( test, config, m_adaptor );
            fail();
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
        m_configurer.configure( test, config, m_adaptor );

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
            m_configurer.configure( test, config, m_adaptor );
            fail();
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
        m_configurer.configure( test, config, m_adaptor );

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
            m_configurer.configure( test, config, m_adaptor );
            fail();
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
        m_configurer.configure( test, config, m_adaptor );

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
        m_configurer.configure( test, config, m_adaptor );

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
        m_configurer.configure( test, config, m_adaptor );

        // Check the configured object
        final ConfigTest1 expected = new ConfigTest1();
        expected.setSomeProp( "some value" );
        assertEquals( expected, test );
    }

    /**
     * Tests that extra content is not allowed in a reference element.
     */
    public void testReferenceElementExtra()
        throws Exception
    {
        // Setup test data
        final DefaultConfiguration config = new DefaultConfiguration( "test", "test" );
        final DefaultConfiguration elem = new DefaultConfiguration( "some-prop-ref", "test" );
        elem.setAttribute( "id", "prop-a" );
        elem.setAttribute( "extra-attr", "some value" );
        config.addChild( elem );

        final ConfigTest1 test = new ConfigTest1();

        try
        {
            // Configure the object
            m_configurer.configure( test, config, m_adaptor );
            fail();
        }
        catch( ConfigurationException e )
        {
            final String message = REZ.getString( "extra-config-for-ref.error" );
            assertSameMessage( message, e );
        }
    }

    /**
     * Tests whether an object with a non-iterface typed adder causes an
     * exception.
     */
    public void testNonInterfaceTypedAdder()
        throws Exception
    {
        // Setup test data
        final DefaultConfiguration config = new DefaultConfiguration( "test", "test" );

        final ConfigTest4 test = new ConfigTest4();

        try
        {
            // Configure the object
            m_configurer.configure( test, config, m_adaptor );
            fail();
        }
        catch( final ConfigurationException ce )
        {
            final String message = REZ.getString( "typed-adder-non-interface.error",
                                                  ConfigTest4.class.getName(),
                                                  Integer.class.getName() );
            assertSameMessage( message, ce );
        }
    }

    /**
     * Tests whether an object with multiple typed adders causes an exception.
     */
    public void testMultipleTypedAdder()
        throws Exception
    {
        // Setup test data
        final DefaultConfiguration config = new DefaultConfiguration( "test", "test" );

        final ConfigTest5 test = new ConfigTest5();

        try
        {
            // Configure the object
            m_configurer.configure( test, config, m_adaptor );
            fail();
        }
        catch( final ConfigurationException ce )
        {
            final String message = REZ.getString( "multiple-adder-methods-for-element.error",
                                                  ConfigTest5.class.getName(),
                                                  "");
            assertSameMessage( message, ce );
        }
    }

    /**
     * Tests to see if typed adder works, with iterface types.
     */
    public void testTypedAdder()
        throws Exception
    {
        // Setup test data
        final DefaultConfiguration config = new DefaultConfiguration( "test", "test" );
        final DefaultConfiguration child1 = new DefaultConfiguration( "my-type1", "test" );
        final DefaultConfiguration child2 = new DefaultConfiguration( "my-type2", "test" );
        config.addChild( child1 );
        config.addChild( child2 );

        final ClassLoader loader = getClass().getClassLoader();
        final DefaultTypeFactory factory = new DefaultTypeFactory( loader );
        factory.addNameClassMapping( "my-type1", MyType1.class.getName() );
        factory.addNameClassMapping( "my-type2", MyType2.class.getName() );
        getTypeManager().registerType( DataType.class, "my-type1", factory );
        getTypeManager().registerType( DataType.class, "my-type2", factory );

        final ConfigTest6 test = new ConfigTest6();

        // Configure the object
        m_configurer.configure( test, config, m_adaptor );

        final ConfigTest6 expected = new ConfigTest6();
        expected.add( new MyType1() );
        expected.add( new MyType2() );
        assertEquals( expected, test );
    }

    /**
     * Tests to see if typed adder can be used via an attribute.
     */
    public void testTypedAdderAttribute()
        throws Exception
    {
        // Setup test data
        final DefaultConfiguration config = new DefaultConfiguration( "test", "test" );
        config.setAttribute( "my-role1", "some value" );

        // Set up the converter and role
        RoleManager roleMgr = (RoleManager)getServiceManager().lookup( RoleManager.ROLE );
        final RoleInfo roleInfo = new RoleInfo("my-role1", MyRole1.class );
        roleMgr.addRole( roleInfo );
        registerConverter( StringToMyRole1Converter.class, String.class, MyRole1.class );

        final ConfigTest6 test = new ConfigTest6();

        // Configure the object
        m_configurer.configure( test, config, m_adaptor );

        // Check result
        final ConfigTest6 expected = new ConfigTest6();
        expected.add( new MyType1() );
        assertEquals( expected, test );
    }

    /**
     * Tests to see if typed adder works, with Configuration type.
     */
    public void testTypedConfigAdder()
        throws Exception
    {
        // Setup test data
        final DefaultConfiguration config = new DefaultConfiguration( "test", "test" );
        final DefaultConfiguration child1 = new DefaultConfiguration( "my-type1", "test" );
        final DefaultConfiguration child2 = new DefaultConfiguration( "my-type2", "test" );
        config.addChild( child1 );
        config.addChild( child2 );

        final ConfigTest7 test = new ConfigTest7();

        // Configure the object
        m_configurer.configure( test, config, m_adaptor );

        final ConfigTest7 expected = new ConfigTest7();
        expected.add( child1 );
        expected.add( child2 );
        assertEquals( expected, test );
    }

    /**
     * Tests to see if adder works, with Configuration objects.
     */
    public void testConfigAdder()
        throws Exception
    {
        // Setup test data
        final DefaultConfiguration config = new DefaultConfiguration( "test", "test" );
        final DefaultConfiguration child1 = new DefaultConfiguration( "config", "test" );
        final DefaultConfiguration child2 = new DefaultConfiguration( "config", "test" );
        config.addChild( child1 );
        config.addChild( child2 );

        final ConfigTest8 test = new ConfigTest8();

        // Configure the object
        m_configurer.configure( test, config, m_adaptor );

        final ConfigTest8 expected = new ConfigTest8();
        expected.addConfig( child1 );
        expected.addConfig( child2 );
        assertEquals( expected, test );
    }

    /**
     * Tests to check that Configurable is handled properly.
     */
    public void testConfigable()
        throws Exception
    {
        // Setup test data
        final DefaultConfiguration config = new DefaultConfiguration( "test", "test" );

        final ConfigTest9 test = new ConfigTest9();

        // Configure the object
        m_configurer.configure( test, config, m_adaptor );

        final ConfigTest9 expected = new ConfigTest9();
        expected.configure( config );
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
        m_configurer.configure( test, config, m_adaptor );

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
            m_configurer.configure( test, config, m_adaptor );
            fail();
        }
        catch( ConfigurationException e )
        {
            final String message = REZ.getString( "get-ref.error",
                                                  "unknown-prop" );
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
            m_configurer.configure( test, config, m_adaptor );
            fail();
        }
        catch( ConfigurationException e )
        {
            final String message = REZ.getString( "mismatch-ref-types.error",
                                                  "prop-a",
                                                  String.class.getName(),
                                                  ConfigTest2.class.getName() );
            assertSameMessage( message, e );
        }
    }

    /**
     * Tests using a reference with a typed adder.  Tests using an attribute
     * and a nested element.
     */
    public void testTypedAdderReference()
        throws Exception
    {
        // Setup test data
        final DefaultConfiguration config = new DefaultConfiguration( "test", "test" );
        config.setAttribute( "my-role1-ref", "id" );
        final DefaultConfiguration child = new DefaultConfiguration( "my-role1-ref", "test" );
        child.setAttribute( "id", "id2" );
        config.addChild( child );

        // Add role mapping, and add to reference to context
        final RoleManager roleMgr = (RoleManager)getServiceManager().lookup( RoleManager.ROLE );
        final RoleInfo roleInfo = new RoleInfo( "my-role1", MyRole1.class );
        roleMgr.addRole( roleInfo );
        m_context.setProperty( "id", new MyType1() );
        m_context.setProperty( "id2", new MyType2() );

        final ConfigTest6 test = new ConfigTest6();

        // Configure the object
        m_configurer.configure( test, config, m_adaptor );

        // Compare against expected value
        final ConfigTest6 expected = new ConfigTest6();
        expected.add( new MyType1() );
        expected.add( new MyType2() );
        assertEquals( expected, test );
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
            m_configurer.configure( test, config, m_adaptor );
            fail();
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
    public void testIgnoreStringMethods()
        throws Exception
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
        m_configurer.configure( test, config, m_adaptor );

        // Test expected value
        final ConfigTest3 expected = new ConfigTest3();
        expected.setProp1( new ConfigTest1() );
        expected.setProp2( new ConfigTest1() );
        expected.addProp3( new ConfigTest1() );
        assertEquals( expected, test );
    }
}
