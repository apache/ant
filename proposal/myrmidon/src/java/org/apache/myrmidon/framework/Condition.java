/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.framework;

import org.apache.avalon.framework.component.Component;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;

/**
 * Class representing a condition.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 * @version $Revision$ $Date$
 */
public class Condition
    implements Component
{
    private String m_condition;
    private boolean m_isIfCondition;

    public Condition( final boolean isIfCondition, final String condition )
    {
        m_isIfCondition = isIfCondition;
        m_condition = condition;
    }

    public String getCondition()
    {
        return m_condition;
    }

    public boolean isIfCondition()
    {
        return m_isIfCondition;
    }

    public boolean evaluate( final TaskContext context )
        throws TaskException
    {
        boolean result = false;

        final Object resolved = context.resolveValue( getCondition() );
        if( null != resolved )
        {
            final Object object = context.getProperty( resolved.toString() );
            if( object != null && !object.toString().equals( "false" ) )
            {
                result = true;
            }
        }

        if( !m_isIfCondition )
        {
            result = !result;
        }

        return result;
    }

    public String toString()
    {
        if( isIfCondition() )
        {
            return "if='" + getCondition() + "'";
        }
        else
        {
            return "unless='" + getCondition() + "'";
        }
    }
}


