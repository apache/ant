/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.util.mappers;

import java.util.ArrayList;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.FileNameMapper;
import org.apache.tools.ant.util.regexp.RegexpMatcher;
import org.apache.tools.ant.util.regexp.RegexpMatcherFactory;

/**
 * Implementation of FileNameMapper that does regular expression replacements.
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 *
 * @ant:type type="mapper" name="regexp"
 */
public class RegexpPatternMapper
    implements FileNameMapper
{
    private RegexpMatcher m_matcher;
    private char[] m_to;
    private StringBuffer m_result = new StringBuffer();

    public RegexpPatternMapper()
        throws TaskException
    {
        m_matcher = ( new RegexpMatcherFactory() ).newRegexpMatcher();
    }

    /**
     * Sets the &quot;from&quot; pattern. Required.
     */
    public void setFrom( final String from )
        throws TaskException
    {
        try
        {
            m_matcher.setPattern( from );
        }
        catch( NoClassDefFoundError e )
        {
            // depending on the implementation the actual RE won't
            // get instantiated in the constructor.
            throw new TaskException( "Cannot load regular expression matcher", e );
        }
    }

    /**
     * Sets the &quot;to&quot; pattern. Required.
     *
     * @param to The new To value
     */
    public void setTo( final String to )
    {
        m_to = to.toCharArray();
    }

    /**
     * Returns null if the source file name doesn't match the &quot;from&quot;
     * pattern, an one-element array containing the translated file otherwise.
     *
     * @param sourceFileName Description of Parameter
     * @return Description of the Returned Value
     */
    public String[] mapFileName( final String sourceFileName, TaskContext context )
        throws TaskException
    {
        if( m_matcher == null || m_to == null ||
            !m_matcher.matches( sourceFileName ) )
        {
            return null;
        }
        else
        {
            return new String[]{replaceReferences( sourceFileName )};
        }
    }

    /**
     * Replace all backreferences in the to pattern with the matched groups of
     * the source.
     */
    private String replaceReferences( final String source )
        throws TaskException
    {
        final ArrayList groups = m_matcher.getGroups( source );

        m_result.setLength( 0 );
        for( int i = 0; i < m_to.length; i++ )
        {
            if( m_to[ i ] == '\\' )
            {
                if( ++i < m_to.length )
                {
                    final int value = Character.digit( m_to[ i ], 10 );
                    if( value > -1 )
                    {
                        m_result.append( (String)groups.get( value ) );
                    }
                    else
                    {
                        m_result.append( m_to[ i ] );
                    }
                }
                else
                {
                    // XXX - should throw an exception instead?
                    m_result.append( '\\' );
                }
            }
            else
            {
                m_result.append( m_to[ i ] );
            }
        }
        return m_result.toString();
    }
}
