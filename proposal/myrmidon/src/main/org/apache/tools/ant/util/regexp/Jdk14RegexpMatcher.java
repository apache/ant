/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.util.regexp;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.apache.myrmidon.api.TaskException;

/**
 * Implementation of RegexpMatcher for the built-in regexp matcher of JDK 1.4.
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @author Matthew Inger <a href="mailto:mattinger@mindless.com">
 *      mattinger@mindless.com</a>
 */
public class Jdk14RegexpMatcher implements RegexpMatcher
{

    private String pattern;

    public Jdk14RegexpMatcher()
    {
    }

    /**
     * Set the regexp pattern from the String description.
     *
     * @param pattern The new Pattern value
     */
    public void setPattern( String pattern )
    {
        this.pattern = pattern;
    }

    /**
     * Returns a ArrayList of matched groups found in the argument. <p>
     *
     * Group 0 will be the full match, the rest are the parenthesized
     * subexpressions</p> .
     *
     * @param argument Description of Parameter
     * @return The Groups value
     * @exception TaskException Description of Exception
     */
    public ArrayList getGroups( String argument )
        throws TaskException
    {
        return getGroups( argument, MATCH_DEFAULT );
    }

    /**
     * Returns a ArrayList of matched groups found in the argument. <p>
     *
     * Group 0 will be the full match, the rest are the parenthesized
     * subexpressions</p> .
     *
     * @param input Description of Parameter
     * @param options Description of Parameter
     * @return The Groups value
     * @exception TaskException Description of Exception
     */
    public ArrayList getGroups( String input, int options )
        throws TaskException
    {
        Pattern p = getCompiledPattern( options );
        Matcher matcher = p.matcher( input );
        if( !matcher.find() )
        {
            return null;
        }
        ArrayList v = new ArrayList();
        int cnt = matcher.groupCount();
        for( int i = 0; i <= cnt; i++ )
        {
            v.add( matcher.group( i ) );
        }
        return v;
    }

    /**
     * Get a String representation of the regexp pattern
     *
     * @return The Pattern value
     */
    public String getPattern()
    {
        return pattern;
    }

    /**
     * Does the given argument match the pattern?
     *
     * @param argument Description of Parameter
     * @return Description of the Returned Value
     * @exception TaskException Description of Exception
     */
    public boolean matches( String argument )
        throws TaskException
    {
        return matches( argument, MATCH_DEFAULT );
    }

    /**
     * Does the given argument match the pattern?
     *
     * @param input Description of Parameter
     * @param options Description of Parameter
     * @return Description of the Returned Value
     * @exception TaskException Description of Exception
     */
    public boolean matches( String input, int options )
        throws TaskException
    {
        try
        {
            Pattern p = getCompiledPattern( options );
            return p.matcher( input ).find();
        }
        catch( Exception e )
        {
            throw new TaskException( "Error", e );
        }
    }

    protected Pattern getCompiledPattern( int options )
        throws TaskException
    {
        int cOptions = getCompilerOptions( options );
        try
        {
            Pattern p = Pattern.compile( this.pattern, cOptions );
            return p;
        }
        catch( PatternSyntaxException e )
        {
            throw new TaskException( e );
        }
    }

    protected int getCompilerOptions( int options )
    {
        int cOptions = 0;

        if( RegexpUtil.hasFlag( options, MATCH_CASE_INSENSITIVE ) ) {
            cOptions |= Pattern.CASE_INSENSITIVE;
        }
        if( RegexpUtil.hasFlag( options, MATCH_MULTILINE ) ) {
            cOptions |= Pattern.MULTILINE;
        }
        if( RegexpUtil.hasFlag( options, MATCH_SINGLELINE ) ) {
            cOptions |= Pattern.DOTALL;
        }

        return cOptions;
    }

}
