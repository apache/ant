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
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.components.AbstractComponentTest;
import org.apache.myrmidon.components.workspace.DefaultTaskContext;
import org.apache.myrmidon.framework.DataType;
import org.apache.myrmidon.interfaces.configurer.Configurer;
import org.apache.myrmidon.interfaces.role.RoleInfo;

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

        final ConfigTestStringProps test = new ConfigTestStringProps();

        // Configure the object
        m_configurer.configure( test, config, m_context );

        // Check result
        final ConfigTestStringProps expected = new ConfigTestStringProps();
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
        registerConverter( StringToIntegerConverter.class, String.class, Integer.class );

        final ConfigTestPrimConvert test = new ConfigTestPrimConvert();

        // Configure the object
        m_configurer.configure( test, config, m_context );

        // Check result
        final ConfigTestPrimConvert expected = new ConfigTestPrimConvert();
        expected.setIntProp( 90 );
        expected.setIntegerProp( new Integer( -401 ) );
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

        final ConfigTestStringProps test = new ConfigTestStringProps();

        // Configure the object
        try
        {
            m_configurer.configure( test, config, m_context );
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

        final ConfigTestObjectProps test = new ConfigTestObjectProps();

        // Configure the object
        m_configurer.configure( test, config, m_context );

        // Check result
        final ConfigTestObjectProps expected = new ConfigTestObjectProps();
        ConfigTestStringProps elem = new ConfigTestStringProps();
        elem.setSomeProp( value1 );
        expected.setProp( elem );
        elem = new ConfigTestStringProps();
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

        final ConfigTestStringProps test = new ConfigTestStringProps();

        // Configure the object
        try
        {
            m_configurer.configure( test, config, m_context );
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

        final ConfigTestStringProps test = new ConfigTestStringProps();

        // Configure the object
        m_configurer.configure( test, config, m_context );

        // Check result
        final ConfigTestStringProps expected = new ConfigTestStringProps();
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

        final ConfigTestObjectProps test = new ConfigTestObjectProps();

        // Configure the object
        try
        {
            m_configurer.configure( test, config, m_context );
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

        final ConfigTestStringProps test = new ConfigTestStringProps();

        m_context.setProperty( "prop-a", "other" );

        // Configure the object
        m_configurer.configure( test, config, m_context );

        // Check the configured object
        final ConfigTestStringProps expected = new ConfigTestStringProps();
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

        final ConfigTestStringProps test = new ConfigTestStringProps();

        m_context.setProperty( "prop-a", "some value" );

        // Configure the object
        m_configurer.configure( test, config, m_context );

        // Check the configured object
        final ConfigTestStringProps expected = new ConfigTestStringProps();
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

        final ConfigTestStringProps test = new ConfigTestStringProps();

        m_context.setProperty( "prop-a", "some value" );

        // Configure the object
        m_configurer.configure( test, config, m_context );

        // Check the configured object
        final ConfigTestStringProps expected = new ConfigTestStringProps();
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

        final ConfigTestStringProps test = new ConfigTestStringProps();

        try
        {
            // Configure the object
            m_configurer.configure( test, config, m_context );
            fail();
        }
        catch( ConfigurationException e )
        {
            final String[] messages = {
                REZ.getString( "bad-configure-element.error", "some-prop-ref" ),
                REZ.getString( "extra-config-for-ref.error" )
            };
            assertSameMessage( messages, e );
        }
    }

    /**
     * Tests reference type conversion.
     */
    public void testReferenceConversion() throws Exception
    {
        // Setup test data
        final DefaultConfiguration config = new DefaultConfiguration( "test", "test" );
        config.setAttribute( "prop-a-ref", "id" );

        final Integer refValue = new Integer( 21 );
        m_context.setProperty( "id", refValue );

        registerConverter( ObjectToMyRole1Converter.class, Object.class, MyRole1.class );

        final ConfigTestInterfaceProp test = new ConfigTestInterfaceProp();

        // Configure
        m_configurer.configure( test, config, m_context );

        // Check result
        final ConfigTestInterfaceProp expected = new ConfigTestInterfaceProp();
        expected.addPropA( new MyRole1Adaptor( refValue ) );
        assertEquals( expected, test );
    }

    /**
     * Tests that the role's default type is used for interface typed
     * elements.
     */
    public void testInterfaceAdder()
        throws Exception
    {
        // Setup test data
        final DefaultConfiguration config = new DefaultConfiguration( "test", "test" );
        final DefaultConfiguration child = new DefaultConfiguration( "prop-a", "test" );
        config.addChild( child );

        registerRole( new RoleInfo( "myrole1", null, MyRole1.class, "default-type" ) );
        registerType( MyRole1.class, "default-type", MyType1.class );

        final ConfigTestInterfaceProp test = new ConfigTestInterfaceProp();

        // Configure object
        m_configurer.configure( test, config, m_context );

        // Check result
        final ConfigTestInterfaceProp expected = new ConfigTestInterfaceProp();
        expected.addPropA( new MyType1() );
        assertEquals( expected, test );
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
            m_configurer.configure( test, config, m_context );
            fail();
        }
        catch( final ConfigurationException ce )
        {
            final String[] messages = {
                REZ.getString( "bad-configure-element.error", "test" ),
                REZ.getString( "typed-adder-non-interface.error",
                               ConfigTest4.class.getName(),
                               Integer.class.getName() )
            };
            assertSameMessage( messages, ce );
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

        final ConfigTestMultiTypedAdder test = new ConfigTestMultiTypedAdder();

        try
        {
            // Configure the object
            m_configurer.configure( test, config, m_context );
            fail();
        }
        catch( final ConfigurationException ce )
        {
            final String[] messages = {
                REZ.getString( "bad-configure-element.error", "test" ),
                REZ.getString( "multiple-adder-methods-for-element.error",
                               ConfigTestMultiTypedAdder.class.getName(),
                               "" )
            };
            assertSameMessage( messages, ce );
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

        registerType( DataType.class, "my-type1", MyType1.class );
        registerType( DataType.class, "my-type2", MyType2.class );

        final ConfigTestTypedProp test = new ConfigTestTypedProp();

        // Configure the object
        m_configurer.configure( test, config, m_context );

        final ConfigTestTypedProp expected = new ConfigTestTypedProp();
        expected.add( new MyType1() );
        expected.add( new MyType2() );
        assertEquals( expected, test );
    }

    /**
     * Tests to check that role is used for typed adder.
     */
    public void testTypedAdderRole()
        throws Exception
    {
        // Setup test data
        final DefaultConfiguration config = new DefaultConfiguration( "test", "test" );
        final DefaultConfiguration child1 = new DefaultConfiguration( "my-type1", "test" );
        config.addChild( child1 );

        // Register incompatible types with the same name, as data-type and myrole1.
        registerRole( new RoleInfo( "myrole1", "myrole1", MyRole1.class ) );
        registerType( MyRole1.class, "my-type1", MyType1.class );
        registerType( DataType.class, "my-type1", StringBuffer.class );

        final ConfigTestTypedProp test = new ConfigTestTypedProp();

        // Configure the object
        m_configurer.configure( test, config, m_context );

        // Check the result
        final ConfigTestTypedProp expected = new ConfigTestTypedProp();
        expected.add( new MyType1() );
        assertEquals( expected, test );
    }

    /**
     * Tests conversion with a typed adder.
     */
    public void testTypedAdderConversion()
        throws Exception
    {
        // Setup test data
        final DefaultConfiguration config = new DefaultConfiguration( "test", "test" );
        final DefaultConfiguration child = new DefaultConfiguration( "some-type", "test" );
        child.setAttribute( "prop", "some value" );
        config.addChild( child );

        registerType( DataType.class, "some-type", ConfigTestStringProps.class );
        registerConverter( ObjectToMyRole1Converter.class, Object.class, MyRole1.class );

        final ConfigTestTypedProp test = new ConfigTestTypedProp();

        // Configure the object
        m_configurer.configure( test, config, m_context );

        // Check the result
        final ConfigTestTypedProp expected = new ConfigTestTypedProp();
        final ConfigTestStringProps nested = new ConfigTestStringProps();
        nested.addProp( "some value" );
        expected.add( new MyRole1Adaptor( nested ) );
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
        registerRole( new RoleInfo( "my-role1", MyRole1.class ) );
        registerConverter( ObjectToMyRole1Converter.class, String.class, MyRole1.class );

        final ConfigTestTypedProp test = new ConfigTestTypedProp();

        // Configure the object
        m_configurer.configure( test, config, m_context );

        // Check result
        final ConfigTestTypedProp expected = new ConfigTestTypedProp();
        expected.add( new MyRole1Adaptor( "some value" ) );
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

        final ConfigTestTypedConfigProp test = new ConfigTestTypedConfigProp();

        // Configure the object
        m_configurer.configure( test, config, m_context );

        final ConfigTestTypedConfigProp expected = new ConfigTestTypedConfigProp();
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

        final ConfigTestConfigProps test = new ConfigTestConfigProps();

        // Configure the object
        m_configurer.configure( test, config, m_context );

        final ConfigTestConfigProps expected = new ConfigTestConfigProps();
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

        final ConfigTestConfigurable test = new ConfigTestConfigurable();

        // Configure the object
        m_configurer.configure( test, config, m_context );

        final ConfigTestConfigurable expected = new ConfigTestConfigurable();
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

        final ConfigTestStringProps test = new ConfigTestStringProps();

        m_context.setProperty( "id", "prop-a" );
        m_context.setProperty( "prop-a", "some indirect value" );

        // Configure the object
        m_configurer.configure( test, config, m_context );

        // Check the configured object
        final ConfigTestStringProps expected = new ConfigTestStringProps();
        expected.setSomeProp( "some indirect value" );
        assertEquals( expected, test );
    }

    /**
     * Tests an unknown reference.
     */
    public void testUnknownReference()
        throws Exception
    {
        // Setup test data
        final DefaultConfiguration config = new DefaultConfiguration( "test", "test" );
        config.setAttribute( "some-prop-ref", "unknown-prop" );

        final ConfigTestStringProps test = new ConfigTestStringProps();

        // Configure the object
        try
        {
            m_configurer.configure( test, config, m_context );
            fail();
        }
        catch( ConfigurationException e )
        {
            final String[] messages = {
                REZ.getString( "bad-set-attribute.error", "test", "some-prop-ref" ),
                REZ.getString( "unknown-reference.error", "unknown-prop" )
            };
            assertSameMessage( messages, e );
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

        final ConfigTestStringProps test = new ConfigTestStringProps();

        m_context.setProperty( "prop-a", new ConfigTestObjectProps() );

        // Configure the object
        try
        {
            m_configurer.configure( test, config, m_context );
            fail();
        }
        catch( ConfigurationException e )
        {
            final String[] messages = {
                REZ.getString( "bad-set-attribute.error", "test", "some-prop-ref" ),
                REZ.getString( "mismatch-ref-types.error",
                               "prop-a",
                               "some-prop" )
            };
            assertSameMessage( messages, e );
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
        registerRole( new RoleInfo( "my-role1", MyRole1.class ) );
        m_context.setProperty( "id", new MyType1() );
        m_context.setProperty( "id2", new MyType2() );

        final ConfigTestTypedProp test = new ConfigTestTypedProp();

        // Configure the object
        m_configurer.configure( test, config, m_context );

        // Compare against expected value
        final ConfigTestTypedProp expected = new ConfigTestTypedProp();
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

        final ConfigTestObjectProps test = new ConfigTestObjectProps();

        try
        {
            // Configure the object
            m_configurer.configure( test, config, m_context );
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

        final ConfigTestMultiSetter test = new ConfigTestMultiSetter();

        // Configure the object
        m_configurer.configure( test, config, m_context );

        // Test expected value
        final ConfigTestMultiSetter expected = new ConfigTestMultiSetter();
        expected.setProp1( new ConfigTestStringProps() );
        expected.setProp2( new ConfigTestStringProps() );
        expected.addProp3( new ConfigTestStringProps() );
        assertEquals( expected, test );
    }
}
