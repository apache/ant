/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.antlib.core;

import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.conditions.AndCondition;
import org.apache.myrmidon.framework.conditions.Condition;

/**
 * &lt;condition&gt; task as a generalization of &lt;available&gt; and
 * &lt;uptodate&gt; <p>
 *
 * This task supports boolean logic as well as pluggable conditions to decide,
 * whether a property should be set.</p> <p>
 *
 * This task does not extend Task to take advantage of ConditionBase.</p>
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @version $Revision$
 *
 * @ant:task name="condition"
 */
public class ConditionTask
    extends AbstractTask
{
    private AndCondition m_condition = new AndCondition();
    private String m_property;
    private String m_value = "true";

    /**
     * Adds a condition.
     */
    public void add( final Condition condition )
    {
        m_condition.add( condition );
    }

    /**
     * The name of the property to set. Required.
     *
     * @param p The new Property value
     */
    public void setProperty( final String p )
    {
        m_property = p;
    }

    /**
     * The value for the property to set. Defaults to "true".
     *
     * @param v The new Value value
     */
    public void setValue( final String v )
    {
        m_value = v;
    }

    /**
     * See whether our nested condition holds and set the property.
     */
    public void execute()
        throws TaskException
    {
        if( m_property == null )
        {
            throw new TaskException( "No property was specified" );
        }

        if( m_condition.evaluate( getContext() ) )
        {
            getContext().setProperty( m_property, m_value );
        }
    }
}
