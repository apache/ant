/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.datatypes;

import org.apache.ant.AntException;

/**
 * Basic data type for holding patterns.
 * 
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class Pattern
    implements DataType
{
    protected String         m_name;
    protected Condition      m_condition;

    public String getName()
    {
        return m_name;
    }
    
    public Condition getCondition()
    {
        return m_condition;
    }
    
    public void setName( final String name )
    {
        m_name = name;
    }

    public void setIf( final String condition )
        throws AntException
    {
        verifyConditionNull();
        m_condition = new Condition( true, condition );
    }

    public void setUnless( final String condition )
        throws AntException
    {
        verifyConditionNull();
        m_condition = new Condition( false, condition );
    }

    protected void verifyConditionNull()
        throws AntException
    {
        if( null != m_condition )
        {
            throw new AntException( "Can only set one of if/else for pattern data type" );
        }
    }
}
