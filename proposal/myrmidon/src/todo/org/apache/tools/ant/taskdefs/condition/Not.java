/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.tools.ant.taskdefs.condition;

import org.apache.myrmidon.api.TaskException;

/**
 * &lt;not&gt; condition. Evaluates to true if the single condition nested into
 * it is false and vice versa.
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @version $Revision$
 */
public class Not extends ConditionBase implements Condition
{

    public boolean eval()
        throws TaskException
    {
        if( countConditions() > 1 )
        {
            throw new TaskException( "You must not nest more than one condition into <not>" );
        }
        if( countConditions() < 1 )
        {
            throw new TaskException( "You must nest a condition into <not>" );
        }
        return !( (Condition)getConditions().nextElement() ).eval();
    }

}
