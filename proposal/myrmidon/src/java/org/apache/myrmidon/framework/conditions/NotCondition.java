/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.framework.conditions;

import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;

/**
 * &lt;not&gt; condition. Evaluates to true if the single condition nested into
 * it is false and vice versa.
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @version $Revision$
 *
 * @ant:type type="condition" name="not"
 */
public class NotCondition
    implements Condition
{
    private Condition m_condition;

    public NotCondition()
    {
    }

    public NotCondition( final Condition condition )
    {
        m_condition = condition;
    }

    /**
     * Sets the nested condition.
     */
    public void set( final Condition condition )
    {
        m_condition = condition;
    }

    /**
     * Evaluates the condition.
     */
    public boolean evaluate( final TaskContext context )
        throws TaskException
    {
        if( m_condition == null )
        {
            throw new TaskException( "no condition set" );
        }

        return ! m_condition.evaluate( context );
    }
}
