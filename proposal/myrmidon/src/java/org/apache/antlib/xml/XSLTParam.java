/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.xml;

import org.apache.myrmidon.api.TaskException;

public class XSLTParam
{
    private String m_name;
    private String m_expression;

    public void setExpression( String expression )
    {
        m_expression = expression;
    }

    public void setName( String name )
    {
        m_name = name;
    }

    public String getExpression()
        throws TaskException
    {
        if( m_expression == null )
        {
            throw new TaskException( "Expression attribute is missing." );
        }
        return m_expression;
    }

    public String getName()
        throws TaskException
    {
        if( m_name == null )
        {
            throw new TaskException( "Name attribute is missing." );
        }
        return m_name;
    }
}
