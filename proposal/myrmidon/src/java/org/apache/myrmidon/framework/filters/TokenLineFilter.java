/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.framework.filters;

import java.util.ArrayList;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;

/**
 * A line filter that replaces tokens.  May have begintoken and endtokens defined.
 *
 * @author <A href="mailto:gholam@xtra.co.nz"> Michael McCallum </A>
 *
 * @ant.type type="line-filter" name="token-filter"
 */
public class TokenLineFilter
    implements LineFilter
{
    /**
     * The default token start string
     */
    private static final char[] DEFAULT_TOKEN_START = {'@'};

    /**
     * The default token end string
     */
    private static final char[] DEFAULT_TOKEN_END = {'@'};

    /**
     * List of ordered filters and filter files.
     */
    private ArrayList m_tokenSets = new ArrayList();

    /**
     * Adds a TokenSet to this filter.
     */
    public void add( final TokenSet tokens )
    {
        m_tokenSets.add( tokens );
    }

    /**
     * Filters a line of text.
     *
     * @param line the text to filter.
     * @param context the context to use when filtering.
     */
    public void filterLine( final StringBuffer line, final TaskContext context )
        throws TaskException
    {
        int index = 0;
        while( index < line.length() )
        {
            // Find the start of the next token
            final int startToken = indexOf( line, DEFAULT_TOKEN_START, index );
            if( startToken == -1 )
            {
                break;
            }
            final int startTokenName = startToken + DEFAULT_TOKEN_START.length;

            // Find the end of the next token
            int endTokenName = indexOf( line, DEFAULT_TOKEN_END, startTokenName );
            if( endTokenName == -1 )
            {
                break;
            }
            int endToken = endTokenName + DEFAULT_TOKEN_END.length;
            if( endTokenName == startTokenName )
            {
                // Empty token name - skip
                index = endToken;
                continue;
            }

            // Extract token name figure out the value
            final String token = line.substring( startTokenName, endTokenName );
            final String value = getTokenValue( token, context );
            if( value == null )
            {
                // Unknown token - skip
                index = endToken;
                continue;
            }

            // Replace token with its value
            line.delete( startToken, endToken );
            line.insert( startToken, value );

            index = startToken + value.length();
        }
    }

    /**
     * Returns the value of a token.
     */
    private String getTokenValue( final String token, final TaskContext context )
        throws TaskException
    {
        String value = null;
        final int count = m_tokenSets.size();
        for( int i = 0; value == null && i < count; i++ )
        {
            final TokenSet tokenSet = (TokenSet)m_tokenSets.get( i );
            value = tokenSet.getValue( token, context );
        }
        return value;
    }

    /**
     * Returns the location of a string in a stringbuffer.
     */
    private int indexOf( final StringBuffer buffer,
                         final char[] str,
                         final int index )
    {
        final int maxIndex = buffer.length() - str.length + 1;
        outer: for( int i = index; i < maxIndex; i++ )
        {
            for( int j = 0; j < str.length; j++ )
            {
                if( buffer.charAt( i + j ) != str[ j ] )
                {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }
}


