/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included  with this distribution in
 * the LICENSE.txt file.
 */
package org.apache.myrmidon.framework.conditions.test;

import org.apache.myrmidon.framework.conditions.Condition;
import org.apache.myrmidon.api.TaskContext;
import org.apache.myrmidon.api.TaskException;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;

/**
 * A condition used for testing.
 *
 * @author <a href="mailto:adammurdoch@apache.org">Adam Murdoch</a>
 * @version $Revision$ $Date$
 *
 * @ant.type type="condition" name="true"
 * @ant.type type="condition" name="false"
 * @ant.type type="condition" name="fail"
 */
public class TestCondition
    implements Condition, Configurable
{
    private String m_action;

    public void configure( final Configuration configuration )
        throws ConfigurationException
    {
        m_action = configuration.getName();
    }

    /**
     * Evaluates this condition.
     */
    public boolean evaluate( final TaskContext context )
        throws TaskException
    {
        if( m_action.equalsIgnoreCase( "true" ) )
        {
            return true;
        }
        else if( m_action.equalsIgnoreCase( "false" ) )
        {
            return false;
        }
        else
        {
            throw new TaskException( "Fail." );
        }
    }
}
