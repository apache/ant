/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.framework.conditions;

import java.util.ArrayList;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;

/**
 * &lt;or&gt; condition container. <p>
 *
 * Iterates over all conditions and returns true as soon as one evaluates to
 * true.  An empty container evaluates to true</p>
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @version $Revision$
 *
 * @ant.type type="condition" name="or"
 */
public class OrCondition
    implements Condition
{
    private final ArrayList m_conditions = new ArrayList();

    /**
     * Adds a condition.
     */
    public void add( final Condition condition )
    {
        m_conditions.add( condition );
    }

    /**
     * Evaluates the condition.
     *
     */
    public boolean evaluate( final TaskContext context )
        throws TaskException
    {
        final int count = m_conditions.size();
        for( int i = 0; i < count; i++ )
        {
            final Condition condition = (Condition)m_conditions.get( i );
            if( condition.evaluate( context ) )
            {
                return true;
            }
        }
        return (count == 0);
    }
}
