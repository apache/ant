/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.ant.datatypes;

import org.apache.ant.AntException;
import org.apache.avalon.Component;
import org.apache.avalon.Context;
import org.apache.avalon.util.PropertyException;
import org.apache.avalon.util.PropertyUtil;

/**
 * Class representing a condition.
 *
 * @author <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class Condition
    implements Component
{
    protected String            m_condition;
    protected boolean           m_isIfCondition; 

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
    
    public boolean evaluate( final Context context )
    {
        try
        {
            final Object resolved = 
                PropertyUtil.resolveProperty( m_condition, context, false ); 

            boolean result = false;

            if( null != resolved ) 
            {
                result = ( null != context.get( resolved ) );
            }

            if( !m_isIfCondition )
            {
                result = !result;
            }

            return result;
        }
        catch( final PropertyException pe )
        {
            throw new AntException( "Error resolving " + m_condition, pe );
        }
    }
}


