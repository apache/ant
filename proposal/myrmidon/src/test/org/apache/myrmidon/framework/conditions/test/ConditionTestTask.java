/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.framework.conditions.test;

import org.apache.myrmidon.api.AbstractTask;
import org.apache.myrmidon.api.TaskException;
import org.apache.myrmidon.framework.conditions.Condition;

/**
 * A simple assert task, used for testing conditions.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 *
 * @ant.task name="assert"
 */
public class ConditionTestTask
    extends AbstractTask
{
    private boolean m_expected = true;
    private Condition m_condition;

    public void setExpected( final boolean expected )
    {
        m_expected = expected;
    }

    public void add( final Condition condition )
    {
        m_condition = condition;
    }

    /**
     * Execute task.
     */
    public void execute()
        throws TaskException
    {
        final boolean result = m_condition.evaluate( getContext() );
        if( result != m_expected )
        {
            throw new TaskException( "Expected " + m_expected + ", got " + result );
        }
    }
}
