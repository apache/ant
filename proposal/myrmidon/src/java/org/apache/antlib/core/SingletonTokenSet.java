/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.core;

import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.filters.TokenSet;

/**
 * A single token and its value.
 *
 * @author Michael McCallum
 * @created 14 March 2001
 *
 * @ant:type type="token-set" name="token"
 */
public class SingletonTokenSet
    implements TokenSet
{
    /**
     * Token which will be replaced in the filter operation
     */
    private String m_token;

    /**
     * The value which will replace the token in the filtering operation
     */
    private String m_value;

    /**
     * Constructor for the Filter object
     *
     * @param token The token which will be replaced when filtering
     * @param value The value which will replace the token when filtering
     */
    public SingletonTokenSet( final String token, final String value )
    {
        m_token = token;
        m_value = value;
    }

    /**
     * No argument conmstructor
     */
    public SingletonTokenSet()
    {
    }

    /**
     * Sets the Token attribute of the Filter object
     */
    public void setToken( final String token )
    {
        m_token = token;
    }

    /**
     * Sets the Value attribute of the Filter object
     */
    public void setValue( final String value )
    {
        m_value = value;
    }

    /**
     * Evaluates the value for a token.
     */
    public String getValue( final String token, final TaskContext context )
        throws TaskException
    {
        if( token.equals( m_token ) )
        {
            return m_value;
        }
        return null;
    }
}
