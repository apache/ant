/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.tools.ant.util.regexp;

import java.util.Vector;
import org.apache.myrmidon.api.TaskException;
import org.apache.oro.text.regex.MatchResult;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.Perl5Compiler;
import org.apache.oro.text.regex.Perl5Matcher;

/**
 * Implementation of RegexpMatcher for Jakarta-ORO.
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @author <a href="mailto:mattinger@mindless.com">Matthew Inger</a>
 */
public class JakartaOroMatcher implements RegexpMatcher
{
    protected final Perl5Compiler compiler = new Perl5Compiler();
    protected final Perl5Matcher matcher = new Perl5Matcher();

    private String pattern;

    public JakartaOroMatcher()
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
     * Returns a Vector of matched groups found in the argument. <p>
     *
     * Group 0 will be the full match, the rest are the parenthesized
     * subexpressions</p> .
     *
     * @param argument Description of Parameter
     * @return The Groups value
     * @exception TaskException Description of Exception
     */
    public Vector getGroups( String argument )
        throws TaskException
    {
        return getGroups( argument, MATCH_DEFAULT );
    }

    /**
     * Returns a Vector of matched groups found in the argument. <p>
     *
     * Group 0 will be the full match, the rest are the parenthesized
     * subexpressions</p> .
     *
     * @param input Description of Parameter
     * @param options Description of Parameter
     * @return The Groups value
     * @exception TaskException Description of Exception
     */
    public Vector getGroups( String input, int options )
        throws TaskException
    {
        if( !matches( input, options ) )
        {
            return null;
        }
        Vector v = new Vector();
        MatchResult mr = matcher.getMatch();
        int cnt = mr.groups();
        for( int i = 0; i < cnt; i++ )
        {
            v.addElement( mr.group( i ) );
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
        return this.pattern;
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
        Pattern p = getCompiledPattern( options );
        return matcher.contains( input, p );
    }

    /**
     * Get a compiled representation of the regexp pattern
     *
     * @param options Description of Parameter
     * @return The CompiledPattern value
     * @exception TaskException Description of Exception
     */
    protected Pattern getCompiledPattern( int options )
        throws TaskException
    {
        try
        {
            // compute the compiler options based on the input options first
            Pattern p = compiler.compile( pattern, getCompilerOptions( options ) );
            return p;
        }
        catch( Exception e )
        {
            throw new TaskException( "Error", e );
        }
    }

    protected int getCompilerOptions( int options )
    {
        int cOptions = Perl5Compiler.DEFAULT_MASK;

        if( RegexpUtil.hasFlag( options, MATCH_CASE_INSENSITIVE ) )
        {
            cOptions |= Perl5Compiler.CASE_INSENSITIVE_MASK;
        }
        if( RegexpUtil.hasFlag( options, MATCH_MULTILINE ) )
        {
            cOptions |= Perl5Compiler.MULTILINE_MASK;
        }
        if( RegexpUtil.hasFlag( options, MATCH_SINGLELINE ) )
        {
            cOptions |= Perl5Compiler.SINGLELINE_MASK;
        }

        return cOptions;
    }

}
