/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.types;

/**
 * Individual filter component of filterset
 *
 * @author Michael McCallum
 * @created 14 March 2001
 */
public class Filter
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
    public Filter( final String token, final String value )
    {
        m_token = token;
        m_value = value;
    }

    /**
     * No argument conmstructor
     */
    public Filter()
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
     * Gets the Token attribute of the Filter object
     */
    public String getToken()
    {
        return m_token;
    }

    /**
     * Gets the Value attribute of the Filter object
     */
    public String getValue()
    {
        return m_value;
    }
}
