/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.property.test;

import java.io.File;
import java.util.Date;
import org.apache.aut.converter.lib.ObjectToStringConverter;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.components.AbstractComponentTest;
import org.apache.myrmidon.components.property.DefaultPropertyResolver;
import org.apache.myrmidon.components.store.DefaultPropertyStore;
import org.apache.myrmidon.interfaces.property.PropertyResolver;
import org.apache.myrmidon.interfaces.property.PropertyStore;

/**
 * General-purpose property resolver test cases.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 */
public abstract class AbstractPropertyResolverTestCase
    extends AbstractComponentTest
{
    protected PropertyResolver m_resolver;
    protected PropertyStore m_store;

    public AbstractPropertyResolverTestCase( final String name )
    {
        super( name );
    }

    protected void setUp() throws Exception
    {
        m_resolver = (PropertyResolver)getServiceManager().lookup( PropertyResolver.ROLE );

        m_store = new DefaultPropertyStore();
        m_store.setProperty( "intProp", new Integer( 333 ) );
        m_store.setProperty( "stringProp", "String property" );

        registerConverter( ObjectToStringConverter.class, Object.class, String.class );
    }

    /**
     * Creates an instance of a component.  Sub-classes can override this
     * method to add a particular implementation to the set of test components.
     */
    protected Object createComponent( String role, Class defaultImpl )
        throws Exception
    {
        if( role.equals( PropertyResolver.ROLE) )
        {
            return createResolver();
        }
        else
        {
            return super.createComponent( role, defaultImpl );
        }
    }

    /**
     * Creates the resolver to test.
     */
    protected abstract PropertyResolver createResolver();

    /**
     * Test property resolution with various different typed properties.
     */
    public void testPropertyTypes() throws Exception
    {
        testPropertyValue( new String( "String value" ) );
        testPropertyValue( new Date() );
        testPropertyValue( new Integer( Integer.MIN_VALUE ) );
        testPropertyValue( new Double( 24234.98453 ) );
        testPropertyValue( this.getClass() );
        testPropertyValue( File.createTempFile( "PropertyResolverTest", null ) );
    }

    /**
     * Simple tests with property on it's own, and accompanied by text.
     */
    private void testPropertyValue( final Object propObject )
        throws Exception
    {
        m_store.setProperty( "typedProp", propObject );
        final String propString = propObject.toString();

        doTestResolution( "${typedProp}", propObject, m_store );
        doTestResolution( "${typedProp} with following text",
                          propString + " with following text", m_store );
        doTestResolution( "Preceding text with ${typedProp}",
                          "Preceding text with " + propString, m_store );
    }

    /**
     * Tests multiple property declarations in a single value.
     */
    public void testMultipleProperties() throws Exception
    {
        m_store.setProperty( "prop1", "value1" );
        m_store.setProperty( "prop2", "value2" );
        m_store.setProperty( "int1", new Integer( 123 ) );

        doTestResolution( "${prop1}${prop2}", "value1value2", m_store );
        doTestResolution( "${prop1}${prop1}${prop1}", "value1value1value1", m_store );
        doTestResolution( "before ${prop2} between ${prop1} after",
                          "before value2 between value1 after", m_store );
        doTestResolution( "${prop1}-${int1}-${prop2}", "value1-123-value2", m_store );
    }

    /**
     * Tests illegal property syntax.
     */
    public void testInvalidTypeDeclarations() throws Exception
    {
        final Resources rez = getResourcesForTested( DefaultPropertyResolver.class );
        doTestFailure( "${unclosed",
                       rez.getString( "prop.mismatched-braces.error" ),
                       m_store );
        doTestFailure( "${",
                       rez.getString( "prop.mismatched-braces.error" ),
                       m_store );

        /* TODO - need to handle these cases. */
        //        testFailure( "${bad${}", "", m_context );
        //        testFailure( "${ }", "", m_context );
    }

    /**
     * Resolves the property using the supplied context, and checks the result.
     */
    protected void doTestResolution( final String value,
                                     final Object expected,
                                     final PropertyStore properties )
        throws Exception
    {
        final Object resolved = m_resolver.resolveProperties( value, properties );

        assertEquals( expected, resolved );
    }

    /**
     * Attempts to resolve the value using the supplied context, expecting to
     * fail with the supplied error message.
     */
    protected void doTestFailure( final String value,
                                  final String expectedErrorMessage,
                                  final PropertyStore properties )
    {
        try
        {
            m_resolver.resolveProperties( value, properties );
            fail( "Unexpected sucess - test should have failed." );
        }
        catch( TaskException e )
        {
            assertSameMessage( expectedErrorMessage, e );
        }
    }
}
