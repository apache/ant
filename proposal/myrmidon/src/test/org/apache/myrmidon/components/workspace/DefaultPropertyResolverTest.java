/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.components.workspace;

import java.io.File;
import java.util.Date;
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.avalon.framework.context.Context;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.components.AbstractComponentTest;
import org.apache.myrmidon.components.property.DefaultPropertyResolver;
import org.apache.myrmidon.interfaces.property.PropertyResolver;

/**
 * Functional tests for {@link DefaultPropertyResolver}.
 *
 * @author <a href="mailto:darrell@apache.org">Darrell DeBoer</a>
 * @version $Revision$ $Date$
 */
public class DefaultPropertyResolverTest
    extends AbstractComponentTest
{
    protected final static Resources REZ
        = ResourceManager.getPackageResources( DefaultPropertyResolver.class );

    protected PropertyResolver m_resolver;
    protected DefaultTaskContext m_context;

    public DefaultPropertyResolverTest( String name )
    {
        super( name );
    }

    protected void setUp() throws Exception
    {
        super.setUp();

        m_resolver = createResolver();

        m_context = new DefaultTaskContext( null, getServiceManager(), getLogger() );
        m_context.setProperty( "intProp", new Integer( 333 ) );
        m_context.setProperty( "stringProp", "String property" );
    }

    protected PropertyResolver createResolver()
    {
        return new DefaultPropertyResolver();
    }

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
    private void testPropertyValue( Object propObject )
        throws Exception
    {
        m_context.setProperty( "typedProp", propObject );
        String propString = propObject.toString();

        doTestResolution( "${typedProp}", propObject, m_context );
        doTestResolution( "${typedProp} with following text",
                          propString + " with following text", m_context );
        doTestResolution( "Preceding text with ${typedProp}",
                          "Preceding text with " + propString, m_context );
    }

    /**
     * Tests multiple property declarations in a single value.
     */
    public void testMultipleProperties() throws Exception
    {
        m_context.setProperty( "prop1", "value1" );
        m_context.setProperty( "prop2", "value2" );
        m_context.setProperty( "int1", new Integer( 123 ) );

        doTestResolution( "${prop1}${prop2}", "value1value2", m_context );
        doTestResolution( "${prop1}${prop1}${prop1}", "value1value1value1", m_context );
        doTestResolution( "before ${prop2} between ${prop1} after",
                          "before value2 between value1 after", m_context );
        doTestResolution( "${prop1}-${int1}-${prop2}", "value1-123-value2", m_context );

    }

    /**
     * Tests handing undefined property.
     */
    public void testUndefinedProp() throws Exception
    {
        String undefinedProp = "undefinedProperty";
        doTestFailure( "${" + undefinedProp + "}",
                       REZ.getString( "prop.missing-value.error", undefinedProp ),
                       m_context );

        //TODO - "" should be disallowed as a property name
        doTestFailure( "${}",
                       REZ.getString( "prop.missing-value.error", "" ),
                       m_context );
    }

    /**
     * Tests illegal property syntax.
     */
    public void testInvalidTypeDeclarations() throws Exception
    {

        doTestFailure( "${unclosed",
                       REZ.getString( "prop.mismatched-braces.error" ),
                       m_context );
        doTestFailure( "${",
                       REZ.getString( "prop.mismatched-braces.error" ),
                       m_context );

        /* TODO - need to handle these cases. */
        //        testFailure( "${bad${}", "", m_context );
        //        testFailure( "${ }", "", m_context );

    }

    /**
     * Resolves the property using the supplied context, and checks the result.
     */
    protected void doTestResolution( String value,
                                     Object expected,
                                     Context context )
        throws Exception
    {
        Object resolved = m_resolver.resolveProperties( value, context );

        assertEquals( expected, resolved );
    }

    /**
     * Attempts to resolve the value using the supplied context, expecting to
     * fail with the supplied error message.
     */
    protected void doTestFailure( String value,
                                  String expectedErrorMessage,
                                  Context context )
    {
        try
        {
            m_resolver.resolveProperties( value, context );
            fail( "Unexpected sucess - test should have failed." );
        }
        catch( TaskException e )
        {
            assertSameMessage( expectedErrorMessage, e );
        }
    }
}
