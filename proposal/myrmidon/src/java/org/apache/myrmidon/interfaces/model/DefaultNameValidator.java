/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.interfaces.model;

import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;

/**
 * Simple helper class which determines the validity of names used
 * in ant projects.
 *
 * @author <a href="mailto:darrell@apache.org">Darrell DeBoer</a>
 * @version $Revision$ $Date$
 */
public class DefaultNameValidator
    implements NameValidator
{
    private final static Resources REZ =
        ResourceManager.getPackageResources( DefaultNameValidator.class );

    /**
     * Determines whether the supplied name may include surrounding whitespace.
     */
    private boolean m_allowSurroundingWhitespace;

    // Settings for initial characters.
    private boolean m_allowInitialDigit;
    private String m_additionalInitialCharacters;

    // Settings for internal characters.
    private boolean m_allowInternalDigits;
    private boolean m_allowInternalWhitespace;
    private String m_additionalInternalCharacters;

    /**
     * Construct a default name validator.
     * Letters, digits and "_" are permitted as initial character.
     * Letters, digits, whitespace and "_-." are permitted as internal characters.
     * Surrounding whitespace is not permitted.
     */
    public DefaultNameValidator()
    {
        this( false, true, "_", true, true, "_-." );
    }

    /**
     * Contstruct a NameValidator with the specified rules.
     * @param allowSurroundingWhitespace
     *          specified if names are trimmed before checking
     * @param allowInitialDigit
     *          specifies if digits are permitted as intial characters
     * @param additionalInitialCharacters
     *          extra characters to allow as initial characters.
     * @param allowInternalDigits
     *          specifies if digits are permitted as internal characters
     * @param allowInternalWhitespace
     *          specifies if whitespace is permitted internally in names
     * @param additionalInternalCharacters
     *          extra characters permitted in names
     */
    public DefaultNameValidator( final boolean allowSurroundingWhitespace,
                                 final boolean allowInitialDigit,
                                 final String additionalInitialCharacters,
                                 final boolean allowInternalDigits,
                                 final boolean allowInternalWhitespace,
                                 final String additionalInternalCharacters )
    {
        setAllowSurroundingWhitespace( allowSurroundingWhitespace );
        setAllowInitialDigit( allowInitialDigit );
        setAdditionalInitialCharacters( additionalInitialCharacters );
        setAllowInternalDigits( allowInternalDigits );
        setAllowInternalWhitespace( allowInternalWhitespace );
        setAdditionalInternalCharacters( additionalInternalCharacters );
    }

    /**
     * Creates a valid name based on the supplied string value, removing invalid
     * characters. If no valid characters are present, an exception is thrown.
     * @param baseName the name used to construct the valid name
     * @throws Exception if no valid name could be constructed.
     */
    public String makeValidName( final String baseName ) throws Exception
    {
        final StringBuffer buffer = new StringBuffer( baseName );
        while( buffer.length() > 0 && !isValidInitialChar( buffer.charAt( 0 ) ) )
        {
            buffer.delete( 0, 1 );
        }
        if( buffer.length() == 0 )
        {
            final String message = REZ.getString( "name.could-not-create.error", baseName );
            throw new Exception( message );
        }

        for( int i = 1; i < buffer.length(); )
        {
            if( !isValidInternalChar( buffer.charAt( i ) ) )
            {
                buffer.delete( i, i + 1 );
            }
            else
            {
                i++;
            }
        }

        return buffer.toString();
    }

    /**
     * Validates the supplied name, failing if it is not.
     * @throws Exception is the supplied name is not valid.
     */
    public void validate( final String name ) throws Exception
    {
        String testName = name;

        // If surrounding whitespace is allowed, trim it. Otherwise, check.
        if( m_allowSurroundingWhitespace )
        {
            testName = testName.trim();
        }
        else
        {
            checkSurroundingWhitespace( testName );
        }

        // Zero-length name is invalid.
        if( testName.length() == 0 )
        {
            final String message = REZ.getString( "name.zero-char-name.error" );
            throw new Exception( message );
        }

        // Check first character.
        final char initial = testName.charAt( 0 );
        checkInitialCharacter( initial, testName );

        // Check the rest of the characters.
        for( int i = 1; i < testName.length(); i++ )
        {
            final char internal = testName.charAt( i );
            checkInternalCharacter( internal, testName );
        }
    }

    /**
     * Checks if the supplied character is permitted as an internal character.
     * @throws Exception if the character is not permitted
     */
    private void checkInternalCharacter( final char internal, final String name )
        throws Exception
    {
        if( !isValidInternalChar( internal ) )
        {
            final String message = REZ.getString( "name.invalid-internal-char.error",
                                                  name,
                                                  describeValidInternalChars() );
            throw new Exception( message );
        }
    }

    /**
     * Checks if the supplied character is permitted as an internal character.
     * @throws Exception if the character is not permitted
     */
    private void checkInitialCharacter( final char initial, final String name )
        throws Exception
    {
        if( !isValidInitialChar( initial ) )
        {
            final String message = REZ.getString( "name.invalid-initial-char.error",
                                                  name,
                                                  describeValidInitialChars() );
            throw new Exception( message );
        }
    }

    /**
     * Checks the name for surrounding whitespace
     * @throws Exception if surrounding whitespace is found
     */
    private void checkSurroundingWhitespace( final String testName )
        throws Exception
    {
        if( testName.length() == 0 )
        {
            return;
        }

        if( Character.isWhitespace( testName.charAt( 0 ) ) ||
            Character.isWhitespace( testName.charAt( testName.length() - 1 ) ) )
        {
            final String message =
                REZ.getString( "name.enclosing-whitespace.error", testName );
            throw new Exception( message );
        }
    }

    /**
     * Determines if a character is allowed as the first character in a name.
     * Valid characters are Letters, Digits, and defined initial characters ("_").
     * @param chr the character to be assessed
     * @return <code>true</code> if the character can be the first character of a name
     */
    protected boolean isValidInitialChar( final char chr )
    {
        if( Character.isLetter( chr ) )
        {
            return true;
        }

        if( m_allowInitialDigit
            && Character.isDigit( chr ) )
        {
            return true;
        }

        if( m_additionalInitialCharacters.indexOf( chr ) != -1 )
        {
            return true;
        }

        return false;
    }

    /**
     * Determines if a character is allowed as a non-initial character in a name.
     * Valid characters are Letters, Digits, whitespace, and defined
     * internal characters ("_-.").
     * @param chr the character to be assessed
     * @return <code>true</code> if the character can be included in a name
     */
    protected boolean isValidInternalChar( final char chr )
    {
        if( Character.isLetter( chr ) )
        {
            return true;
        }

        if( m_allowInternalDigits
            && Character.isDigit( chr ) )
        {
            return true;
        }

        if( m_allowInternalWhitespace
            && Character.isWhitespace( chr ) )
        {
            return true;
        }

        if( m_additionalInternalCharacters.indexOf( chr ) != -1 )
        {
            return true;
        }

        return false;
    }

    /**
     * Builds a message detailing the valid initial characters.
     */
    protected String describeValidInitialChars()
    {
        StringBuffer validChars = new StringBuffer( "letters" );
        if( m_allowInitialDigit )
        {
            validChars.append( ", digits" );
        }
        validChars.append( ", and \"" );
        validChars.append( m_additionalInitialCharacters );
        validChars.append( "\"" );
        return validChars.toString();
    }

    /**
     * Builds a message detailing the valid internal characters.
     */
    protected String describeValidInternalChars()
    {
        StringBuffer validChars = new StringBuffer( "letters" );
        if( m_allowInternalDigits )
        {
            validChars.append( ", digits" );
        }
        if( m_allowInternalWhitespace )
        {
            validChars.append( ", whitespace" );
        }
        validChars.append( ", and \"" );
        validChars.append( m_additionalInternalCharacters );
        validChars.append( "\"" );
        return validChars.toString();
    }

    /**
     * @param allowSurroundingWhitespace
     *          specified if names are trimmed before checking
     */
    public void setAllowSurroundingWhitespace( boolean allowSurroundingWhitespace )
    {
        m_allowSurroundingWhitespace = allowSurroundingWhitespace;
    }

    /**
     * @param allowInitialDigit
     *          specifies if digits are permitted as intial characters
     */
    public void setAllowInitialDigit( boolean allowInitialDigit )
    {
        m_allowInitialDigit = allowInitialDigit;
    }

    /**
     * @param additionalInitialCharacters
     *          extra characters to allow as initial characters.
     */
    public void setAdditionalInitialCharacters( String additionalInitialCharacters )
    {
        m_additionalInitialCharacters = additionalInitialCharacters;
    }

    /**
     * @param allowInternalDigits
     *          specifies if digits are permitted as internal characters
     */
    public void setAllowInternalDigits( boolean allowInternalDigits )
    {
        m_allowInternalDigits = allowInternalDigits;
    }

    /**
     * @param allowInternalWhitespace
     *          specifies if whitespace is permitted internally in names
     */
    public void setAllowInternalWhitespace( boolean allowInternalWhitespace )
    {
        m_allowInternalWhitespace = allowInternalWhitespace;
    }

    /**
     * @param additionalInternalCharacters
     *          extra characters permitted in names
     */
    public void setAdditionalInternalCharacters( String additionalInternalCharacters )
    {
        m_additionalInternalCharacters = additionalInternalCharacters;
    }

}
