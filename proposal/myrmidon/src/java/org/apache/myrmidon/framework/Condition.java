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
import org.apache.avalon.excalibur.property.PropertyException;
import org.apache.avalon.excalibur.property.PropertyUtil;
import org.apache.avalon.framework.component.Component;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.ContextException;

/**
 * Class representing a condition.
 *
 * @author <a href="mailto:peter@apache.org">Peter Donald</a>
 */
public class Condition
    implements Component
{
    private final static Resources REZ =
        ResourceManager.getPackageResources( Condition.class );

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

    public boolean evaluate( final Context context )
        throws ContextException
    {
        boolean result = false;

        try
        {
            final Object resolved =
                PropertyUtil.resolveProperty( getCondition(), context, false );

            if( null != resolved )
            {
                final Object object = context.get( resolved );
                //TODO: Do more than just check for presence????????????

                //true as object present
                result = true;
            }
        }
        catch( final ContextException ce )
        {
            result = false;
        }
        catch( final PropertyException pe )
        {
            final String message = REZ.getString( "condition.no-resolve.error", m_condition );
            throw new ContextException( message, pe );
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


