/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.myrmidon.framework;

import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.myrmidon.api.TaskException;

/**
 * Basic data type for holding patterns.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 */
public class Pattern
    implements DataType
{
    private static final Resources REZ =
        ResourceManager.getPackageResources( Pattern.class );

    private String         m_value;
    private Condition      m_condition;

    /**
     * Retrieve value of pattern.
     *
     * @return the value of pattern
     */
    public String getValue()
    {
        return m_value;
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
     * Setter method for value of pattern.
     * Conforms to setter patterns
     *
     * @param value the value
     */
    public void setValue( final String value )
    {
        m_value = value;
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
        String result = "Pattern['" + m_value + "',";
        if( null != m_condition ) result = result + m_condition;
        return result + "]";
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
            final String message = REZ.getString( "pattern.ifelse-duplicate.error" );
            throw new TaskException( message );
        }
    }
}
