/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.framework;

import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.conditions.Condition;
import org.apache.myrmidon.framework.conditions.IsSetCondition;
import org.apache.myrmidon.framework.conditions.NotCondition;

/**
 * Basic data type for holding patterns.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 * @ant.data-type name="pattern"
 */
public class Pattern
    implements DataType
{
    private final static Resources REZ =
        ResourceManager.getPackageResources( Pattern.class );

    private String m_name;
    private Condition m_condition;

    public Pattern()
    {
    }

    public Pattern( final String name )
    {
        m_name = name;
    }

    /**
     * Retrieve value of pattern.
     *
     * @return the value of pattern
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
     * Setter method for name of pattern.
     *
     * @param name the name
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
        m_condition = new IsSetCondition( condition );
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
        m_condition = new NotCondition( new IsSetCondition( condition ) );
    }

    public String evaluateName( final TaskContext context )
    {
        try
        {
            final Condition condition = getCondition();
            final boolean result = ( condition == null || condition.evaluate( context ) );
            if( result )
            {
                return getName();
            }
        }
        catch( final TaskException te )
        {
            //ignore for the moment
        }
        return null;
    }

    public String toString()
    {
        String result = "Pattern['" + m_name + "',";
        if( null != m_condition )
        {
            result = result + m_condition;
        }
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
