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
import org.apache.avalon.excalibur.i18n.ResourceManager;
import org.apache.avalon.excalibur.i18n.Resources;

/**
 * &lt;not&gt; condition. Evaluates to true if the single condition nested into
 * it is false and vice versa.
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @version $Revision$
 *
 * @ant.type type="condition" name="not"
 */
public class NotCondition
    implements Condition
{
    private final static Resources REZ =
        ResourceManager.getPackageResources( NotCondition.class );

    private Condition m_condition;

    public NotCondition()
    {
    }

    public NotCondition( final Condition condition )
    {
        m_condition = condition;
    }

    /**
     * Adds a nested condition.
     */
    public void add( final Condition condition )
        throws TaskException
    {
        if( m_condition != null )
        {
            final String message = REZ.getString( "not.too-many-conditions.error" );
            throw new TaskException( message );
        }
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
            final String message = REZ.getString( "not.no-condition.error" );
            throw new TaskException( message );
        }

        return ! m_condition.evaluate( context );
    }
}
