/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.framework;

import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * Named collection of include/exclude tags. <p>
 *
 * @author <a href="mailto:ajkuiper@wxs.nl">Arnout J. Kuiper</a>
 * @author <a href="mailto:stefano@apache.org">Stefano Mazzocchi</a>
 * @author <a href="mailto:rubys@us.ibm.com">Sam Ruby</a>
 * @author <a href="mailto:jon@clearink.com">Jon S. Stevens</a>
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 */
public class PatternSet
{
    private ArrayList m_includes = new ArrayList();
    private ArrayList m_excludes = new ArrayList();

    /**
     * Sets the set of exclude patterns. Patterns may be separated by a comma or
     * a space.
     *
     * @param excludes the string containing the exclude patterns
     */
    public void setExcludes( final String excludes )
    {
        final Pattern[] patterns = parsePatterns( excludes );
        for( int i = 0; i < patterns.length; i++ )
        {
            addExclude( patterns[ i ] );
        }
    }

    /**
     * Sets the set of include patterns. Patterns may be separated by a comma or
     * a space.
     *
     * @param includes the string containing the include patterns
     */
    public void setIncludes( final String includes )
    {
        final Pattern[] patterns = parsePatterns( includes );
        for( int i = 0; i < patterns.length; i++ )
        {
            addInclude( patterns[ i ] );
        }
    }

    /**
     * add a name entry on the exclude list
     */
    public void addExclude( final Pattern pattern )
    {
        m_excludes.add( pattern );
    }

    /**
     * add a name entry on the include list
     */
    public void addInclude( final Pattern pattern )
    {
        m_includes.add( pattern );
    }

    public final ArrayList getIncludes()
    {
        return m_includes;
    }

    public final ArrayList getExcludes()
    {
        return m_excludes;
    }

    /**
     * Adds the patterns of the other instance to this set.
     */
    public void append( final PatternSet other )
    {
        m_includes.addAll( other.m_includes );
        m_excludes.addAll( other.m_excludes );
    }

    public String toString()
    {
        return "PatternSet [ includes: " + m_includes +
            " excludes: " + m_excludes + " ]";
    }

    private Pattern[] parsePatterns( final String patternString )
    {
        final ArrayList patterns = new ArrayList();
        if( patternString != null && patternString.length() > 0 )
        {
            StringTokenizer tok = new StringTokenizer( patternString, ", ", false );
            while( tok.hasMoreTokens() )
            {
                final Pattern pattern = new Pattern( tok.nextToken() );
                patterns.add( pattern );
            }
        }

        return (Pattern[])patterns.toArray( new Pattern[ patterns.size() ] );
    }
}
