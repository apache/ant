/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.interfaces.model.test;

import org.apache.myrmidon.AbstractMyrmidonTest;
import org.apache.myrmidon.interfaces.model.DefaultNameValidator;

/**
 * TestCases for {@link org.apache.myrmidon.interfaces.model.DefaultNameValidator}.
 *
 * @author <a href="mailto:darrell@apache.org">Darrell DeBoer</a>
 * @version $Revision$ $Date$
 */
public class DefaultNameValidatorTestCase
    extends AbstractMyrmidonTest
{
    private DefaultNameValidator m_validator = new DefaultNameValidator();

    public DefaultNameValidatorTestCase( String name )
    {
        super( name );
    }

    /**
     * Test valid names for the default validator.
     */
    public void testValidNames() throws Exception
    {
        testValid( "aName" );
        testValid( "123456" );
        testValid( "s p     a ce s" );
        testValid( "d-a-s-h-e-s-" );
        testValid( "d.o.t.s." );
        testValid( "_u_n_d_e_r_s_c_o_r_e_s" );
        testValid( "a" );
        testValid( "1" );
        testValid( "_" );

    }

    /**
     * Test invalid names for the default validator.
     */
    public void testInvalidNames() throws Exception
    {
        testInvalid( "" );
        testInvalid( " " );
        testInvalid( "           " );
        testInvalid( " bad" );
        testInvalid( "bad " );
        testInvalid( " bad " );
        testInvalid( "-dashfirst" );
        testInvalid( ".dotfirst" );
        testInvalid( "question?" );
    }

    /**
     * Test that certain characters are disallowed in the default validator.
     */
    public void testReservedChars() throws Exception
    {
        String reserved = "!@#$%^&*()+=~`{}[]|\\/?<>,:;";

        for( int pos = 0; pos < reserved.length(); pos++ )
        {
            char chr = reserved.charAt( pos );
            testReservedChar( chr );
        }
    }

    private void testReservedChar( char chr ) throws Exception
    {
        String test = "a" + String.valueOf( chr );
        testInvalid( test );
    }

    /**
     * Test validation using a restrictive set of validation rules.
     */
    public void testStrictNames() throws Exception
    {
        m_validator = new DefaultNameValidator( false, false, "", false, false, "." );

        testValid( "name" );
        testValid( "a" );
        testValid( "yep.ok" );

        testInvalid( "_nogood" );
        testInvalid( "no_good" );
        testInvalid( "nope1" );
        testInvalid( "123" );
        testInvalid( "not ok" );
    }

    private void testValid( String name )
    {
        try
        {
            m_validator.validate( name );
        }
        catch( Exception e )
        {
            fail( e.getMessage() );
        }
    }

    private void testInvalid( String name )
    {
        try
        {
            m_validator.validate( name );
            fail( "Name \"" + name + "\" should be invalid." );
        }
        catch( Exception e )
        {
        }
    }
}
