/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs;

import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.api.AbstractTask;
import org.apache.tools.ant.taskdefs.condition.Condition;
import org.apache.tools.ant.taskdefs.condition.ConditionBase;

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
 */
public class ConditionTask extends ConditionBase
{
    private String value = "true";

    private String property;

    /**
     * The name of the property to set. Required.
     *
     * @param p The new Property value
     * @since 1.1
     */
    public void setProperty( String p )
    {
        property = p;
    }

    /**
     * The value for the property to set. Defaults to "true".
     *
     * @param v The new Value value
     * @since 1.1
     */
    public void setValue( String v )
    {
        value = v;
    }

    /**
     * See whether our nested condition holds and set the property.
     *
     * @exception TaskException Description of Exception
     * @since 1.1
     */
    public void execute()
        throws TaskException
    {
        if( countConditions() > 1 )
        {
            throw new TaskException( "You must not nest more than one condition into <condition>" );
        }
        if( countConditions() < 1 )
        {
            throw new TaskException( "You must nest a condition into <condition>" );
        }
        Condition c = (Condition)getConditions().nextElement();
        if( c.eval() )
        {
            final String name = property;
            final Object value1 = value;
            getContext().setProperty( name, value1 );
        }
    }
}
