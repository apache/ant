/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.myrmidon.framework;

import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.components.model.Condition;

/**
 * Basic data type for holding patterns.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class Pattern
    implements DataType
{
    private String         m_name;
    private Condition      m_condition;

    /**
     * Retrieve name (aka value) of pattern.
     *
     * @return the name/value of pattern
     */
    public String getName()
    {
        return m_name;
    }

    /**
     * Get condition associated with pattern if any.
     *
     * @return the Condition
     */
    public Condition getCondition()
    {
        return m_condition;
    }

    /**
     * Setter method for name/value of pattern.
     * Conforms to setter patterns
     *
     * @param name the value
     */
    public void setName( final String name )
    {
        m_name = name;
    }

    /**
     * Set if clause on pattern.
     *
     * @param condition the condition
     * @exception TaskException if an error occurs
     */
    public void setIf( final String condition )
        throws TaskException
    {
        verifyConditionNull();
        m_condition = new Condition( true, condition );
    }

    /**
     * Set unless clause of pattern.
     *
     * @param condition the unless clause
     * @exception TaskException if an error occurs
     */
    public void setUnless( final String condition )
        throws TaskException
    {
        verifyConditionNull();
        m_condition = new Condition( false, condition );
    }

    public String toString()
    {
        return "Pattern['" + m_name + "'," + m_condition + "]" ;
    }

    /**
     * Utility method to make sure condition unset.
     * Made so that it is not possible for both if and unless to be set.
     *
     * @exception TaskException if an error occurs
     */
    private void verifyConditionNull()
        throws TaskException
    {
        if( null != m_condition )
        {
            throw new TaskException( "Can only set one of if/else for pattern data type" );
        }
    }
}
